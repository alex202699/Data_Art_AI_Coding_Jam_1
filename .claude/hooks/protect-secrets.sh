#!/bin/sh
# PreToolUse guard — protect secrets.
# Blocks reading/writing real secret files (.env, private keys) and git-committing or
# dumping them, while allowing the shareable .env.example / .sample / .template templates.
# Reads the tool-call JSON on stdin; exit code 2 blocks the call (stderr = reason).

input=$(cat)

tool=$(printf '%s' "$input" \
  | grep -oE '"tool_name"[[:space:]]*:[[:space:]]*"[^"]*"' | head -n1 \
  | sed -E 's/.*:[[:space:]]*"([^"]*)"/\1/')

# Is a path a REAL secret file (not a committed template)?
is_secret_path() {
  base=$(basename "$1")
  case "$base" in
    .env.example|.env.sample|.env.template) return 1 ;;
    .env|.env.*)                            return 0 ;;
    *.pem|*.key|id_rsa|id_rsa.*|id_ed25519) return 0 ;;
  esac
  return 1
}

case "$tool" in
  Read|Edit|Write|MultiEdit)
    fp=$(printf '%s' "$input" \
      | grep -oE '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -n1 \
      | sed -E 's/.*:[[:space:]]*"([^"]*)"/\1/')
    if [ -n "$fp" ] && is_secret_path "$fp"; then
      echo "Blocked: '$fp' is a secret file. Don't read or write real secrets — use .env.example (the committed template) instead." >&2
      exit 2
    fi
    ;;
  Bash)
    # Inspect only the command being run — drop the "description" field so explanatory
    # text mentioning .env doesn't trigger a false block.
    cmd=$(printf '%s' "$input" | sed -E 's/"description"[[:space:]]*:[[:space:]]*"([^"\\]|\\.)*"//g')

    # Any .env* token referenced by the command (skip the safe templates).
    for tok in $(printf '%s' "$cmd" | grep -oE '\.env[.A-Za-z0-9_-]*'); do
      case "$tok" in
        .env.example|.env.sample|.env.template) continue ;;
      esac
      if printf '%s' "$cmd" | grep -qE 'git[[:space:]]+(add|commit|stash)'; then
        echo "Blocked: refusing a git add/commit/stash referencing '$tok'. Real .env files hold secrets and must never be committed (they are gitignored; share config via .env.example)." >&2
        exit 2
      fi
      if printf '%s' "$cmd" | grep -qE '(^|[^A-Za-z])(cat|less|more|head|tail|xxd|base64|strings)([^A-Za-z]|$)'; then
        echo "Blocked: refusing to print the contents of '$tok' (avoid leaking secrets into logs/context). Use .env.example." >&2
        exit 2
      fi
    done
    ;;
esac

exit 0

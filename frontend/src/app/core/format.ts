// Timestamp formatting helpers. The API sends ISO-8601 UTC strings; these render
// them for the surfaces the wireframes call for. Each falls back to the raw string
// if parsing ever fails. (Ported from the reference kanban-board frontend.)

const startOfDay = (d: Date): number =>
  new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();

/**
 * Compact date for the "Modified" columns on the Teams/Epics tables:
 * "Today, 12:40" / "Yesterday" / "Jun 20" / "Jun 20, 2024".
 */
export function formatTimestamp(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }

  const now = new Date();
  const dayDiff = Math.round((startOfDay(now) - startOfDay(date)) / 86_400_000);
  const time = date.toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
  if (dayDiff === 0) {
    return `Today, ${time}`;
  }
  if (dayDiff === 1) {
    return 'Yesterday';
  }

  const sameYear = date.getFullYear() === now.getFullYear();
  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    ...(sameYear ? {} : { year: 'numeric' }),
  });
}

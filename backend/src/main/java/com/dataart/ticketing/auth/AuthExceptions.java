package com.dataart.ticketing.auth;

/** Domain exceptions thrown by {@link AuthService}, mapped to HTTP status by the advice. */
public final class AuthExceptions {

    private AuthExceptions() {
    }

    /** 409 — email already registered. */
    public static class EmailAlreadyRegisteredException extends RuntimeException {
        public EmailAlreadyRegisteredException(String message) {
            super(message);
        }
    }

    /** 401 — bad credentials (generic, no user enumeration). */
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }

    /** 403 — valid credentials but the email is not verified. */
    public static class EmailNotVerifiedException extends RuntimeException {
        public EmailNotVerifiedException(String message) {
            super(message);
        }
    }

    /** 400 — supplied token is unknown, already used, or expired. */
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}

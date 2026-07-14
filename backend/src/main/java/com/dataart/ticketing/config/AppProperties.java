package com.dataart.ticketing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the {@code app.*} configuration tree from application.yml / environment. */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Base URL of the frontend, used to build verification / reset links. */
    private String baseUrl = "http://localhost:4200";

    private final Mail mail = new Mail();
    private final Jwt jwt = new Jwt();
    private final Verification verification = new Verification();
    private final Reset reset = new Reset();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Mail getMail() {
        return mail;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Verification getVerification() {
        return verification;
    }

    public Reset getReset() {
        return reset;
    }

    public static class Mail {
        /** From address for outgoing mail. */
        private String from = "no-reply@dataart.com";

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }

    public static class Jwt {
        /** HS256 signing secret. Must be set (>= 32 bytes) in every environment. */
        private String secret;
        /** Session lifetime in seconds (default 7 days). */
        private long ttlSeconds = 7 * 24 * 60 * 60;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class Verification {
        /** Email-verification token lifetime in hours. */
        private long tokenTtlHours = 24;

        public long getTokenTtlHours() {
            return tokenTtlHours;
        }

        public void setTokenTtlHours(long tokenTtlHours) {
            this.tokenTtlHours = tokenTtlHours;
        }
    }

    public static class Reset {
        /** Password-reset token lifetime in minutes. */
        private long tokenTtlMinutes = 60;

        public long getTokenTtlMinutes() {
            return tokenTtlMinutes;
        }

        public void setTokenTtlMinutes(long tokenTtlMinutes) {
            this.tokenTtlMinutes = tokenTtlMinutes;
        }
    }
}

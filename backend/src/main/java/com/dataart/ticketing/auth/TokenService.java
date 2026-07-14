package com.dataart.ticketing.auth;

import java.security.SecureRandom;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

/** Generates single-use, high-entropy tokens for email verification and password reset. */
@Service
public class TokenService {

    private static final int TOKEN_BYTES = 32; // -> 64 hex characters

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}

package com.dataart.ticketing.auth;

import java.util.UUID;

import com.dataart.ticketing.auth.dto.AuthDtos.EmailRequest;
import com.dataart.ticketing.auth.dto.AuthDtos.LoginRequest;
import com.dataart.ticketing.auth.dto.AuthDtos.LoginResponse;
import com.dataart.ticketing.auth.dto.AuthDtos.MessageResponse;
import com.dataart.ticketing.auth.dto.AuthDtos.ResetPasswordRequest;
import com.dataart.ticketing.auth.dto.AuthDtos.SignupRequest;
import com.dataart.ticketing.auth.dto.AuthDtos.UserResponse;
import com.dataart.ticketing.auth.dto.AuthDtos.VerifyResponse;
import com.dataart.ticketing.domain.User;
import com.dataart.ticketing.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest req) {
        authService.signup(req.email(), req.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Account created. Check your email to verify."));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.email(), req.password());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Stateless bearer tokens: logout is client-side (drop the token).
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify")
    public ResponseEntity<VerifyResponse> verify(@RequestParam String token) {
        boolean verified = authService.verify(token);
        if (verified) {
            return ResponseEntity.ok(new VerifyResponse("verified"));
        }
        return ResponseEntity.badRequest().body(new VerifyResponse("invalid"));
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody EmailRequest req) {
        authService.resendVerification(req.email());
        return new MessageResponse(
                "If that account exists and is unverified, a new email has been sent.");
    }

    @PostMapping("/request-password-reset")
    public MessageResponse requestPasswordReset(@Valid @RequestBody EmailRequest req) {
        authService.requestPasswordReset(req.email());
        return new MessageResponse("If that account exists, a password reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.token(), req.password());
        return new MessageResponse("Password updated. You can now log in.");
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return authService.currentUser(user);
    }
}

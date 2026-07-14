package com.dataart.ticketing.auth;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.dataart.ticketing.auth.AuthExceptions.EmailAlreadyRegisteredException;
import com.dataart.ticketing.auth.AuthExceptions.EmailNotVerifiedException;
import com.dataart.ticketing.auth.AuthExceptions.InvalidCredentialsException;
import com.dataart.ticketing.auth.AuthExceptions.InvalidTokenException;
import com.dataart.ticketing.auth.dto.AuthDtos.LoginResponse;
import com.dataart.ticketing.auth.dto.AuthDtos.UserResponse;
import com.dataart.ticketing.config.AppProperties;
import com.dataart.ticketing.domain.EmailVerificationToken;
import com.dataart.ticketing.domain.PasswordResetToken;
import com.dataart.ticketing.domain.User;
import com.dataart.ticketing.mail.MailService;
import com.dataart.ticketing.repository.EmailVerificationTokenRepository;
import com.dataart.ticketing.repository.PasswordResetTokenRepository;
import com.dataart.ticketing.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final MailService mailService;
    private final AppProperties props;

    public AuthService(UserRepository users,
                       EmailVerificationTokenRepository verificationTokens,
                       PasswordResetTokenRepository resetTokens,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       JwtService jwtService,
                       MailService mailService,
                       AppProperties props) {
        this.users = users;
        this.verificationTokens = verificationTokens;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.props = props;
    }

    @Transactional
    public void signup(String rawEmail, String password) {
        String email = normalize(rawEmail);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }
        User user = users.save(new User(email, passwordEncoder.encode(password)));
        issueVerificationToken(user);
    }

    @Transactional
    public LoginResponse login(String rawEmail, String password) {
        User user = users.findByEmailIgnoreCase(normalize(rawEmail))
                .orElse(null);
        // Verify against a real hash even when the user is missing, and use a generic
        // error either way, to avoid leaking which emails are registered.
        boolean matches = user != null && passwordEncoder.matches(password, user.getPasswordHash());
        if (!matches) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }
        String jwt = jwtService.issue(user.getId());
        return new LoginResponse(jwt, toUserResponse(user));
    }

    @Transactional
    public boolean verify(String token) {
        Optional<EmailVerificationToken> found = verificationTokens.findByToken(token);
        if (found.isEmpty() || !found.get().isUsable(now())) {
            return false;
        }
        EmailVerificationToken vt = found.get();
        vt.setUsedAt(now());
        User user = vt.getUser();
        user.setEmailVerified(true);
        return true;
    }

    /** Anti-enumeration: always succeeds from the caller's perspective. */
    @Transactional
    public void resendVerification(String rawEmail) {
        users.findByEmailIgnoreCase(normalize(rawEmail))
                .filter(u -> !u.isEmailVerified())
                .ifPresent(this::issueVerificationToken);
    }

    /** Anti-enumeration: always succeeds from the caller's perspective. */
    @Transactional
    public void requestPasswordReset(String rawEmail) {
        users.findByEmailIgnoreCase(normalize(rawEmail))
                .ifPresent(this::issueResetToken);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = resetTokens.findByToken(token)
                .filter(t -> t.isUsable(now()))
                .orElseThrow(() -> new InvalidTokenException("This reset link is invalid or has expired"));
        prt.setUsedAt(now());
        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    public UserResponse currentUser(User user) {
        return toUserResponse(user);
    }

    private void issueVerificationToken(User user) {
        verificationTokens.invalidateActiveTokens(user);
        String token = tokenService.generate();
        OffsetDateTime expiresAt = now().plusHours(props.getVerification().getTokenTtlHours());
        verificationTokens.save(new EmailVerificationToken(user, token, expiresAt));
        mailService.sendVerificationEmail(user.getEmail(), token);
    }

    private void issueResetToken(User user) {
        resetTokens.invalidateActiveTokens(user);
        String token = tokenService.generate();
        OffsetDateTime expiresAt = now().plusMinutes(props.getReset().getTokenTtlMinutes());
        resetTokens.save(new PasswordResetToken(user, token, expiresAt));
        mailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    private static UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId().toString(), user.getEmail());
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim();
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}

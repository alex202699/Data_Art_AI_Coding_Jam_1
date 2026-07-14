package com.dataart.ticketing.mail;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.dataart.ticketing.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends transactional email through the configured SMTP service.
 *
 * <p>Send failures are caught and logged rather than propagated, so a transient SMTP problem
 * never fails sign-up. The link is always logged at INFO to make local testing easy (e.g. when
 * pointing at Mailpit).
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final AppProperties props;

    public MailService(JavaMailSender mailSender, AppProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    public void sendVerificationEmail(String to, String token) {
        String link = props.getBaseUrl() + "/verify?token=" + encode(token);
        String body = "Welcome to Ticket Tracker.\n\n"
                + "Verify your email within 24 hours to activate your account:\n" + link + "\n";
        send(to, "Verify your email — Ticket Tracker", body, link);
    }

    public void sendPasswordResetEmail(String to, String token) {
        String link = props.getBaseUrl() + "/reset-password?token=" + encode(token);
        String body = "We received a request to reset your Ticket Tracker password.\n\n"
                + "Reset it within 1 hour using this link:\n" + link + "\n\n"
                + "If you didn't request this, you can ignore this email.\n";
        send(to, "Reset your password — Ticket Tracker", body, link);
    }

    private void send(String to, String subject, String body, String link) {
        // The link contains a single-use token — keep it out of INFO logs. Enable DEBUG
        // for this logger locally (e.g. when not using the Mailpit inbox) to see it.
        log.debug("Email link for {}: {}", to, link);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(props.getMail().getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent '{}' email to {}", subject, to);
        } catch (MailException e) {
            log.warn("Failed to send '{}' email to {}: {}", subject, to, e.getMessage());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

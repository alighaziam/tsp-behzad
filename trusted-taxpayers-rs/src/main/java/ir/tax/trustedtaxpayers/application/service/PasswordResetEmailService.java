package ir.tax.trustedtaxpayers.application.service;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Profile("prod")
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final Environment environment;

    public PasswordResetEmailService(JavaMailSender mailSender, Environment environment) {
        this.mailSender = mailSender;
        this.environment = environment;
    }

    public void send(String recipient, String token) {
        if (recipient == null || !recipient.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("A valid email recipient is required.");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("A reset token is required.");
        }
        String baseUrl = environment.getRequiredProperty("app.public-base-url");
        URI uri = URI.create(baseUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalStateException("Password reset public URL must use HTTPS without embedded credentials.");
        }

        String resetUrl = baseUrl.replaceAll("/+$", "") + "/reset-password?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(environment.getRequiredProperty("spring.mail.username"));
        message.setTo(recipient);
        message.setSubject("Password reset request");
        message.setText("A password reset was requested for your account. Use this single-use link within 30 minutes:\n\n"
                + resetUrl + "\n\nIf you did not request this, ignore this message.");
        mailSender.send(message);
    }
}

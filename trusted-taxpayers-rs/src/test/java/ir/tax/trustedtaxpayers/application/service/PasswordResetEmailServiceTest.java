package ir.tax.trustedtaxpayers.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentCaptor.forClass;

class PasswordResetEmailServiceTest {

    @Test
    void sendsSingleUseResetLinkOnlyToValidatedEmailAddress() {
        JavaMailSender sender = mock(JavaMailSender.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.public-base-url", "https://app.example.test")
                .withProperty("spring.mail.username", "noreply@example.test");
        PasswordResetEmailService service = new PasswordResetEmailService(sender, environment);

        service.send("user@example.test", "token-123");

        var captor = forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().getText()
                .contains("https://app.example.test/reset-password?token=token-123"));
        org.junit.jupiter.api.Assertions.assertEquals("user@example.test", captor.getValue().getTo()[0]);
    }

    @Test
    void rejectsInvalidRecipientBeforeSending() {
        JavaMailSender sender = mock(JavaMailSender.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.public-base-url", "https://app.example.test")
                .withProperty("spring.mail.username", "noreply@example.test");
        PasswordResetEmailService service = new PasswordResetEmailService(sender, environment);
        assertThrows(IllegalArgumentException.class, () -> service.send("not-an-email", "token"));
    }

    @Test
    void rejectsNonHttpsPublicResetUrl() {
        JavaMailSender sender = mock(JavaMailSender.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.public-base-url", "http://app.example.test")
                .withProperty("spring.mail.username", "noreply@example.test");
        PasswordResetEmailService service = new PasswordResetEmailService(sender, environment);
        assertThrows(IllegalStateException.class, () -> service.send("user@example.test", "token"));
    }
}

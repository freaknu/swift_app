package com.swiftapp_backend.noticationservice.notification.infrastructure.mail;

import com.swiftapp_backend.noticationservice.notification.application.port.EmailSenderPort;
import com.swiftapp_backend.noticationservice.notification.domain.model.EmailNotification;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JavaMailEmailSender implements EmailSenderPort {
    private final JavaMailSender javaMailSender;

    @Override
    public void send(EmailNotification notification) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true,"UTF-8");
            helper.setTo(notification.recipient());
            helper.setSubject(notification.subject());
            helper.setText(notification.htmlContent(), true);
            javaMailSender.send(message);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to send email notification", exception);
        }
    }
}

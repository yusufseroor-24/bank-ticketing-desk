package com.ga.bankdesk.service;


import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class EmailDispatcher {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@bankdesk.local}")
    private String from;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void send(EmailMessage msg){
        try{
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            helper.setFrom(from, "BankDesk");
            helper.setTo(msg.to());
            helper.setSubject(msg.subject());
            helper.setText(msg.html(), true);
            mailSender.send(mime);
            log.info("Email '{}' sent to {}", msg.subject(), msg.to());
        } catch (Exception e){
            log.error("Failed to send email '{}' to {}: {]", msg.subject(), msg.to(), e.getMessage());
        }
    }
}

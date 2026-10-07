package com.ga.bankdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String toEmail, String token){
        String link = "http://localhost:8080/api/auth/verify-email?token=" + token;

        SimpleMailMessage emailMessage = new SimpleMailMessage();
        emailMessage.setTo(toEmail);
        emailMessage.setSubject("Verify your BankDesk email account");
        emailMessage.setText("Click the link provided to verify your email: " + link);

        mailSender.send(emailMessage);
        log.info("Verification email sent to {}", toEmail);
    }

    public void sendPasswordResetEmail(String toEmail, String token){
        //creates a link with a token
        String link = "http://localhost:8080/api/auth/verify-email?token=" + token;
        SimpleMailMessage emailMessage = new SimpleMailMessage();
        emailMessage.setTo(toEmail);
        emailMessage.setSubject("Reset your BankDesk account password");
        emailMessage.setText("Click the link provided to reset your password: " + link);

        mailSender.send(emailMessage);
        log.info("Password reset email sent to {}", toEmail);

    }
}

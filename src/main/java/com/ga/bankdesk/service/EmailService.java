package com.ga.bankdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds HTML emails
 * Publishes an EmailMessage event for EmailDispatcher
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {

    private final ApplicationEventPublisher publisher;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @Value("${app.frontend-base-url:http://localhost:8080}")
    private String baseUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        String link = baseUrl + "/api/auth/verify-email?token=" + token;
        publish(toEmail, "Verify your BankDesk email",
                "Verify your email",
                "Welcome to BankDesk! Confirm your email address to activate your account.",
                "Verify email", link,
                "If you didn't create an account, you can ignore this email.");
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = baseUrl + "/?resetToken=" + token;   // reset page comes in the frontend step
        publish(toEmail, "Reset your BankDesk password",
                "Reset your password",
                "We received a request to reset your password. This link can be used once.",
                "Reset password", link,
                "If you didn't ask for this, ignore this email. Your password won't change.");
    }

    public void sendTicketCreatedEmail(String toEmail, Long ticketId, String title, String category) {
        publish(toEmail, "Ticket #" + ticketId + " received",
                "We received your ticket",
                "Ticket #" + ticketId + " (" + esc(title) + ") was created in the " + esc(category)
                        + " category. An agent will review it soon.",
                null, null,
                "You'll get another email when its status changes.");
    }

    public void sendTicketStatusChangedEmail(String toEmail, Long ticketId, String title, String newStatus) {
        publish(toEmail, "Ticket #" + ticketId + " is now " + newStatus,
                "Your ticket was updated",
                "Ticket #" + ticketId + " (" + esc(title) + ") is now <b>" + esc(newStatus) + "</b>.",
                null, null,
                "Log in to BankDesk to see the details.");
    }

    private void publish(String to, String subject, String heading, String intro,
                         String buttonText, String link, String footnote) {
        publisher.publishEvent(new EmailMessage(to, subject, layout(heading, intro, buttonText, link, footnote)));
    }

    private String esc(String s) {
        return HtmlUtils.htmlEscape(s);
    }

    String layout(String heading, String intro, String buttonText, String link, String footnote) {
        String action = (link == null) ? "" : """
                <a href="%LINK%" style="background:#0f3d6e;color:#ffffff;text-decoration:none;
                   padding:12px 28px;border-radius:6px;display:inline-block;font-weight:bold;">%BUTTON%</a>
                <p style="font-size:12px;color:#6b7280;margin:24px 0 0 0;">
                   Button not working? Copy this link:<br>%LINK%</p>
                """.replace("%LINK%", link).replace("%BUTTON%", buttonText);

        return """
            <div style="background:#f4f6f8;padding:32px 0;font-family:Arial,Helvetica,sans-serif;">
              <table align="center" width="560" cellpadding="0" cellspacing="0"
                     style="background:#ffffff;border-radius:8px;overflow:hidden;">
                <tr><td style="background:#0f3d6e;color:#ffffff;padding:20px 32px;font-size:20px;font-weight:bold;">
                  BankDesk</td></tr>
                <tr><td style="padding:32px;color:#1f2933;">
                  <h2 style="margin:0 0 12px 0;">{{heading}}</h2>
                  <p style="line-height:1.5;margin:0 0 24px 0;">{{intro}}</p>
                  {{action}}
                </td></tr>
                <tr><td style="background:#f4f6f8;padding:16px 32px;font-size:12px;color:#6b7280;">
                  {{footnote}}</td></tr>
              </table>
            </div>
            """
                .replace("{{heading}}", heading)
                .replace("{{intro}}", intro)
                .replace("{{action}}", action)
                .replace("{{footnote}}", footnote);
    }

    public void sendSlaAlertEmail(String toEmail, Long ticketId, String title, String headline, LocalDateTime dueAt){
        publish(toEmail, "SLA Alert: ticket #" + ticketId,
                headline,
                "Ticket #" + ticketId + " (" + esc(title) + ") has a resolution deadline of <b>" + dueAt.format(FORMATTER) + "</b>.",
                null, null,
                "Open BankDesk to act on the ticket");
    }

    public void sendSlaReportEmail(String toEmail, String period, Integer totalTickets, Double compliance) {
        String percent = (compliance == null) ? "n/a" : String.format(java.util.Locale.ROOT, "%.1f%%", compliance);
        publish(toEmail, "Monthly SLA report " + period,
                "Monthly SLA report is ready",
                "The report for <b>" + esc(period) + "</b> covers " + totalTickets
                        + " tickets. Resolution SLA compliance: <b>" + percent + "</b>.",
                null, null,
                "Download the full CSV from the admin area.");
    }
}

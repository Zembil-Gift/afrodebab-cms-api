package com.afrodebab.cms.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;

@Service
public class SendGridEmailService {
    private static final String LOGO_URL = "https://www.afrodebab.com/afrodebab-logo.png";

    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public SendGridEmailService(@Value("${app.sendgrid.apiKey:}") String apiKey,
                                @Value("${app.sendgrid.fromEmail:}") String fromEmail,
                                @Value("${app.sendgrid.fromName:AfroDebab CMS}") String fromName) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    public void sendEmployeePasswordEmail(String recipientEmail, String recipientName, String generatedPassword) {
        String plainBody = "Hello " + recipientName + ",\n\n"
                + "Your employee account has been created.\n"
                + "Temporary password: " + generatedPassword + "\n\n"
                + "Please log in and change your password immediately.";
        String htmlBody = buildEmailTemplate(
                "Your employee account is ready",
                "Hello " + escapeHtml(recipientName) + ",",
                "<p style=\"margin:0 0 14px;\">Your employee account has been created.</p>"
                        + "<p style=\"margin:0 0 14px;\"><strong>Temporary password:</strong> "
                        + escapeHtml(generatedPassword) + "</p>"
                        + "<p style=\"margin:0;\">Please log in and change your password immediately to keep your account secure.</p>"
        );

        sendEmail(recipientEmail, "Your AfroDebab employee account", plainBody, htmlBody, "Failed to send employee password email");
    }

    public void sendAdminPayrollReminderEmail(String recipientEmail, String recipientName, int dueCount) {
        String plainBody = "Hello " + recipientName + ",\n\n"
                + "Payroll reminder: " + dueCount + " employee payment(s) are due soon or overdue.\n"
                + "Please review /admin/payments/due and mark payments as paid after transfer.";
        String htmlBody = buildEmailTemplate(
                "Payroll reminder",
                "Hello " + escapeHtml(recipientName) + ",",
                "<p style=\"margin:0 0 14px;\">You have <strong>" + dueCount
                        + " employee payment(s)</strong> that are due soon or overdue.</p>"
                        + "<p style=\"margin:0;\">Please review <strong>/admin/payments/due</strong> and mark payments as paid after transfer.</p>"
        );

        sendEmail(recipientEmail, "AfroDebab payroll reminder", plainBody, htmlBody, "Failed to send payroll reminder email");
    }

    public void sendEmployeePaymentReceivedEmail(String recipientEmail,
                                                 String recipientName,
                                                 Long paidAmountMinor,
                                                 String transactionReference,
                                                 LocalDate dueDate) {
        String plainBody = "Hello " + recipientName + ",\n\n"
                + "Your salary payment has been marked as paid.\n"
                + "Amount (ETB): " + paidAmountMinor + "\n"
                + "Transaction reference: " + transactionReference + "\n"
                + "Salary due date: " + dueDate + "\n\n"
                + "You can review your payment details on your profile.";
        String htmlBody = buildEmailTemplate(
                "Salary payment received",
                "Hello " + escapeHtml(recipientName) + ",",
                "<p style=\"margin:0 0 14px;\">Great news - your salary payment has been marked as paid.</p>"
                        + "<p style=\"margin:0 0 6px;\"><strong>Amount (ETB):</strong> " + escapeHtml(String.valueOf(paidAmountMinor)) + "</p>"
                        + "<p style=\"margin:0 0 6px;\"><strong>Transaction reference:</strong> " + escapeHtml(transactionReference) + "</p>"
                        + "<p style=\"margin:0;\"><strong>Salary due date:</strong> " + escapeHtml(String.valueOf(dueDate)) + "</p>"
        );

        sendEmail(recipientEmail, "AfroDebab salary payment received", plainBody, htmlBody, "Failed to send employee payment email");
    }

    public void sendCandidateSelectedForInterviewEmail(String recipientEmail, String recipientName, String jobTitle) {
        String plainBody = "Hello " + recipientName + ",\n\n"
                + "You were selected for an interview for the role: " + jobTitle + ".\n"
                + "Our team will contact you with next steps.";
        String htmlBody = buildEmailTemplate(
                "You have been selected for an interview!",
                "Hello " + escapeHtml(recipientName) + ",",
                "<p style=\"margin:0 0 14px;\">Congratulations! You were selected for an interview for the role of <strong>"
                        + escapeHtml(jobTitle) + "</strong>.</p>"
                        + "<p style=\"margin:0;\">Our team will contact you soon with the next steps.</p>"
        );
        sendEmail(recipientEmail, "Interview selection - AfroDebab", plainBody, htmlBody, "Failed to send interview selection email");
    }

    public void sendCandidateRejectedBeforeInterviewEmail(String recipientEmail, String recipientName, String jobTitle) {
        String plainBody = "Hello " + recipientName + ",\n\n"
                + "Thank you for applying for " + jobTitle + ".\n"
                + "We appreciate your interest, but we will not proceed with your application at this stage.";
        String htmlBody = buildEmailTemplate(
                "Application update",
                "Hello " + escapeHtml(recipientName) + ",",
                "<p style=\"margin:0 0 14px;\">Thank you for applying for <strong>" + escapeHtml(jobTitle) + "</strong>.</p>"
                        + "<p style=\"margin:0;\">We appreciate your interest, but we will not proceed with your application at this stage.</p>"
        );
        sendEmail(recipientEmail, "Application update - AfroDebab", plainBody, htmlBody, "Failed to send pre-interview rejection email");
    }

    public void sendCandidateHiredEmail(String recipientEmail, String recipientName, String jobTitle) {
        String plainBody = "Hello " + recipientName + ",\n\n"
                + "Congratulations. You have been selected for the role: " + jobTitle + ".\n"
                + "Welcome to AfroDebab.";
        String htmlBody = buildEmailTemplate(
                "Congratulations and welcome!",
                "Hello " + escapeHtml(recipientName) + ",",
                "<p style=\"margin:0 0 14px;\">Congratulations! You have been selected for the role of <strong>"
                        + escapeHtml(jobTitle) + "</strong>.</p>"
                        + "<p style=\"margin:0;\">Welcome to AfroDebab - we are excited to have you with us.</p>"
        );
        sendEmail(recipientEmail, "Offer update - AfroDebab", plainBody, htmlBody, "Failed to send hired email");
    }

    public void sendCandidateRejectedPostInterviewEmail(String recipientEmail, String recipientName, String jobTitle) {
        String plainBody = "Hello " + recipientName + ",\n\n"
                + "Thank you for interviewing for " + jobTitle + ".\n"
                + "After review, we will not proceed further for this position.";
        String htmlBody = buildEmailTemplate(
                "Interview update",
                "Hello " + escapeHtml(recipientName) + ",",
                "<p style=\"margin:0 0 14px;\">Thank you for taking the time to interview for <strong>"
                        + escapeHtml(jobTitle) + "</strong>.</p>"
                        + "<p style=\"margin:0;\">After careful review, we will not proceed further for this position.</p>"
        );
        sendEmail(recipientEmail, "Interview result - AfroDebab", plainBody, htmlBody, "Failed to send post-interview rejection email");
    }

    private void sendEmail(String recipientEmail,
                           String subject,
                           String plainBody,
                           String htmlBody,
                           String errorMessage) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("SENDGRID_API_KEY is not configured");
        if (fromEmail == null || fromEmail.isBlank()) throw new IllegalStateException("SENDGRID_FROM_EMAIL is not configured");

        Mail mail = new Mail();
        mail.setFrom(new Email(fromEmail, fromName));
        mail.setSubject(subject);

        Personalization personalization = new Personalization();
        personalization.addTo(new Email(recipientEmail));
        mail.addPersonalization(personalization);
        mail.addContent(new Content("text/plain", plainBody));
        mail.addContent(new Content("text/html", htmlBody));

        SendGrid sg = new SendGrid(apiKey);
        try {
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            int status = response.getStatusCode();
            if (status < 200 || status >= 300) {
                throw new RuntimeException("SendGrid email failed with status " + status);
            }
        } catch (IOException ex) {
            throw new RuntimeException(errorMessage, ex);
        }
    }

    private String buildEmailTemplate(String title, String greeting, String bodyHtml) {
        return "<!doctype html>"
                + "<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>"
                + "<body style=\"margin:0;padding:0;background:#f4f6fb;font-family:Arial,sans-serif;color:#1f2937;\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding:24px 12px;background:#f4f6fb;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:620px;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e5e7eb;\">"
                + "<tr><td style=\"background:linear-gradient(135deg,#ff7a18 0%,#af002d 100%);padding:24px 24px 20px;text-align:center;\">"
                + "<img src=\"" + LOGO_URL + "\" alt=\"AfroDebab\" style=\"max-width:190px;width:100%;height:auto;display:inline-block;\">"
                + "<p style=\"margin:14px 0 0;color:#ffffff;font-size:15px;letter-spacing:0.3px;\">AfroDebab</p>"
                + "</td></tr>"
                + "<tr><td style=\"padding:28px 26px 18px;\">"
                + "<h1 style=\"margin:0 0 14px;font-size:24px;line-height:1.25;color:#111827;\">" + escapeHtml(title) + "</h1>"
                + "<p style=\"margin:0 0 14px;font-size:16px;line-height:1.6;\">" + greeting + "</p>"
                + "<div style=\"font-size:15px;line-height:1.7;color:#374151;\">" + bodyHtml + "</div>"
                + "</td></tr>"
                + "<tr><td style=\"padding:18px 26px 26px;border-top:1px solid #e5e7eb;\">"
                + "<p style=\"margin:0;font-size:13px;line-height:1.6;color:#6b7280;\">Thanks for choosing AfroDebab.</p>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

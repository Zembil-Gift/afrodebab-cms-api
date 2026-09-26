package com.afrodebab.cms.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** SendGrid transport only. Email content is rendered by {@link EmailTemplateService}. */
@Service
public class SendGridEmailService {
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

    public void send(String recipientEmail, String subject, String plainBody, String htmlBody) {
        send(recipientEmail, subject, plainBody, htmlBody, null);
    }

    /** {@code calendarInvite} (iCalendar text) is attached as invite.ics when present. */
    public void send(String recipientEmail, String subject, String plainBody, String htmlBody, String calendarInvite) {
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
        if (calendarInvite != null) {
            Attachments invite = new Attachments();
            invite.setContent(Base64.getEncoder().encodeToString(calendarInvite.getBytes(StandardCharsets.UTF_8)));
            invite.setType("text/calendar");
            invite.setFilename("invite.ics");
            invite.setDisposition("attachment");
            mail.addAttachments(invite);
        }

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
            throw new RuntimeException("Failed to send email: " + subject, ex);
        }
    }
}

package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.EmailPreviewResponse;
import com.afrodebab.cms.dto.EmailTemplateAdminResponse;
import com.afrodebab.cms.dto.EmailTemplateUpdateRequest;
import com.afrodebab.cms.dto.EmailTemplatesAdminResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.EmailNotification.NotificationType;
import com.afrodebab.cms.jpa.entity.EmailTemplate;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.entity.Organization;
import com.afrodebab.cms.jpa.repository.EmailTemplateRepository;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.jpa.repository.OrganizationRepository;
import com.afrodebab.cms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Owns every outgoing email's content: the default subject/heading/message per notification
 * type, each organization's overrides and email logo, and the shared green-on-black layout.
 * Previews, test sends and real sends all render through {@link #render}, so what a manager
 * previews is exactly what recipients get.
 */
@Service
public class EmailTemplateService {
    private static final String DEFAULT_LOGO_URL = "https://www.afrodebab.com/afrodebab-logo.png";
    private static final String DEFAULT_ORG_NAME = "AfroDebab";
    private static final String PASSWORD = "password";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private final EmailTemplateRepository templateRepo;
    private final OrganizationRepository organizationRepo;
    private final ManagerRepository managerRepo;
    private final SendGridEmailService sendGridEmailService;
    private final CloudflareR2Service cloudflareR2Service;

    public EmailTemplateService(EmailTemplateRepository templateRepo,
                                OrganizationRepository organizationRepo,
                                ManagerRepository managerRepo,
                                SendGridEmailService sendGridEmailService,
                                CloudflareR2Service cloudflareR2Service) {
        this.templateRepo = templateRepo;
        this.organizationRepo = organizationRepo;
        this.managerRepo = managerRepo;
        this.sendGridEmailService = sendGridEmailService;
        this.cloudflareR2Service = cloudflareR2Service;
    }

    public record Rendered(String subject, String html, String text) {}

    // ---------------------------------------------------------------- sending

    /** Renders with the org's saved overrides and branding (orgId null = platform defaults) and sends. */
    public void send(NotificationType type, String recipientEmail, Map<String, String> vars, Long orgId) {
        Rendered rendered = render(type, vars, orgId, savedOverride(type, orgId));
        sendGridEmailService.send(recipientEmail, rendered.subject(), rendered.text(), rendered.html());
    }

    public String subject(NotificationType type, Map<String, String> vars, Long orgId) {
        return render(type, vars, orgId, savedOverride(type, orgId)).subject();
    }

    /** Public preview tool: every type with sample data and default content. */
    public List<String> sampleCases() {
        return Arrays.stream(NotificationType.values()).map(Enum::name).toList();
    }

    public void sendSample(String type, String recipientEmail) {
        NotificationType parsed = parseType(type);
        send(parsed, recipientEmail, sampleVars(parsed, recipientEmail, null), null);
    }

    // ---------------------------------------------------------------- email builder (manager)

    @Transactional(readOnly = true)
    public EmailTemplatesAdminResponse list() {
        Long orgId = TenantContext.get();
        Organization org = organizationRepo.findById(orgId).orElseThrow(() -> new NotFoundException("Organization not found"));
        Map<NotificationType, EmailTemplate> saved = templateRepo.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(EmailTemplate::getType, t -> t));
        List<EmailTemplateAdminResponse> templates = DEFINITIONS.entrySet().stream()
                .filter(e -> e.getValue().customizable())
                .map(e -> toResponse(e.getKey(), saved.get(e.getKey())))
                .toList();
        return new EmailTemplatesAdminResponse(org.getEmailLogoUrl(), branding(orgId).logoUrl(), templates);
    }

    @Transactional
    public EmailTemplateAdminResponse update(String type, EmailTemplateUpdateRequest req) {
        NotificationType parsed = parseCustomizableType(type);
        Long orgId = TenantContext.get();
        EmailTemplate template = templateRepo.findByOrganizationIdAndType(orgId, parsed).orElseGet(EmailTemplate::new);
        template.setType(parsed);
        template.setSubject(blankToNull(req.subject()));
        template.setHeading(blankToNull(req.heading()));
        template.setMessage(blankToNull(req.message()));
        return toResponse(parsed, templateRepo.save(template));
    }

    @Transactional
    public EmailTemplateAdminResponse reset(String type) {
        NotificationType parsed = parseCustomizableType(type);
        templateRepo.findByOrganizationIdAndType(TenantContext.get(), parsed).ifPresent(templateRepo::delete);
        return toResponse(parsed, null);
    }

    /** Renders an unsaved draft with sample data so the builder can show it before saving. */
    @Transactional(readOnly = true)
    public EmailPreviewResponse preview(String type, EmailTemplateUpdateRequest draft) {
        NotificationType parsed = parseCustomizableType(type);
        Rendered rendered = render(parsed, DEFINITIONS.get(parsed).sample(), TenantContext.get(), draft);
        return new EmailPreviewResponse(rendered.subject(), rendered.html(), rendered.text());
    }

    /** Sends the draft, filled with sample data, to the logged-in manager's own inbox. */
    @Transactional(readOnly = true)
    public Map<String, String> sendTest(String type, EmailTemplateUpdateRequest draft, String viewerEmail) {
        NotificationType parsed = parseCustomizableType(type);
        Long orgId = TenantContext.get();
        Rendered rendered = render(parsed, sampleVars(parsed, viewerEmail, viewerName(viewerEmail)), orgId, draft);
        sendGridEmailService.send(viewerEmail, "[Test] " + rendered.subject(), rendered.text(), rendered.html());
        return Map.of("sentTo", viewerEmail, "type", parsed.name());
    }

    @Transactional
    public EmailTemplatesAdminResponse uploadLogo(MultipartFile file) {
        Long orgId = TenantContext.get();
        Organization org = organizationRepo.findById(orgId).orElseThrow(() -> new NotFoundException("Organization not found"));
        org.setEmailLogoUrl(cloudflareR2Service.uploadOrgEmailLogo(orgId, file));
        organizationRepo.save(org);
        return list();
    }

    @Transactional
    public EmailTemplatesAdminResponse clearLogo() {
        Organization org = organizationRepo.findById(TenantContext.get())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        org.setEmailLogoUrl(null);
        organizationRepo.save(org);
        return list();
    }

    // ---------------------------------------------------------------- rendering

    private Rendered render(NotificationType type, Map<String, String> vars, Long orgId, EmailTemplateUpdateRequest override) {
        Definition def = DEFINITIONS.get(type);
        Branding branding = branding(orgId);
        Map<String, String> values = new HashMap<>(vars);
        values.putIfAbsent("organization", branding.name());

        String subject = fill(pick(override == null ? null : override.subject(), def.subject()), values);
        String heading = fill(pick(override == null ? null : override.heading(), def.heading()), values);
        String message = fill(pick(override == null ? null : override.message(), def.message()), values);
        String greeting = greeting(values.get("name"));

        String html = layout(branding, heading, greeting, paragraphsHtml(message) + detailsHtml(def, values));
        String text = greeting + "\n\n" + message + detailsText(def, values) + "\n\n- " + branding.name();
        return new Rendered(subject, html, text);
    }

    private EmailTemplateUpdateRequest savedOverride(NotificationType type, Long orgId) {
        if (orgId == null || !DEFINITIONS.get(type).customizable()) return null;
        return templateRepo.findByOrganizationIdAndType(orgId, type)
                .map(t -> new EmailTemplateUpdateRequest(t.getSubject(), t.getHeading(), t.getMessage()))
                .orElse(null);
    }

    private Branding branding(Long orgId) {
        Organization org = orgId == null ? null : organizationRepo.findById(orgId).orElse(null);
        if (org == null) return new Branding(DEFAULT_ORG_NAME, DEFAULT_LOGO_URL);
        return new Branding(org.getName(), pick(org.getEmailLogoUrl(), pick(org.getLogoUrl(), DEFAULT_LOGO_URL)));
    }

    // The password is never exposed as a placeholder: it only ever appears in the credentials box.
    private static String fill(String template, Map<String, String> values) {
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String value = PASSWORD.equals(key) ? null : values.get(key);
            m.appendReplacement(out, Matcher.quoteReplacement(value != null ? value : m.group()));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String greeting(String name) {
        return name == null || name.isBlank() ? "Hello," : "Hello " + name.trim() + ",";
    }

    private static String paragraphsHtml(String message) {
        return Arrays.stream(message.trim().split("\\n\\s*\\n"))
                .map(p -> "<p style=\"margin:0 0 14px;\">" + escapeHtml(p.trim()).replace("\n", "<br>") + "</p>")
                .collect(Collectors.joining());
    }

    private static String detailsHtml(Definition def, Map<String, String> values) {
        if (def.credentials()) {
            return "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin:4px 0 18px;background:#0a0c0b;border:1px solid #232826;border-left:3px solid #34d399;border-radius:10px;\">"
                    + "<tr><td style=\"padding:16px 18px;font-size:14px;line-height:1.7;color:#e8ede9;\">"
                    + "<span style=\"color:#9aa8a0;\">Email</span><br>"
                    + "<strong style=\"color:#ffffff;\">" + escapeHtml(values.get("email")) + "</strong><br>"
                    + "<span style=\"color:#9aa8a0;\">Temporary password</span><br>"
                    + "<strong style=\"color:#34d399;font-family:'Courier New',monospace;font-size:16px;letter-spacing:0.5px;\">"
                    + escapeHtml(values.get(PASSWORD)) + "</strong>"
                    + "</td></tr></table>";
        }
        String rows = def.rows().stream()
                .filter(r -> hasText(values.get(r.key())))
                .map(r -> "<tr><td style=\"padding:6px 0;color:#9aa8a0;font-size:14px;width:45%;vertical-align:top;\">" + escapeHtml(r.label())
                        + "</td><td style=\"padding:6px 0;color:#ffffff;font-size:14px;font-weight:bold;\">" + escapeHtml(values.get(r.key())) + "</td></tr>")
                .collect(Collectors.joining());
        if (rows.isEmpty()) return "";
        return "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin:4px 0 18px;background:#0a0c0b;border:1px solid #232826;border-left:3px solid #34d399;border-radius:10px;\">"
                + "<tr><td style=\"padding:10px 18px;\"><table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">"
                + rows + "</table></td></tr></table>";
    }

    private static String detailsText(Definition def, Map<String, String> values) {
        if (def.credentials()) {
            return "\n\nEmail: " + values.get("email") + "\nTemporary password: " + values.get(PASSWORD);
        }
        String rows = def.rows().stream()
                .filter(r -> hasText(values.get(r.key())))
                .map(r -> r.label() + ": " + values.get(r.key()))
                .collect(Collectors.joining("\n"));
        return rows.isEmpty() ? "" : "\n\n" + rows;
    }

    // Solid colors only (no gradients/CSS vars): Outlook and Gmail strip them. Palette mirrors frontend/app/globals.css.
    private static String layout(Branding branding, String heading, String greeting, String bodyHtml) {
        return "<!doctype html>"
                + "<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<meta name=\"color-scheme\" content=\"dark\"><meta name=\"supported-color-schemes\" content=\"dark\"></head>"
                + "<body style=\"margin:0;padding:0;background:#0a0c0b;font-family:Arial,Helvetica,sans-serif;color:#e8ede9;\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" bgcolor=\"#0a0c0b\" style=\"padding:24px 12px;background:#0a0c0b;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" bgcolor=\"#121614\" style=\"max-width:620px;background:#121614;border-radius:14px;overflow:hidden;border:1px solid #232826;\">"
                + "<tr><td bgcolor=\"#0f1412\" style=\"background:#0f1412;padding:24px 24px 20px;text-align:center;border-bottom:3px solid #34d399;\">"
                + "<img src=\"" + escapeHtml(branding.logoUrl()) + "\" alt=\"" + escapeHtml(branding.name()) + "\" style=\"max-width:190px;max-height:80px;height:auto;display:inline-block;\">"
                + "</td></tr>"
                + "<tr><td style=\"padding:28px 26px 18px;\">"
                + "<h1 style=\"margin:0 0 14px;font-size:24px;line-height:1.25;color:#34d399;\">" + escapeHtml(heading) + "</h1>"
                + "<p style=\"margin:0 0 14px;font-size:16px;line-height:1.6;color:#ffffff;\">" + escapeHtml(greeting) + "</p>"
                + "<div style=\"font-size:15px;line-height:1.7;color:#cdd6d1;\">" + bodyHtml + "</div>"
                + "</td></tr>"
                + "<tr><td style=\"padding:18px 26px 26px;border-top:1px solid #232826;\">"
                + "<p style=\"margin:0;font-size:13px;line-height:1.6;color:#9aa8a0;\">Sent by <span style=\"color:#34d399;\">"
                + escapeHtml(branding.name()) + "</span></p>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ---------------------------------------------------------------- helpers

    private EmailTemplateAdminResponse toResponse(NotificationType type, EmailTemplate saved) {
        Definition def = DEFINITIONS.get(type);
        List<String> placeholders = Stream.concat(def.sample().keySet().stream(), Stream.of("organization"))
                .filter(k -> !PASSWORD.equals(k))
                .distinct()
                .toList();
        return new EmailTemplateAdminResponse(
                type.name(),
                def.audience(),
                saved == null ? null : saved.getSubject(),
                saved == null ? null : saved.getHeading(),
                saved == null ? null : saved.getMessage(),
                saved != null,
                def.subject(),
                def.heading(),
                def.message(),
                placeholders
        );
    }

    /** Sample data for previews and tests, addressed to whoever will look at it. */
    private static Map<String, String> sampleVars(NotificationType type, String recipientEmail, String recipientName) {
        Map<String, String> vars = new HashMap<>(DEFINITIONS.get(type).sample());
        vars.put("email", recipientEmail);
        if (hasText(recipientName)) vars.put("name", recipientName);
        return vars;
    }

    private String viewerName(String email) {
        return managerRepo.findByEmailIgnoreCase(email).map(Manager::getName).orElse(null);
    }

    private static NotificationType parseType(String type) {
        try {
            return NotificationType.valueOf(Objects.requireNonNullElse(type, "").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown email type: " + type);
        }
    }

    private static NotificationType parseCustomizableType(String type) {
        NotificationType parsed = parseType(type);
        if (!DEFINITIONS.get(parsed).customizable()) {
            throw new BadRequestException("Email type " + parsed + " cannot be customized");
        }
        return parsed;
    }

    private static String pick(String preferred, String fallback) {
        return hasText(preferred) ? preferred : fallback;
    }

    private static String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // ---------------------------------------------------------------- catalog

    private record Branding(String name, String logoUrl) {}

    private record Row(String label, String key) {}

    /** sample keys double as the placeholders a manager may use (except the password). */
    private record Definition(String audience, boolean customizable, String subject, String heading, String message,
                              boolean credentials, List<Row> rows, Map<String, String> sample) {}

    private static Map<String, String> sample(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) map.put(keyValues[i], keyValues[i + 1]);
        return map;
    }

    private static final Map<NotificationType, Definition> DEFINITIONS = new EnumMap<>(NotificationType.class);

    static {
        DEFINITIONS.put(NotificationType.EMPLOYEE_PASSWORD, new Definition("Employee", true,
                "Your {{organization}} employee account",
                "Your employee account is ready",
                "Your employee account at {{organization}} has been created. Use the credentials below to sign in.\n\n"
                        + "Please log in and change your password immediately to keep your account secure.",
                true, List.of(),
                sample("name", "Rekik Haile", "email", "rekik@example.com", PASSWORD, "Temp#1234")));

        DEFINITIONS.put(NotificationType.EMPLOYEE_PAYMENT_RECEIVED, new Definition("Employee", true,
                "{{organization}} salary payment received",
                "Salary payment received",
                "Great news - your salary payment has been marked as paid.\n\n"
                        + "You can review your payment details on your profile.",
                false, List.of(new Row("Amount (ETB)", "amount"), new Row("Transaction reference", "reference"),
                        new Row("Salary due date", "dueDate")),
                sample("name", "Rekik Haile", "email", "rekik@example.com", "amount", "80000",
                        "reference", "TX-Ref-543235622353", "dueDate", "2026-09-30")));

        DEFINITIONS.put(NotificationType.HIRING_SELECTED_FOR_INTERVIEW, new Definition("Candidate", true,
                "Interview selection - {{organization}}",
                "You have been selected for an interview!",
                "Congratulations! You were selected for an interview for the role of {{jobTitle}}.\n\n"
                        + "Our team will contact you soon with the next steps.",
                false, List.of(),
                sample("name", "Rekik Haile", "email", "rekik@example.com", "jobTitle", "Frontend Developer")));

        DEFINITIONS.put(NotificationType.HIRING_REJECTED_PRE_INTERVIEW, new Definition("Candidate", true,
                "Application update - {{organization}}",
                "Application update",
                "Thank you for applying for {{jobTitle}}.\n\n"
                        + "We appreciate your interest, but we will not proceed with your application at this stage.",
                false, List.of(),
                sample("name", "Rekik Haile", "email", "rekik@example.com", "jobTitle", "Frontend Developer")));

        DEFINITIONS.put(NotificationType.HIRING_HIRED, new Definition("Candidate", true,
                "Offer update - {{organization}}",
                "Congratulations and welcome!",
                "Congratulations! You have been selected for the role of {{jobTitle}}.\n\n"
                        + "Welcome to {{organization}} - we are excited to have you with us.",
                false, List.of(),
                sample("name", "Rekik Haile", "email", "rekik@example.com", "jobTitle", "Frontend Developer")));

        DEFINITIONS.put(NotificationType.HIRING_REJECTED_POST_INTERVIEW, new Definition("Candidate", true,
                "Interview result - {{organization}}",
                "Interview update",
                "Thank you for taking the time to interview for {{jobTitle}}.\n\n"
                        + "After careful review, we will not proceed further for this position.",
                false, List.of(),
                sample("name", "Rekik Haile", "email", "rekik@example.com", "jobTitle", "Frontend Developer")));

        DEFINITIONS.put(NotificationType.ADMIN_PAYROLL_REMINDER, new Definition("Manager", true,
                "{{organization}} payroll reminder",
                "Payroll reminder",
                "You have {{count}} employee payment(s) that are due soon or overdue.\n\n"
                        + "Please review them on the Payroll page and mark them as paid after transfer.",
                false, List.of(new Row("Payments due", "count")),
                sample("name", "Abel Tesfaye", "email", "manager@example.com", "count", "4")));

        DEFINITIONS.put(NotificationType.MANAGER_NEW_JOB_APPLICATION, new Definition("Manager", true,
                "New application for {{jobTitle}}",
                "New job application",
                "{{candidateName}} has applied for {{jobTitle}}.\n\n"
                        + "Review the application on the Jobs page.",
                false, List.of(new Row("Candidate", "candidateName"), new Row("Candidate email", "candidateEmail"),
                        new Row("Role", "jobTitle")),
                sample("name", "Abel Tesfaye", "email", "manager@example.com", "candidateName", "Rekik Haile",
                        "candidateEmail", "rekik@example.com", "jobTitle", "Frontend Developer")));

        DEFINITIONS.put(NotificationType.VICE_MANAGER_WELCOME, new Definition("Vice manager", true,
                "Your {{organization}} Vice Manager account",
                "Vice Manager account created",
                "You have been appointed as Vice Manager for {{branch}}. Use the credentials below to sign in.\n\n"
                        + "Please log in, change your password, and manage your branch's performance, attendance and payroll.",
                true, List.of(),
                sample("name", "Selam Bekele", "email", "vice@example.com", PASSWORD, "Temp#1234",
                        "branch", "Addis Ababa Branch")));

        DEFINITIONS.put(NotificationType.VICE_MANAGER_PAYROLL_REMINDER, new Definition("Vice manager", true,
                "{{branch}} payroll reminder",
                "Payroll reminder for {{branch}}",
                "{{count}} employee payment(s) in {{branch}} are due soon or overdue.\n\n"
                        + "Please review them on the Payroll page and mark them as paid after transfer.",
                false, List.of(new Row("Branch", "branch"), new Row("Payments due", "count")),
                sample("name", "Selam Bekele", "email", "vice@example.com", "branch", "Addis Ababa Branch", "count", "2")));

        DEFINITIONS.put(NotificationType.VICE_MANAGER_NEW_EMPLOYEE, new Definition("Vice manager", true,
                "New employee in {{branch}}: {{employeeName}}",
                "A new employee joined your branch",
                "{{employeeName}} has been added to {{branch}}.\n\n"
                        + "They will receive their login credentials by email. You can view their profile on the Employees page.",
                false, List.of(new Row("Employee", "employeeName"), new Row("Employee email", "employeeEmail"),
                        new Row("Position", "position"), new Row("Branch", "branch")),
                sample("name", "Selam Bekele", "email", "vice@example.com", "employeeName", "Rekik Haile",
                        "employeeEmail", "rekik@example.com", "position", "Frontend Developer", "branch", "Addis Ababa Branch")));

        DEFINITIONS.put(NotificationType.EMPLOYEE_EMAIL_CHANGED, new Definition("Employee", true,
                "Your {{organization}} sign-in email was updated",
                "Your sign-in email was updated",
                "Your employee account at {{organization}} now signs in with this email address. A new temporary password has been issued below.\n\n"
                        + "Please log in and change your password immediately to keep your account secure.",
                true, List.of(),
                sample("name", "Rekik Haile", "email", "rekik@example.com", PASSWORD, "Temp#1234")));

        // Sent before/outside any organization's customization, so these stay on the defaults.
        DEFINITIONS.put(NotificationType.MANAGER_WELCOME, new Definition("Manager", false,
                "Your {{organization}} workspace is ready",
                "Your workspace is ready",
                "Your workspace {{organization}} has been created and you are its first manager. Use the credentials below to sign in.\n\n"
                        + "Please log in and change your password immediately to keep your account secure.",
                true, List.of(),
                sample("name", "Abel Tesfaye", "email", "manager@example.com", PASSWORD, "Temp#1234",
                        "organization", "Acme Corp")));

        DEFINITIONS.put(NotificationType.PLATFORM_SIGNUP_REQUEST, new Definition("Platform admin", false,
                "New signup request - {{companyName}}",
                "New signup request",
                "A new organization signup request has arrived.\n\n"
                        + "Review it in the platform admin dashboard and click Register to provision the organization.",
                false, List.of(new Row("Company", "companyName"), new Row("Contact", "contactName"),
                        new Row("Email", "contactEmail"), new Row("Message", "message")),
                sample("name", "Platform Admin", "email", "admin@example.com", "companyName", "Acme Corp",
                        "contactName", "Rekik Haile", "contactEmail", "rekik@acme.example",
                        "message", "We'd like to try the platform for our 40-person team.")));

        DEFINITIONS.put(NotificationType.VERIFICATION_CODE, new Definition("Account", false,
                "Your {{organization}} verification code: {{code}}",
                "Your verification code",
                "Use the code below to {{action}}. It expires in {{minutes}} minutes.\n\n"
                        + "If you did not request this, you can ignore this email.",
                false, List.of(new Row("Verification code", "code")),
                sample("email", "rekik@acme.example", "code", "482913", "minutes", "10",
                        "action", "confirm your email and finish your workspace request")));
    }
}

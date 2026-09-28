-- Platform-wide Terms of Service and Privacy Policy, edited by the platform admin and shown
-- on the public /terms and /privacy pages. One row per document type.
CREATE TABLE IF NOT EXISTS legal_documents (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL UNIQUE CHECK (type IN ('TERMS', 'PRIVACY')),
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO legal_documents (type, title, content) VALUES
('TERMS', 'Terms of Service',
'By creating an account or using Mahberix you agree to these terms.

1. Accounts
You are responsible for the accuracy of the information you provide and for keeping your login credentials secure. Organization managers are responsible for the users they invite.

2. Acceptable use
Do not use the platform for unlawful purposes, to upload content you have no right to share, or to interfere with the service or other organizations.

3. Your data
Your organization owns the data it stores in Mahberix. We process it only to provide the service, as described in our Privacy Policy.

4. Availability and changes
We work to keep the service available but do not guarantee uninterrupted access. We may update these terms; continued use after an update means you accept the new terms.

5. Termination
You may stop using the service at any time. We may suspend accounts that violate these terms.

6. Contact
Questions about these terms: hello@mahberix.com'),
('PRIVACY', 'Privacy Policy',
'This policy explains what information Mahberix collects and how it is used.

1. Information we collect
Account details (name, email, phone), organization profile data, and the operational records your organization stores: employees, attendance, payroll, reviews, job applications and content.

2. How we use it
To provide and secure the service, send transactional emails (such as sign-in codes and notifications), and support your organization. We do not sell personal information.

3. Third-party services
We use service providers for email delivery, file storage and, when your organization connects them, integrations such as GitHub, Trello and Google. They process data only on our behalf.

4. Retention
Data is kept while your organization''s account is active and deleted or anonymized after it is closed, unless the law requires otherwise.

5. Your rights
You may request access to, correction of, or deletion of your personal information by contacting your organization or us.

6. Contact
Privacy questions: hello@mahberix.com')
ON CONFLICT (type) DO NOTHING;

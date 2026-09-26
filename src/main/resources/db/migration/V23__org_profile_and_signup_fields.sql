-- Rich company-profile fields for organizations (surfaced on the public /o/{slug} page and
-- edited by the org's manager), plus a few lead fields captured on the public signup form.

ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS tagline          VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description       TEXT,
    ADD COLUMN IF NOT EXISTS logo_url          VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS cover_image_url   VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS business_type     VARCHAR(120),
    ADD COLUMN IF NOT EXISTS industry          VARCHAR(120),
    ADD COLUMN IF NOT EXISTS company_size      VARCHAR(60),
    ADD COLUMN IF NOT EXISTS founded_year      INTEGER,
    ADD COLUMN IF NOT EXISTS phone             VARCHAR(60),
    ADD COLUMN IF NOT EXISTS company_email     VARCHAR(200),
    ADD COLUMN IF NOT EXISTS website_url       VARCHAR(512),
    ADD COLUMN IF NOT EXISTS address_line      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS city              VARCHAR(120),
    ADD COLUMN IF NOT EXISTS country           VARCHAR(120),
    ADD COLUMN IF NOT EXISTS linkedin_url      VARCHAR(512),
    ADD COLUMN IF NOT EXISTS twitter_url       VARCHAR(512),
    ADD COLUMN IF NOT EXISTS facebook_url      VARCHAR(512),
    ADD COLUMN IF NOT EXISTS instagram_url     VARCHAR(512);

ALTER TABLE signup_requests
    ADD COLUMN IF NOT EXISTS phone             VARCHAR(60),
    ADD COLUMN IF NOT EXISTS industry          VARCHAR(120),
    ADD COLUMN IF NOT EXISTS website_url       VARCHAR(512);

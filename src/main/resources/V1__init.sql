CREATE TABLE admins (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(150) NOT NULL,
                        email VARCHAR(200) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL,
                        is_active BOOLEAN NOT NULL DEFAULT TRUE,
                        last_login_at TIMESTAMPTZ NULL,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE blogs (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(250) NOT NULL,
                       slug VARCHAR(300) NOT NULL UNIQUE,
                       excerpt VARCHAR(400),
                       content TEXT NOT NULL,
                       cover_image_url TEXT,
                       status VARCHAR(20) NOT NULL,
                       published_at TIMESTAMPTZ NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE events (
                        id BIGSERIAL PRIMARY KEY,
                        title VARCHAR(250) NOT NULL,
                        slug VARCHAR(300) NOT NULL UNIQUE,
                        description TEXT NOT NULL,
                        event_type VARCHAR(20) NOT NULL,
                        location VARCHAR(250),
                        start_date TIMESTAMPTZ NOT NULL,
                        end_date TIMESTAMPTZ NULL,
                        registration_url TEXT,
                        status VARCHAR(20) NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE jobs (
                      id BIGSERIAL PRIMARY KEY,
                      title VARCHAR(250) NOT NULL,
                      slug VARCHAR(300) NOT NULL UNIQUE,
                      department VARCHAR(150),
                      employment_type VARCHAR(30) NOT NULL,
                      location VARCHAR(250),
                      description TEXT NOT NULL,
                      status VARCHAR(20) NOT NULL,
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE job_applications (
                                  id BIGSERIAL PRIMARY KEY,
                                  job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
                                  full_name VARCHAR(200) NOT NULL,
                                  email VARCHAR(200) NOT NULL,
                                  phone_number VARCHAR(50),
                                  github_url TEXT,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_applications_job_id ON job_applications(job_id);

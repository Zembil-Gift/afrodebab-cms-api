AfroDebab CMS API

A Spring Boot 3 REST API for managing Blogs, Events, Jobs, and Job Applications, featuring public endpoints, JWT-secured admin APIs, pagination, sorting, and Swagger/OpenAPI documentation.

Built with clean architecture and backend best practices.

🚀 Features

🌍 Public APIs for Blogs, Events, and Jobs

🔐 JWT Authentication for Admin endpoints

🧑‍💼 Admin management (Create / Update / Delete)

📄 Pagination & sorting

🧾 Job application, hiring workflow, and review

📚 Swagger / OpenAPI documentation

🗄️ PostgreSQL + Flyway migrations

⚠️ Centralized exception handling

🧱 DTO-based API responses (no entity exposure)

🛠️ Tech Stack

Java 17

Spring Boot 3

Spring Security (JWT)

Spring Data JPA

PostgreSQL

Flyway

Swagger / OpenAPI (springdoc)

Maven

📂 Project Structure
src/main/java/com/afrodebab/cms
├── admin            # Admin auth & controllers
├── blogs            # Blog domain
├── events           # Event domain
├── jobs             # Job domain
├── applications     # Job applications
├── security         # JWT & security config
├── common           # Exceptions & utilities
└── config           # Swagger/OpenAPI config

🔑 Authentication

Public endpoints → No authentication required

Admin endpoints (/admin/**) → JWT Bearer token required

Employee self-service endpoints (/employee/me/**) → Employee JWT Bearer token required

Admin Login
POST /admin/auth/login

Employee Login
POST /employee/auth/login


Request

{
  "email": "admin@afrodebab.com",
  "password": "Admin@123"
}


Response

{
  "token": "JWT_TOKEN"
}


Use the token as:

Authorization: Bearer <JWT_TOKEN>

🌍 Public API Examples
Get Events
GET /events?page=0&size=10&sortBy=startDate&direction=desc

Get Blogs
GET /blogs?page=0&size=10

Apply for a Job
POST /jobs/{jobId}/apply

Apply for a Job with Resume Upload
POST /jobs/{jobId}/apply/form

🧑‍💼 Admin API Examples
POST   /admin/blogs
PUT    /admin/blogs/{id}
DELETE /admin/blogs/{id}

POST   /admin/events
PUT    /admin/events/{id}

POST   /admin/jobs
PUT    /admin/jobs/{id}

GET    /admin/job-applications
GET    /admin/job-applications/{jobId}
PATCH  /admin/job-applications/{id}/status
POST   /admin/job-applications/{jobId}/select-interview
POST   /admin/job-applications/{jobId}/hire

POST   /admin/employees
GET    /admin/employees
GET    /admin/employees/{id}
PUT    /admin/employees/{id}
DELETE /admin/employees/{id}   # soft delete (is_active=false)
POST   /admin/employees/{id}/photo   # multipart/form-data, file field: "file"
PUT    /admin/employees/{id}/attendance   # upsert attendance for a date (clock-in/out)
GET    /admin/employees/{id}/attendance   # attendance history (latest date first)
GET    /admin/email-notifications/failed
POST   /admin/email-notifications/{id}/retry

POST   /employee/auth/login
POST   /employee/me/password

📚 API Documentation (Swagger)

Swagger UI is enabled for easy frontend integration:

http://localhost:8080/swagger-ui.html


Features:

Try APIs directly

JWT support via Authorize button

Request/response schemas

Hiring workflow:
- Application statuses: APPLIED, UNDER_REVIEW, SELECTED_FOR_INTERVIEW, REJECTED, HIRED
- Selecting interview candidates auto-rejects non-selected applicants for the same job
- Hiring one candidate auto-rejects the remaining interviewed candidates for the same job
- Hiring creates an employee profile

Email delivery workflow:
- All transactional emails are queued in `email_notifications`
- Daily dispatcher runs at 00:00 UTC
- Dispatcher processes PENDING and FAILED notifications with attempts < 3
- Failed deliveries are visible in admin and support manual retry endpoint

🗄️ Database Setup
Create Database
CREATE DATABASE afrodebab_cms;

Migrations

Flyway runs automatically on startup:

src/main/resources/db/migration

⚙️ Configuration
application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/afrodebab_cms
    username: postgres
    password: postgres

app:
  jwt:
    secret: CHANGE_ME_TO_A_LONG_RANDOM_SECRET
    expiresMinutes: 120
  cloudflare:
    r2:
      s3Api: https://<accountid>.r2.cloudflarestorage.com/<bucket-name>
      publicDevelopmentUrl: https://<public-r2-dev-domain>
      tokenValue: <TOKEN_VALUE>
      accessKeyId: <ACCESS_KEY_ID>
      secretAccessKey: <SECRET_ACCESS_KEY>
      region: auto
  sendgrid:
    apiKey: <SENDGRID_API_KEY>
    fromEmail: no-reply@example.com
    fromName: AfroDebab CMS

.env is auto-loaded (via `spring-dotenv`), so these keys can be set directly in a root `.env` file.

▶️ Run the Application
mvn clean install
mvn spring-boot:run


Application runs at:

http://localhost:8080

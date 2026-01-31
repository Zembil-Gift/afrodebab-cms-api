AfroDebab CMS API

A Spring Boot 3 REST API for managing Blogs, Events, Jobs, and Job Applications, featuring public endpoints, JWT-secured admin APIs, pagination, sorting, and Swagger/OpenAPI documentation.

Built with clean architecture and backend best practices.

🚀 Features

🌍 Public APIs for Blogs, Events, and Jobs

🔐 JWT Authentication for Admin endpoints

🧑‍💼 Admin management (Create / Update / Delete)

📄 Pagination & sorting

🧾 Job application submission & review

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

Admin Login
POST /admin/auth/login


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

📚 API Documentation (Swagger)

Swagger UI is enabled for easy frontend integration:

http://localhost:8080/swagger-ui.html


Features:

Try APIs directly

JWT support via Authorize button

Request/response schemas

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

▶️ Run the Application
mvn clean install
mvn spring-boot:run


Application runs at:

http://localhost:8080


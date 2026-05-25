# Internal Metrics API: Client Guide

This guide lists:
1. **New endpoints** added for internal metrics and peer reviews
2. **Existing endpoints affected** by request/response shape changes

## Auth and base rules

- **Admin endpoints**: `/admin/**` require `Authorization: Bearer <admin_jwt>`
- **Employee endpoints**: `/employee/me/**` require `Authorization: Bearer <employee_jwt>`
- Content type for JSON endpoints: `application/json`
- Date format: `YYYY-MM-DD`
- Datetime format: ISO-8601 UTC (`2026-05-18T09:30:00Z`)

---

## 1) New endpoints

### 1.1 Employee metrics summary (self)

**GET** `/employee/me/metrics`

Query params:
- `periodStart` (required, date)
- `periodEnd` (required, date)
- `persistSnapshot` (optional, boolean, default `false`)

Example:
```bash
curl -X GET "http://localhost:8080/employee/me/metrics?periodStart=2026-04-01&periodEnd=2026-06-30&persistSnapshot=false" \
  -H "Authorization: Bearer <employee_jwt>"
```

Response `200` (`EmployeeMetricSummaryResponse`):
```json
{
  "employeeId": 12,
  "employeeName": "John Doe",
  "role": "DEVELOPER",
  "department": "ENGINEERING",
  "employmentType": "FULL_TIME",
  "employeeStatus": "ACTIVE",
  "periodStart": "2026-04-01",
  "periodEnd": "2026-06-30",
  "leadershipScore": 78.57,
  "attendanceScore": 90.50,
  "taskScore": null,
  "supportScore": null,
  "overallScore": 83.05,
  "strengthSummary": "Strongest area: Attendance (90.50%)",
  "improvementSummary": "Needs improvement: Leadership (78.57%)"
}
```

---

### 1.2 List active leadership principles

**GET** `/employee/me/peer-reviews/principles`

Example:
```bash
curl -X GET "http://localhost:8080/employee/me/peer-reviews/principles" \
  -H "Authorization: Bearer <employee_jwt>"
```

Response `200` (`LeadershipPrincipleResponse[]`):
```json
[
  {
    "id": 1,
    "name": "Ownership & Accountability",
    "description": "Takes responsibility, follows through, and proactively solves problems.",
    "isActive": true
  }
]
```

---

### 1.3 Admin: initiate peer review period

**POST** `/admin/metrics/peer-reviews/periods`

Request body (`PeerReviewPeriodCreateRequest`):
```json
{
  "name": "2026 Q2",
  "periodStart": "2026-04-01",
  "periodEnd": "2026-06-30"
}
```

Example:
```bash
curl -X POST "http://localhost:8080/admin/metrics/peer-reviews/periods" \
  -H "Authorization: Bearer <admin_jwt>" \
  -H "Content-Type: application/json" \
  -d '{"name":"2026 Q2","periodStart":"2026-04-01","periodEnd":"2026-06-30"}'
```

Response `200` (`PeerReviewPeriodResponse`):
```json
{
  "id": 3,
  "name": "2026 Q2",
  "periodStart": "2026-04-01",
  "periodEnd": "2026-06-30",
  "createdAt": "2026-05-19T12:30:11.232Z"
}
```

Note: re-initiating the same period returns the existing period.
Name must be unique.

---

### 1.4 Employee: list initiated peer review periods

**GET** `/employee/me/peer-reviews/periods`

Example:
```bash
curl -X GET "http://localhost:8080/employee/me/peer-reviews/periods" \
  -H "Authorization: Bearer <employee_jwt>"
```

Response `200` (`PeerReviewPeriodStatusResponse[]`):
```json
[
  {
    "id": 3,
    "name": "2026 Q2",
    "periodStart": "2026-04-01",
    "periodEnd": "2026-06-30",
    "submitted": true
  }
]
```

Note: `submitted` is true when the employee has completed all active principles for at least one reviewee in the period.

---

### 1.5 Admin: list initiated peer review periods

**GET** `/admin/metrics/peer-reviews/periods`

Example:
```bash
curl -X GET "http://localhost:8080/admin/metrics/peer-reviews/periods" \
  -H "Authorization: Bearer <admin_jwt>"
```

Response `200` (`PeerReviewPeriodResponse[]`).

---

### 1.6 Available employees for peer review

**GET** `/employee/me/peer-reviews/available-employees`

Example:
```bash
curl -X GET "http://localhost:8080/employee/me/peer-reviews/available-employees" \
  -H "Authorization: Bearer <employee_jwt>"
```

Response `200` (`PeerReviewAvailableEmployeeResponse[]`):
```json
[
  {
    "id": 14,
    "name": "Jane Doe",
    "department": "ENGINEERING",
    "role": "DEVELOPER",
    "employmentType": "FULL_TIME"
  }
]
```

Admin variant:

**GET** `/admin/metrics/peer-reviews/available-employees`

---

### 1.7 Admin: combined peer review results by period

**GET** `/admin/metrics/peer-reviews/periods/{periodId}/results`

Example:
```bash
curl -X GET "http://localhost:8080/admin/metrics/peer-reviews/periods/3/results" \
  -H "Authorization: Bearer <admin_jwt>"
```

Response `200` (`PeerReviewPeriodResultsResponse`):
```json
{
  "periodId": 3,
  "periodName": "2026 Q2",
  "periodStart": "2026-04-01",
  "periodEnd": "2026-06-30",
  "employees": [
    {
      "employeeId": 12,
      "employeeName": "John Doe",
      "department": "ENGINEERING",
      "role": "DEVELOPER",
      "employmentType": "FULL_TIME",
      "leadershipScore": 78.57,
      "principleAverages": [
        {"principleId": 1, "principleName": "Ownership & Accountability", "averageRating": 2.67, "ratingCount": 3}
      ]
    }
  ]
}
```

Note: Results are aggregated and do not include reviewer identity.

---

### 1.8 Employee: own combined peer review results by period

**GET** `/employee/me/peer-reviews/periods/{periodId}/results`

Example:
```bash
curl -X GET "http://localhost:8080/employee/me/peer-reviews/periods/3/results" \
  -H "Authorization: Bearer <employee_jwt>"
```

Response `200` (`PeerReviewSelfResultsResponse`).

Note: Results are aggregated and do not include reviewer identity.

---

### 1.9 Submit peer reviews

**POST** `/employee/me/peer-reviews`

Request body (`PeerReviewSubmitRequest`):
```json
{
  "revieweeId": 14,
  "periodStart": "2026-04-01",
  "periodEnd": "2026-06-30",
  "ratings": [
    {
      "principleId": 1,
      "rating": "EXCEEDS_THE_BAR",
      "comment": "Strong ownership on delivery."
    },
    {
      "principleId": 2,
      "rating": "MEETS_THE_BAR",
      "comment": "Consistent and transparent updates."
    }
  ]
}
```

`rating` enum values:
- `EXCEEDS_THE_BAR`
- `MEETS_THE_BAR`
- `NEEDS_IMPROVEMENT`

Example:
```bash
curl -X POST "http://localhost:8080/employee/me/peer-reviews" \
  -H "Authorization: Bearer <employee_jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "revieweeId": 14,
    "periodStart": "2026-04-01",
    "periodEnd": "2026-06-30",
    "ratings": [
      {"principleId": 1, "rating": "EXCEEDS_THE_BAR", "comment": "Strong ownership"}
    ]
  }'
```

Response `200` (`PeerReviewResponse[]`)  
Note: reviewer identity is **not returned**. The period must be initiated by an admin. Once a rating is submitted for a principle, it **cannot be updated** (submitting again returns `400`).
```json
[
  {
    "id": 55,
    "revieweeId": 14,
    "revieweeName": "Jane Doe",
    "periodStart": "2026-04-01",
    "periodEnd": "2026-06-30",
    "principleId": 1,
    "principleName": "Ownership & Accountability",
    "rating": "EXCEEDS_THE_BAR",
    "comment": "Strong ownership",
    "createdAt": "2026-05-18T13:41:11.232Z"
  }
]
```

---

### 1.10 Get received peer reviews (self)

**GET** `/employee/me/peer-reviews/received`

Query params:
- `periodStart` (required)
- `periodEnd` (required)

Example:
```bash
curl -X GET "http://localhost:8080/employee/me/peer-reviews/received?periodStart=2026-04-01&periodEnd=2026-06-30" \
  -H "Authorization: Bearer <employee_jwt>"
```

Response: `PeerReviewResponse[]`

---

### 1.11 Admin: metrics page (paginated)

**GET** `/admin/metrics/employees`

Query params:
- `periodStart` (required)
- `periodEnd` (required)
- `department` (optional)
- `role` (optional)
- `page` (optional, default `0`)
- `size` (optional, default `10`)
- `sortBy` (optional, default `createdAt`)
- `direction` (optional, `asc|desc`, default `desc`)
- `persistSnapshot` (optional, default `false`)

Example:
```bash
curl -X GET "http://localhost:8080/admin/metrics/employees?periodStart=2026-04-01&periodEnd=2026-06-30&department=ENGINEERING&role=DEVELOPER&page=0&size=10" \
  -H "Authorization: Bearer <admin_jwt>"
```

Response `200`: Spring `Page<EmployeeMetricSummaryResponse>` (includes `content`, paging fields, etc.)

---

### 1.12 Admin: single employee metric summary

**GET** `/admin/metrics/employees/{employeeId}`

Query params:
- `periodStart` (required)
- `periodEnd` (required)
- `persistSnapshot` (optional, default `false`)

Example:
```bash
curl -X GET "http://localhost:8080/admin/metrics/employees/12?periodStart=2026-04-01&periodEnd=2026-06-30" \
  -H "Authorization: Bearer <admin_jwt>"
```

Response: `EmployeeMetricSummaryResponse`

---

### 1.13 Admin: force snapshot refresh

**POST** `/admin/metrics/employees/{employeeId}/snapshot`

Query params:
- `periodStart` (required)
- `periodEnd` (required)

Example:
```bash
curl -X POST "http://localhost:8080/admin/metrics/employees/12/snapshot?periodStart=2026-04-01&periodEnd=2026-06-30" \
  -H "Authorization: Bearer <admin_jwt>"
```

Response: `EmployeeMetricSummaryResponse`

---

### 1.14 Admin: list peer reviews by period

**GET** `/admin/metrics/peer-reviews`

Query params:
- `periodStart` (required)
- `periodEnd` (required)
- `revieweeId` (optional)

Example:
```bash
curl -X GET "http://localhost:8080/admin/metrics/peer-reviews?periodStart=2026-04-01&periodEnd=2026-06-30&revieweeId=14" \
  -H "Authorization: Bearer <admin_jwt>"
```

Response: `PeerReviewResponse[]` (reviewer identity hidden)

---

### 1.15 Time spent endpoints (day/week/month)

These endpoints calculate worked time from:
- `clockInAt -> lunchBreakInAt`
- `lunchBreakOutAt -> clockOutAt`

Equivalent formula:
- `(clockOutAt - clockInAt) - (lunchBreakOutAt - lunchBreakInAt)`

Assumptions:
- Required time is **8 hours/day** (`480` minutes).
- **Weekly/monthly required time uses employee office schedule days only**.
- Approved leave days are excluded from weekly/monthly required office-day count.

#### Admin endpoints

1. **GET** `/admin/metrics/employees/{employeeId}/time-spent/daily?date=YYYY-MM-DD`
2. **GET** `/admin/metrics/employees/{employeeId}/time-spent/weekly?date=YYYY-MM-DD`
3. **GET** `/admin/metrics/employees/{employeeId}/time-spent/monthly?date=YYYY-MM-DD`

#### Employee self endpoints

1. **GET** `/employee/me/time-spent/daily?date=YYYY-MM-DD`
2. **GET** `/employee/me/time-spent/weekly?date=YYYY-MM-DD`
3. **GET** `/employee/me/time-spent/monthly?date=YYYY-MM-DD`

Example response (`EmployeeTimeSpentResponse`):
```json
{
  "employeeId": 12,
  "employeeName": "Jane Doe",
  "periodType": "WEEKLY",
  "periodStart": "2026-05-18",
  "periodEnd": "2026-05-24",
  "officeDaysCount": 5,
  "workedMinutes": 2140,
  "requiredMinutes": 2400,
  "remainingMinutes": 260,
  "completionPercent": 89.17
}
```

Notes:
- Weekly window is Monday-Sunday based on the provided `date`.
- Monthly window is the whole month of the provided `date`.
- Daily endpoint uses one date with required `480` minutes.

---

## 2) Existing endpoints affected by this change

## 2.1 Employee profile fields expanded

The following endpoints now include these extra profile fields:
- `role`
- `department`
- `employmentType`
- `employeeStatus`

Affected endpoints:
- **POST** `/admin/employees`
- **POST** `/admin/employees/form`
- **PUT** `/admin/employees/{id}`
- **GET** `/admin/employees`
- **GET** `/admin/employees/{id}`
- **GET** `/employee/me`
- **POST** `/employee/me/photo` (returns updated `EmployeeResponse`)
- **POST** `/admin/employees/{id}/photo` (returns updated `EmployeeResponse`)

### Updated JSON request shape (`POST /admin/employees`, `PUT /admin/employees/{id}`)
```json
{
  "name": "Jane Doe",
  "email": "jane@afrodebab.com",
  "phone": "+2519...",
  "position": "Software Engineer",
  "role": "DEVELOPER",
  "department": "ENGINEERING",
  "employmentType": "FULL_TIME",
  "employeeStatus": "ACTIVE",
  "linkedinUrl": "https://linkedin.com/in/jane",
  "photo": "https://...",
  "salaryDate": "2026-05-01",
  "salaryAmountMinor": 1200000,
  "salaryScheduleDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
}
```

### Updated multipart form params (`POST /admin/employees/form`)
New optional fields:
- `role`
- `department`
- `employmentType`
- `employeeStatus`

---

## 2.2 Attendance payload expanded

### Affected endpoints
- **PUT** `/admin/employees/{id}/attendance` (request + response changed)
- **GET** `/admin/employees/{id}/attendance` (response changed)
- Employee attendance actions now return expanded response shape:
  - **POST** `/employee/me/clock-in`
  - **POST** `/employee/me/clock-out`
  - **POST** `/employee/me/lunch-break-in`
  - **POST** `/employee/me/lunch-break-out`

### Updated request (`EmployeeAttendanceUpsertRequest`)
```json
{
  "date": "2026-05-18",
  "clockInAt": "2026-05-18T05:30:00Z",
  "clockOutAt": "2026-05-18T14:30:00Z",
  "lunchBreakInAt": "2026-05-18T09:00:00Z",
  "lunchBreakOutAt": "2026-05-18T09:45:00Z",
  "notes": "Worked from office"
}
```

Status is computed automatically from attendance times on both admin upsert and employee clock endpoints.

### Updated response (`EmployeeAttendanceResponse`)
```json
{
  "id": 101,
  "employeeId": 12,
  "date": "2026-05-18",
  "clockInAt": "2026-05-18T05:30:00Z",
  "clockOutAt": "2026-05-18T14:30:00Z",
  "lunchBreakInAt": "2026-05-18T09:00:00Z",
  "lunchBreakOutAt": "2026-05-18T09:45:00Z",
  "attendanceStatus": {
    "entry": "ON_TIME",
    "exit": "ON_TIME",
    "lunch": "ON_TIME",
    "final": "ON_TIME"
  },
  "notes": "Worked from office",
  "createdAt": "2026-05-18T05:30:02Z",
  "updatedAt": "2026-05-18T14:30:10Z"
}
```

`attendanceStatus` JSON keys:
- `entry`: `ON_TIME | LATE_IN | ABSENT`
- `exit`: `ON_TIME | EARLY_OUT | LATE_OUT | ABSENT`
- `lunch`: `ON_TIME | LATE_OUT | ABSENT`
- `final`: `ON_TIME | LATE | ABSENT | APPROVED_LEAVE | REMOTE_APPROVED`

Policy baselines are env-configurable:
- `ATTENDANCE_ENTRY_TIME` (default `09:00`)
- `ATTENDANCE_EXIT_TIME` (default `17:00`)
- `ATTENDANCE_LUNCH_START_TIME` (default `12:30`)
- `ATTENDANCE_LUNCH_END_TIME` (default `13:30`)
- `ATTENDANCE_GRACE_MINUTES` (default `30`)
- `ATTENDANCE_MAX_LUNCH_BREAK_MINUTES` (default `70`)

### Admin-only manual final status override

**PATCH** `/admin/employees/{id}/attendance/status?date=YYYY-MM-DD`

Request body:
```json
{
  "finalStatus": "LATE"
}
```

`finalStatus` enum values:
- `ON_TIME`
- `LATE`
- `ABSENT`
- `APPROVED_LEAVE`
- `REMOTE_APPROVED`

---

## 3) Client integration notes

- Treat score fields (`leadershipScore`, `attendanceScore`, `taskScore`, `supportScore`, `overallScore`) as nullable.
- For paginated admin metrics, read rows from `content`.
- Do not expect reviewer identity in peer review responses.
- Use exact date range values when querying peer reviews and metrics for consistency with backend calculation windows.

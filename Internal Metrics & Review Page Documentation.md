## **1\. Purpose**

Build an internal employee performance and culture metrics page inside the existing AfroDebab website:

[https://afrodebab.com/](https://afrodebab.com/)

This page will help leadership review employee performance based on:

* Leadership principles  
* Peer reviews every 3 months  
* Attendance and punctuality  
* Task delivery  
* Customer support activity  
* GitHub / ClickUp work contribution

The goal is not to punish employees, but to create visibility, accountability, fairness, and continuous improvement.

---

# **2\. Page Access**

This page must be private and only available to full-time employees.

## **Access Rules**

Only authenticated full-time employees can access the page.

Roles may include:

* Founder / Admin  
* Manager  
* Vice Manager  
* Full-Time Employee

Restricted from:

* Public users  
* Vendors  
* Customers  
* Contractors unless approved  
* Unauthenticated visitors

Recommended route:

/internal/metrics

or

/employee-dashboard/metrics  
---

# **3\. Core Features**

## **A. Employee Profile Metrics**

Each employee should have a profile that shows:

* Name  
* Role  
* Department  
* Employment type  
* Review period  
* Leadership principle score  
* Attendance score  
* Task delivery score  
* Support/customer service score  
* Overall performance score  
* Strength areas  
* Improvement areas  
* Peer feedback summary

---

# **4\. Leadership Principle Review Logic**

Every 3 months, employees review each other based on AfroDebab/goGerami leadership principles.

## **Leadership Principles**

Review categories:

* Ownership & Accountability  
* Integrity & Transparency  
* Customer-Centered Thinking  
* Bias for Action  
* Continuous Growth  
* Team Collaboration  
* Communication Discipline

## **Review Rating Options**

For each principle, reviewers choose one:

* Exceeds the Bar  
* Meets the Bar  
* Needs Improvement

## **Example Review Question**

For Ownership & Accountability:

“How well does this employee take responsibility, follow through, and proactively solve problems?”

Options:

* Exceeds the Bar  
* Meets the Bar  
* Needs Improvement

## **Scoring Logic**

Assign points:

Exceeds the Bar \= 3 points  
Meets the Bar \= 2 points  
Needs Improvement \= 1 point

For each employee:

Leadership Principle Score \=  
Total points received / Maximum possible points × 100

Example:

If an employee receives 42 points out of 63 possible:

42 / 63 × 100 \= 66.6%  
---

# **5\. Peer Review Rules**

## **Review Frequency**

Peer reviews should happen every 3 months.

## **Review Access**

Each full-time employee reviews other team members.

Optional rule:

Employees should not review themselves.

## **Review Privacy**

Leadership can see detailed review results.

Employees may see:

* Their own summary  
* Strength areas  
* Improvement areas

But not necessarily who gave each review, unless leadership decides otherwise.

Recommended approach:

Keep peer feedback anonymous to encourage honesty.

---

# **6\. Attendance & Punctuality Logic**

The system should track:

* On-time attendance  
* Late arrivals  
* Missed workdays  
* Approved leave  
* Unapproved absence

## **Attendance Status**

Each workday should be recorded as:

* On Time  
* Late  
* Absent  
* Approved Leave  
* Remote Approved

## **Suggested Scoring**

On Time \= 100%  
Late \= 70%  
Absent \= 0%  
Approved Leave \= Neutral / Not counted  
Remote Approved \= 100%

## **Attendance Score Formula**

Attendance Score \=  
Total attendance points / Total counted workdays

Example:

20 working days:

* 16 on time  
* 3 late  
* 1 absent

Score:

(16×100 \+ 3×70 \+ 1×0) / 20 \= 90.5%  
---

# **7\. Task Performance Logic**

Task performance should be calculated from tools like:

* ClickUp  
* GitHub Issues  
* Internal task tracker

## **Metrics to Track**

* Assigned tasks  
* Completed tasks  
* Overdue tasks  
* Blocked tasks  
* Sprint participation  
* Task quality review

## **Suggested Task Score**

Task Completion Rate \= Completed Tasks / Assigned Tasks × 100

Additional modifiers:

* Overdue task penalty  
* High-impact task bonus  
* Reopened task penalty

Example:

Base Task Score \= 80%  
Overdue penalty \= \-10%  
High-impact contribution \= \+5%

Final Task Score \= 75%  
---

# **8\. Customer Support Performance Logic**

For customer support representatives, the system should track activity from the Telegram support bot.

## **Metrics to Track**

* Number of customer conversations handled  
* Average response time  
* Resolved tickets  
* Escalated issues  
* Customer satisfaction rating if available  
* Follow-up completion

## **Suggested Support Score**

Support Score \=  
Resolved Tickets \+ Response Time \+ Quality Review \+ Follow-Up Rate

Example weighting:

Resolved Tickets \= 40%  
Response Time \= 25%  
Quality of Response \= 25%  
Follow-Up Completion \= 10%  
---

# **9\. Overall Employee Score**

Each employee should receive an overall score based on role-relevant categories.

## **General Employee Score**

Overall Score \=  
Leadership Score 30%  
Attendance Score 20%  
Task Performance Score 40%  
Team Contribution Score 10%

## **Customer Support Score**

Overall Score \=  
Leadership Score 25%  
Attendance Score 20%  
Support Performance Score 45%  
Team Contribution Score 10%

## **Developer Score**

Overall Score \=  
Leadership Score 25%  
Attendance Score 15%  
GitHub / ClickUp Task Score 50%  
Team Contribution Score 10%  
---

# **10\. Dashboard Pages**

## **Admin View**

Leadership should see:

* All employees  
* Overall ranking  
* Leadership principle breakdown  
* Attendance summary  
* Task delivery summary  
* Improvement flags  
* Top performers  
* Employees needing support

## **Employee View**

Each employee should see:

* Their own metrics  
* Strengths  
* Improvement areas  
* Review history  
* Attendance summary  
* Task contribution summary

## **Review Page**

Admins initiate named peer review periods, and employees can submit reviews only for initiated periods.

Fields:

* Open review period (initiated by admin)  
* Employee being reviewed (active employees, excluding self)  
* Leadership principle ratings  
* Optional written feedback  
* Submit button

---

# **11\. Suggested Database Tables**

## **employees**

id  
full\_name  
email  
role  
department  
employment\_type  
status  
created\_at  
updated\_at

## **leadership\_principles**

id  
name  
description  
is\_active

## **peer\_reviews**

id  
reviewer\_id  
reviewee\_id  
review\_period  
principle\_id  
rating  
comment  
created\_at

## **peer\_review\_periods**

id  
name  
period\_start  
period\_end  
created\_at  
updated\_at

## **attendance\_records**

id  
employee\_id  
work\_date  
status  
check\_in\_time  
check\_out\_time  
notes  
created\_at

## **task\_metrics**

id  
employee\_id  
source  
source\_task\_id  
task\_title  
status  
assigned\_date  
completed\_date  
due\_date  
is\_overdue  
created\_at

## **support\_metrics**

id  
employee\_id  
telegram\_ticket\_id  
customer\_name  
issue\_type  
status  
response\_time\_minutes  
resolved\_at  
created\_at

## **employee\_metric\_scores**

id  
employee\_id  
review\_period  
leadership\_score  
attendance\_score  
task\_score  
support\_score  
overall\_score  
strength\_summary  
improvement\_summary  
created\_at  
---

# **12\. Integrations**

## **Telegram Support Bot**

Use bot data to track:

* Who handled the request  
* Customer issue category  
* Response time  
* Resolution status  
* Escalation status

## **GitHub**

Use GitHub API to track:

* Assigned issues  
* Closed issues  
* Pull requests  
* Reviews  
* Contributions

## **ClickUp**

Use ClickUp API to track:

* Assigned tasks  
* Completed tasks  
* Overdue tasks  
* Sprint progress

---

# **13\. Privacy & Fairness Rules**

Important:

* Metrics should support growth, not create fear.  
* Peer reviews should be handled carefully.  
* Employees should know how scores are calculated.  
* Leadership should review context before making decisions.  
* Approved leave should not reduce attendance score.  
* Quantity should not replace quality.

Example:

A customer support employee who handles many tickets but gives poor responses should not automatically receive a high score.

---

# **14\. Acceptance Criteria**

The implementation is complete when:

* Only full-time employees can access the internal metrics page  
* Employees can submit quarterly peer reviews  
* Leadership can view employee scorecards  
* Attendance and punctuality can be recorded  
* Task performance can be imported or manually entered  
* Telegram support bot activity can be tracked  
* Overall score is calculated automatically  
* Each employee profile shows strengths and improvement areas  
* Admin can filter by department, role, and review period  
* Sensitive review data is protected

# **Final Goal**

This internal metrics and review system should help AfroDebab build a high-accountability culture where employees are recognized not only for task completion, but also for leadership behavior, consistency, teamwork, and customer impact.

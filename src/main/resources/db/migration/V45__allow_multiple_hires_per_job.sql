-- A job can fill several seats; hiring is gated on the job being OPEN in the service layer.
DROP INDEX IF EXISTS uk_job_applications_job_hired;

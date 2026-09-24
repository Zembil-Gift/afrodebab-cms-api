package com.afrodebab.cms.jpa.entity;

/**
 * An applicant's answer to one {@link JobApplicationField}, stored as JSON in
 * {@code job_applications.answers}. Label and type are snapshotted so later edits
 * to the job's form don't change what the applicant saw. {@code value} is the text,
 * the link, or the uploaded file's URL; {@code fileName} is set for FILE answers only.
 */
public record JobApplicationAnswer(
        String fieldId,
        String label,
        JobApplicationField.Type type,
        String value,
        String fileName
) {}

package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    Optional<EmailOtp> findByPurposeAndEmail(EmailOtp.Purpose purpose, String email);
}

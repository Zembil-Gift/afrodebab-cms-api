package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.SignupRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignupRequestRepository extends JpaRepository<SignupRequest, Long> {
    List<SignupRequest> findAllByOrderByCreatedAtDesc();
    List<SignupRequest> findAllByStatusOrderByCreatedAtDesc(SignupRequest.Status status);
}

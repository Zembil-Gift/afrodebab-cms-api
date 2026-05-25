package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.AdminPeerReviewResponse;
import com.afrodebab.cms.dto.AdminPeerReviewUpsertRequest;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Admin;
import com.afrodebab.cms.jpa.entity.AdminPeerReview;
import com.afrodebab.cms.jpa.entity.Employee;
import com.afrodebab.cms.jpa.entity.PeerReviewPeriod;
import com.afrodebab.cms.jpa.repository.AdminPeerReviewRepository;
import com.afrodebab.cms.jpa.repository.AdminRepository;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.PeerReviewPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPeerReviewService {
    private final AdminPeerReviewRepository adminPeerReviewRepository;
    private final PeerReviewPeriodRepository peerReviewPeriodRepository;
    private final EmployeeRepository employeeRepository;
    private final AdminRepository adminRepository;

    public AdminPeerReviewService(AdminPeerReviewRepository adminPeerReviewRepository,
                                 PeerReviewPeriodRepository peerReviewPeriodRepository,
                                 EmployeeRepository employeeRepository,
                                 AdminRepository adminRepository) {
        this.adminPeerReviewRepository = adminPeerReviewRepository;
        this.peerReviewPeriodRepository = peerReviewPeriodRepository;
        this.employeeRepository = employeeRepository;
        this.adminRepository = adminRepository;
    }

    @Transactional
    public AdminPeerReviewResponse upsert(String adminEmail,
                                         Long periodId,
                                         Long revieweeId,
                                         AdminPeerReviewUpsertRequest request) {
        PeerReviewPeriod period = peerReviewPeriodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Peer review period not found"));

        Admin reviewer = adminRepository.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        Employee reviewee = employeeRepository.findById(revieweeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        AdminPeerReview row = adminPeerReviewRepository.findByRevieweeIdAndPeriodId(revieweeId, periodId)
                .orElseGet(AdminPeerReview::new);

        row.setReviewer(reviewer);
        row.setReviewee(reviewee);
        row.setPeriod(period);
        row.setRating(request.rating());
        row.setFeedback(request.feedback());

        AdminPeerReview saved = adminPeerReviewRepository.save(row);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AdminPeerReviewResponse getForAdmin(Long periodId, Long revieweeId) {
        PeerReviewPeriod period = peerReviewPeriodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Peer review period not found"));

        Employee reviewee = employeeRepository.findById(revieweeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        return adminPeerReviewRepository.findByRevieweeIdAndPeriodId(revieweeId, periodId)
                .map(this::toResponse)
                .orElseGet(() -> emptyResponse(period, reviewee));
    }

    @Transactional(readOnly = true)
    public AdminPeerReviewResponse getForEmployee(String employeeEmail, Long periodId) {
        PeerReviewPeriod period = peerReviewPeriodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Peer review period not found"));

        Employee employee = employeeRepository.findByEmailIgnoreCase(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        return adminPeerReviewRepository.findByRevieweeIdAndPeriodId(employee.getId(), periodId)
                .map(this::toResponse)
                .orElseGet(() -> emptyResponse(period, employee));
    }

    private AdminPeerReviewResponse emptyResponse(PeerReviewPeriod period, Employee reviewee) {
        return new AdminPeerReviewResponse(
                null,
                period.getId(),
                period.getName(),
                period.getPeriodStart(),
                period.getPeriodEnd(),
                null,
                null,
                reviewee.getId(),
                reviewee.getName(),
                null,
                null,
                null,
                null
        );
    }

    private AdminPeerReviewResponse toResponse(AdminPeerReview row) {
        return new AdminPeerReviewResponse(
                row.getId(),
                row.getPeriod().getId(),
                row.getPeriod().getName(),
                row.getPeriod().getPeriodStart(),
                row.getPeriod().getPeriodEnd(),
                row.getReviewer() != null ? row.getReviewer().getId() : null,
                row.getReviewer() != null ? row.getReviewer().getName() : null,
                row.getReviewee().getId(),
                row.getReviewee().getName(),
                row.getRating(),
                row.getFeedback(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }
}

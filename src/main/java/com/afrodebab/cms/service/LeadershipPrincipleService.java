package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.LeadershipPrincipleRequest;
import com.afrodebab.cms.dto.LeadershipPrincipleResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.LeadershipPrinciple;
import com.afrodebab.cms.jpa.repository.LeadershipPrincipleRepository;
import com.afrodebab.cms.jpa.repository.PeerReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Per-organization CRUD for the principles peer reviews are rated against. */
@Service
public class LeadershipPrincipleService {

    // The platform's starter set; an org opts in when it opens its first review period.
    private static final List<List<String>> DEFAULT_PRINCIPLES = List.of(
            List.of("Ownership & Accountability", "Takes responsibility, follows through, and proactively solves problems."),
            List.of("Integrity & Transparency", "Acts with honesty, openness, and ethical behavior in all situations."),
            List.of("Customer-Centered Thinking", "Prioritizes customer value and long-term trust."),
            List.of("Bias for Action", "Executes decisively and avoids unnecessary delay."),
            List.of("Continuous Growth", "Seeks feedback, learns quickly, and improves consistently."),
            List.of("Team Collaboration", "Collaborates effectively and supports cross-team outcomes."),
            List.of("Communication Discipline", "Communicates clearly, consistently, and with ownership.")
    );

    private final LeadershipPrincipleRepository principleRepo;
    private final PeerReviewRepository peerReviewRepo;

    public LeadershipPrincipleService(LeadershipPrincipleRepository principleRepo,
                                      PeerReviewRepository peerReviewRepo) {
        this.principleRepo = principleRepo;
        this.peerReviewRepo = peerReviewRepo;
    }

    @Transactional(readOnly = true)
    public List<LeadershipPrincipleResponse> listAll() {
        return principleRepo.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LeadershipPrincipleResponse> listDefaults() {
        return DEFAULT_PRINCIPLES.stream()
                .map(p -> new LeadershipPrincipleResponse(null, p.get(0), p.get(1), true))
                .toList();
    }

    @Transactional
    public LeadershipPrincipleResponse create(LeadershipPrincipleRequest req) {
        String name = req.name().trim();
        if (principleRepo.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("A principle with this name already exists");
        }
        LeadershipPrinciple principle = LeadershipPrinciple.builder()
                .name(name)
                .description(trimToNull(req.description()))
                .active(req.active() == null || req.active())
                .build();
        return toResponse(principleRepo.save(principle));
    }

    @Transactional
    public LeadershipPrincipleResponse update(Long id, LeadershipPrincipleRequest req) {
        LeadershipPrinciple principle = getEntityOrThrow(id);
        String name = req.name().trim();
        if (!name.equalsIgnoreCase(principle.getName()) && principleRepo.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("A principle with this name already exists");
        }
        principle.setName(name);
        principle.setDescription(trimToNull(req.description()));
        if (req.active() != null) principle.setActive(req.active());
        return toResponse(principleRepo.save(principle));
    }

    /**
     * Principles already used in submitted reviews are deactivated instead of deleted so
     * historical results keep their principle names.
     */
    @Transactional
    public void delete(Long id) {
        LeadershipPrinciple principle = getEntityOrThrow(id);
        if (peerReviewRepo.existsByPrincipleId(id)) {
            principle.setActive(false);
            principleRepo.save(principle);
            return;
        }
        principleRepo.delete(principle);
    }

    /** Adds any default principle the org does not have yet (matched by name). */
    @Transactional
    public List<LeadershipPrincipleResponse> addDefaults() {
        DEFAULT_PRINCIPLES.stream()
                .filter(p -> !principleRepo.existsByNameIgnoreCase(p.get(0)))
                .map(p -> LeadershipPrinciple.builder().name(p.get(0)).description(p.get(1)).active(true).build())
                .forEach(principleRepo::save);
        return listAll();
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return principleRepo.countByActiveTrue();
    }

    private LeadershipPrinciple getEntityOrThrow(Long id) {
        return principleRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Principle not found"));
    }

    private LeadershipPrincipleResponse toResponse(LeadershipPrinciple p) {
        return new LeadershipPrincipleResponse(p.getId(), p.getName(), p.getDescription(), p.getActive());
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

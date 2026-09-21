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

/**
 * Platform-wide principles every organization's peer reviews are rated against. Managers and
 * employees only read them; the platform admin owns create/update/delete.
 */
@Service
public class LeadershipPrincipleService {

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
        if (peerReviewRepo.existsByPrincipleIdInAnyOrganization(id)) {
            principle.setActive(false);
            principleRepo.save(principle);
            return;
        }
        principleRepo.delete(principle);
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

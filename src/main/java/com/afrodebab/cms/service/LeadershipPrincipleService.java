package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.LeadershipPrincipleRequest;
import com.afrodebab.cms.dto.LeadershipPrincipleResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.LeadershipPrinciple;
import com.afrodebab.cms.jpa.repository.DefaultLeadershipPrincipleRepository;
import com.afrodebab.cms.jpa.repository.LeadershipPrincipleRepository;
import com.afrodebab.cms.jpa.repository.PeerReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Per-organization CRUD for the principles peer reviews are rated against. */
@Service
public class LeadershipPrincipleService {

    private final LeadershipPrincipleRepository principleRepo;
    private final PeerReviewRepository peerReviewRepo;
    private final DefaultLeadershipPrincipleRepository defaultRepo;

    public LeadershipPrincipleService(LeadershipPrincipleRepository principleRepo,
                                      PeerReviewRepository peerReviewRepo,
                                      DefaultLeadershipPrincipleRepository defaultRepo) {
        this.principleRepo = principleRepo;
        this.peerReviewRepo = peerReviewRepo;
        this.defaultRepo = defaultRepo;
    }

    @Transactional(readOnly = true)
    public List<LeadershipPrincipleResponse> listAll() {
        return principleRepo.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    /** The platform admin's active default set, offered to the org as a starting point. */
    @Transactional(readOnly = true)
    public List<LeadershipPrincipleResponse> listDefaults() {
        return defaultRepo.findAllByActiveTrueOrderByIdAsc().stream()
                .map(d -> new LeadershipPrincipleResponse(d.getId(), d.getName(), d.getDescription(), true))
                .toList();
    }

    /** Copies every active default the org does not have yet (matched by name). */
    @Transactional
    public List<LeadershipPrincipleResponse> addDefaults() {
        defaultRepo.findAllByActiveTrueOrderByIdAsc().stream()
                .filter(d -> !principleRepo.existsByNameIgnoreCase(d.getName()))
                .map(d -> LeadershipPrinciple.builder().name(d.getName()).description(d.getDescription()).active(true).build())
                .forEach(principleRepo::save);
        return listAll();
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

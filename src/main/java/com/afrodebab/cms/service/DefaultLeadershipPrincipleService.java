package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.LeadershipPrincipleRequest;
import com.afrodebab.cms.dto.LeadershipPrincipleResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.DefaultLeadershipPrinciple;
import com.afrodebab.cms.jpa.repository.DefaultLeadershipPrincipleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Platform-admin CRUD for the default principle set. Organizations copy the active ones into
 * their own principles, so edits here never change an org's existing principles or reviews.
 */
@Service
public class DefaultLeadershipPrincipleService {

    private final DefaultLeadershipPrincipleRepository defaultRepo;

    public DefaultLeadershipPrincipleService(DefaultLeadershipPrincipleRepository defaultRepo) {
        this.defaultRepo = defaultRepo;
    }

    @Transactional(readOnly = true)
    public List<LeadershipPrincipleResponse> listAll() {
        return defaultRepo.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public LeadershipPrincipleResponse create(LeadershipPrincipleRequest req) {
        String name = req.name().trim();
        if (defaultRepo.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("A principle with this name already exists");
        }
        DefaultLeadershipPrinciple principle = DefaultLeadershipPrinciple.builder()
                .name(name)
                .description(trimToNull(req.description()))
                .active(req.active() == null || req.active())
                .build();
        return toResponse(defaultRepo.save(principle));
    }

    @Transactional
    public LeadershipPrincipleResponse update(Long id, LeadershipPrincipleRequest req) {
        DefaultLeadershipPrinciple principle = getEntityOrThrow(id);
        String name = req.name().trim();
        if (!name.equalsIgnoreCase(principle.getName()) && defaultRepo.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("A principle with this name already exists");
        }
        principle.setName(name);
        principle.setDescription(trimToNull(req.description()));
        if (req.active() != null) principle.setActive(req.active());
        return toResponse(defaultRepo.save(principle));
    }

    @Transactional
    public void delete(Long id) {
        defaultRepo.delete(getEntityOrThrow(id));
    }

    private DefaultLeadershipPrinciple getEntityOrThrow(Long id) {
        return defaultRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Principle not found"));
    }

    private LeadershipPrincipleResponse toResponse(DefaultLeadershipPrinciple p) {
        return new LeadershipPrincipleResponse(p.getId(), p.getName(), p.getDescription(), p.getActive());
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

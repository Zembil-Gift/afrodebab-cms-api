package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.SubOrganizationCreateRequest;
import com.afrodebab.cms.dto.SubOrganizationRenameRequest;
import com.afrodebab.cms.dto.SubOrganizationResponse;
import com.afrodebab.cms.dto.SubOrganizationUpdateRequest;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Manager;
import com.afrodebab.cms.jpa.entity.SubOrganization;
import com.afrodebab.cms.jpa.repository.EmployeeRepository;
import com.afrodebab.cms.jpa.repository.ManagerRepository;
import com.afrodebab.cms.jpa.repository.SubOrganizationRepository;
import com.afrodebab.cms.util.SlugUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubOrganizationService {

    private final SubOrganizationRepository subOrganizationRepository;
    private final EmployeeRepository employeeRepository;
    private final ManagerRepository managerRepository;

    public SubOrganizationService(SubOrganizationRepository subOrganizationRepository,
                                  EmployeeRepository employeeRepository,
                                  ManagerRepository managerRepository) {
        this.subOrganizationRepository = subOrganizationRepository;
        this.employeeRepository = employeeRepository;
        this.managerRepository = managerRepository;
    }

    @Transactional(readOnly = true)
    public List<SubOrganizationResponse> listAll() {
        return subOrganizationRepository.findAllByOrderByCreatedAtAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubOrganizationResponse getById(Long id) {
        return toResponse(getEntityOrThrow(id));
    }

    @Transactional(readOnly = true)
    public SubOrganization getEntityOrThrow(Long id) {
        return subOrganizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sub-organization not found with id: " + id));
    }


    @Transactional(readOnly = true)
    public SubOrganization getDefaultSubOrganizationOrThrow() {
        return subOrganizationRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new NotFoundException("Default sub-organization not found for current organization"));
    }

    @Transactional
    public SubOrganizationResponse create(SubOrganizationCreateRequest req) {
        String trimmedName = req.name().trim();
        if (subOrganizationRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BadRequestException("Sub-organization name already exists: " + trimmedName);
        }

        String slug = req.slug() != null && !req.slug().isBlank()
                ? req.slug().trim().toLowerCase()
                : SlugUtil.toSlug(trimmedName);

        if (slug.isBlank()) {
            throw new BadRequestException("Slug could not be generated from name");
        }

        if (subOrganizationRepository.existsBySlugIgnoreCase(slug)) {
            throw new BadRequestException("Sub-organization slug already exists: " + slug);
        }

        SubOrganization subOrg = SubOrganization.builder()
                .name(trimmedName)
                .slug(slug)
                .isDefault(false)
                .latitude(req.latitude())
                .longitude(req.longitude())
                .geoRadiusM(req.geoRadiusM() != null ? req.geoRadiusM() : 200)
                .addressLabel(req.addressLabel() != null ? req.addressLabel().trim() : null)
                .entryTime(req.entryTime())
                .exitTime(req.exitTime())
                .lunchStartTime(req.lunchStartTime())
                .lunchEndTime(req.lunchEndTime())
                .graceMinutes(req.graceMinutes())
                .maxLunchBreakMinutes(req.maxLunchBreakMinutes())
                .build();

        return toResponse(subOrganizationRepository.save(subOrg));
    }

    @Transactional
    public SubOrganizationResponse update(Long id, SubOrganizationUpdateRequest req) {
        SubOrganization subOrg = getEntityOrThrow(id);

        if (req.name() != null && !req.name().isBlank()) {
            String trimmedName = req.name().trim();
            if (!trimmedName.equalsIgnoreCase(subOrg.getName()) && subOrganizationRepository.existsByNameIgnoreCase(trimmedName)) {
                throw new BadRequestException("Sub-organization name already exists: " + trimmedName);
            }
            subOrg.setName(trimmedName);
        }

        if (req.slug() != null && !req.slug().isBlank()) {
            String slug = req.slug().trim().toLowerCase();
            if (!slug.equalsIgnoreCase(subOrg.getSlug()) && subOrganizationRepository.existsBySlugIgnoreCase(slug)) {
                throw new BadRequestException("Sub-organization slug already exists: " + slug);
            }
            subOrg.setSlug(slug);
        }

        if (req.latitude() != null) {
            subOrg.setLatitude(req.latitude());
        }
        if (req.longitude() != null) {
            subOrg.setLongitude(req.longitude());
        }
        if (req.geoRadiusM() != null) {
            subOrg.setGeoRadiusM(req.geoRadiusM());
        }
        if (req.addressLabel() != null) {
            subOrg.setAddressLabel(req.addressLabel().trim().isEmpty() ? null : req.addressLabel().trim());
        }

        if (req.entryTime() != null) {
            subOrg.setEntryTime(req.entryTime());
        }
        if (req.exitTime() != null) {
            subOrg.setExitTime(req.exitTime());
        }
        if (req.lunchStartTime() != null) {
            subOrg.setLunchStartTime(req.lunchStartTime());
        }
        if (req.lunchEndTime() != null) {
            subOrg.setLunchEndTime(req.lunchEndTime());
        }
        if (req.graceMinutes() != null) {
            subOrg.setGraceMinutes(req.graceMinutes());
        }
        if (req.maxLunchBreakMinutes() != null) {
            subOrg.setMaxLunchBreakMinutes(req.maxLunchBreakMinutes());
        }

        return toResponse(subOrganizationRepository.save(subOrg));
    }

    @Transactional
    public SubOrganizationResponse rename(Long id, SubOrganizationRenameRequest req) {
        SubOrganization subOrg = getEntityOrThrow(id);
        String trimmedName = req.name().trim();
        if (!trimmedName.equalsIgnoreCase(subOrg.getName()) && subOrganizationRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BadRequestException("Sub-organization name already exists: " + trimmedName);
        }
        subOrg.setName(trimmedName);
        return toResponse(subOrganizationRepository.save(subOrg));
    }

    @Transactional
    public void delete(Long id) {
        SubOrganization subOrg = getEntityOrThrow(id);
        if (subOrg.isDefault()) {
            throw new BadRequestException("Cannot delete the default sub-organization");
        }

        long employeeCount = employeeRepository.countBySubOrganizationId(id);
        if (employeeCount > 0) {
            throw new BadRequestException("Cannot delete sub-organization with " + employeeCount + " assigned employee(s). Reassign them first.");
        }

        subOrganizationRepository.delete(subOrg);
    }

    @Transactional
    public SubOrganization createDefaultSubOrganization(String orgName) {
        SubOrganization subOrg = SubOrganization.builder()
                .name(orgName + " Main")
                .slug("main")
                .isDefault(true)
                .geoRadiusM(200)
                .build();
        return subOrganizationRepository.save(subOrg);
    }

    public SubOrganizationResponse toResponse(SubOrganization subOrg) {
        long employeeCount = employeeRepository.countBySubOrganizationId(subOrg.getId());
        long viceManagerCount = managerRepository.countBySubOrganizationIdAndRole(subOrg.getId(), Manager.ManagerRole.VICE_MANAGER);

        return new SubOrganizationResponse(
                subOrg.getId(),
                subOrg.getName(),
                subOrg.getSlug(),
                subOrg.isDefault(),
                subOrg.getLatitude(),
                subOrg.getLongitude(),
                subOrg.getGeoRadiusM(),
                subOrg.getAddressLabel(),
                subOrg.getEntryTime(),
                subOrg.getExitTime(),
                subOrg.getLunchStartTime(),
                subOrg.getLunchEndTime(),
                subOrg.getGraceMinutes(),
                subOrg.getMaxLunchBreakMinutes(),
                employeeCount,
                viceManagerCount,
                subOrg.getCreatedAt(),
                subOrg.getUpdatedAt()
        );
    }
}

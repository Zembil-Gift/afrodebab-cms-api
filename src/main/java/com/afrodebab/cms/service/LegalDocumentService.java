package com.afrodebab.cms.service;

import com.afrodebab.cms.dto.LegalDocumentRequest;
import com.afrodebab.cms.dto.LegalDocumentResponse;
import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.LegalDocument;
import com.afrodebab.cms.jpa.entity.LegalDocumentType;
import com.afrodebab.cms.jpa.repository.LegalDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Terms of Service and Privacy Policy: one document per type, edited by the platform admin. */
@Service
public class LegalDocumentService {

    private final LegalDocumentRepository legalRepo;

    public LegalDocumentService(LegalDocumentRepository legalRepo) {
        this.legalRepo = legalRepo;
    }

    @Transactional(readOnly = true)
    public List<LegalDocumentResponse> listAll() {
        return legalRepo.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LegalDocumentResponse get(String type) {
        return toResponse(getEntityOrThrow(parseType(type)));
    }

    /** Creates the document if it was deleted (or never existed), otherwise replaces it. */
    @Transactional
    public LegalDocumentResponse save(String type, LegalDocumentRequest req) {
        LegalDocumentType docType = parseType(type);
        LegalDocument doc = legalRepo.findByType(docType)
                .orElseGet(() -> LegalDocument.builder().type(docType).build());
        doc.setTitle(req.title().trim());
        doc.setContent(req.content().trim());
        return toResponse(legalRepo.save(doc));
    }

    @Transactional
    public void delete(String type) {
        legalRepo.delete(getEntityOrThrow(parseType(type)));
    }

    private LegalDocument getEntityOrThrow(LegalDocumentType type) {
        return legalRepo.findByType(type)
                .orElseThrow(() -> new NotFoundException("Document not found"));
    }

    private static LegalDocumentType parseType(String type) {
        try {
            return LegalDocumentType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("type must be 'terms' or 'privacy'");
        }
    }

    private LegalDocumentResponse toResponse(LegalDocument d) {
        return new LegalDocumentResponse(
                d.getType().name().toLowerCase(Locale.ROOT), d.getTitle(), d.getContent(), d.getUpdatedAt());
    }
}

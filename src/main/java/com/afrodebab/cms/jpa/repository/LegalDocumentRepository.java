package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.LegalDocument;
import com.afrodebab.cms.jpa.entity.LegalDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {
    Optional<LegalDocument> findByType(LegalDocumentType type);
    List<LegalDocument> findAllByOrderByIdAsc();
}

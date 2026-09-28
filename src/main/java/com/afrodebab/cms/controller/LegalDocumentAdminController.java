package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.LegalDocumentRequest;
import com.afrodebab.cms.dto.LegalDocumentResponse;
import com.afrodebab.cms.service.LegalDocumentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Platform Admin - Legal")
@RestController
@RequestMapping("/admin/legal")
public class LegalDocumentAdminController {

    private final LegalDocumentService service;

    public LegalDocumentAdminController(LegalDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<LegalDocumentResponse> list() {
        return service.listAll();
    }

    @PutMapping("/{type}")
    public LegalDocumentResponse save(@PathVariable String type, @Valid @RequestBody LegalDocumentRequest req) {
        return service.save(type, req);
    }

    @DeleteMapping("/{type}")
    public void delete(@PathVariable String type) {
        service.delete(type);
    }
}

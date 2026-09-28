package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.LegalDocumentResponse;
import com.afrodebab.cms.service.LegalDocumentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public - Legal")
@RestController
@RequestMapping("/legal")
public class LegalDocumentPublicController {

    private final LegalDocumentService service;

    public LegalDocumentPublicController(LegalDocumentService service) {
        this.service = service;
    }

    @GetMapping("/{type}")
    public LegalDocumentResponse get(@PathVariable String type) {
        return service.get(type);
    }
}

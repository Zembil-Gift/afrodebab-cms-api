package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.BlogAdminResponse;
import com.afrodebab.cms.dto.BlogCreateRequest;
import com.afrodebab.cms.dto.BlogUpdateRequest;
import com.afrodebab.cms.service.BlogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - Blogs")
@RestController
@RequestMapping("/admin/blogs")
public class BlogAdminController {

    private final BlogService service;

    public BlogAdminController(BlogService service) { this.service = service; }

    @PostMapping
    public BlogAdminResponse create(@Valid @RequestBody BlogCreateRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public BlogAdminResponse update(@PathVariable Long id, @RequestBody BlogUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}


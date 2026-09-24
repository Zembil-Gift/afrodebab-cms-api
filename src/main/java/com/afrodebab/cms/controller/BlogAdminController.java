package com.afrodebab.cms.controller;

import com.afrodebab.cms.dto.BlogAdminResponse;
import com.afrodebab.cms.dto.BlogCreateRequest;
import com.afrodebab.cms.dto.BlogUpdateRequest;
import com.afrodebab.cms.dto.ImageUploadResponse;
import com.afrodebab.cms.service.CloudflareR2Service;
import com.afrodebab.cms.service.BlogService;
import com.afrodebab.cms.tenant.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin - Blogs")
@RestController
@RequestMapping("/manager/blogs")
public class BlogAdminController {

    private final BlogService service;
    private final CloudflareR2Service r2Service;

    public BlogAdminController(BlogService service, CloudflareR2Service r2Service) {
        this.service = service;
        this.r2Service = r2Service;
    }

    // Uploads a cover image to R2 and returns its URL; the client then saves it as coverImageUrl.
    @PostMapping(value = "/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadResponse uploadCoverImage(@RequestParam("file") MultipartFile file) {
        return new ImageUploadResponse(r2Service.uploadOrgContentImage(TenantContext.get(), "blogs", file));
    }

    // Tenant-scoped: returns only the logged-in manager's org blogs (all statuses).
    @GetMapping
    public Page<BlogAdminResponse> list(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(defaultValue = "createdAt") String sortBy,
                                        @RequestParam(defaultValue = "desc") String direction) {
        var dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return service.listAllAdmin(PageRequest.of(page, size, Sort.by(dir, sortBy)));
    }

    @GetMapping("/{id}")
    public BlogAdminResponse get(@PathVariable Long id) { return service.getAdmin(id); }

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


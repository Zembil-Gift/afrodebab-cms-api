package com.afrodebab.cms.controller;


import com.afrodebab.cms.dto.BlogPublicResponse;
import com.afrodebab.cms.service.BlogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Public - Blogs")
@RestController
@RequestMapping("/blogs")
public class BlogPublicController {

    private final BlogService service;

    public BlogPublicController(BlogService service) { this.service = service; }

    @GetMapping
    public Page<BlogPublicResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        var dir = "asc".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        return service.listPublished(pageable);
    }

    @GetMapping("/{slug}")
    public BlogPublicResponse getBySlug(@PathVariable String slug) {
        return service.getPublishedBySlug(slug);
    }
}


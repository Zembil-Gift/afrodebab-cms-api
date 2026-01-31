package com.afrodebab.cms.controller;


import com.afrodebab.cms.dto.JobResponse;
import com.afrodebab.cms.service.JobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Public - Job")
@RestController
@RequestMapping("/jobs")
public class JobPublicController {
    private final JobService service;

    public JobPublicController(JobService service) {
        this.service = service;
    }

    @GetMapping
    public Page<JobResponse> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(defaultValue = "createdAt") String sortBy,
                                  @RequestParam(defaultValue = "desc") String direction
    ) {
        var dir = "asc".equalsIgnoreCase(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        var pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        return service.listOpen(pageable);
    }

    @GetMapping("/{slug}")
    public JobResponse get(@PathVariable String slug) {
        return service.getBySlugPublic(slug);
    }
}


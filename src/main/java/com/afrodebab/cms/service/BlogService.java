package com.afrodebab.cms.service;


import com.afrodebab.cms.dto.BlogAdminResponse;
import com.afrodebab.cms.dto.BlogCreateRequest;
import com.afrodebab.cms.dto.BlogPublicResponse;
import com.afrodebab.cms.dto.BlogUpdateRequest;
import com.afrodebab.cms.exception.NotFoundException;
import com.afrodebab.cms.jpa.entity.Blog;
import com.afrodebab.cms.jpa.repository.BlogRepository;
import com.afrodebab.cms.util.SlugUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class BlogService {

    private final BlogRepository repo;

    public BlogService(BlogRepository repo) {
        this.repo = repo;
    }

    // PUBLIC
    @Transactional(readOnly = true)
    public Page<BlogPublicResponse> listPublished(Pageable pageable) {
        return repo.findAllByStatus(Blog.Status.PUBLISHED, pageable)
                .map(this::toPublic);
    }

    @Transactional(readOnly = true)
    public BlogPublicResponse getPublishedBySlug(String slug) {
        Blog blog = repo.findBySlugAndStatus(slug, Blog.Status.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Blog not found"));
        return toPublic(blog);
    }

    // ADMIN
    public BlogAdminResponse create(BlogCreateRequest req) {
        Blog b = new Blog();
        b.setTitle(req.title());
        b.setExcerpt(req.excerpt());
        b.setContent(req.content());
        b.setCoverImageUrl(req.coverImageUrl());

        Blog.Status status = (req.status() == null) ? Blog.Status.DRAFT : req.status();
        b.setStatus(status);

        String baseSlug = (req.slug() != null && !req.slug().isBlank())
                ? SlugUtil.toSlug(req.slug())
                : SlugUtil.toSlug(req.title());

        b.setSlug(uniqueSlug(baseSlug));

        if (status == Blog.Status.PUBLISHED) b.setPublishedAt(Instant.now());

        repo.save(b);
        return toAdmin(b);
    }

    public BlogAdminResponse update(Long id, BlogUpdateRequest req) {
        Blog b = repo.findById(id).orElseThrow(() -> new NotFoundException("Blog not found"));

        if (req.title() != null) b.setTitle(req.title());
        if (req.excerpt() != null) b.setExcerpt(req.excerpt());
        if (req.content() != null) b.setContent(req.content());
        if (req.coverImageUrl() != null) b.setCoverImageUrl(req.coverImageUrl());

        if (req.slug() != null) {
            String s = uniqueSlug(SlugUtil.toSlug(req.slug()));
            b.setSlug(s);
        }

        if (req.status() != null && req.status() != b.getStatus()) {
            b.setStatus(req.status());
            if (req.status() == Blog.Status.PUBLISHED && b.getPublishedAt() == null) {
                b.setPublishedAt(Instant.now());
            }
            if (req.status() == Blog.Status.DRAFT) {
                b.setPublishedAt(null);
            }
        }

        repo.save(b);
        return toAdmin(b);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new NotFoundException("Blog not found");
        repo.deleteById(id);
    }

    private String uniqueSlug(String base) {
        String slug = base;
        int i = 2;
        while (repo.existsBySlug(slug)) {
            slug = base + "-" + i;
            i++;
        }
        return slug;
    }

    private BlogPublicResponse toPublic(Blog b) {
        return new BlogPublicResponse(
                b.getId(), b.getTitle(), b.getSlug(), b.getExcerpt(),
                b.getContent(), b.getCoverImageUrl(), b.getPublishedAt()
        );
    }

    private BlogAdminResponse toAdmin(Blog b) {
        return new BlogAdminResponse(
                b.getId(), b.getTitle(), b.getSlug(), b.getExcerpt(),
                b.getContent(), b.getCoverImageUrl(), b.getStatus(), b.getPublishedAt()
        );
    }
}


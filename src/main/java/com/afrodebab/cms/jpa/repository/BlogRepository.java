package com.afrodebab.cms.jpa.repository;


import com.afrodebab.cms.jpa.entity.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlogRepository extends JpaRepository<Blog, Long> {
    Optional<Blog> findBySlug(String slug);
    Optional<Blog> findBySlugAndStatus(String slug, Blog.Status status);
    Page<Blog> findAllByStatus(Blog.Status status, Pageable pageable);
    boolean existsBySlug(String slug);
}
package com.security.forecsic.repositery.jpa;

import com.security.forecsic.model.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, String>, JpaSpecificationExecutor<Blog> {

    Optional<Blog> findBySlug(String slug);

    Optional<Blog> findByIdOrSlug(String id, String slug);

    boolean existsBySlug(String slug);

    Page<Blog> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<Blog> findByStatusAndCategoryIgnoreCaseOrderByCreatedAtDesc(String status, String category, Pageable pageable);

    List<Blog> findByStatusOrderByCreatedAtDesc(String status);

    List<Blog> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE Blog b SET b.views = COALESCE(b.views, 0) + 1 WHERE b.id = :id")
    int incrementViewsById(@Param("id") String id);

    @Modifying
    @Query("UPDATE Blog b SET b.views = COALESCE(b.views, 0) + 1 WHERE b.slug = :slug")
    int incrementViewsBySlug(@Param("slug") String slug);
}

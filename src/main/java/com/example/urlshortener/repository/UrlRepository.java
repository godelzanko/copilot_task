package com.example.urlshortener.repository;

import com.example.urlshortener.entity.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for UrlEntity.
 * Provides CRUD operations and custom query methods for URL persistence.
 */
@Repository
public interface UrlRepository extends JpaRepository<UrlEntity, String> {
    
    /**
     * Finds a URL entity by its normalized original URL.
     * Normalization uses LOWER(TRIM()) to match the database index for idempotency.
     * 
     * Used for idempotency: check if a URL has already been shortened.
     * 
     * @param url the URL to search for (will be normalized with LOWER(TRIM()))
     * @return Optional containing the UrlEntity if found, empty otherwise
     */
    @Query("SELECT u FROM UrlEntity u WHERE LOWER(TRIM(u.originalUrl)) = LOWER(TRIM(:url))")
    Optional<UrlEntity> findByNormalizedUrl(@Param("url") String url);
}

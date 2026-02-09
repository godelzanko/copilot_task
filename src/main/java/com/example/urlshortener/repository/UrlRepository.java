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
     * URLs are normalized at the application level before storage.
     * 
     * Used for idempotency: check if a URL has already been shortened.
     * 
     * @param url the normalized original URL to search for
     * @return Optional containing the UrlEntity if found, empty otherwise
     */
    @Query("SELECT u FROM UrlEntity u WHERE u.originalUrl = :url")
    Optional<UrlEntity> findByNormalizedUrl(@Param("url") String url);
}

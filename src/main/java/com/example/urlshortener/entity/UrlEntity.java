package com.example.urlshortener.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * JPA Entity representing a URL mapping in the database.
 * Maps to the 'urls' table with columns: short_code (PK), original_url, created_at.
 */
@Entity
@Table(name = "urls")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlEntity {
    
    /**
     * Primary key: The shortened URL code (Base62-encoded Snowflake ID).
     * Not auto-generated - must be set manually before persisting.
     */
    @Id
    @Column(name = "short_code", length = 10)
    private String shortCode;
    
    /**
     * The original (long) URL to redirect to.
     * Stored as TEXT in database (unlimited length).
     */
    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;
    
    /**
     * Timestamp when the URL mapping was created.
     * Automatically populated by Hibernate before INSERT.
     * Immutable after creation (updatable = false).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}

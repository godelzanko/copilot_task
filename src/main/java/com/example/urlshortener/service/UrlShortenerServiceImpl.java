package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.entity.UrlEntity;
import com.example.urlshortener.exception.ShortCodeNotFoundException;
import com.example.urlshortener.generator.SnowflakeIdGenerator;
import com.example.urlshortener.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * implementation of UrlShortenerService with database-enforced idempotency.
 * 
 * <p>This service ensures the same URL always returns the same short code, even under
 * concurrent requests, using a try-insert-catch-select pattern:
 * <ol>
 *   <li>Normalize URL (trim + lowercase)</li>
 *   <li>Try to insert new mapping with generated short code</li>
 *   <li>If constraint violation (URL exists), catch and query for existing mapping</li>
 *   <li>Return short code (new or existing)</li>
 * </ol>
 * 
 * <p>The database UNIQUE constraint on original_url ensures atomicity.
 */
@Service
@Primary
public class UrlShortenerServiceImpl implements UrlShortenerService {
    
    private static final Logger log = LoggerFactory.getLogger(UrlShortenerServiceImpl.class);
    
    private final UrlRepository urlRepository;
    private final SnowflakeIdGenerator generator;
    private final String baseUrl;
    
    public UrlShortenerServiceImpl(UrlRepository urlRepository, 
                                  SnowflakeIdGenerator generator,
                                  @Value("${app.base-url}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.generator = generator;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
    
    /**
     * Shortens a URL with database-enforced idempotency.
     * 
     * <p>Same URL (after normalization) always returns the same short code.
     * Thread-safe under concurrent requests via database UNIQUE constraint.
     * 
     * @param url the original URL to shorten
     * @return ShortenResponse with short code and full short URL
     */
    @Override
    public ShortenResponse shortenUrl(String url) {
        // Step 1: Normalize URL (trim whitespace + lowercase)
        String normalizedUrl = normalizeUrl(url);
        
        log.debug("Shortening URL: original='{}', normalized='{}'", url, normalizedUrl);
        
        try {
            // Get the proxied instance to ensure transaction boundaries work correctly
            UrlShortenerServiceImpl proxy = (UrlShortenerServiceImpl) AopContext.currentProxy();
            return proxy.tryInsert(normalizedUrl);
        } catch (DataIntegrityViolationException e) {
            // Step 3: Constraint violation - URL already exists (idempotency hit)
            log.info("Idempotency hit for URL: '{}'. Retrieving existing mapping.", normalizedUrl);
            UrlShortenerServiceImpl proxy = (UrlShortenerServiceImpl) AopContext.currentProxy();
            return proxy.findExisting(normalizedUrl);
        } catch (IllegalStateException e) {
            // Not in AOP proxy context (e.g., unit tests) - call directly
            return tryInsertDirect(normalizedUrl);
        }
    }
    
    /**
     * Direct implementation without proxy for unit tests.
     */
    private ShortenResponse tryInsertDirect(String normalizedUrl) {
        try {
            return tryInsert(normalizedUrl);
        } catch (DataIntegrityViolationException e) {
            log.info("Idempotency hit for URL: '{}'. Retrieving existing mapping.", normalizedUrl);
            return findExisting(normalizedUrl);
        }
    }
    
    /**
     * Attempts to insert a new URL mapping.
     * 
     * @param normalizedUrl the normalized URL
     * @return ShortenResponse if insert succeeds
     * @throws DataIntegrityViolationException if URL already exists
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ShortenResponse tryInsert(String normalizedUrl) {
        String shortCode = generator.generateShortCode();
        UrlEntity entity = new UrlEntity(shortCode, normalizedUrl, null);
        urlRepository.save(entity);
        urlRepository.flush(); // Force immediate flush to trigger constraint violation
        
        log.info("Created new short code: '{}' -> '{}'", shortCode, normalizedUrl);
        return toDto(entity);
    }
    
    /**
     * Finds an existing URL mapping.
     * Executed in a new transaction after constraint violation.
     * 
     * @param normalizedUrl the normalized URL
     * @return ShortenResponse with existing mapping
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public ShortenResponse findExisting(String normalizedUrl) {
        UrlEntity existing = urlRepository.findByNormalizedUrl(normalizedUrl)
                .orElseThrow(() -> new IllegalStateException(
                        "Constraint violation occurred but no existing mapping found for: " + normalizedUrl));
        
        log.info("Returned existing short code: '{}' -> '{}'", existing.getShortCode(), normalizedUrl);
        return toDto(existing);
    }
    
    /**
     * Retrieves the original URL for a given short code.
     * 
     * @param shortCode the short code to look up
     * @return the original URL
     * @throws ShortCodeNotFoundException if short code doesn't exist
     */
    @Override
    public String getOriginalUrl(String shortCode) {
        return urlRepository.findById(shortCode)
                .map(UrlEntity::getOriginalUrl)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
    }
    
    /**
     * Normalizes a URL for consistent storage and lookup.
     * 
     * <p>Normalization rules per RFC 3986:
     * <ol>
     *   <li>Trim whitespace</li>
     *   <li>Convert scheme and host to lowercase (case-insensitive)</li>
     *   <li>Preserve case in path, query, and fragment (case-sensitive)</li>
     * </ol>
     * 
     * @param url the URL to normalize
     * @return the normalized URL
     */
    String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        
        String trimmed = url.trim();
        try {
            java.net.URI uri = new java.net.URI(trimmed);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : null;
            String authority = uri.getAuthority() != null ? uri.getAuthority().toLowerCase() : null;
            
            return new java.net.URI(scheme, authority, uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
        } catch (java.net.URISyntaxException e) {
            return trimmed;
        }
    }
    
    /**
     * Converts UrlEntity to ShortenResponse DTO.
     * 
     * @param entity the entity to convert
     * @return the DTO
     */
    private ShortenResponse toDto(UrlEntity entity) {
        String shortUrl = baseUrl + "/" + entity.getShortCode();
        return new ShortenResponse(entity.getShortCode(), shortUrl);
    }
}

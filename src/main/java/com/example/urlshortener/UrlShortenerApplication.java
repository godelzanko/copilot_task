package com.example.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * URL Shortener Service - Main Application Entry Point
 * 
 * A minimalist URL shortener REST API demonstrating:
 * - Snowflake ID generation with Base62 encoding
 * - Database-enforced idempotency using PostgreSQL constraints
 * - RESTful API design patterns
 * - Docker-based containerized deployment
 * 
 * Core Philosophy: "HashMap-via-REST" - A persistent key-value store
 * exposed through HTTP interface.
 * 
 * @author Slavaz
 * @version 1.0.0
 * @since 2026-02-06
 */
@SpringBootApplication
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}

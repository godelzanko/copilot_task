package com.example.urlshortener.exception;

/**
 * Exception thrown when the system clock moves backwards.
 * 
 * <p>This typically occurs during NTP adjustments, daylight saving time changes,
 * or manual system clock modifications. When detected, ID generation is halted
 * to prevent generating potentially invalid or duplicate IDs.
 * 
 * <p>The exception includes both the previous timestamp and the current (backwards)
 * timestamp to aid in debugging and monitoring.
 * 
 * @see com.example.urlshortener.generator.SnowflakeId
 */
public class ClockMovedBackwardsException extends RuntimeException {
    
    /**
     * Constructs a new ClockMovedBackwardsException.
     * 
     * @param lastTimestamp the previous timestamp in milliseconds since custom epoch
     * @param currentTimestamp the current (backwards) timestamp in milliseconds since custom epoch
     */
    public ClockMovedBackwardsException(long lastTimestamp, long currentTimestamp) {
        super(String.format("Clock moved backwards. Refusing to generate id for %dms",
                lastTimestamp - currentTimestamp));
    }
}

package com.logcollector.collector.position;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Tracks the last read position for each server/log file combination.
 *
 * Enables incremental log collection by storing line positions in Redis.
 * Positions are persisted to allow resuming collection after restarts.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CollectionPositionTracker {

    private static final String POSITION_KEY_PREFIX = "log-collection:position:";
    private static final Duration POSITION_TTL = Duration.ofDays(30);

    private final RedisTemplate<String, Long> redisTemplate;

    /**
     * Gets the last collected line position for a server/file.
     *
     * @param serverId server ID
     * @param logFilePath log file path
     * @return last read line number (1-based), or empty if never collected
     */
    public Optional<Long> getLastPosition(Long serverId, String logFilePath) {
        String key = buildKey(serverId, logFilePath);
        Long position = redisTemplate.opsForValue().get(key);

        if (position != null) {
            log.debug("Retrieved position {} for server {} file {}", position, serverId, logFilePath);
        } else {
            log.debug("No previous position found for server {} file {}", serverId, logFilePath);
        }

        return Optional.ofNullable(position);
    }

    /**
     * Updates the last collected line position for a server/file.
     *
     * @param serverId server ID
     * @param logFilePath log file path
     * @param lineNumber last read line number (1-based)
     */
    public void updatePosition(Long serverId, String logFilePath, long lineNumber) {
        String key = buildKey(serverId, logFilePath);
        redisTemplate.opsForValue().set(key, lineNumber, POSITION_TTL);

        log.debug("Updated position to {} for server {} file {}", lineNumber, serverId, logFilePath);
    }

    /**
     * Increments the position by the number of lines collected.
     *
     * @param serverId server ID
     * @param logFilePath log file path
     * @param linesCollected number of lines just collected
     * @return new position after increment
     */
    public long incrementPosition(Long serverId, String logFilePath, int linesCollected) {
        String key = buildKey(serverId, logFilePath);
        Long newPosition = redisTemplate.opsForValue().increment(key, linesCollected);

        if (newPosition == null) {
            newPosition = (long) linesCollected;
            redisTemplate.opsForValue().set(key, newPosition, POSITION_TTL);
        }

        // Refresh TTL after increment
        redisTemplate.expire(key, POSITION_TTL);

        log.debug("Incremented position by {} to {} for server {} file {}",
                linesCollected, newPosition, serverId, logFilePath);

        return newPosition;
    }

    /**
     * Resets the position for a server/file (starts from beginning).
     *
     * @param serverId server ID
     * @param logFilePath log file path
     */
    public void resetPosition(Long serverId, String logFilePath) {
        String key = buildKey(serverId, logFilePath);
        redisTemplate.delete(key);

        log.info("Reset position for server {} file {}", serverId, logFilePath);
    }

    /**
     * Checks if a position exists for a server/file.
     *
     * @param serverId server ID
     * @param logFilePath log file path
     * @return true if position exists
     */
    public boolean hasPosition(Long serverId, String logFilePath) {
        String key = buildKey(serverId, logFilePath);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String buildKey(Long serverId, String logFilePath) {
        // Sanitize file path for Redis key (replace slashes with colons)
        String sanitizedPath = logFilePath.replace("/", ":").replace("\\", ":");
        return POSITION_KEY_PREFIX + serverId + ":" + sanitizedPath;
    }
}

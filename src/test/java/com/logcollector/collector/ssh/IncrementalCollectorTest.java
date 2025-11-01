package com.logcollector.collector.ssh;

import com.logcollector.collector.position.CollectionPositionTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for incremental log collection logic.
 *
 * Tests position tracking and incremental reading.
 */
@ExtendWith(MockitoExtension.class)
class IncrementalCollectorTest {

    @Mock
    private RedisTemplate<String, Long> redisTemplate;

    @Mock
    private ValueOperations<String, Long> valueOperations;

    private CollectionPositionTracker collector;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        collector = new CollectionPositionTracker(redisTemplate);
    }

    @Test
    void testTrackLastPosition() {
        // Test that we correctly track the last read position for each server/file
        // Given
        Long serverId = 1L;
        String filePath = "/var/log/app.log";
        long lastPosition = 12345L;

        // When
        collector.updatePosition(serverId, filePath, lastPosition);

        // Then - verify Redis was called
        verify(valueOperations).set(anyString(), eq(lastPosition), any(Duration.class));
    }

    @Test
    void testGetLastPosition() {
        // Test retrieving last position
        // Given
        Long serverId = 1L;
        String filePath = "/var/log/app.log";
        long expectedPosition = 5000L;
        when(valueOperations.get(anyString())).thenReturn(expectedPosition);

        // When
        var position = collector.getLastPosition(serverId, filePath);

        // Then
        assertTrue(position.isPresent());
        assertEquals(expectedPosition, position.get());
    }

    @Test
    void testIncrementalPositionUpdate() {
        // Test incrementing position by lines collected
        // Given
        Long serverId = 1L;
        String filePath = "/var/log/app.log";
        int linesCollected = 100;
        when(valueOperations.increment(anyString(), anyLong())).thenReturn(100L);

        // When
        long newPosition = collector.incrementPosition(serverId, filePath, linesCollected);

        // Then
        assertEquals(100L, newPosition);
        verify(valueOperations).increment(anyString(), eq((long) linesCollected));
    }

    @Test
    void testResetPosition() {
        // Test resetting position (e.g., after file rotation)
        // Given
        Long serverId = 1L;
        String filePath = "/var/log/app.log";

        // When
        collector.resetPosition(serverId, filePath);

        // Then - verify Redis delete was called
        verify(redisTemplate).delete(anyString());
    }
}

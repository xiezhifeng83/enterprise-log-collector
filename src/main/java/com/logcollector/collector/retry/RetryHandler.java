package com.logcollector.collector.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Handles retry logic with exponential backoff for failed operations.
 *
 * Provides configurable retry attempts and backoff strategy.
 */
@Component
@Slf4j
public class RetryHandler {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_INITIAL_DELAY_MS = 1000;
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    private static final long DEFAULT_MAX_DELAY_MS = 30000;

    /**
     * Executes an operation with retry logic and exponential backoff.
     *
     * @param operation operation to execute
     * @param maxAttempts maximum number of retry attempts
     * @param operationName name of operation for logging
     * @param <T> return type
     * @return result of successful operation
     * @throws RetryExhaustedException if all retry attempts fail
     */
    public <T> T executeWithRetry(Supplier<T> operation, int maxAttempts, String operationName) {
        return executeWithRetry(
                operation,
                maxAttempts,
                DEFAULT_INITIAL_DELAY_MS,
                DEFAULT_BACKOFF_MULTIPLIER,
                DEFAULT_MAX_DELAY_MS,
                operationName
        );
    }

    /**
     * Executes an operation with retry logic using default max attempts.
     *
     * @param operation operation to execute
     * @param operationName name of operation for logging
     * @param <T> return type
     * @return result of successful operation
     */
    public <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        return executeWithRetry(operation, DEFAULT_MAX_ATTEMPTS, operationName);
    }

    /**
     * Executes an operation with full retry configuration.
     *
     * @param operation operation to execute
     * @param maxAttempts maximum number of retry attempts
     * @param initialDelayMs initial delay before first retry
     * @param backoffMultiplier multiplier for exponential backoff
     * @param maxDelayMs maximum delay between retries
     * @param operationName name of operation for logging
     * @param <T> return type
     * @return result of successful operation
     */
    public <T> T executeWithRetry(
            Supplier<T> operation,
            int maxAttempts,
            long initialDelayMs,
            double backoffMultiplier,
            long maxDelayMs,
            String operationName) {

        Exception lastException = null;
        long delay = initialDelayMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("Executing {} (attempt {}/{})", operationName, attempt, maxAttempts);
                return operation.get();

            } catch (Exception e) {
                lastException = e;

                if (attempt < maxAttempts) {
                    log.warn("Operation {} failed (attempt {}/{}), retrying in {}ms: {}",
                            operationName, attempt, maxAttempts, delay, e.getMessage());

                    sleep(delay);

                    // Calculate next delay with exponential backoff
                    delay = Math.min((long) (delay * backoffMultiplier), maxDelayMs);
                } else {
                    log.error("Operation {} failed after {} attempts", operationName, maxAttempts, e);
                }
            }
        }

        throw new RetryExhaustedException(
                String.format("Operation %s failed after %d attempts", operationName, maxAttempts),
                lastException
        );
    }

    /**
     * Executes a void operation with retry logic.
     *
     * @param operation operation to execute
     * @param maxAttempts maximum number of retry attempts
     * @param operationName name of operation for logging
     */
    public void executeWithRetryVoid(Runnable operation, int maxAttempts, String operationName) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, maxAttempts, operationName);
    }

    /**
     * Executes a void operation with retry logic using default max attempts.
     *
     * @param operation operation to execute
     * @param operationName name of operation for logging
     */
    public void executeWithRetryVoid(Runnable operation, String operationName) {
        executeWithRetryVoid(operation, DEFAULT_MAX_ATTEMPTS, operationName);
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry sleep interrupted", e);
        }
    }

    /**
     * Exception thrown when all retry attempts are exhausted.
     */
    public static class RetryExhaustedException extends RuntimeException {
        public RetryExhaustedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

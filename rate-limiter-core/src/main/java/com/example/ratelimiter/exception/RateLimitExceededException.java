package com.example.ratelimiter.exception;

/**
 * Thrown by RateLimitAspect when a caller has exhausted their token bucket.
 * Deliberately a plain domain exception with no HTTP/JSON knowledge - the
 * translation to an HTTP 429 response happens in GlobalExceptionHandler,
 * keeping the aspect and this class focused purely on the rate-limit domain.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Rate limit exceeded, retry after " + retryAfterSeconds + "s");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

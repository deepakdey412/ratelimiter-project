package com.example.ratelimiter.limiter;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for TokenBucket - no Spring context involved at all, so
 * these run in milliseconds and test only the algorithm itself.
 */
class TokenBucketTest {

    @Test
    void allowsBurstUpToLimitThenDeniesTheNextRequest() {
        // A long window relative to the test's runtime means refill during
        // the test is negligible, so we're purely testing "start full, then
        // drain to zero after `capacity` consumes".
        TokenBucket bucket = new TokenBucket(3, Duration.ofMinutes(1).toNanos());

        assertTrue(bucket.tryConsume().isAllowed(), "1st request should be allowed (bucket starts full)");
        assertTrue(bucket.tryConsume().isAllowed(), "2nd request should be allowed");
        assertTrue(bucket.tryConsume().isAllowed(), "3rd request should be allowed (capacity reached)");

        RateLimitResult fourth = bucket.tryConsume();
        assertFalse(fourth.isAllowed(), "4th request should be denied - bucket is exhausted");
        assertTrue(fourth.getRetryAfterSeconds() > 0, "a denied result should suggest a positive retry-after");
    }

    @Test
    void refillsAtLeastOneTokenAfterTheFullWindowElapses() throws InterruptedException {
        // Small capacity + short window keeps this test fast while still
        // exercising real wall-clock refill math (no time-mocking needed).
        TokenBucket bucket = new TokenBucket(1, Duration.ofMillis(200).toNanos());

        assertTrue(bucket.tryConsume().isAllowed(), "first request consumes the only token");
        assertFalse(bucket.tryConsume().isAllowed(), "immediate second request should be denied");

        Thread.sleep(250); // sleep past the 200ms window to guarantee a full refill

        assertTrue(bucket.tryConsume().isAllowed(), "token should have refilled after the window elapsed");
    }

    @Test
    void deniedResultReportsARetryAfterBoundedByTheWindow() {
        // Capacity 1, 10s window: right after the single token is consumed,
        // the next token is at most ~10s away and at least 1s away.
        TokenBucket bucket = new TokenBucket(1, Duration.ofSeconds(10).toNanos());

        assertTrue(bucket.tryConsume().isAllowed());
        RateLimitResult denied = bucket.tryConsume();

        assertFalse(denied.isAllowed());
        assertTrue(denied.getRetryAfterSeconds() >= 1 && denied.getRetryAfterSeconds() <= 10,
                "retryAfterSeconds should be a sane estimate within the configured window");
    }
}

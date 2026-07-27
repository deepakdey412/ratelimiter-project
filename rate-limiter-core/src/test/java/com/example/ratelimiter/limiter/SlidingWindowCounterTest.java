package com.example.ratelimiter.limiter;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for SlidingWindowCounter - mirrors TokenBucketTest's
 * style of testing the low-level algorithm class directly (bypassing the
 * ConcurrentHashMap-per-key wrapper in SlidingWindowRateLimiter).
 *
 * These tests use the package-private constructor that accepts a
 * controllable clock (an AtomicLong standing in for System.nanoTime())
 * instead of Thread.sleep(), so the window-boundary test below is
 * deterministic rather than depending on real wall-clock timing.
 */
class SlidingWindowCounterTest {

    private static final long ONE_SECOND_NANOS = 1_000_000_000L;

    @Test
    void allowsUpToLimitWithinASingleWindowThenDeniesTheNext() {
        AtomicLong clock = new AtomicLong(0L);
        SlidingWindowCounter counter = new SlidingWindowCounter(5, ONE_SECOND_NANOS, clock::get);

        for (int i = 1; i <= 5; i++) {
            assertTrue(counter.tryConsume().isAllowed(), "request " + i + " of 5 should be allowed");
        }

        RateLimitResult sixth = counter.tryConsume();
        assertFalse(sixth.isAllowed(), "6th request in the same window should be denied");
        assertTrue(sixth.getRetryAfterSeconds() > 0);
    }

    @Test
    void deniesAsSoonAsTheWeightedEstimateReachesTheLimitEvenMidWindow() {
        // Start halfway through a window - the counter should behave
        // correctly regardless of when within a window it first sees traffic.
        AtomicLong clock = new AtomicLong(500_000_000L);
        SlidingWindowCounter counter = new SlidingWindowCounter(3, ONE_SECOND_NANOS, clock::get);

        assertTrue(counter.tryConsume().isAllowed());
        assertTrue(counter.tryConsume().isAllowed());
        assertTrue(counter.tryConsume().isAllowed());

        assertFalse(counter.tryConsume().isAllowed(), "4th request should exceed the limit of 3");
    }

    @Test
    void doesNotAllowADoubleBurstAcrossAWindowBoundary() {
        // limit = 10 per 1s window. A naive FIXED-WINDOW counter has a
        // well-known flaw: 10 requests right at the end of window 0, then
        // 10 more right at the start of window 1, gives 20 requests inside
        // ~20ms of real time - a 2x burst over the configured limit. This
        // test proves the sliding-window-counter approach does NOT allow
        // that, because it keeps weighting the previous window's traffic
        // as time moves into the new one.
        AtomicLong clock = new AtomicLong();
        SlidingWindowCounter counter = new SlidingWindowCounter(10, ONE_SECOND_NANOS, clock::get);

        // Burst 10 requests right at the END of window 0 (t = 0.99s).
        clock.set(990_000_000L);
        for (int i = 1; i <= 10; i++) {
            assertTrue(counter.tryConsume().isAllowed(), "burst request " + i + " in window 0 should be allowed");
        }
        assertFalse(counter.tryConsume().isAllowed(), "window 0 is already at its limit of 10");

        // Cross into window 1 - only 20ms of real time later (t = 1.01s).
        clock.set(1_010_000_000L);

        // A naive fixed-window counter would reset its count to 0 here and
        // instantly allow another 10 requests. The sliding window counter
        // must not: window 0's 10 requests still carry ~99% weight this
        // early into window 1, so the very next request should be denied.
        RateLimitResult firstRequestJustAfterBoundary = counter.tryConsume();
        assertFalse(firstRequestJustAfterBoundary.isAllowed(),
                "must NOT immediately allow a fresh burst just after crossing the window boundary");

        // Move to the middle of window 1 (t = 1.5s). Previous window's
        // weight is now exactly 50%, so the estimated count from window 0's
        // carry-over is 10 * 0.5 = 5, leaving room for exactly 5 more
        // requests before the limit of 10 is hit again.
        clock.set(1_500_000_000L);
        int allowedHalfwayThroughWindow1 = 0;
        for (int i = 0; i < 10; i++) {
            if (counter.tryConsume().isAllowed()) {
                allowedHalfwayThroughWindow1++;
            }
        }
        assertEquals(5, allowedHalfwayThroughWindow1,
                "halfway into window 1, only 5 more requests should fit under the weighted limit of 10");
    }
}

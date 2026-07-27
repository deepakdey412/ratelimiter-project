package com.example.ratelimiter.limiter;

import java.util.function.LongSupplier;

/**
 * A sliding-window-COUNTER rate limiter for ONE key (e.g. one client IP
 * calling one specific endpoint). This is the low-level algorithm class,
 * playing the same role here that TokenBucket plays for the token-bucket
 * strategy - SlidingWindowRateLimiter (the Spring bean) owns one instance
 * of this per key.
 *
 * Algorithm, in plain terms:
 *   - Time is divided into fixed-size "sub-windows" of length `windowNanos`,
 *     back to back (window 0 = [0, windowNanos), window 1 = [windowNanos,
 *     2*windowNanos), and so on).
 *   - We track two counts: how many requests landed in the CURRENT
 *     sub-window, and how many landed in the PREVIOUS one.
 *   - To estimate "how many requests happened in the last `windowNanos`",
 *     we don't just look at the current sub-window (that's what a naive
 *     fixed-window counter does, and it's exactly what allows the classic
 *     boundary-burst problem). Instead we blend in the previous
 *     sub-window's count, weighted by how much of it still overlaps the
 *     trailing window:
 *
 *       estimatedCount = previousWindowCount * (1 - fractionElapsedInCurrentWindow)
 *                        + currentWindowCount
 *
 *     Early in the current sub-window, fractionElapsed is small, so almost
 *     all of the previous sub-window's traffic still counts against the
 *     limit. As time moves further into the current sub-window, the
 *     previous sub-window's weight fades out linearly. This assumes
 *     requests were spread roughly evenly across the previous sub-window,
 *     which is an approximation - but a much closer approximation to a
 *     true sliding window than a hard reset every sub-window.
 *
 * Thread-safety: all state is only read/written inside the synchronized
 * tryConsume() method, so concurrent requests for the same key are safely
 * serialized on this instance's monitor.
 */
public class SlidingWindowCounter {

    private final long limit;
    private final long windowNanos;
    private final LongSupplier nanoTimeSource;

    // Sentinel so the very first call always starts a fresh window 0 with no
    // "previous window" carried over.
    private long currentWindowIndex = Long.MIN_VALUE;
    private long previousWindowCount = 0;
    private long currentWindowCount = 0;

    public SlidingWindowCounter(long limit, long windowNanos) {
        this(limit, windowNanos, System::nanoTime);
    }

    /**
     * Package-private constructor that accepts a controllable clock, used
     * by unit tests to deterministically simulate crossing a window
     * boundary without flaky Thread.sleep()-based timing.
     */
    SlidingWindowCounter(long limit, long windowNanos, LongSupplier nanoTimeSource) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
        if (windowNanos <= 0) {
            throw new IllegalArgumentException("windowNanos must be > 0");
        }
        this.limit = limit;
        this.windowNanos = windowNanos;
        this.nanoTimeSource = nanoTimeSource;
    }

    public synchronized RateLimitResult tryConsume() {
        long now = nanoTimeSource.getAsLong();
        long windowIndex = Math.floorDiv(now, windowNanos);

        if (windowIndex != currentWindowIndex) {
            // We've moved into a sub-window we haven't seen a request in yet.
            if (windowIndex == currentWindowIndex + 1) {
                // Adjacent sub-window: the count that was "current" a moment
                // ago is now the "previous" window for weighting purposes.
                previousWindowCount = currentWindowCount;
            } else {
                // Either the very first request ever, or a gap of one or
                // more completely idle sub-windows passed - nothing recent
                // enough to carry over.
                previousWindowCount = 0;
            }
            currentWindowIndex = windowIndex;
            currentWindowCount = 0;
        }

        long currentWindowStartNanos = windowIndex * windowNanos;
        double fractionElapsed = (now - currentWindowStartNanos) / (double) windowNanos; // 0.0 .. 1.0
        double previousWindowWeight = 1.0 - fractionElapsed;

        double estimatedCount = (previousWindowCount * previousWindowWeight) + currentWindowCount;

        if (estimatedCount + 1 <= limit) {
            currentWindowCount++;
            return RateLimitResult.allowed();
        }

        // Denied. Simple, conservative retry estimate: the time remaining
        // until the current sub-window ends. (The true "wait until exactly
        // 1 slot frees up" moment is a bit earlier than this in general,
        // since previousWindowWeight keeps shrinking continuously - but
        // "wait for the next sub-window" is an easy-to-explain, always-safe
        // upper bound and is precise enough for a Retry-After hint.)
        long nextWindowStartNanos = currentWindowStartNanos + windowNanos;
        long retryAfterSeconds = Math.max(1L, (long) Math.ceil((nextWindowStartNanos - now) / 1_000_000_000.0));
        return RateLimitResult.denied(retryAfterSeconds);
    }
}

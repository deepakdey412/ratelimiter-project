package com.example.ratelimiter.annotation;

/**
 * Which rate-limiting algorithm a @RateLimit method should use.
 *
 * TOKEN_BUCKET (Phase 1): allows short bursts up to the limit, then refills
 * continuously. More lenient - good for "protect the backend from spikes"
 * use cases where an occasional burst is fine.
 *
 * SLIDING_WINDOW (Phase 2): strictly enforces "no more than `limit` requests
 * in any trailing `window` of time", with no burst allowance beyond the
 * limit. Stricter and fairer across window boundaries - good for "hard
 * quota" use cases (e.g. billing-relevant API quotas).
 */
public enum RateLimitStrategy {
    TOKEN_BUCKET,
    SLIDING_WINDOW
}

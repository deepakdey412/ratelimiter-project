package com.example.ratelimiter.annotation;

/**
 * Defines which RateLimitKeyResolver a @RateLimit annotation should use to
 * identify the caller.
 *
 * IP: rate-limit by client IP address (Phase 1-3 behavior, still the default).
 * USER: rate-limit by authenticated user ID (requires Spring Security).
 * API_KEY: rate-limit by API key header (header name configurable via properties).
 * CUSTOM: use a custom resolver bean identified by @RateLimit's keyResolverBeanName.
 */
public enum RateLimitScope {
    IP,
    USER,
    API_KEY,
    CUSTOM
}

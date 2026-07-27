package com.example.ratelimiter.resolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extracts the client IP address as the rate-limit key. This is the original
 * Phases 1-3 behavior, now extracted into a pluggable resolver.
 *
 * NOTE on X-Forwarded-For: request.getRemoteAddr() returns the direct TCP
 * peer's address. If this app runs behind a reverse proxy or load balancer,
 * that will be the proxy's IP for every client, not the real end-user IP -
 * which would make every user share one bucket. In that setup you would
 * normally read the "X-Forwarded-For" (or a trusted proxy's own header,
 * e.g. X-Real-IP) instead, taking care to only trust it when the request
 * truly comes from a known proxy (since the header is client-suppliable and
 * can otherwise be spoofed to dodge limits). Deliberately kept simple for
 * this phase - see README limitations.
 */
public class IpAddressKeyResolver implements RateLimitKeyResolver {

    @Override
    public String resolveKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}

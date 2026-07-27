package com.example.ratelimiter.resolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extracts an API key from a configurable HTTP header as the rate-limit key.
 * If the header is absent or empty, falls back to IP address.
 *
 * The header name is injected via constructor from RateLimiterProperties
 * (default: "X-API-Key").
 */
public class ApiKeyHeaderKeyResolver implements RateLimitKeyResolver {

    private final String headerName;
    private final IpAddressKeyResolver fallbackResolver = new IpAddressKeyResolver();

    public ApiKeyHeaderKeyResolver(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public String resolveKey(HttpServletRequest request) {
        String apiKey = request.getHeader(headerName);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return "apikey:" + apiKey;
        }
        // No API key present: fall back to IP-based limiting.
        return fallbackResolver.resolveKey(request);
    }
}

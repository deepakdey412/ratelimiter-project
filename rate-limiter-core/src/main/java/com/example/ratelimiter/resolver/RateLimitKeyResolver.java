package com.example.ratelimiter.resolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Pluggable strategy for extracting the rate-limit key from an HTTP request.
 * The key identifies "who is making this request" for rate-limiting purposes -
 * could be an IP address, a user ID, an API key, or anything else.
 *
 * Deliberately depends on jakarta.servlet.http.HttpServletRequest (not a
 * Spring type) so rate-limiter-core stays Spring-free. The servlet-api
 * dependency is added as "provided" scope in rate-limiter-core's pom.xml
 * since any app using this will have servlet-api on its classpath already.
 */
public interface RateLimitKeyResolver {

    /**
     * @param request the incoming HTTP request
     * @return a string uniquely identifying the caller for rate-limit tracking
     */
    String resolveKey(HttpServletRequest request);
}

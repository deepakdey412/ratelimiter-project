package com.example.ratelimiter.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Extracts the authenticated user ID (from Spring Security's
 * SecurityContextHolder) as the rate-limit key. If no authentication is
 * present, falls back to IP address.
 *
 * This class is only registered as a bean when Spring Security is on the
 * classpath (@ConditionalOnClass in RateLimiterAutoConfiguration), so it
 * won't cause ClassNotFoundException if the consuming app doesn't use
 * Spring Security.
 */
public class UserIdKeyResolver implements RateLimitKeyResolver {

    private final IpAddressKeyResolver fallbackResolver = new IpAddressKeyResolver();

    @Override
    public String resolveKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user:" + authentication.getName();
        }
        // No authenticated user: fall back to IP-based limiting.
        return fallbackResolver.resolveKey(request);
    }
}

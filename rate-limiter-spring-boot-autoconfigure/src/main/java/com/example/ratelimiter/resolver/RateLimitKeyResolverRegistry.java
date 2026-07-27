package com.example.ratelimiter.resolver;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.example.ratelimiter.annotation.RateLimitScope;

/**
 * Registry that maps RateLimitScope enum values to their corresponding
 * RateLimitKeyResolver implementations, plus handles CUSTOM scope by looking
 * up a user-supplied bean name.
 *
 * Structurally similar to RateLimiterStrategyRegistry: explicit hand-wired
 * EnumMap for the built-in scopes, dependency-injected from
 * RateLimiterAutoConfiguration.
 */
public class RateLimitKeyResolverRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitKeyResolverRegistry.class);

    private final Map<RateLimitScope, RateLimitKeyResolver> resolversByScope = new EnumMap<>(RateLimitScope.class);
    private final ApplicationContext applicationContext;
    private final RateLimitKeyResolver fallbackResolver;

    public RateLimitKeyResolverRegistry(IpAddressKeyResolver ipResolver,
                                         RateLimitKeyResolver userResolver,
                                         ApiKeyHeaderKeyResolver apiKeyResolver,
                                         ApplicationContext applicationContext) {
        resolversByScope.put(RateLimitScope.IP, ipResolver);
        resolversByScope.put(RateLimitScope.USER, userResolver);
        resolversByScope.put(RateLimitScope.API_KEY, apiKeyResolver);
        this.applicationContext = applicationContext;
        this.fallbackResolver = ipResolver; // Always safe fallback
    }

    /**
     * @param scope the scope from @RateLimit
     * @param customBeanName optional bean name for CUSTOM scope
     * @return the appropriate resolver, or IP resolver with logged warning if missing
     */
    public RateLimitKeyResolver resolve(RateLimitScope scope, String customBeanName) {
        if (scope == RateLimitScope.CUSTOM) {
            if (customBeanName == null || customBeanName.trim().isEmpty()) {
                logger.warn("RateLimitScope.CUSTOM specified but keyResolverBeanName is empty. Falling back to IP resolver.");
                return fallbackResolver;
            }
            try {
                return applicationContext.getBean(customBeanName, RateLimitKeyResolver.class);
            } catch (Exception e) {
                logger.warn("Could not find custom RateLimitKeyResolver bean '{}'. Falling back to IP resolver. Error: {}", 
                        customBeanName, e.getMessage());
                return fallbackResolver;
            }
        }

        RateLimitKeyResolver resolver = resolversByScope.get(scope);
        if (resolver == null) {
            logger.warn("No resolver registered for scope {}. Falling back to IP resolver.", scope);
            return fallbackResolver;
        }
        return resolver;
    }
}

package com.example.ratelimiter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.example.ratelimiter.aspect.RateLimitAspect;
import com.example.ratelimiter.autoconfigure.web.GlobalExceptionHandler;
import com.example.ratelimiter.limiter.RateLimiterStrategyRegistry;
import com.example.ratelimiter.limiter.SlidingWindowRateLimiter;
import com.example.ratelimiter.limiter.TokenBucketRateLimiter;
import com.example.ratelimiter.resolver.ApiKeyHeaderKeyResolver;
import com.example.ratelimiter.resolver.IpAddressKeyResolver;
import com.example.ratelimiter.resolver.RateLimitKeyResolver;
import com.example.ratelimiter.resolver.RateLimitKeyResolverRegistry;
import com.example.ratelimiter.resolver.UserIdKeyResolver;

/**
 * Auto-configuration for the rate limiter starter: registers every bean the
 * feature needs, entirely explicitly, so a consuming app gets working rate
 * limiting the instant rate-limiter-spring-boot-starter is on its classpath -
 * zero manual @Bean methods, zero @ComponentScan changes required on their
 * end.
 *
 * WHY EXPLICIT @Bean METHODS INSTEAD OF @Component/@Aspect/@RestControllerAdvice
 * + COMPONENT SCANNING:
 *   @ComponentScan (which @SpringBootApplication implies) only scans the
 *   consuming application's own base package and its sub-packages. Our demo
 *   app's classes happen to live under com.example.ratelimiter.demo, a
 *   sub-package of com.example.ratelimiter - so if TokenBucketRateLimiter,
 *   RateLimitAspect, etc. still carried @Component/@Aspect, the demo app's
 *   scan WOULD find them purely by accidental package overlap. A real
 *   external consumer's app - say, com.acme.orders - would never scan
 *   com.example.ratelimiter.* at all, so those beans would silently never
 *   get created, and the whole "just add the starter dependency" promise
 *   would break for anyone but us. Explicit @Bean methods in an
 *   @AutoConfiguration class sidestep this entirely: they're registered via
 *   the AutoConfiguration.imports mechanism (see that file), which Spring
 *   Boot processes for every application regardless of package structure.
 *
 * @ConditionalOnMissingBean on every @Bean method means a consuming app can
 * override any single piece (e.g. provide its own SlidingWindowRateLimiter
 * backed by Redis someday) just by declaring their own bean of that type -
 * no flags, no exclusions list, ours simply steps aside.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "ratelimiter", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TokenBucketRateLimiter tokenBucketRateLimiter() {
        return new TokenBucketRateLimiter();
    }

    @Bean
    @ConditionalOnMissingBean
    public SlidingWindowRateLimiter slidingWindowRateLimiter() {
        return new SlidingWindowRateLimiter();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterStrategyRegistry rateLimiterStrategyRegistry(TokenBucketRateLimiter tokenBucketRateLimiter,
                                                                    SlidingWindowRateLimiter slidingWindowRateLimiter) {
        return new RateLimiterStrategyRegistry(tokenBucketRateLimiter, slidingWindowRateLimiter);
    }

    @Bean
    @ConditionalOnMissingBean
    public IpAddressKeyResolver ipAddressKeyResolver() {
        return new IpAddressKeyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.security.core.Authentication")
    public UserIdKeyResolver userIdKeyResolver() {
        return new UserIdKeyResolver();
    }

    /**
     * Fallback UserIdKeyResolver when Spring Security is not on the classpath.
     * Just delegates to IP resolver. This ensures RateLimitKeyResolverRegistry
     * always has a non-null USER resolver to register, even if the app doesn't
     * use Spring Security.
     */
    @Bean("userIdKeyResolver")
    @ConditionalOnMissingBean(UserIdKeyResolver.class)
    public RateLimitKeyResolver userIdKeyResolverFallback(IpAddressKeyResolver ipResolver) {
        return new RateLimitKeyResolver() {
            @Override
            public String resolveKey(jakarta.servlet.http.HttpServletRequest request) {
                return ipResolver.resolveKey(request);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiKeyHeaderKeyResolver apiKeyHeaderKeyResolver(RateLimiterProperties properties) {
        return new ApiKeyHeaderKeyResolver(properties.getApiKeyHeader());
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitKeyResolverRegistry rateLimitKeyResolverRegistry(
            IpAddressKeyResolver ipResolver,
            @org.springframework.beans.factory.annotation.Qualifier("userIdKeyResolver") RateLimitKeyResolver userIdKeyResolver,
            ApiKeyHeaderKeyResolver apiKeyResolver,
            ApplicationContext applicationContext) {
        return new RateLimitKeyResolverRegistry(ipResolver, userIdKeyResolver, apiKeyResolver, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(RateLimiterStrategyRegistry rateLimiterStrategyRegistry,
                                            RateLimitKeyResolverRegistry keyResolverRegistry) {
        return new RateLimitAspect(rateLimiterStrategyRegistry, keyResolverRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}

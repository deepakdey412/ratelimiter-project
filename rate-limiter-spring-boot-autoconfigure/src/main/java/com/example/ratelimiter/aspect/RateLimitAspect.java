package com.example.ratelimiter.aspect;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.ratelimiter.annotation.RateLimit;
import com.example.ratelimiter.exception.RateLimitExceededException;
import com.example.ratelimiter.limiter.RateLimitResult;
import com.example.ratelimiter.limiter.RateLimiterStrategy;
import com.example.ratelimiter.limiter.RateLimiterStrategyRegistry;
import com.example.ratelimiter.resolver.RateLimitKeyResolver;
import com.example.ratelimiter.resolver.RateLimitKeyResolverRegistry;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Intercepts every method annotated with @RateLimit and enforces a
 * per-client-IP limit before the real method is allowed to run, using
 * whichever RateLimiterStrategy the annotation's `strategy` attribute
 * selects (TOKEN_BUCKET by default, or SLIDING_WINDOW).
 *
 * How the interception works (Spring AOP mechanics):
 *   1. At startup, Spring scans its beans (e.g. DemoController) for methods
 *      carrying @RateLimit.
 *   2. For any bean that has at least one such method, Spring wraps that
 *      bean in a runtime PROXY (by default a JDK dynamic proxy if the bean
 *      implements an interface, otherwise a CGLIB subclass proxy). Spring
 *      then registers that proxy in the application context INSTEAD of the
 *      raw bean.
 *   3. Because of that swap, when Spring MVC's DispatcherServlet routes an
 *      HTTP request to, say, DemoController.ping(), it is actually calling
 *      ping() on the PROXY, not on the original object.
 *   4. The proxy's job is to run any matching advice (this class's
 *      enforceRateLimit method) before/instead of forwarding the call to
 *      the real target object.
 *   5. @Around("@annotation(rateLimit)") tells Spring: "run this advice for
 *      any method whose invocation is annotated with @RateLimit, and bind
 *      that specific annotation instance to the `rateLimit` parameter."
 *   6. Inside the advice, calling joinPoint.proceed() is what actually
 *      invokes the real controller method. If we DON'T call proceed() (as
 *      happens below when the bucket is empty - we throw instead), the
 *      real method body never runs at all.
 *
 * This is why AOP is a clean fit for cross-cutting concerns like rate
 * limiting: the controller method itself has zero rate-limiting code in it;
 * the concern is fully externalized into this one aspect.
 *
 * No @Component here on purpose: @Aspect only needs to be present on the
 * bean's class for Spring's AnnotationAwareAspectJAutoProxyCreator to weave
 * it in - it doesn't care how the bean itself got registered. This bean is
 * registered explicitly via @Bean in RateLimiterAutoConfiguration, not
 * discovered via component-scanning (a consuming app's @ComponentScan has no
 * reason to ever reach this package - see RateLimiterAutoConfiguration's
 * Javadoc for the full reasoning).
 */
@Aspect
public class RateLimitAspect {

    private static final Pattern WINDOW_PATTERN = Pattern.compile("^(\\d+)([smh])$");

    private final RateLimiterStrategyRegistry strategyRegistry;
    private final RateLimitKeyResolverRegistry keyResolverRegistry;

    public RateLimitAspect(RateLimiterStrategyRegistry strategyRegistry,
                            RateLimitKeyResolverRegistry keyResolverRegistry) {
        this.strategyRegistry = strategyRegistry;
        this.keyResolverRegistry = keyResolverRegistry;
    }

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = resolveRequest();
        if (request == null) {
            // Should not normally happen for a request handled by Spring MVC,
            // but guards against calling this outside a web request thread.
            return joinPoint.proceed();
        }

        RateLimitKeyResolver keyResolver = keyResolverRegistry.resolve(rateLimit.scope(), rateLimit.keyResolverBeanName());
        String callerKey = keyResolver.resolveKey(request);
        String key = buildRateLimitKey(joinPoint, callerKey);
        Duration window = parseWindow(rateLimit.window());

        RateLimiterStrategy strategy = strategyRegistry.resolve(rateLimit.strategy());
        RateLimitResult result = strategy.tryConsume(key, rateLimit.limit(), window);

        if (!result.isAllowed()) {
            // Skip the real method entirely - proceed() is never called.
            throw new RateLimitExceededException(result.getRetryAfterSeconds());
        }

        // Allowed: let the original controller method run normally.
        return joinPoint.proceed();
    }

    /**
     * Builds a rate-limit key from caller identity + method signature, so that two
     * different @RateLimit endpoints track independent state even when hit
     * by the same caller, and so one endpoint's traffic can't "use up" a
     * different endpoint's quota.
     */
    private String buildRateLimitKey(ProceedingJoinPoint joinPoint, String callerKey) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return callerKey + "::" + signature.toShortString();
    }

    private HttpServletRequest resolveRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return attrs.getRequest();
    }

    private Duration parseWindow(String window) {
        Matcher matcher = WINDOW_PATTERN.matcher(window.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid @RateLimit window '" + window + "'. Expected a number plus unit, e.g. '30s', '1m', '1h'.");
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        return switch (unit) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            default -> throw new IllegalArgumentException("Unsupported window unit: " + unit);
        };
    }
}

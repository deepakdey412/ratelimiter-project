package com.example.ratelimiter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.example.ratelimiter.annotation.RateLimitScope;
import com.example.ratelimiter.annotation.RateLimitStrategy;

/**
 * Binds application.yml / application.properties settings under the
 * `ratelimiter.*` prefix.
 *
 *   ratelimiter.enabled=true|false          (default: true)
 *   ratelimiter.default-strategy=TOKEN_BUCKET|SLIDING_WINDOW   (default: TOKEN_BUCKET)
 *   ratelimiter.default-scope=IP|USER|API_KEY|CUSTOM   (default: IP)
 *   ratelimiter.api-key-header=X-API-Key   (default: X-API-Key)
 *
 * A plain mutable JavaBean (getters + setters) rather than a record/
 * constructor-bound class - the simplest, most common style for
 * @ConfigurationProperties and the easiest to explain: Spring just calls the
 * setters that match property names it finds, after constructing the object
 * with its no-arg constructor.
 *
 * On `defaultStrategy`: @RateLimit's own `strategy` attribute is a
 * compile-time annotation value - every usage always carries a concrete
 * RateLimitStrategy (TOKEN_BUCKET unless the developer wrote something
 * else), so there's no "absent" state for this property to fill in at
 * request time. It's exposed here now as a documented, forward-looking
 * configuration point (e.g. for a future non-annotation-based configuration
 * path, or tooling/observability that wants to know the project's intended
 * default) rather than something that silently changes existing
 * @RateLimit behavior - doing that would violate this phase's "structural
 * refactor only, no behavior change" constraint.
 *
 * Same reasoning applies to `defaultScope`: forward-looking, doesn't
 * override @RateLimit's compile-time scope attribute.
 */
@ConfigurationProperties(prefix = "ratelimiter")
public class RateLimiterProperties {

    private boolean enabled = true;

    private RateLimitStrategy defaultStrategy = RateLimitStrategy.TOKEN_BUCKET;

    private RateLimitScope defaultScope = RateLimitScope.IP;

    private String apiKeyHeader = "X-API-Key";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RateLimitStrategy getDefaultStrategy() {
        return defaultStrategy;
    }

    public void setDefaultStrategy(RateLimitStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }

    public RateLimitScope getDefaultScope() {
        return defaultScope;
    }

    public void setDefaultScope(RateLimitScope defaultScope) {
        this.defaultScope = defaultScope;
    }

    public String getApiKeyHeader() {
        return apiKeyHeader;
    }

    public void setApiKeyHeader(String apiKeyHeader) {
        this.apiKeyHeader = apiKeyHeader;
    }
}

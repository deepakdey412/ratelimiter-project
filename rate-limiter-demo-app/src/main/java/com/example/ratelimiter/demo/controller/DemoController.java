package com.example.ratelimiter.demo.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ratelimiter.annotation.RateLimit;
import com.example.ratelimiter.annotation.RateLimitScope;
import com.example.ratelimiter.annotation.RateLimitStrategy;

/**
 * Demo controller for manually exercising @RateLimit.
 * Notice this class has NO rate-limiting code in it at all - that concern
 * is entirely handled by RateLimitAspect, which this module never even
 * imports directly. This app only depends on rate-limiter-spring-boot-starter,
 * proving the starter delivers working rate limiting on its own.
 */
@RestController
public class DemoController {

    // No `strategy` or `scope` specified -> defaults to TOKEN_BUCKET + IP
    // (Phase 1 behavior, completely unchanged).
    @RateLimit(limit = 5, window = "1m")
    @GetMapping("/api/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "message", "pong",
                "serverTime", Instant.now().toString()
        );
    }

    // Explicit SLIDING_WINDOW strategy: strictly at most 10
    // requests in any trailing 30s window, no boundary-burst allowance.
    @RateLimit(limit = 10, window = "30s", strategy = RateLimitStrategy.SLIDING_WINDOW)
    @GetMapping("/api/ping-strict")
    public Map<String, Object> pingStrict() {
        return Map.of(
                "message", "pong (sliding window)",
                "serverTime", Instant.now().toString()
        );
    }

    // Rate limit by authenticated user (if Spring Security is present),
    // otherwise falls back to IP.
    @RateLimit(limit = 20, window = "1m", scope = RateLimitScope.USER)
    @GetMapping("/api/user-limited")
    public Map<String, Object> userLimited() {
        return Map.of(
                "message", "user-scoped rate limit",
                "serverTime", Instant.now().toString()
        );
    }

    //  Rate limit by API key from X-API-Key header,
    // falls back to IP if header is absent.
    @RateLimit(limit = 100, window = "1m", scope = RateLimitScope.API_KEY)
    @GetMapping("/api/key-limited")
    public Map<String, Object> apiKeyLimited() {
        return Map.of(
                "message", "API key-scoped rate limit",
                "serverTime", Instant.now().toString()
        );
    }

    // Demo: USER scope with fake auth header simulation
    @RateLimit(limit = 3, window = "10s", scope = RateLimitScope.USER)
    @GetMapping("/api/user-demo")
    public Map<String, Object> userDemo() {
        // In real app, Spring Security would handle auth
        // This endpoint demonstrates USER-scoped limiting
        return Map.of(
                "message", "USER-scoped demo (3 requests per 10s per user)",
                "note", "Add Spring Security for real user auth",
                "serverTime", Instant.now().toString()
        );
    }

    // Demo: API_KEY scope with header
    @RateLimit(limit = 5, window = "15s", scope = RateLimitScope.API_KEY)
    @GetMapping("/api/apikey-demo")
    public Map<String, Object> apiKeyDemo() {
        // Requires X-API-Key header, falls back to IP if missing
        return Map.of(
                "message", "API-KEY-scoped demo (5 requests per 15s per key)",
                "note", "Send X-API-Key header, or will use IP as fallback",
                "serverTime", Instant.now().toString()
        );
    }
}

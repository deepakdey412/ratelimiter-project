package com.example.ratelimiter.autoconfigure.web;

import com.example.ratelimiter.exception.RateLimitExceededException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts RateLimitExceededException, thrown anywhere in the app, into an
 * HTTP 429 response with a small JSON body:
 *   { "error": "Too Many Requests", "retryAfterSeconds": <n> }
 *
 * Using @RestControllerAdvice here (instead of building the ResponseEntity
 * inside the aspect) keeps the aspect's return type generic - it can wrap
 * ANY controller method regardless of that method's own return type,
 * because on the happy path it just returns whatever joinPoint.proceed()
 * returns, and on the rate-limited path it throws rather than trying to
 * fabricate a fake return value.
 *
 * Package note: this class moved from com.example.ratelimiter.exception
 * (where RateLimitExceededException still lives, in rate-limiter-core) to
 * com.example.ratelimiter.autoconfigure.web, purely to avoid having the same
 * Java package split across two different JARs (core.jar and
 * autoconfigure.jar). That's legal on a plain classpath but is generally
 * avoided as sloppy practice, so each package now lives in exactly one JAR.
 *
 * Registration note: like RateLimitAspect, this is registered explicitly as
 * a @Bean in RateLimiterAutoConfiguration rather than relying on
 * @RestControllerAdvice's built-in @Component meta-annotation plus
 * component-scanning - a consuming app's scan has no reason to ever reach
 * this package. Spring's @ControllerAdvice bean-detection machinery
 * (ControllerAdviceBean) finds beans of this type in the context regardless
 * of whether they were registered via scanning or an explicit @Bean method,
 * so this works exactly the same either way.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Too Many Requests");
        body.put("retryAfterSeconds", ex.getRetryAfterSeconds());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(body);
    }
}

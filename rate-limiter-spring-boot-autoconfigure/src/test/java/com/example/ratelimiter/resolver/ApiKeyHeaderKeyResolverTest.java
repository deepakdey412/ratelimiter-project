package com.example.ratelimiter.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

class ApiKeyHeaderKeyResolverTest {

    @Test
    void resolveKey_withApiKey_returnsApiKeyPrefix() {
        ApiKeyHeaderKeyResolver resolver = new ApiKeyHeaderKeyResolver("X-API-Key");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-API-Key")).thenReturn("abc123");

        String key = resolver.resolveKey(request);

        assertEquals("apikey:abc123", key);
    }

    @Test
    void resolveKey_withoutApiKey_fallsBackToIp() {
        ApiKeyHeaderKeyResolver resolver = new ApiKeyHeaderKeyResolver("X-API-Key");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.50");

        String key = resolver.resolveKey(request);

        assertEquals("192.168.1.50", key);
    }

    @Test
    void resolveKey_withEmptyApiKey_fallsBackToIp() {
        ApiKeyHeaderKeyResolver resolver = new ApiKeyHeaderKeyResolver("X-API-Key");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-API-Key")).thenReturn("  ");
        when(request.getRemoteAddr()).thenReturn("10.20.30.40");

        String key = resolver.resolveKey(request);

        assertEquals("10.20.30.40", key);
    }

    @Test
    void resolveKey_withCustomHeader_usesCustomHeader() {
        ApiKeyHeaderKeyResolver resolver = new ApiKeyHeaderKeyResolver("Authorization");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer xyz789");

        String key = resolver.resolveKey(request);

        assertEquals("apikey:Bearer xyz789", key);
    }
}

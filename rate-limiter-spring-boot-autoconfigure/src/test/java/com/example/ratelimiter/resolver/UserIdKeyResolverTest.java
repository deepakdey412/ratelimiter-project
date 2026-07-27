package com.example.ratelimiter.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Basic tests for UserIdKeyResolver. Full authentication tests require
 * Spring Security integration test context, which is beyond the scope
 * of isolated unit tests. These tests verify fallback behavior.
 */
class UserIdKeyResolverTest {

    @Test
    void resolveKey_withoutAuthentication_fallsBackToIp() {
        UserIdKeyResolver resolver = new UserIdKeyResolver();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");

        String key = resolver.resolveKey(request);

        assertEquals("172.16.0.1", key);
    }

    @Test
    void resolveKey_differentIps_returnDifferentKeys() {
        UserIdKeyResolver resolver = new UserIdKeyResolver();
        HttpServletRequest request1 = mock(HttpServletRequest.class);
        HttpServletRequest request2 = mock(HttpServletRequest.class);
        when(request1.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request2.getRemoteAddr()).thenReturn("10.0.0.2");

        String key1 = resolver.resolveKey(request1);
        String key2 = resolver.resolveKey(request2);

        assertEquals("10.0.0.1", key1);
        assertEquals("10.0.0.2", key2);
    }
}

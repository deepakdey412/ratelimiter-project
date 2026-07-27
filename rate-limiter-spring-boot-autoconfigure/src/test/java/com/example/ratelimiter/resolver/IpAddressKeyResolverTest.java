package com.example.ratelimiter.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

class IpAddressKeyResolverTest {

    @Test
    void resolveKey_returnsRemoteAddr() {
        IpAddressKeyResolver resolver = new IpAddressKeyResolver();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        String key = resolver.resolveKey(request);

        assertEquals("192.168.1.100", key);
    }

    @Test
    void resolveKey_handlesDifferentIps() {
        IpAddressKeyResolver resolver = new IpAddressKeyResolver();
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

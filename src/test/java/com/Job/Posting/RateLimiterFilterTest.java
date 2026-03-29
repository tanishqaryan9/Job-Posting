package com.Job.Posting;

import com.Job.Posting.security.RateLimiterFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterFilterTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private RateLimiterFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimiterFilter();
    }

    @Test
    void shouldPassThrough_whenNotAuthEndpoint() throws Exception {
        when(request.getRequestURI()).thenReturn("/jobs");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void shouldAllow_whenUnderRateLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldBlock_whenRateLimitExceeded() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Fire 11 requests (limit is 10)
        for (int i = 0; i < 11; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(response, atLeastOnce()).setStatus(429);
    }

    @Test
    void shouldUseForwardedIp_whenProxyHeaderPresent() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/signup");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        //should use 10.0.0.1 (first IP) not the proxy's IP
        verify(request, never()).getRemoteAddr();
    }
}

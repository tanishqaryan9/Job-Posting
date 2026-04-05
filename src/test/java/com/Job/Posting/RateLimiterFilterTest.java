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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterFilterTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private RateLimiterFilter filter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        filter = new RateLimiterFilter(redisTemplate);
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
        // Return count well within the 30-request limit
        when(valueOperations.increment(anyString())).thenReturn(5L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldBlock_whenRateLimitExceeded() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Return count above MAX_REQUESTS (30) to trigger rate limiting
        when(valueOperations.increment(anyString())).thenReturn(31L);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldSetTTL_onFirstRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/signup");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(valueOperations.increment(anyString())).thenReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        // TTL should be set when count == 1
        verify(redisTemplate).expire(anyString(), any());
        verify(filterChain).doFilter(request, response);
    }
}
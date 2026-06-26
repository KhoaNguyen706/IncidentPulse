package com.example.IncidentPulse.Security;

import com.example.IncidentPulse.ApplicationCofig.LoginRateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Pure unit test of the token-bucket logic (no Spring context). Capacity is 2
 * with a slow refill, so the third attempt from the same IP must be blocked.
 */
class LoginRateLimitFilterTest {

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        LoginRateLimitProperties props = new LoginRateLimitProperties();
        props.setCapacity(2);
        props.setRefillTokens(1);
        props.setRefillPeriodMinutes(60); // no refill within the test window
        // findAndRegisterModules() picks up jackson-datatype-jsr310 so the 429
        // body (which contains a LocalDateTime) serializes, like the real app's mapper.
        filter = new LoginRateLimitFilter(props, new ObjectMapper().findAndRegisterModules());
    }

    private MockHttpServletRequest loginRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    void thirdAttemptFromSameIpIsRateLimited() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(loginRequest("203.0.113.7"), new MockHttpServletResponse(), chain);
        filter.doFilter(loginRequest("203.0.113.7"), new MockHttpServletResponse(), chain);

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(loginRequest("203.0.113.7"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        // The chain was invoked only for the two allowed attempts.
        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    void nonLoginRequestsAreNotThrottled() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        // A GET to a different path: shouldNotFilter() => true, always passes.
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/incident");
            request.setRemoteAddr("203.0.113.7");
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }

        verify(chain, times(5)).doFilter(any(), any());
    }

    @Test
    void differentIpsHaveIndependentBuckets() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        // Exhaust IP A's bucket (capacity 2).
        filter.doFilter(loginRequest("203.0.113.7"), new MockHttpServletResponse(), chain);
        filter.doFilter(loginRequest("203.0.113.7"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blockedA = new MockHttpServletResponse();
        filter.doFilter(loginRequest("203.0.113.7"), blockedA, chain);
        assertThat(blockedA.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

        // A different IP still has a full bucket and is allowed.
        MockHttpServletResponse okB = new MockHttpServletResponse();
        filter.doFilter(loginRequest("198.51.100.42"), okB, chain);
        assertThat(okB.getStatus()).isEqualTo(HttpStatus.OK.value());
    }
}

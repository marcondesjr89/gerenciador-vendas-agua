package com.distribuidora.agua.platform.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TraceIdHeaderFilter Unit Tests")
class TraceIdHeaderFilterTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    @Mock
    private FilterChain filterChain;

    @Test
    @DisplayName("Should inject X-Trace-Id header into HTTP response when trace context is present")
    void shouldInjectTraceIdHeaderWhenTraceContextPresent() throws ServletException, IOException {
        TraceIdHeaderFilter filter = new TraceIdHeaderFilter(Optional.of(tracer));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader(TraceIdHeaderFilter.TRACE_HEADER)).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain when tracer has no active span")
    void shouldContinueFilterChainWhenNoActiveSpan() throws ServletException, IOException {
        TraceIdHeaderFilter filter = new TraceIdHeaderFilter(Optional.of(tracer));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tracer.currentSpan()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader(TraceIdHeaderFilter.TRACE_HEADER)).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain when tracer is not present (e.g. slice tests)")
    void shouldContinueFilterChainWhenTracerNotPresent() throws ServletException, IOException {
        TraceIdHeaderFilter filter = new TraceIdHeaderFilter(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader(TraceIdHeaderFilter.TRACE_HEADER)).isNull();
        verify(filterChain).doFilter(request, response);
    }
}

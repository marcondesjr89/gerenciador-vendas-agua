package com.distribuidora.agua.platform.observability;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * HTTP filter that extracts the active distributed traceId from Micrometer Tracing
 * and attaches it to the HTTP response header (X-Trace-Id) for client-side observability correlation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdHeaderFilter extends OncePerRequestFilter {

    public static final String TRACE_HEADER = "X-Trace-Id";

    private final Optional<Tracer> tracer;

    public TraceIdHeaderFilter(Optional<Tracer> tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        injectTraceIdHeader(response);
        filterChain.doFilter(request, response);
    }

    private void injectTraceIdHeader(HttpServletResponse response) {
        tracer.ifPresent(t -> {
            if (t.currentSpan() != null && t.currentSpan().context() != null) {
                String traceId = t.currentSpan().context().traceId();
                if (traceId != null && !traceId.isBlank()) {
                    response.setHeader(TRACE_HEADER, traceId);
                }
            }
        });
    }
}

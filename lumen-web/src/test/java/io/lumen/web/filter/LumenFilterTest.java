package io.lumen.web.filter;

import io.lumen.core.annotation.Order;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LumenFilterTest {

    static class PlainFilter implements LumenFilter {
        @Override
        public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) {}
    }

    @Order(101)
    static class AnnotatedFilter implements LumenFilter {
        @Override
        public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) {}
    }

    static class OverridingFilter implements LumenFilter {
        @Override
        public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) {}

        @Override
        public int getOrder() { return -100; }
    }

    @Order(101)
    static class OverridingAndAnnotatedFilter implements LumenFilter {
        @Override
        public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) {}

        @Override
        public int getOrder() { return -200; }
    }

    @Test
    void getOrder_noAnnotationNoOverride_defaultsToZero() {
        assertEquals(0, new PlainFilter().getOrder());
    }

    @Test
    void getOrder_classAnnotatedNoOverride_fallsBackToAnnotationValue() {
        // This is the case that used to be silently ignored: a filter that expresses its
        // order via @Order instead of overriding getOrder() (e.g. LumenSecurityFilter).
        assertEquals(101, new AnnotatedFilter().getOrder());
    }

    @Test
    void getOrder_overridden_ignoresAbsentAnnotation() {
        assertEquals(-100, new OverridingFilter().getOrder());
    }

    @Test
    void getOrder_overridden_takesPrecedenceOverAnnotation() {
        // An explicit getOrder() override always wins over @Order — the annotation is only
        // a fallback for filters that don't override the method at all.
        assertEquals(-200, new OverridingAndAnnotatedFilter().getOrder());
    }
}
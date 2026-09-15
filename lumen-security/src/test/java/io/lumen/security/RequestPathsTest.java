package io.lumen.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestPathsTest {

    private HttpServletRequest request(String servletPath, String pathInfo, String uri, String contextPath) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getServletPath()).thenReturn(servletPath);
        when(req.getPathInfo()).thenReturn(pathInfo);
        when(req.getRequestURI()).thenReturn(uri);
        when(req.getContextPath()).thenReturn(contextPath);
        return req;
    }

    @Test
    void prefersServletPathWhenPresent() {
        HttpServletRequest req = request("/api/users", null, "/api/users", "");

        assertEquals("/api/users", RequestPaths.resolve(req));
    }

    @Test
    void fallsBackToPathInfoWhenServletPathEmpty() {
        HttpServletRequest req = request("", "/api/users", "/api/users", "");

        assertEquals("/api/users", RequestPaths.resolve(req));
    }

    @Test
    void fallsBackToRequestUriWithContextPathStripped() {
        HttpServletRequest req = request(null, null, "/app/api/users", "/app");

        assertEquals("/api/users", RequestPaths.resolve(req));
    }

    @Test
    void antPathMatcherAndRegexMatcherAgreeOnResolvedPath() {
        // servletPath empty, pathInfo carries the path — the exact case that made the two
        // matchers disagree before both delegated to RequestPaths.resolve()
        HttpServletRequest req = request("", "/admin", "/admin", "");

        assertTrue(new AntPathRequestMatcher("/admin").matches(req));
        assertTrue(new RegexRequestMatcher("/admin").matches(req));
    }
}
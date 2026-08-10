package io.lumen.security;

import io.lumen.security.context.SecurityContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class LogoutFilterTest {

    private final LogoutFilter filter = new LogoutFilter("/logout", "/login?logout");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    private HttpServletRequest request(String servletPath, String pathInfo, String method) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getServletPath()).thenReturn(servletPath);
        when(req.getPathInfo()).thenReturn(pathInfo);
        when(req.getMethod()).thenReturn(method);
        return req;
    }

    @Test
    void postToLogoutUrl_performsLogout() throws Exception {
        HttpServletRequest req = request("/logout", null, "POST");
        HttpSession session = mock(HttpSession.class);
        when(req.getSession(false)).thenReturn(session);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(session).invalidate();
        verify(resp).sendRedirect("/login?logout");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void getToLogoutUrl_passesThrough() throws Exception {
        HttpServletRequest req = request("/logout", null, "GET");
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(resp, never()).sendRedirect(any());
        verify(chain).doFilter(req, resp);
    }

    @Test
    void postToOtherPath_passesThrough() throws Exception {
        HttpServletRequest req = request("/api/data", null, "POST");
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(resp, never()).sendRedirect(any());
        verify(chain).doFilter(req, resp);
    }

    @Test
    void postToLogoutUrl_noSession_noNpe() throws Exception {
        HttpServletRequest req = request("/logout", null, "POST");
        when(req.getSession(false)).thenReturn(null);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(resp).sendRedirect("/login?logout");
        verify(chain, never()).doFilter(any(), any());
    }

    // Non-root context path: getRequestURI() would be "/app/logout", but getServletPath() is still "/logout"
    @Test
    void nonRootContextPath_postToLogoutUrl_performsLogout() throws Exception {
        HttpServletRequest req = request("/logout", null, "POST");
        when(req.getRequestURI()).thenReturn("/app/logout");
        when(req.getContextPath()).thenReturn("/app");
        when(req.getSession(false)).thenReturn(null);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(resp).sendRedirect("/login?logout");
        verify(chain, never()).doFilter(any(), any());
    }

    // Prefix-mapped servlet: getServletPath() = "/api", getPathInfo() = "/logout"
    @Test
    void prefixMappedServlet_pathInfo_performsLogout() throws Exception {
        HttpServletRequest req = request("/api", "/logout", "POST");
        when(req.getSession(false)).thenReturn(null);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(resp).sendRedirect("/login?logout");
        verify(chain, never()).doFilter(any(), any());
    }
}
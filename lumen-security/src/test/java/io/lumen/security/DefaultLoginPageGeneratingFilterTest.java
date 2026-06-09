package io.lumen.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultLoginPageGeneratingFilterTest {

    private final DefaultLoginPageGeneratingFilter filter = new DefaultLoginPageGeneratingFilter();

    private HttpServletRequest request(String servletPath, String pathInfo, String method) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getServletPath()).thenReturn(servletPath);
        when(req.getPathInfo()).thenReturn(pathInfo);
        when(req.getMethod()).thenReturn(method);
        return req;
    }

    // Embedded Tomcat with DispatcherServlet mapped to /*:
    // getServletPath() = "/login", getPathInfo() = null
    @Test
    void embeddedTomcat_servletPath_rendersLoginPage() throws Exception {
        HttpServletRequest req = request("/login", null, "GET");
        when(req.getParameter("error")).thenReturn(null);
        when(req.getParameter("logout")).thenReturn(null);

        StringWriter body = new StringWriter();
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getWriter()).thenReturn(new PrintWriter(body));

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, resp, chain);

        verify(chain, never()).doFilter(any(), any());
        assertTrue(body.toString().contains("<form action=\"/login\""), "login form should be rendered");
    }

    @Test
    void pathInfo_rendersLoginPage() throws Exception {
        HttpServletRequest req = request("", "/login", "GET");
        when(req.getParameter("error")).thenReturn(null);
        when(req.getParameter("logout")).thenReturn(null);

        StringWriter body = new StringWriter();
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getWriter()).thenReturn(new PrintWriter(body));

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, resp, chain);

        verify(chain, never()).doFilter(any(), any());
        assertTrue(body.toString().contains("<form action=\"/login\""), "login form should be rendered");
    }

    @Test
    void nonLoginPath_passesThrough() throws Exception {
        HttpServletRequest req = request("/api/tasks", null, "GET");
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(req, resp);
    }

    @Test
    void postToLogin_passesThrough() throws Exception {
        HttpServletRequest req = request("/login", null, "POST");
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(chain).doFilter(req, resp);
    }

    @Test
    void errorParam_rendersErrorMessage() throws Exception {
        HttpServletRequest req = request("/login", null, "GET");
        when(req.getParameter("error")).thenReturn("");
        when(req.getParameter("logout")).thenReturn(null);

        StringWriter body = new StringWriter();
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilter(req, resp, mock(FilterChain.class));

        assertTrue(body.toString().contains("Invalid credentials"), "error message should appear");
    }
}
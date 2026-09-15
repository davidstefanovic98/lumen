package io.lumen.security;

import io.lumen.security.authentication.Authentication;
import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.manager.AuthenticationManager;
import io.lumen.security.context.SecurityContext;
import io.lumen.security.repository.HttpSessionSecurityContextRepository;
import io.lumen.security.repository.SecurityContextRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsernamePasswordAuthenticationFilterTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final UsernamePasswordAuthenticationFilter filter = new UsernamePasswordAuthenticationFilter(
            authenticationManager, securityContextRepository, "/login", "/", "/login?error");

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

    // Embedded Tomcat, root context path: getServletPath() = "/login", getPathInfo() = null
    @Test
    void rootContextPath_postToLogin_authenticates() throws Exception {
        HttpServletRequest req = request("/login", null, "POST");
        when(req.getParameter("username")).thenReturn("alice");
        when(req.getParameter("password")).thenReturn("secret");
        when(req.getSession(true)).thenReturn(mock(jakarta.servlet.http.HttpSession.class));

        Authentication result = new UsernamePasswordAuthenticationToken("alice", null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(result);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(authenticationManager).authenticate(any());
        verify(chain, never()).doFilter(any(), any());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
    }

    // The filter must delegate the post-login save to the SecurityContextRepository abstraction
    // (not write the session attribute itself) so a swapped repository implementation is honored.
    @Test
    void successfulLogin_savesThroughConfiguredRepository() throws Exception {
        SecurityContextRepository repository = mock(SecurityContextRepository.class);
        UsernamePasswordAuthenticationFilter filterWithMockRepo = new UsernamePasswordAuthenticationFilter(
                authenticationManager, repository, "/login", "/", "/login?error");

        HttpServletRequest req = request("/login", null, "POST");
        when(req.getParameter("username")).thenReturn("alice");
        when(req.getParameter("password")).thenReturn("secret");

        Authentication result = new UsernamePasswordAuthenticationToken("alice", null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(result);

        HttpServletResponse resp = mock(HttpServletResponse.class);

        filterWithMockRepo.doFilter(req, resp, mock(FilterChain.class));

        ArgumentCaptor<SecurityContext> captor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(repository).saveContext(captor.capture(), eq(req), eq(resp));
        assertEquals(result, captor.getValue().getAuthentication());
        verify(resp).sendRedirect("/");
    }

    // Non-root context path: getRequestURI() would be "/app/login", but getServletPath() is still "/login"
    @Test
    void nonRootContextPath_postToLogin_authenticates() throws Exception {
        HttpServletRequest req = request("/login", null, "POST");
        when(req.getRequestURI()).thenReturn("/app/login");
        when(req.getContextPath()).thenReturn("/app");
        when(req.getParameter("username")).thenReturn("alice");
        when(req.getParameter("password")).thenReturn("secret");
        when(req.getSession(true)).thenReturn(mock(jakarta.servlet.http.HttpSession.class));

        Authentication result = new UsernamePasswordAuthenticationToken("alice", null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(result);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(authenticationManager).authenticate(any());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void getToLogin_passesThrough() throws Exception {
        HttpServletRequest req = request("/login", null, "GET");
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(authenticationManager, never()).authenticate(any());
        verify(chain).doFilter(req, resp);
    }

    @Test
    void postToOtherPath_passesThrough() throws Exception {
        HttpServletRequest req = request("/api/tasks", null, "POST");
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, resp, chain);

        verify(authenticationManager, never()).authenticate(any());
        verify(chain).doFilter(req, resp);
    }

    // Prefix-mapped DispatcherServlet (e.g. /api/*): getServletPath() = "/api", getPathInfo() = "/login"
    @Test
    void prefixMappedServlet_pathInfo_authenticates() throws Exception {
        UsernamePasswordAuthenticationFilter prefixFilter = new UsernamePasswordAuthenticationFilter(
                authenticationManager, securityContextRepository, "/login", "/", "/login?error");
        HttpServletRequest req = request("/api", "/login", "POST");
        when(req.getParameter("username")).thenReturn("alice");
        when(req.getParameter("password")).thenReturn("secret");
        when(req.getSession(true)).thenReturn(mock(jakarta.servlet.http.HttpSession.class));

        Authentication result = new UsernamePasswordAuthenticationToken("alice", null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(result);

        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        prefixFilter.doFilter(req, resp, chain);

        verify(authenticationManager).authenticate(any());
        verify(chain, never()).doFilter(any(), any());
    }
}
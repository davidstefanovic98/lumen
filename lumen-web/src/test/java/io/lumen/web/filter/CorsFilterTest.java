package io.lumen.web.filter;

import io.lumen.web.cors.CorsConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsFilterTest {

    private static HttpServletRequest request(String method, String origin) {
        var req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(method);
        when(req.getHeader("Origin")).thenReturn(origin);
        return req;
    }

    @Test
    void matchedSingleOrigin_isEchoedBack() throws Exception {
        var config = new CorsConfiguration().allowedOrigins("https://app.example.com");
        var filter = new CorsFilter(config);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        filter.doFilter(request("GET", "https://app.example.com"), response, chain);

        verify(response).setHeader("Access-Control-Allow-Origin", "https://app.example.com");
        verify(response).setHeader("Vary", "Origin");
        verify(chain).doFilter(any(), any());
    }

    @Test
    void multipleConfiguredOrigins_onlyMatchedOneIsEchoed_notJoined() throws Exception {
        var config = new CorsConfiguration().allowedOrigins("https://a.example.com", "https://b.example.com");
        var filter = new CorsFilter(config);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        filter.doFilter(request("GET", "https://b.example.com"), response, chain);

        verify(response).setHeader("Access-Control-Allow-Origin", "https://b.example.com");
        verify(response, never()).setHeader(eq("Access-Control-Allow-Origin"),
                contains("https://a.example.com, https://b.example.com"));
    }

    @Test
    void unmatchedOrigin_getsNoCorsHeaders() throws Exception {
        var config = new CorsConfiguration().allowedOrigins("https://allowed.example.com");
        var filter = new CorsFilter(config);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        filter.doFilter(request("GET", "https://evil.example.com"), response, chain);

        verify(response, never()).setHeader(eq("Access-Control-Allow-Origin"), anyString());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void noOriginHeader_isNotTreatedAsCorsRequest() throws Exception {
        var config = new CorsConfiguration().allowedOrigins("https://allowed.example.com");
        var filter = new CorsFilter(config);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        filter.doFilter(request("GET", null), response, chain);

        verify(response, never()).setHeader(eq("Access-Control-Allow-Origin"), anyString());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void wildcardWithoutCredentials_emitsLiteralWildcard() throws Exception {
        var config = new CorsConfiguration().allowedOrigins("*").allowCredentials(false);
        var filter = new CorsFilter(config);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        filter.doFilter(request("GET", "https://anyone.example.com"), response, chain);

        verify(response).setHeader("Access-Control-Allow-Origin", "*");
        verify(response, never()).setHeader(eq("Access-Control-Allow-Credentials"), anyString());
    }

    @Test
    void wildcardWithCredentials_echoesRequestOriginInsteadOfWildcard() throws Exception {
        // allowCredentials defaults to true; browsers reject "*" combined with credentials,
        // so the actual request origin must be reflected back instead.
        var config = new CorsConfiguration().allowedOrigins("*");
        var filter = new CorsFilter(config);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        filter.doFilter(request("GET", "https://anyone.example.com"), response, chain);

        verify(response).setHeader("Access-Control-Allow-Origin", "https://anyone.example.com");
        verify(response).setHeader("Access-Control-Allow-Credentials", "true");
        verify(response, never()).setHeader("Access-Control-Allow-Origin", "*");
    }

    @Test
    void optionsPreflight_matchedOrigin_returns200AndDoesNotInvokeChain() throws Exception {
        var config = new CorsConfiguration().allowedOrigins("https://app.example.com");
        var filter = new CorsFilter(config);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);

        filter.doFilter(request("OPTIONS", "https://app.example.com"), response, chain);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(chain, never()).doFilter(any(), any());
    }
}
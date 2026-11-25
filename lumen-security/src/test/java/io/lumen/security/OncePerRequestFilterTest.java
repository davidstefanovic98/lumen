package io.lumen.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OncePerRequestFilterTest {

    static class CountingFilter extends OncePerRequestFilter {
        int invocations;

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws java.io.IOException, jakarta.servlet.ServletException {
            invocations++;
            chain.doFilter(request, response);
        }
    }

    private final CountingFilter filter = new CountingFilter();
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        // One request object with real attributes, re-dispatched with different dispatcher types
        request = mock(HttpServletRequest.class);
        Map<String, Object> attributes = new HashMap<>();
        doAnswer(inv -> attributes.put(inv.getArgument(0), inv.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        when(request.getAttribute(anyString())).thenAnswer(inv -> attributes.get(inv.<String>getArgument(0)));
    }

    private void dispatch(DispatcherType type) throws Exception {
        when(request.getDispatcherType()).thenReturn(type);
        filter.doFilter(request, response, chain);
    }

    @Test
    void initialRequest_runsFilter() throws Exception {
        dispatch(DispatcherType.REQUEST);

        assertEquals(1, filter.invocations);
        verify(chain).doFilter(request, response);
    }

    @Test
    void forwardWithinSameRequest_skipsFilterButContinuesChain() throws Exception {
        dispatch(DispatcherType.REQUEST);
        dispatch(DispatcherType.FORWARD);

        assertEquals(1, filter.invocations);
        verify(chain, times(2)).doFilter(request, response);
    }

    @Test
    void errorDispatch_skipsFilter() throws Exception {
        dispatch(DispatcherType.REQUEST);
        dispatch(DispatcherType.ERROR);

        assertEquals(1, filter.invocations);
    }

    @Test
    void asyncDispatch_runsFilterAgain() throws Exception {
        dispatch(DispatcherType.REQUEST);
        dispatch(DispatcherType.ASYNC);

        // A CompletableFuture handler's outcome is processed on the ASYNC dispatch, after the
        // original filter invocation has returned - security must be re-established there.
        assertEquals(2, filter.invocations);
    }
}
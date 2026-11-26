package io.lumen.web.exception.handle;

import io.lumen.web.handler.ResponseBodyWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Turns an exception into an HTTP response, in order:
 * <ol>
 *   <li>a matching {@code @ExceptionHandler} ({@link GlobalExceptionHandleResolver}), then
 *       request-body validation failures ({@link MethodArgumentNotValidExceptionResolver}) - the
 *       specific resolvers;</li>
 *   <li>an exception claimed by a {@link FilterChainExceptionMatcher} is rethrown, unwritten, for
 *       the servlet filter that owns it (e.g. security exceptions → ExceptionTranslationFilter).
 *       This runs <em>after</em> the specific resolvers, so a user's own handler for such an
 *       exception wins - the same precedence as Spring MVC;</li>
 *   <li>{@link DefaultExceptionResolver} terminates the chain ({@code @ResponseStatus} on the
 *       exception type, otherwise a logged 500).</li>
 * </ol>
 * Unless step 2 applies, {@link #resolve} always writes a response, so callers never need an
 * "unresolved" fallback of their own.
 */
public class CompositeExceptionResolver {
    private final List<ExceptionResolver> resolvers;
    private final Supplier<List<FilterChainExceptionMatcher>> filterChainMatchers;
    private final DefaultExceptionResolver terminal;

    public CompositeExceptionResolver(ControllerAdviceRegistry adviceRegistry,
                                      ResponseBodyWriter bodyWriter,
                                      Supplier<List<FilterChainExceptionMatcher>> filterChainMatchers) {
        ErrorResponseWriter errorWriter = new ErrorResponseWriter(bodyWriter);
        List<ExceptionResolver> list = new ArrayList<>();
        list.add(new GlobalExceptionHandleResolver(adviceRegistry, bodyWriter));
        list.add(new MethodArgumentNotValidExceptionResolver(errorWriter));

        this.resolvers = list;
        this.filterChainMatchers = filterChainMatchers;
        this.terminal = new DefaultExceptionResolver(errorWriter);
    }

    /**
     * @throws ServletException wrapping a checked exception claimed by a
     *         {@link FilterChainExceptionMatcher}; an unchecked one is rethrown as-is
     */
    public void resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) throws ServletException {
        for (ExceptionResolver resolver : resolvers) {
            if (resolver.resolve(req, resp, ex)) {
                return;
            }
        }
        if (isClaimedByFilterChain(ex)) {
            if (ex instanceof RuntimeException re) throw re;
            throw new ServletException(ex);
        }
        terminal.resolve(req, resp, ex);
    }

    private boolean isClaimedByFilterChain(Throwable ex) {
        List<FilterChainExceptionMatcher> matchers = filterChainMatchers.get();
        for (Throwable t = ex; t != null; t = t.getCause()) {
            for (FilterChainExceptionMatcher matcher : matchers) {
                if (matcher.matches(t)) return true;
            }
            if (t.getCause() == t) break;
        }
        return false;
    }
}
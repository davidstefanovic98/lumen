package io.lumen.security.context;

import io.lumen.core.task.TaskDecorator;
import io.lumen.security.authentication.Authentication;

/**
 * Propagates the {@link SecurityContext} across {@code @Async} thread boundaries.
 *
 * <p>The current {@link Authentication} is captured on the submitting thread when the task
 * is decorated, then re-established on the worker thread for the duration of the run and
 * cleared afterwards. This makes {@code SecurityContextHolder.getContext().getAuthentication()}
 * (and therefore {@code @PreAuthorize}, {@code hasRole(...)}, etc.) work inside async methods.
 *
 * <p>Discovered automatically by {@code lumen-async} as a {@link TaskDecorator} bean — no user
 * configuration required. A fresh {@link DefaultSecurityContext} is created per task so worker
 * threads never share a mutable context with the caller.
 */
public class SecurityContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Captured on the submitting thread.
        Authentication captured = SecurityContextHolder.getContext().getAuthentication();

        return () -> {
            SecurityContext previous = SecurityContextHolder.getContext();
            try {
                DefaultSecurityContext ctx = new DefaultSecurityContext();
                ctx.setAuthentication(captured);
                SecurityContextHolder.setContext(ctx);
                runnable.run();
            } finally {
                if (previous != null) {
                    SecurityContextHolder.setContext(previous);
                } else {
                    SecurityContextHolder.clear();
                }
            }
        };
    }
}
package io.lumen.security.context;

public class SecurityContextHolder {
    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();

    public static SecurityContext getContext() {
        SecurityContext ctx = contextHolder.get();
        if (ctx == null) {
            ctx = new DefaultSecurityContext();
            contextHolder.set(ctx);
        }
        return ctx;
    }

    public static void setContext(SecurityContext context) {
        contextHolder.set(context);
    }

    public static void clear() {
        contextHolder.remove();
    }
}

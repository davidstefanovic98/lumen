package io.lumen.security.method;

import io.lumen.core.interceptor.MethodInvocation;
import io.lumen.security.annotation.PostAuthorize;
import io.lumen.security.annotation.PreAuthorize;
import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.authority.SimpleGrantedAuthority;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MethodSecurityInterceptorTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    interface Greeter {
        String greet();
        String adminOnly();
        String classLevelProtected();
    }

    static class GreeterImpl implements Greeter {
        @Override public String greet()                { return "hello"; }
        @PreAuthorize("hasRole('ADMIN')")
        @Override public String adminOnly()           { return "secret"; }
        @Override public String classLevelProtected() { return "data"; }
    }

    @PreAuthorize("isAuthenticated()")
    static class FullyProtectedGreeterImpl implements Greeter {
        @Override public String greet()                { return "hello"; }
        @Override public String adminOnly()            { return "secret"; }
        @Override public String classLevelProtected()  { return "data"; }
    }

    static class PostAuthorizeGreeterImpl implements Greeter {
        @PostAuthorize("isAuthenticated()")
        @Override public String greet()                { return "hello"; }
        @Override public String adminOnly()            { return "secret"; }
        @Override public String classLevelProtected()  { return "data"; }
    }

    private void authenticate(String... roles) {
        var authorities = List.of(roles).stream()
                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, authorities));
    }

    private MethodInvocation invocation(Object target, String methodName) throws Exception {
        // Resolve method from the target's actual class so @PreAuthorize/@PostAuthorize are visible
        Method method = target.getClass().getMethod(methodName);
        return new MethodInvocation() {
            @Override public Method getMethod()      { return method; }
            @Override public Object[] getArguments() { return new Object[0]; }
            @Override public Object proceed() throws Throwable {
                return method.invoke(target);
            }
        };
    }

    @Test
    void unannotatedMethod_passesThrough() throws Throwable {
        MethodSecurityInterceptor interceptor = new MethodSecurityInterceptor();
        Object result = interceptor.invoke(invocation(new GreeterImpl(), "greet"));
        assertEquals("hello", result);
    }

    @Test
    void preAuthorize_passes_whenRoleMatches() throws Throwable {
        authenticate("ADMIN");
        MethodSecurityInterceptor interceptor = new MethodSecurityInterceptor();
        Object result = interceptor.invoke(invocation(new GreeterImpl(), "adminOnly"));
        assertEquals("secret", result);
    }

    @Test
    void preAuthorize_throws_whenRoleMissing() {
        authenticate("USER");
        MethodSecurityInterceptor interceptor = new MethodSecurityInterceptor();
        assertThrows(AccessDeniedException.class,
                () -> interceptor.invoke(invocation(new GreeterImpl(), "adminOnly")));
    }

    @Test
    void postAuthorize_evaluated_afterMethod() throws Throwable {
        authenticate("USER");
        MethodSecurityInterceptor interceptor = new MethodSecurityInterceptor();
        Object result = interceptor.invoke(invocation(new PostAuthorizeGreeterImpl(), "greet"));
        assertEquals("hello", result);
    }

    @Test
    void postAuthorize_throws_whenExpressionFails() {
        MethodSecurityInterceptor interceptor = new MethodSecurityInterceptor();
        assertThrows(AccessDeniedException.class,
                () -> interceptor.invoke(invocation(new PostAuthorizeGreeterImpl(), "greet")));
    }

    @Test
    void classLevelPreAuthorize_blocksAllMethods_whenNotAuthenticated() throws Throwable {
        MethodSecurityInterceptor interceptor = new MethodSecurityInterceptor();

        assertThrows(AccessDeniedException.class,
                () -> interceptor.invoke(invocation(new FullyProtectedGreeterImpl(), "greet")));
        assertThrows(AccessDeniedException.class,
                () -> interceptor.invoke(invocation(new FullyProtectedGreeterImpl(), "adminOnly")));
    }

    @Test
    void classLevelPreAuthorize_allowsAllMethods_whenAuthenticated() throws Throwable {
        authenticate("USER");
        MethodSecurityInterceptor interceptor = new MethodSecurityInterceptor();

        assertEquals("hello",  interceptor.invoke(invocation(new FullyProtectedGreeterImpl(), "greet")));
        assertEquals("secret", interceptor.invoke(invocation(new FullyProtectedGreeterImpl(), "adminOnly")));
    }
}
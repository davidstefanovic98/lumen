package io.lumen.security;

import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.context.DefaultSecurityContext;
import io.lumen.security.context.SecurityContext;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.security.exception.AccessDeniedException;
import io.lumen.security.repository.SecurityContextRepository;
import io.lumen.web.DispatcherServlet;
import io.lumen.web.Route;
import io.lumen.web.RouteInvoker;
import io.lumen.web.RouteRegistry;
import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.exception.handle.CompositeExceptionResolver;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;
import io.lumen.web.handler.ResponseBodyWriter;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.resource.ResourceProvider;
import io.lumen.web.resource.StaticResourceResultHandler;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Real embedded-Tomcat round trip for controllers returning {@link CompletableFuture}: the
 * future's outcome must go through the same response and exception path as a synchronous
 * return value, including the security filter chain's exception translation.
 */
class AsyncDispatchSecurityTest {

    public static class AsyncController {
        static final AtomicInteger invocations = new AtomicInteger();

        public CompletableFuture<String> ok() {
            invocations.incrementAndGet();
            return CompletableFuture.supplyAsync(() -> "hello");
        }

        public CompletableFuture<String> denied() {
            invocations.incrementAndGet();
            return CompletableFuture.supplyAsync(() -> { throw new AccessDeniedException("nope"); });
        }

        public CompletableFuture<String> boom() {
            invocations.incrementAndGet();
            return CompletableFuture.supplyAsync(() -> { throw new IllegalStateException("async-boom"); });
        }
    }

    /** Every request is authenticated, so an AccessDeniedException must become 403 (not 401). */
    static class AlwaysAuthenticatedRepository implements SecurityContextRepository {
        @Override
        public SecurityContext loadContext(HttpServletRequest request) {
            SecurityContext ctx = new DefaultSecurityContext();
            ctx.setAuthentication(new UsernamePasswordAuthenticationToken("alice", null, List.of()));
            return ctx;
        }

        @Override
        public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {}

        @Override
        public boolean containsContext(HttpServletRequest request) {
            return true;
        }
    }

    private static Tomcat tomcat;
    private static int port;
    private static final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void startServer() throws Exception {
        RouteRegistry registry = new RouteRegistry();
        AsyncController controller = new AsyncController();
        for (String name : List.of("ok", "denied", "boom")) {
            Route route = new Route(controller, AsyncController.class.getMethod(name), "GET", "/async/" + name);
            route.setRest(true);
            registry.register(route);
        }

        ResponseBodyWriter bodyWriter = new ResponseBodyWriter(new HttpMessageConverterRegistry());
        CompositeExceptionResolver exceptionResolver =
                new CompositeExceptionResolver(new ControllerAdviceRegistry(), bodyWriter);
        DispatcherServlet dispatcher = new DispatcherServlet(registry, exceptionResolver,
                new RouteInvoker(bodyWriter, new CompositeMethodArgumentResolver()),
                mock(ResourceProvider.class), new StaticResourceResultHandler());

        LumenSecurityFilter securityFilter = new LumenSecurityFilter(List.of(new SecurityFilterChain("/**", List.of(
                new SecurityContextPersistenceFilter(new AlwaysAuthenticatedRepository()),
                new ExceptionTranslationFilter("/login", false)))));

        tomcat = new Tomcat();
        String baseDir = Files.createTempDirectory("lumen-async-it").toString();
        tomcat.setBaseDir(baseDir);
        tomcat.setPort(0);
        tomcat.getConnector();
        Context context = tomcat.addContext("", baseDir);
        // Registered exactly as AnnotationWebApplicationContext does: async-supported, all dispatcher types.
        context.addServletContainerInitializer((classes, servletContext) -> {
            FilterRegistration.Dynamic filter = servletContext.addFilter("security", securityFilter);
            filter.setAsyncSupported(true);
            filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
            ServletRegistration.Dynamic servlet = servletContext.addServlet("dispatcher", dispatcher);
            servlet.setAsyncSupported(true);
            servlet.addMapping("/*");
        }, null);
        tomcat.start();
        port = tomcat.getConnector().getLocalPort();
    }

    @AfterAll
    static void stopServer() throws Exception {
        tomcat.stop();
        tomcat.destroy();
        SecurityContextHolder.clear();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Accept", "application/json").GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void completedFuture_writesValueThroughNormalResultHandling() throws Exception {
        int before = AsyncController.invocations.get();

        HttpResponse<String> response = get("/async/ok");

        assertEquals(200, response.statusCode());
        assertEquals("hello", response.body());
        // The ASYNC re-dispatch must pick up the stored outcome, not re-invoke the controller
        assertEquals(before + 1, AsyncController.invocations.get());
    }

    @Test
    void futureFailingWithAccessDenied_isTranslatedBySecurityChain_notA500() throws Exception {
        HttpResponse<String> response = get("/async/denied");

        assertEquals(403, response.statusCode());
    }

    @Test
    void futureFailingWithOtherException_goesThroughExceptionResolvers() throws Exception {
        HttpResponse<String> response = get("/async/boom");

        assertEquals(500, response.statusCode());
        assertEquals("{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"async-boom\"}", response.body());
    }
}
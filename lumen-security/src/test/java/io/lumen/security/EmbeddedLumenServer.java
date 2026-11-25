package io.lumen.security;

import io.lumen.security.authentication.UsernamePasswordAuthenticationToken;
import io.lumen.security.context.DefaultSecurityContext;
import io.lumen.security.context.SecurityContext;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * A real embedded Tomcat running Lumen's web stack behind a security filter chain, wired the way
 * {@code LumenWebModule}/{@code LumenSecurityModule}/{@code AnnotationWebApplicationContext} wire
 * it - filters async-supported and mapped for every dispatcher type - for tests whose subject is
 * the interaction between the servlet container, the security filters and the DispatcherServlet.
 */
final class EmbeddedLumenServer implements AutoCloseable {

    /** Every request is authenticated, so an AccessDeniedException translates to 403 (not 401). */
    static final class AlwaysAuthenticatedRepository implements SecurityContextRepository {
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

    private static final HttpClient client = HttpClient.newHttpClient();

    private final Tomcat tomcat;
    private final int port;

    private EmbeddedLumenServer(Tomcat tomcat) {
        this.tomcat = tomcat;
        this.port = tomcat.getConnector().getLocalPort();
    }

    /**
     * Starts a server exposing every public method of {@code controller} as a REST {@code GET}
     * route at {@code pathPrefix + "/" + methodName}.
     */
    static EmbeddedLumenServer start(Object controller, String pathPrefix, Object... advices) throws Exception {
        RouteRegistry registry = new RouteRegistry();
        for (var method : controller.getClass().getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) continue;
            Route route = new Route(controller, method, "GET", pathPrefix + "/" + method.getName());
            route.setRest(true);
            registry.register(route);
        }

        ControllerAdviceRegistry adviceRegistry = new ControllerAdviceRegistry();
        for (Object advice : advices) adviceRegistry.registerAdvice(advice);

        ResponseBodyWriter bodyWriter = new ResponseBodyWriter(new HttpMessageConverterRegistry());
        // What LumenWebModule collects from the container once LumenSecurityModule has registered its matcher
        CompositeExceptionResolver exceptionResolver = new CompositeExceptionResolver(adviceRegistry, bodyWriter,
                () -> List.of(new SecurityExceptionMatcher()));
        DispatcherServlet dispatcher = new DispatcherServlet(registry, exceptionResolver,
                new RouteInvoker(bodyWriter, new CompositeMethodArgumentResolver()),
                mock(ResourceProvider.class), new StaticResourceResultHandler());

        LumenSecurityFilter securityFilter = new LumenSecurityFilter(List.of(new SecurityFilterChain("/**", List.of(
                new SecurityContextPersistenceFilter(new AlwaysAuthenticatedRepository()),
                new ExceptionTranslationFilter("/login", false)))));

        Tomcat tomcat = new Tomcat();
        String baseDir = Files.createTempDirectory("lumen-embedded-it").toString();
        tomcat.setBaseDir(baseDir);
        tomcat.setPort(0);
        tomcat.getConnector();
        Context context = tomcat.addContext("", baseDir);
        context.addServletContainerInitializer((classes, servletContext) -> {
            FilterRegistration.Dynamic filter = servletContext.addFilter("security", securityFilter);
            filter.setAsyncSupported(true);
            filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
            ServletRegistration.Dynamic servlet = servletContext.addServlet("dispatcher", dispatcher);
            servlet.setAsyncSupported(true);
            servlet.addMapping("/*");
        }, null);
        tomcat.start();
        return new EmbeddedLumenServer(tomcat);
    }

    HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Accept", "application/json").GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Override
    public void close() throws Exception {
        tomcat.stop();
        tomcat.destroy();
    }
}
package io.lumen.web;

import io.lumen.context.PackageScanner;
import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.util.ReflectionUtil;
import io.lumen.core.util.Suppliers;
import io.lumen.web.annotation.Controller;
import io.lumen.web.annotation.ControllerAdvice;
import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.context.ControllerAdviceProcessor;
import io.lumen.web.context.ControllerProcessor;
import io.lumen.web.context.WebLumenInitializer;
import io.lumen.web.cors.CorsConfiguration;
import io.lumen.web.exception.handle.CompositeExceptionResolver;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;
import io.lumen.web.exception.handle.FilterChainExceptionMatcher;
import io.lumen.web.filter.CorsFilter;
import io.lumen.web.filter.LoggingFilter;
import io.lumen.web.handler.ResponseBodyWriter;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.resource.ResourceProvider;

@Order(1)
public class LumenWebModule implements LumenModule {

    @Override
    public void init(LightContainer container, String... basePackages) {
        RouteRegistry registry = new RouteRegistry();
        HttpMessageConverterRegistry converterRegistry = new HttpMessageConverterRegistry();
        CompositeMethodArgumentResolver argumentResolver = new CompositeMethodArgumentResolver();

        // One writer shared by controller routes, @ExceptionHandler methods and built-in error
        // responses, so the same return value always produces the same HTTP response.
        ResponseBodyWriter bodyWriter = new ResponseBodyWriter(converterRegistry);

        ControllerAdviceRegistry adviceRegistry = new ControllerAdviceRegistry();
        // Matchers are contributed by other modules (e.g. lumen-security) and by user lights, so
        // they're collected on first use - after the container is initialized - not here.
        CompositeExceptionResolver exceptionResolver = new CompositeExceptionResolver(adviceRegistry, bodyWriter,
                Suppliers.memoize(() -> container.getLights(FilterChainExceptionMatcher.class)));
        RouteInvoker invoker = new RouteInvoker(bodyWriter, argumentResolver);

        container.registerExternalInstance(RouteRegistry.class, registry);
        container.registerExternalInstance(HttpMessageConverterRegistry.class, converterRegistry);
        container.registerExternalInstance(ResponseBodyWriter.class, bodyWriter);
        container.registerExternalInstance(CompositeMethodArgumentResolver.class, argumentResolver);
        container.registerExternalInstance(ControllerAdviceRegistry.class, adviceRegistry);
        container.registerExternalInstance(CompositeExceptionResolver.class, exceptionResolver);
        container.registerExternalInstance(RouteInvoker.class, invoker);

        container.addPostProcessor(new ControllerProcessor(registry));
        container.addPostProcessor(new ControllerAdviceProcessor(adviceRegistry));

        PackageScanner.scan(basePackages).forEach(clazz -> {
            if (ReflectionUtil.hasAnnotation(clazz, Controller.class)) {
                container.register(clazz);
            }
            if (ReflectionUtil.hasAnnotation(clazz, ControllerAdvice.class)) {
                container.register(clazz);
            }
        });
        container.registerExternalInstance(LightContainer.class, container);
        container.registerExternalInstance(LoggingFilter.class, new LoggingFilter());
        container.register(WebLumenInitializer.class);

        // When the user provides a CorsConfiguration bean, auto-create the CorsFilter.
        container.addPostProcessor(new LightProcessor() {
            @Override
            public Object afterInstantiation(LightInstance light, Object instance) {
                if (instance instanceof CorsConfiguration corsConfig
                        && !container.hasLight(CorsFilter.class)) {
                    container.registerExternalInstance(CorsFilter.class, new CorsFilter(corsConfig));
                }
                return instance;
            }
        });
    }
}

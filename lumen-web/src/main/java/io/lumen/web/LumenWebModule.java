package io.lumen.web;

import io.lumen.context.PackageScanner;
import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.component.LightInstance;
import io.lumen.core.component.processor.LightProcessor;
import io.lumen.core.util.ReflectionUtil;
import io.lumen.web.annotation.Controller;
import io.lumen.web.annotation.ControllerAdvice;
import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.context.ControllerAdviceProcessor;
import io.lumen.web.context.ControllerProcessor;
import io.lumen.web.context.WebLumenInitializer;
import io.lumen.web.cors.CorsConfiguration;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;
import io.lumen.web.filter.CorsFilter;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.resource.ResourceProvider;

@Order(1)
public class LumenWebModule implements LumenModule {

    @Override
    public void init(LightContainer container, String... basePackages) {
        RouteRegistry registry = new RouteRegistry();
        HttpMessageConverterRegistry converterRegistry = new HttpMessageConverterRegistry();
        CompositeMethodArgumentResolver argumentResolver = new CompositeMethodArgumentResolver();

        ControllerAdviceRegistry adviceRegistry = new ControllerAdviceRegistry();
        RouteInvoker invoker = new RouteInvoker(converterRegistry, argumentResolver);

        container.registerExternalInstance(RouteRegistry.class, registry);
        container.registerExternalInstance(HttpMessageConverterRegistry.class, converterRegistry);
        container.registerExternalInstance(CompositeMethodArgumentResolver.class, argumentResolver);
        container.registerExternalInstance(ControllerAdviceRegistry.class, adviceRegistry);
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

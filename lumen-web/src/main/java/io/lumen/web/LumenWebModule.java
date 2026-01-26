package io.lumen.web;

import io.lumen.context.PackageScanner;
import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.web.annotation.Controller;
import io.lumen.web.annotation.ControllerAdvice;
import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.context.ControllerAdviceProcessor;
import io.lumen.web.context.ControllerProcessor;
import io.lumen.web.context.WebLumenInitializer;
import io.lumen.web.exception.handle.ControllerAdviceRegistry;
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
            if (clazz.isAnnotationPresent(Controller.class)) {
                container.register(clazz);
            }
            if (clazz.isAnnotationPresent(ControllerAdvice.class)) {
                container.register(clazz);
            }
        });
        container.register(WebLumenInitializer.class);
    }
}

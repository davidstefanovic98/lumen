package io.lumen.mvc;

import io.lumen.context.annotation.Lazy;
import io.lumen.core.LumenInitializer;
import io.lumen.core.annotation.Order;
import io.lumen.core.context.ApplicationContext;
import io.lumen.core.context.ApplicationContextAware;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.mvc.argument.ModelAndViewArgumentResolver;
import io.lumen.mvc.handler.ViewResultHandler;
import io.lumen.mvc.view.ViewResolver;
import io.lumen.web.RouteInvoker;
import io.lumen.web.argument.CompositeMethodArgumentResolver;
import jakarta.servlet.ServletContext;

@Order(2)
public class WebMvcLumenInitializer implements LumenInitializer, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(WebMvcLumenInitializer.class);
    private final ServletContext servletContext;
    private final RouteInvoker routeInvoker;
    private final CompositeMethodArgumentResolver argumentResolver;
    private ApplicationContext applicationContext;

    public WebMvcLumenInitializer(
            ServletContext servletContext,
            RouteInvoker routeInvoker,
            CompositeMethodArgumentResolver argumentResolver
    ) {
        this.servletContext = servletContext;
        this.routeInvoker = routeInvoker;
        this.argumentResolver = argumentResolver;
    }

    @Override
    public void onStartup() {
        if (!applicationContext.getLightContainer().hasLight(ViewResolver.class)) {
            logger.warn("No ViewResolver found in context. Skipping MVC configuration.");
            return;
        }
        ViewResolver viewResolver = applicationContext.getLightContainer().getLight(ViewResolver.class);
        routeInvoker.addResultHandler(new ViewResultHandler(viewResolver));
        argumentResolver.addResolver(new ModelAndViewArgumentResolver());
        String rawContextPath = servletContext.getContextPath();
        String contextPath = rawContextPath.isEmpty() ? "/" : rawContextPath;
        logger.info("Configuring MVC for path: {}", contextPath);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}

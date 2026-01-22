package io.lumen.mvc;

import io.lumen.context.annotation.Component;
import io.lumen.context.annotation.Lazy;
import io.lumen.core.LumenInitializer;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.mvc.argument.ModelAndViewArgumentResolver;
import io.lumen.mvc.handler.ViewResultHandler;
import io.lumen.web.RouteInvoker;
import io.lumen.web.argument.CompositeMethodArgumentResolver;
import io.lumen.web.view.ViewResolver;
import jakarta.servlet.ServletContext;

@Component
public class WebMvcLumenInitializer implements LumenInitializer {
    private static final Logger logger = LoggerFactory.getLogger(WebMvcLumenInitializer.class);
    private final ServletContext servletContext;
    private final RouteInvoker routeInvoker;
    private final CompositeMethodArgumentResolver argumentResolver;
    private final ViewResolver viewResolver;

    public WebMvcLumenInitializer(
            ServletContext servletContext,
            RouteInvoker routeInvoker,
            // Use lazy to avoid because it's something user needs to define.
            @Lazy ViewResolver viewResolver,
            CompositeMethodArgumentResolver argumentResolver
    ) {
        this.servletContext = servletContext;
        this.routeInvoker = routeInvoker;
        this.argumentResolver = argumentResolver;
        this.viewResolver = viewResolver;
    }

    @Override
    public void onStartup() {
        if (viewResolver == null) {
            logger.warn("No ViewResolver found in context. Skipping MVC configuration.");
            return;
        }
        routeInvoker.addResultHandler(new ViewResultHandler(viewResolver));
        argumentResolver.addResolver(new ModelAndViewArgumentResolver());
        String contextPath = servletContext.getContextPath();
        logger.info("Configuring MVC for path: ", "".equals(servletContext.getContextPath()) ? "/" : servletContext.getContextPath());
    }
}

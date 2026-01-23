package io.lumen.web.context;

import io.lumen.context.annotation.Component;
import io.lumen.core.LumenInitializer;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.argument.*;
import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.ServletContext;

@Component
@Order(1)
public class WebLumenInitializer implements LumenInitializer {
    private final CompositeMethodArgumentResolver argumentResolver;
    private final HttpMessageConverterRegistry converterRegistry;
    private final ServletContext servletContext;
    private static final Logger logger = LoggerFactory.getLogger(WebLumenInitializer.class);

    public WebLumenInitializer(CompositeMethodArgumentResolver argumentResolver,
                                HttpMessageConverterRegistry converterRegistry,
                                ServletContext servletContext) {
        this.argumentResolver = argumentResolver;
        this.converterRegistry = converterRegistry;
        this.servletContext = servletContext;
    }

    @Override
    public void onStartup() {
        argumentResolver.addResolver(new PathVariableArgumentResolver());
        argumentResolver.addResolver(new RequestParamArgumentResolver());
        argumentResolver.addResolver(new RequestBodyArgumentResolver(converterRegistry));
        argumentResolver.addResolver(new HttpServletRequestArgumentResolver());
        argumentResolver.addResolver(new HttpServletResponseArgumentResolver());

        String rawContextPath = servletContext.getContextPath();
        String contextPath = rawContextPath.isEmpty() ? "/" : rawContextPath;
        logger.info("Web Module initialized for context: {}", contextPath);
    }
}

package io.lumen.web.context;

import io.lumen.core.LumenInitializer;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.argument.*;
import io.lumen.web.http.HttpMessageConverterRegistry;
import jakarta.servlet.ServletContext;

@Order(1)
public class WebLumenInitializer implements LumenInitializer {

    private final CompositeMethodArgumentResolver argumentResolver;
    private final HttpMessageConverterRegistry converterRegistry;
    private final ServletContext servletContext;
    private final LightContainer container;
    private static final Logger logger = LoggerFactory.getLogger(WebLumenInitializer.class);

    public WebLumenInitializer(CompositeMethodArgumentResolver argumentResolver,
                                HttpMessageConverterRegistry converterRegistry,
                                ServletContext servletContext,
                                LightContainer container) {
        this.argumentResolver = argumentResolver;
        this.converterRegistry = converterRegistry;
        this.servletContext = servletContext;
        this.container = container;
    }

    @Override
    public void onStartup() {
        argumentResolver.addResolver(new PathVariableArgumentResolver());
        argumentResolver.addResolver(new RequestParamArgumentResolver());
        argumentResolver.addResolver(new RequestHeaderArgumentResolver());

        // onStartup() runs after container.initialize(), so every RequestBodyValidator light -
        // contributed by a module (e.g. lumen-validation) or by the user - already exists here.
        argumentResolver.addResolver(new RequestBodyArgumentResolver(converterRegistry,
                container.getLights(RequestBodyValidator.class)));

        argumentResolver.addResolver(new HttpServletRequestArgumentResolver());
        argumentResolver.addResolver(new HttpServletResponseArgumentResolver());

        String rawContextPath = servletContext.getContextPath();
        String contextPath = rawContextPath.isEmpty() ? "/" : rawContextPath;
        logger.info("Web Module initialized for context: {}", contextPath);
    }
}

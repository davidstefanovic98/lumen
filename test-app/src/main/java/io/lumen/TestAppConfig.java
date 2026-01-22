package io.lumen;

import io.lumen.context.annotation.ComponentScan;
import io.lumen.context.annotation.Configuration;
import io.lumen.context.annotation.Light;
import io.lumen.core.annotation.PostConstruct;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.web.thymeleaf.ThymeleafViewResolver;
import io.lumen.web.view.ViewResolver;
import jakarta.servlet.ServletContext;

@Configuration
@ComponentScan(basePackages = "io.lumen")
public class TestAppConfig {
    private static final Logger logger = LoggerFactory.getLogger(TestAppConfig.class);

    @PostConstruct
    public void init() {
        logger.info("PostConstruct called on TestAppConfig (Proxy)");
    }

    @Light
    public ViewResolver viewResolver(ServletContext context) {
        return new ThymeleafViewResolver(context);
    }
}

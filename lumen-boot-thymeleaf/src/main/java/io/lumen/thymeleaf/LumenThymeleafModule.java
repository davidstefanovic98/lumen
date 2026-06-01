package io.lumen.thymeleaf;

import io.lumen.core.LumenModule;
import io.lumen.core.annotation.Order;
import io.lumen.core.component.LightContainer;
import io.lumen.core.context.Environment;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Order(2)
public class LumenThymeleafModule implements LumenModule {

    private static final Logger logger = LoggerFactory.getLogger(LumenThymeleafModule.class);

    private static final String VIEW_RESOLVER_CLASS = "io.lumen.mvc.view.ViewResolver";

    @Override
    public void init(LightContainer container, String... basePackages) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        container.registerExternalInstance(TemplateEngine.class, engine);
        logger.info("Thymeleaf TemplateEngine registered (prefix=templates/, suffix=.html)");

        // First we check if mvc is on the classpath
        // then we check if the user actually wants thymeleaf as a view resolver
        // by putting the property inside the application.properties
        if (isMvcOnClasspath()) {
            if (isMvcIntegrationEnabled(container)) {
                container.register(ThymeleafViewResolver.class);
                logger.info("ThymeleafViewResolver auto-configured (lumen-web-mvc detected on classpath)");
            } else {
                logger.info("ThymeleafViewResolver skipped: lumen-web-mvc is present, but integration was explicitly disabled via properties");
            }
        } else {
            logger.debug("ThymeleafViewResolver skipped: lumen-web-mvc classes not found on classpath");
        }
    }

    private boolean isMvcOnClasspath() {
        try {
            Class.forName(VIEW_RESOLVER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean isMvcIntegrationEnabled(LightContainer container) {
        Environment env = container.getLight(Environment.class);
        if (env == null) {
            return true;
        }
        String property = env.getProperty("lumen.thymeleaf.mvc.enabled", "true");
        return Boolean.parseBoolean(property);
    }
}
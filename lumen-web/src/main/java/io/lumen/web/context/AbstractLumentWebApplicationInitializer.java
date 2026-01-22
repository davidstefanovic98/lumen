package io.lumen.web.context;

import jakarta.servlet.ServletContext;

public abstract class AbstractLumentWebApplicationInitializer implements WebApplicationInitializer {
    @Override
    public void onStartup(ServletContext servletContext) {
        AnnotationWebApplicationContext lumenContext =
                new AnnotationWebApplicationContext(getConfigClass(), getPort());

        var container = lumenContext.getApplicationContext().getLightContainer();
        container.registerExternalInstance(ServletContext.class, servletContext);

        lumenContext.getApplicationContext().initialize();
        lumenContext.registerFilters(servletContext);
        lumenContext.refreshWebComponents();
    }
    protected abstract Class<?> getConfigClass();
    protected abstract int getPort();
}

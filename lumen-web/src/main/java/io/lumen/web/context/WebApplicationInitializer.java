package io.lumen.web.context;

import jakarta.servlet.ServletContext;

public interface WebApplicationInitializer {
    void onStartup(ServletContext ctx);
}

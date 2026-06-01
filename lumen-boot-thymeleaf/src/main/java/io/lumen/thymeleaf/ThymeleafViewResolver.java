package io.lumen.thymeleaf;

import io.lumen.mvc.view.ViewResolver;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.Map;

public class ThymeleafViewResolver implements ViewResolver {

    private final TemplateEngine engine;
    private final JakartaServletWebApplication webApp;

    public ThymeleafViewResolver(TemplateEngine engine, ServletContext servletContext) {
        this.engine = engine;
        this.webApp = JakartaServletWebApplication.buildApplication(servletContext);
    }

    @Override
    public void resolve(String viewName, Map<String, Object> model,
                        HttpServletRequest req, HttpServletResponse resp) throws Exception {
        WebContext context = new WebContext(webApp.buildExchange(req, resp));
        context.setVariables(model);
        resp.setContentType("text/html;charset=UTF-8");
        engine.process(viewName, context, resp.getWriter());
    }
}
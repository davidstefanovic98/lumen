package io.lumen.mvc.view;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface ViewResolver {
    void resolve(String viewName, Map<String, Object> model,
                 HttpServletRequest req, HttpServletResponse resp) throws Exception;
}
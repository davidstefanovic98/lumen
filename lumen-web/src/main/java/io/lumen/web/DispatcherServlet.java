package io.lumen.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class DispatcherServlet extends HttpServlet {

    private final RouteRegistry registry;
    private final RouteInvoker invoker;

    public DispatcherServlet(RouteRegistry registry) {
        this.registry = registry;
        this.invoker = new RouteInvoker();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        String method = req.getMethod();

        try {
            RouteMatch match = registry.findMatch(path, method);

            if (match == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("Not Found");
                return;
            }

            Object result = invoker.invoke(match, req);
            resp.getWriter().write(result.toString());

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal Server Error: " + e.getMessage());
        }
    }
}
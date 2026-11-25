package io.lumen.web.handler;

import io.lumen.web.Route;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RestResultHandler implements RouteResultHandler {
    private final ResponseBodyWriter bodyWriter;

    public RestResultHandler(ResponseBodyWriter bodyWriter) {
        this.bodyWriter = bodyWriter;
    }

    @Override
    public boolean supports(Object result, Route route) {
        return route.isRest();
    }

    @Override
    public void handle(Object result, Object[] args, Route route, HttpServletRequest req, HttpServletResponse resp) {
        bodyWriter.write(result, route.getMethod(), req, resp);
    }
}

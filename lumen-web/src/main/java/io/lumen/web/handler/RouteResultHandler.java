package io.lumen.web.handler;

import io.lumen.web.Route;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface RouteResultHandler {
    boolean supports(Object returnValue, Route route);

    void handle(Object result, Object[] args, Route route, HttpServletRequest req, HttpServletResponse resp) throws Exception;
}

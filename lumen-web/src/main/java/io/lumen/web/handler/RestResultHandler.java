package io.lumen.web.handler;

import io.lumen.web.Route;
import io.lumen.web.http.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RestResultHandler implements RouteResultHandler {
    private final HttpMessageConverterRegistry converterRegistry;

    public RestResultHandler(HttpMessageConverterRegistry converterRegistry) {
        this.converterRegistry = converterRegistry;
    }

    @Override
    public boolean supports(Object result, Route route) {
        return route.isRest();
    }

    @Override
    public void handle(Object result, Object[] args, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Object body = result;

        if (result instanceof ResponseEntity<?> entity) {
            resp.setStatus(entity.getStatus());
            entity.getHeaders().forEach(resp::setHeader);
            body = entity.getBody();
            if (body == null) return;
        } else {
            resp.setStatus(HttpStatus.OK.value());
        }

        if (resp.getContentType() == null) {
            String acceptHeader = req.getHeader("Accept");
            HttpMessageConverter best = converterRegistry.findBestConverter(body.getClass(), acceptHeader);

            if (best != null) {
                MediaType selected = best.getSupportedMediaTypes().stream()
                        .filter(m -> !m.equals(MediaType.ALL))
                        .findFirst()
                        .orElse(MediaType.APPLICATION_JSON);

                resp.setContentType(selected + ";charset=UTF-8");
            } else {
                resp.setContentType("application/json;charset=UTF-8");
            }
        }
        converterRegistry.write(body, body.getClass(), resp);
    }
}

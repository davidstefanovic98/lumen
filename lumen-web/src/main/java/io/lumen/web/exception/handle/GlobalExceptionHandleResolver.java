package io.lumen.web.exception.handle;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.core.util.ReflectionUtil;
import io.lumen.web.http.HttpMessageConverterRegistry;
import io.lumen.web.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.InvocationTargetException;

public class GlobalExceptionHandleResolver implements ExceptionResolver {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandleResolver.class);

    private final ControllerAdviceRegistry registry;
    private final HttpMessageConverterRegistry converterRegistry;

    public GlobalExceptionHandleResolver(ControllerAdviceRegistry registry,
                                         HttpMessageConverterRegistry converterRegistry) {
        this.registry = registry;
        this.converterRegistry = converterRegistry;
    }


    @Override
    public boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        return resolveRecursively(req, resp, ex);
    }

    private boolean resolveRecursively(HttpServletRequest req, HttpServletResponse resp, Throwable throwable) {
        if (throwable == null) return false;

        HandlerMethod handler = registry.findHandler(throwable);

        if (handler != null) {
            try {
                Object result = handler.method().invoke(handler.light(), throwable);
                handleResult(result, resp);
                return true;
            } catch (Exception e) {
                Throwable cause = e instanceof InvocationTargetException ite ? ite.getTargetException() : e;
                logger.error("@ExceptionHandler {}.{}() threw while handling {}: {}",
                        ReflectionUtil.getUserClass(handler.light().getClass()).getSimpleName(),
                        handler.method().getName(), throwable.getClass().getName(), cause.getMessage(), cause);
                return false;
            }
        }

        Throwable nextToTry = (throwable instanceof InvocationTargetException ite)
                ? ite.getTargetException()
                : throwable.getCause();

        if (nextToTry != null && nextToTry != throwable) {
            return resolveRecursively(req, resp, nextToTry);
        }

        return false;
    }

    private void handleResult(Object result, HttpServletResponse resp) {
        if (result instanceof ResponseEntity<?> entity) {
            resp.setStatus(entity.getStatus());
            entity.getHeaders().forEach(resp::setHeader);
            Object body = entity.getBody();
            if (body == null)
                return;

            if (resp.getContentType() == null) {
                if (body instanceof String) {
                    resp.setContentType("text/plain;charset=UTF-8");
                } else {
                    resp.setContentType("application/json;charset=UTF-8");
                }
            }
            converterRegistry.write(body, body.getClass(), resp);
        } else if (result != null) {
            resp.setStatus(200);
            converterRegistry.write(result, result.getClass(), resp);
        }
    }
}

package io.lumen.web.exception.handle;

import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import io.lumen.core.util.ReflectionUtil;
import io.lumen.web.handler.ResponseBodyWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.InvocationTargetException;

public class GlobalExceptionHandleResolver implements ExceptionResolver {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandleResolver.class);

    private final ControllerAdviceRegistry registry;
    private final ResponseBodyWriter bodyWriter;

    public GlobalExceptionHandleResolver(ControllerAdviceRegistry registry, ResponseBodyWriter bodyWriter) {
        this.registry = registry;
        this.bodyWriter = bodyWriter;
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
                bodyWriter.write(result, handler.method(), req, resp);
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
}

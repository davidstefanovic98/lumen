package io.lumen.web.exception.handle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles ValidationException from lumen-validation (optional dependency).
 * Uses reflection throughout to avoid hard linkage to the validation module.
 */
class ValidationExceptionResolver implements ExceptionResolver {

    private static final String VALIDATION_EXCEPTION = "io.lumen.validation.ValidationException";

    @Override
    public boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex) {
        Throwable ve = unwrap(ex);
        if (ve == null) return false;

        resp.setStatus(422);
        resp.setContentType("application/json");
        try {
            Method getViolations = ve.getClass().getMethod("getViolations");
            @SuppressWarnings("unchecked")
            List<?> violations = (List<?>) getViolations.invoke(ve);

            String json = violations.stream()
                    .map(v -> {
                        try {
                            String field = (String) v.getClass().getMethod("field").invoke(v);
                            String message = (String) v.getClass().getMethod("message").invoke(v);
                            return "{\"field\":\"" + field + "\",\"message\":\"" + message + "\"}";
                        } catch (Exception e2) {
                            return "{}";
                        }
                    })
                    .collect(Collectors.joining(","));

            resp.getWriter().write(
                    "{\"status\":422,\"error\":\"Unprocessable Entity\",\"violations\":[" + json + "]}"
            );
        } catch (Exception ignored) {}
        return true;
    }

    private Throwable unwrap(Throwable t) {
        while (t != null) {
            if (t.getClass().getName().equals(VALIDATION_EXCEPTION)) return t;
            t = t.getCause();
        }
        return null;
    }
}
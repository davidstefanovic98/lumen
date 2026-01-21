package io.lumen.web.exception.handle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

interface ExceptionResolver {

    /**
     * @return true if it handled the exception and wrote the response,
     * false to let the next resolver try.
     */
    boolean resolve(HttpServletRequest req, HttpServletResponse resp, Exception ex);
}

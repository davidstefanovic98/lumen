package io.lumen.web.argument;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.Map;

/**
 *
 */
public interface MethodArgumentResolver {

    /**
     * This method decides whether this resolver can handle the given method parameter.
     * @param parameter - the method parameter to check
     * @return true if this resolver can handle the parameter, false otherwise
     */
    boolean supports(Parameter parameter);

    /**
     * This method resolves the argument value for the given method parameter.
     * @param parameter - the method parameter to resolve
     * @param request - the current HTTP request
     * @param pathVariables - the path variables extracted from the URL
     * @return the resolved argument value
     */
    Object resolve(Parameter parameter, HttpServletRequest request,
                   HttpServletResponse response, Map<String, String> pathVariables);
}

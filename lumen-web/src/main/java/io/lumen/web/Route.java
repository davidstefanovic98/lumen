package io.lumen.web;

import java.lang.reflect.Method;

/**
 * Immutable route definition
 */
public record Route(Object controller, Method method, String httpMethod, String pathPattern) {}
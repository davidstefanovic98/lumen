package io.lumen.web.exception.handle;

import java.lang.reflect.Method;

public record HandlerMethod(Object light, Method method) {}

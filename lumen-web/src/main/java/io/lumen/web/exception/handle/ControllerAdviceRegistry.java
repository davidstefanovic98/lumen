package io.lumen.web.exception.handle;

import io.lumen.web.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


public class ControllerAdviceRegistry {
    private final Map<Class<? extends Throwable>, HandlerMethod> exceptionLookup = new HashMap<>();

    /**
     * Scans a @ControllerAdvice bean for @ExceptionHandler methods and registers them.
     */
    public void registerAdvice(Object adviceLight) {
        Method[] methods = adviceLight.getClass().getDeclaredMethods();

        for (Method method : methods) {
            if (method.isAnnotationPresent(ExceptionHandler.class)) {
                ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);
                Class<? extends Throwable>[] targetExceptions = annotation.value();

                for (Class<? extends Throwable> exType : targetExceptions) {
                    if (exceptionLookup.containsKey(exType)) {
                        throw new IllegalStateException("Duplicate @ExceptionHandler for " + exType.getName() +
                                ". Found in " + adviceLight.getClass().getSimpleName() + " and " +
                                exceptionLookup.get(exType).light().getClass().getSimpleName());
                    }

                    exceptionLookup.put(exType, new HandlerMethod(adviceLight, method));
                }
            }
        }
    }

    public HandlerMethod findHandler(Throwable ex) {
        Class<?> currentExClass = ex.getClass();

        HandlerMethod handler = exceptionLookup.get(currentExClass);
        if (handler != null) return handler;

        return findClosestMatch(currentExClass);
    }

    private HandlerMethod findClosestMatch(Class<?> exClass) {
        Class<?> bestMatch = null;

        for (Class<? extends Throwable> registeredEx : exceptionLookup.keySet()) {
            if (registeredEx.isAssignableFrom(exClass)) {
                if (bestMatch == null || bestMatch.isAssignableFrom(registeredEx)) {
                    bestMatch = registeredEx;
                }
            }
        }

        return (bestMatch != null) ? exceptionLookup.get(bestMatch) : null;
    }

    public int getCount() {
        return exceptionLookup.size();
    }

    public boolean isEmpty() {
        return exceptionLookup.isEmpty();
    }
}
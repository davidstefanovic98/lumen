package io.lumen.web;

import java.lang.reflect.Method;

public class Route {

    private final Object controller;
    private final Method method;
    private final String httpMethod;
    private final String pathPattern;
    private boolean rest;
    private String[] consumes;
    private String[] produces;

    public Route(Object controller, Method method, String httpMethod, String pathPattern) {
        this.controller = controller;
        this.method = method;
        this.httpMethod = httpMethod;
        this.pathPattern = pathPattern;
        this.rest = false;
    }

    public Object getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public boolean isRest() {
        return rest;
    }

    public void setRest(boolean rest) {
        this.rest = rest;
    }

    public String[] getConsumes() {
        return consumes;
    }

    public void setConsumes(String[] consumes) {
        this.consumes = consumes;
    }

    public String[] getProduces() {
        return produces;
    }

    public void setProduces(String[] produces) {
        this.produces = produces;
    }
}

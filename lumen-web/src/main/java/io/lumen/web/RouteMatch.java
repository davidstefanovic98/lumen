package io.lumen.web;

import java.util.Map;

record RouteMatch(Route route, Map<String, String> pathVariables) {}

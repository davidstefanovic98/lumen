package io.lumen.data.web;

import io.lumen.data.pageable.PageRequest;
import io.lumen.data.pageable.Pageable;
import io.lumen.data.pageable.Sort;
import io.lumen.web.argument.MethodArgumentResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PageableMethodArgumentResolver implements MethodArgumentResolver {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 2000;

    @Override
    public boolean supports(Parameter parameter) {
        return Pageable.class.isAssignableFrom(parameter.getType());
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request,
                          HttpServletResponse response, Map<String, String> pathVariables) {
        int page = parseIntParam(request, "page", DEFAULT_PAGE);
        int size = Math.min(parseIntParam(request, "size", DEFAULT_SIZE), MAX_SIZE);
        Sort sort = parseSort(request.getParameterValues("sort"));
        return PageRequest.of(page, size, sort);
    }

    private Sort parseSort(String[] sortParams) {
        if (sortParams == null || sortParams.length == 0) return Sort.unsorted();
        List<Sort.Order> orders = new ArrayList<>();
        for (String param : sortParams) {
            String[] parts = param.split(",", 2);
            String property = parts[0].trim();
            if (property.isBlank()) continue;
            Sort.Direction direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, property));
        }
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders.toArray(new Sort.Order[0]));
    }

    private int parseIntParam(HttpServletRequest request, String name, int defaultValue) {
        String value = request.getParameter(name);
        if (value == null) return defaultValue;
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
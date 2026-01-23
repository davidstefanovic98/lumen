package io.lumen.security;

import io.lumen.web.http.HttpMethod;
import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

public class RegexRequestMatcher implements RequestMatcher {
    private final Pattern pattern;
    private final HttpMethod method;

    public RegexRequestMatcher(String regex, HttpMethod method) {
        this.pattern = Pattern.compile(regex);
        this.method = method;
    }

    public RegexRequestMatcher(String regex) {
        this(regex, null);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        if (method != null && !method.matches(request.getMethod())) {
            return false;
        }
        String path = request.getPathInfo();
        if (path == null) {
            path = request.getServletPath();
        }
        return pattern.matcher(path).matches();
    }
}

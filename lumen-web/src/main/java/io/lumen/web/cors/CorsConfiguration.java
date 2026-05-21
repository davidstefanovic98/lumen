package io.lumen.web.cors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CorsConfiguration {

    private List<String> allowedOrigins = new ArrayList<>();
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("*");
    private List<String> exposedHeaders = new ArrayList<>();
    private boolean allowCredentials = true;
    private long maxAge = 3600;

    public CorsConfiguration allowedOrigins(String... origins) {
        this.allowedOrigins = new ArrayList<>(Arrays.asList(origins));
        return this;
    }

    public CorsConfiguration allowedMethods(String... methods) {
        this.allowedMethods = new ArrayList<>(Arrays.asList(methods));
        return this;
    }

    public CorsConfiguration allowedHeaders(String... headers) {
        this.allowedHeaders = new ArrayList<>(Arrays.asList(headers));
        return this;
    }

    public CorsConfiguration exposedHeaders(String... headers) {
        this.exposedHeaders = new ArrayList<>(Arrays.asList(headers));
        return this;
    }

    public CorsConfiguration allowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
        return this;
    }

    public CorsConfiguration maxAge(long seconds) {
        this.maxAge = seconds;
        return this;
    }

    public List<String> getAllowedOrigins()  { return allowedOrigins; }
    public List<String> getAllowedMethods()  { return allowedMethods; }
    public List<String> getAllowedHeaders()  { return allowedHeaders; }
    public List<String> getExposedHeaders()  { return exposedHeaders; }
    public boolean isAllowCredentials()      { return allowCredentials; }
    public long getMaxAge()                  { return maxAge; }
}
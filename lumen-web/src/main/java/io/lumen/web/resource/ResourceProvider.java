package io.lumen.web.resource;

import io.lumen.context.annotation.Component;
import jakarta.servlet.ServletContext;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Component
public class ResourceProvider {
    private final ServletContext servletContext;
    private static final Map<String, String> MIME_MAPPING = new HashMap<>();

    static {
        MIME_MAPPING.put("html", "text/html");
        MIME_MAPPING.put("css",  "text/css");
        MIME_MAPPING.put("js",   "application/javascript");
        MIME_MAPPING.put("png",  "image/png");
        MIME_MAPPING.put("jpg",  "image/jpeg");
        MIME_MAPPING.put("ico",  "image/x-icon");
        MIME_MAPPING.put("json", "application/json");
    }

    public ResourceProvider(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public StaticResource getResource(String path) {
        if (path == null || path.equals("/")) {
            return null;
        }

        return resolve(path);
    }

    public StaticResource getWelcomePage() {
        return resolve("/index.html");
    }

    private StaticResource resolve(String path) {
        String resourcePath = "/static" + (path.startsWith("/") ? path : "/" + path);
        URL url = getClass().getResource(resourcePath);

        if (url == null) return null;

        String mimeType = servletContext.getMimeType(path);
        if (mimeType == null) {
            String extension = getExtension(path);
            mimeType = MIME_MAPPING.getOrDefault(extension.toLowerCase(), "application/octet-stream");
        }

        return new StaticResource(url, mimeType);
    }

    private String getExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        return (lastDot == -1) ? "" : path.substring(lastDot + 1);
    }

    public record StaticResource(URL url, String contentType) {
        public InputStream getInputStream() throws Exception {
            return url.openStream();
        }
    }
}
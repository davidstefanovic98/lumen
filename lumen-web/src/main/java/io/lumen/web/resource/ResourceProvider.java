package io.lumen.web.resource;

import io.lumen.context.annotation.Component;
import io.lumen.core.logging.Logger;
import io.lumen.core.logging.LoggerFactory;
import jakarta.servlet.ServletContext;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Component
public class ResourceProvider {
    private final ServletContext servletContext;
    private static final Map<String, String> MIME_MAPPING = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ResourceProvider.class);

    static {
        MIME_MAPPING.put("html", "text/html");
        MIME_MAPPING.put("htm",  "text/html");
        MIME_MAPPING.put("css",  "text/css");
        MIME_MAPPING.put("js",   "application/javascript");
        MIME_MAPPING.put("png",  "image/png");
        MIME_MAPPING.put("jpg",  "image/jpeg");
        MIME_MAPPING.put("jpeg", "image/jpeg");
        MIME_MAPPING.put("gif",  "image/gif");
        MIME_MAPPING.put("svg",  "image/svg+xml");
        MIME_MAPPING.put("ico",  "image/x-icon");
        MIME_MAPPING.put("json", "application/json");
        MIME_MAPPING.put("txt",  "text/plain");
        MIME_MAPPING.put("xml",  "application/xml");
    }

    public ResourceProvider(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public StaticResource getResource(String path) {
        String normalizedPath = (path == null || path.equals("/")) ? "/index.html" : path;
        String resourcePath = "/static" + (normalizedPath.startsWith("/") ? normalizedPath : "/" + normalizedPath);

        URL url = getClass().getResource(resourcePath);
        if (url == null)
            return null;

        String mimeType = servletContext.getMimeType(normalizedPath);

        if (mimeType == null) {
            String extension = getExtension(normalizedPath);
            mimeType = MIME_MAPPING.getOrDefault(extension.toLowerCase(), "application/octet-stream");
            logger.info("Determined MIME type for extension '{}': {}", extension, mimeType);
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

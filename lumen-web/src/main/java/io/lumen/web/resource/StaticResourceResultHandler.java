package io.lumen.web.resource;

import io.lumen.context.annotation.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;

public class StaticResourceResultHandler {

    public void handle(ResourceProvider.StaticResource resource, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        URLConnection connection = resource.url().openConnection();
        long lastModified = connection.getLastModified();

        if (isNotModified(req, lastModified)) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        String contentType = resource.contentType();
        if (contentType.startsWith("text/")) {
            contentType += ";charset=UTF-8";
        }
        resp.setContentType(contentType);

        long length = connection.getContentLengthLong();
        if (length > -1) {
            resp.setContentLengthLong(length);
        }

        if (lastModified > 0) {
            resp.setDateHeader("Last-Modified", lastModified);
        }

        resp.setHeader("Cache-Control", "public, max-age=3600");
        resp.setHeader("Content-Disposition", "inline");

        try (InputStream is = connection.getInputStream();
             OutputStream os = resp.getOutputStream()) {
            is.transferTo(os);
            os.flush();
        }
    }

    private boolean isNotModified(HttpServletRequest req, long lastModified) {
        long ifModifiedSince = req.getDateHeader("If-Modified-Since");
        return (ifModifiedSince != -1 && (lastModified / 1000) <= (ifModifiedSince / 1000));
    }
}
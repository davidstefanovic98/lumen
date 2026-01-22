package io.lumen.web.resource;

import io.lumen.context.annotation.Component;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.io.OutputStream;

@Component
public class StaticResourceResultHandler {

    public void handle(ResourceProvider.StaticResource resource, HttpServletResponse resp) throws Exception {
        String contentType = resource.contentType();
        if (contentType.startsWith("text/")) {
            contentType += ";charset=UTF-8";
        }
        resp.setContentType(contentType);

        long length = resource.url().openConnection().getContentLengthLong();
        if (length > -1) {
            resp.setContentLengthLong(length);
        }

        resp.setHeader("Content-Disposition", "inline");
        resp.setHeader("Cache-Control", "no-cache");

        try (InputStream is = resource.getInputStream();
             OutputStream os = resp.getOutputStream()) {
             is.transferTo(os);
        }
    }
}

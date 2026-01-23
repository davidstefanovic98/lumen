package io.lumen.web.multipart;

import jakarta.servlet.MultipartConfigElement;

public record MultipartConfig(
        String location,
        long maxFileSize,
        long maxRequestSize,
        int fileSizeThreshold
) {

    public static MultipartConfig defaultConfiguration() {
        return new MultipartConfig(
                System.getProperty("java.io.tmpdir"),
                1024 * 1024 * 10,
                1024 * 1024 * 50,
                1024 * 1024
        );
    }

    public MultipartConfigElement toServletConfig() {
        return new MultipartConfigElement(location, maxFileSize, maxRequestSize, fileSizeThreshold);
    }
}

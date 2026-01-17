package io.lumen;

import io.lumen.web.context.AnnotationWebApplicationContext;
import io.lumen.web.context.WebApplicationContext;

public class Main {
    public static void main(String[] args) {
        WebApplicationContext context =
                new AnnotationWebApplicationContext(TestAppConfig.class, 8080);
        context.startWebServer();
    }
}
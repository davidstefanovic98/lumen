package io.lumen.controller;

import io.lumen.context.annotation.Controller;
import io.lumen.core.annotation.Inject;
import io.lumen.data.User;
import io.lumen.mvc.argument.ModelAndView;
import io.lumen.web.RouteRegistry;
import io.lumen.web.annotation.GetMapping;

import java.time.LocalDateTime;

@Controller
public class HelloController {

    @Inject
    private RouteRegistry routeRegistry;

    @GetMapping("/test")
    public String test(ModelAndView model) {
        model.addObject("frameworkName", "Lumen");
        model.addObject("user", new User("Marija", "marijailicc019@gmail.com", null, null, null));
        model.addObject("isDev", true);
        model.addObject("currentTime", LocalDateTime.now());
        model.addObject("serverPort", 8080);
        model.addObject("javaVersion", System.getProperty("java.version"));

        model.addObject("routes", routeRegistry.getAllRoutes());
        return "hello";
    }
}

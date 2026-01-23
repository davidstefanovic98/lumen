package io.lumen.controller;

import io.lumen.context.annotation.Controller;
import io.lumen.core.annotation.Inject;
import io.lumen.data.User;
import io.lumen.mvc.argument.ModelAndView;
import io.lumen.security.authentication.Authentication;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.web.RouteRegistry;
import io.lumen.web.annotation.GetMapping;

import java.time.LocalDateTime;

@Controller
public class IndexController {

    @Inject
    private RouteRegistry routeRegistry;

    @GetMapping("/")
    public String index(ModelAndView model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addObject("frameworkName", "Lumen");
        model.addObject("user", auth != null ? (User) auth.getPrincipal() : null);
        model.addObject("isDev", true);
        model.addObject("currentTime", LocalDateTime.now());
        model.addObject("serverPort", 8080);
        model.addObject("javaVersion", System.getProperty("java.version"));

        model.addObject("routes", routeRegistry.getAllRoutes());
        return "redirect:/test";
    }
}

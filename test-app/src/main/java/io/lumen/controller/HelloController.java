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
public class HelloController {

    @GetMapping("/test")
    public String test(ModelAndView model) {
        return "hello";
    }
}

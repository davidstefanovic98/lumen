package io.lumen;

import io.lumen.context.annotation.Controller;
import io.lumen.web.annotation.GetMapping;
import io.lumen.web.annotation.PostMapping;

@Controller
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello GET";
    }

    @PostMapping("/hello")
    public String helloPost() {
        return "Hello POST";
    }
}

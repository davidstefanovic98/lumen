package io.lumen;

import io.lumen.context.annotation.Controller;
import io.lumen.data.UserForm;
import io.lumen.web.annotation.GetMapping;
import io.lumen.web.annotation.PathVariable;
import io.lumen.web.annotation.RequestParam;

@Controller
public class TestController {

    @GetMapping("/users/{id}")
    public String getUser(@PathVariable("id") Long id) {
        return "User ID: " + id;
    }

    @GetMapping("/users/{id}/posts/{postId}")
    public String getUserPost(
            @PathVariable("id") Long userId,
            @PathVariable("postId") Long postId) {
        return "User ID: " + userId + ", Post ID: " + postId;
    }

    @GetMapping("/greet/{name}")
    public String greet(@PathVariable("name") String name) {
        return "Hello, " + name + "!";
    }

    @GetMapping("/submit")
    public String submitForm(UserForm form) {
        return "User: " + form.getUsername() + ", Age: " + form.getAge();
    }
}

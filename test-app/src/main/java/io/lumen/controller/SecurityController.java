package io.lumen.controller;

import io.lumen.security.authentication.Authentication;
import io.lumen.security.context.SecurityContextHolder;
import io.lumen.web.annotation.GetMapping;
import io.lumen.web.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class SecurityController {

    @GetMapping("/debug/security")
    public Map<String, Object> debugSecurity() {
        Map<String, Object> report = new HashMap<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            report.put("status", "No Security Context Found");
            report.put("authenticated", false);
        } else {
            report.put("status", "Security Context Active");
            report.put("principal", auth.getPrincipal());
            report.put("username", auth.getName());
            report.put("authenticated", auth.isAuthenticated());

            report.put("authorities", auth.getAuthorities());
        }
        report.put("threadName", Thread.currentThread().getName());

        return report;
    }
}

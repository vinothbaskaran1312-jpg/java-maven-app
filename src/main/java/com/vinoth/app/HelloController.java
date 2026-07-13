package com.vinoth.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String home() {
        return "Hello from Java Maven App! Built by Jenkins CI/CD Pipeline";
    }

    @GetMapping("/health")
    public String health() {
        return "{\"status\": \"UP\", \"app\": \"java-maven-app\"}";
    }
}
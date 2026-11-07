package com.codecrafthub;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CodeCraftHubApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(CodeCraftHubApplication.class);

        // Bind the local lab API to this computer by default.
        application.setDefaultProperties(Map.of("server.address", "127.0.0.1"));
        application.run(args);
    }
}

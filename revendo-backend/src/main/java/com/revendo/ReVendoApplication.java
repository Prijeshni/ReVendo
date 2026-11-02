package com.revendo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ReVendo Application Entry Point.
 * When you run this class, Spring Boot starts the embedded Tomcat
 * server on port 8080 and initializes all components.
 */
@SpringBootApplication
public class ReVendoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReVendoApplication.class, args);
    }
}
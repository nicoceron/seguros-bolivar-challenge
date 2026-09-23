// Purpose of this file: Starts the single Spring Boot app and enables the scheduled outbox worker.
package com.segurosbolivar.policy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PolicyManagementApplication {

    /** Starts Spring Boot, which makes the API routes and scheduled worker available. */
    public static void main(String[] args) {
        SpringApplication.run(PolicyManagementApplication.class, args);
    }
}


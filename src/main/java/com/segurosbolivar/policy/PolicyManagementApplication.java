package com.segurosbolivar.policy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PolicyManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolicyManagementApplication.class, args);
    }
}


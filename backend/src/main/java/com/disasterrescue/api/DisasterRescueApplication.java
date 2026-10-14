package com.disasterrescue.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DisasterRescueApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisasterRescueApplication.class, args);
    }
}

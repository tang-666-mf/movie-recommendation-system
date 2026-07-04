package com.stage4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Stage4Application {
    public static void main(String[] args) {
        SpringApplication.run(Stage4Application.class, args);
    }
}
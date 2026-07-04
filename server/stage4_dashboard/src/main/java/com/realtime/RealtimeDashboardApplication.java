package com.realtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RealtimeDashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(RealtimeDashboardApplication.class, args);
    }
}

package com.movie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MovieSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(MovieSystemApplication.class, args);
    }
}

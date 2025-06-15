package com.debate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = {"com.debate", "config", "util"})
public class DebateApplication {

    public static void main(String[] args) {
        SpringApplication.run(DebateApplication.class, args);
    }

}

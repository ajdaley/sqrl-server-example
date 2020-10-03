package com.github.sqrlserverjava.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
@ServletComponentScan("com.github.sqrlserverjava.util")
public class SqrlTestApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SqrlTestApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SqrlTestApplication.class, args);
    }

}
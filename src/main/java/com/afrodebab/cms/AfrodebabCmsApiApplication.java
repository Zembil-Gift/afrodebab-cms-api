package com.afrodebab.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AfrodebabCmsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AfrodebabCmsApiApplication.class, args);
    }

}

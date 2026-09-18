package com.haruhi.botServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HaruhiBotServer {

    public static void main(String[] args) {
        SpringApplication.run(HaruhiBotServer.class,args);
    }
}

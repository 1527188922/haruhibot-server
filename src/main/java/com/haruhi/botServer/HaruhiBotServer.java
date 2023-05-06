package com.haruhi.botServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class HaruhiBotServer {

    public static void main(String[] args) {
        SpringApplication.run(HaruhiBotServer.class,args);
    }
}

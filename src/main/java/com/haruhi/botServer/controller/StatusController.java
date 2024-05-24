package com.haruhi.botServer.controller;

import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {
    
    private final AbstractWebResourceConfig abstractPathConfig;

    public StatusController(AbstractWebResourceConfig abstractPathConfig) {
        this.abstractPathConfig = abstractPathConfig;
    }

    @GetMapping("/web-home")
    public String status(){
        return abstractPathConfig.webHomePath();
    }
}

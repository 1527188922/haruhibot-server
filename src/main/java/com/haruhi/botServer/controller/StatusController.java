package com.haruhi.botServer.controller;

import com.haruhi.botServer.config.path.AbstractPathConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {
    
    private final AbstractPathConfig abstractPathConfig;

    public StatusController(AbstractPathConfig abstractPathConfig) {
        this.abstractPathConfig = abstractPathConfig;
    }

    @GetMapping("/web-home")
    public String status(){
        return abstractPathConfig.webHomePath();
    }
}

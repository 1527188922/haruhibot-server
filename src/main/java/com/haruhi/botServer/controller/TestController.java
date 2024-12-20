package com.haruhi.botServer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/ping")
    public HttpResp<Map<String,Object>> ping(){
        HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
            put("time",ZonedDateTime.now());
        }};
        return HttpResp.success("pong",hashMap);
    }
}

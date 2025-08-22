package com.haruhi.botServer.controller;

import com.haruhi.botServer.annotation.IgnoreAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.vo.HttpResp;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/test")
public class TestController {

    @IgnoreAuthentication
    @GetMapping("/ping")
    public HttpResp<Map<String,Object>> ping(){
        HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
            put("time",ZonedDateTime.now());
        }};
        return HttpResp.success("pong",hashMap);
    }

    @IgnoreAuthentication
    @PostMapping("/ping/post")
    public HttpResp<Map<String,Object>> pingPost(){
        HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
            put("time",ZonedDateTime.now());
        }};
        return HttpResp.success("pong",hashMap);
    }
}

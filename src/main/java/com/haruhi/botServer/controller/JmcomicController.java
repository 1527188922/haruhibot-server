package com.haruhi.botServer.controller;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.annotation.IgnoreAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.jmcomic.Album;
import com.haruhi.botServer.service.JmcomicService;
import com.haruhi.botServer.vo.HttpResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Slf4j
@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/jmcomic")
public class JmcomicController {


    @Autowired
    private JmcomicService jmcomicService;

    @IgnoreAuthentication
    @GetMapping("/download/{aid}")
    public ResponseEntity<Object> download(@PathVariable("aid") String aid) {
        try {
            Album album = jmcomicService.requestAlbum(aid);
            BaseResp<File> fileBaseResp = jmcomicService.downloadAlbumAsZip(album);
            if (!BaseResp.SUCCESS_CODE.equals(fileBaseResp.getCode())) {
                return ResponseEntity.ok().headers(getResponseHeader(false,null)).body(jsonBody(HttpResp.fail(fileBaseResp.getMsg())));
            }
            return ResponseEntity.ok().headers(getResponseHeader(true,fileBaseResp.getData()))
                    .body(new InputStreamResource(Files.newInputStream(fileBaseResp.getData().toPath())));
        } catch (Exception e) {
            return ResponseEntity.ok().headers(getResponseHeader(false,null)).body(jsonBody(HttpResp.fail(e.getMessage())));
        }
    }

    @IgnoreAuthentication
    @GetMapping("/download/pdf/{aid}")
    public ResponseEntity<Object> downloadPdf(@PathVariable("aid") String aid) {
        try {
            Album album = jmcomicService.requestAlbum(aid);
            BaseResp<File> fileBaseResp = jmcomicService.downloadAlbumAsPdf(album);
            if (!BaseResp.SUCCESS_CODE.equals(fileBaseResp.getCode())) {
                return ResponseEntity.ok().headers(getResponseHeader(false,null)).body(jsonBody(HttpResp.fail(fileBaseResp.getMsg())));
            }
            return ResponseEntity.ok().headers(getResponseHeader(true,fileBaseResp.getData()))
                    .body(new InputStreamResource(Files.newInputStream(fileBaseResp.getData().toPath())));
        } catch (Exception e) {
            return ResponseEntity.ok().headers(getResponseHeader(false,null)).body(jsonBody(HttpResp.fail(e.getMessage())));
        }
    }

    private String jsonBody(HttpResp resp) {
        return JSONObject.toJSONString(resp);
    }

    private HttpHeaders getResponseHeader(boolean isFile,File file) {
        HttpHeaders headers = new HttpHeaders();
        if (isFile) {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", new String(file.getName().getBytes(StandardCharsets.UTF_8), Charset.forName("ISO8859-1")));
            headers.setContentLength(file.length());
        }else{
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return headers;
    }
}

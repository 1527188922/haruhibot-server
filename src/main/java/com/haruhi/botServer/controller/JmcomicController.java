package com.haruhi.botServer.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botServer.annotation.IgnoreAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.jmcomic.Album;
import com.haruhi.botServer.dto.jmcomic.Chapter;
import com.haruhi.botServer.service.JmcomicService;
import com.haruhi.botServer.service.JmcomicSqliteService;
import com.haruhi.botServer.vo.HttpResp;
import com.haruhi.botServer.vo.JmAlbumDeleteReq;
import com.haruhi.botServer.vo.JmAlbumManageResp;
import com.haruhi.botServer.vo.JmAlbumQueryReq;
import com.haruhi.botServer.vo.JmChapterImageDeleteReq;
import com.haruhi.botServer.vo.JmChapterImageManageResp;
import com.haruhi.botServer.vo.JmChapterImageQueryReq;
import com.haruhi.botServer.vo.JmChapterImageResp;
import com.haruhi.botServer.vo.JmChapterInfoResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/jmcomic")
public class JmcomicController {


    @Autowired
    private JmcomicService jmcomicService;

    @Autowired
    private JmcomicSqliteService jmcomicSqliteService;

    @IgnoreAuthentication
    @GetMapping("/download/{aid}")
    public ResponseEntity<Object> download(@PathVariable("aid") String aid) {
        try {
            BaseResp<Album> albumBaseResp = jmcomicService.requestAlbum(aid);
            if (!albumBaseResp.isSuccess()) {
                return ResponseEntity.ok().headers(getResponseHeader(false,null)).body(jsonBody(HttpResp.fail(albumBaseResp.getMsg())));
            }
            Album album = albumBaseResp.getData();
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
            BaseResp<Album> albumBaseResp = jmcomicService.requestAlbum(aid);
            if (!albumBaseResp.isSuccess()) {
                return ResponseEntity.ok().headers(getResponseHeader(false,null)).body(jsonBody(HttpResp.fail(albumBaseResp.getMsg())));
            }
            Album album = albumBaseResp.getData();
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

    @GetMapping("/album/{aid}/chapters")
    public HttpResp<List<JmChapterInfoResp>> chapters(@PathVariable("aid") Long aid) {
        return HttpResp.success(jmcomicSqliteService.listChapters(aid));
    }

    @GetMapping("/album/{aid}/chapter/{chapterId}/images")
    public HttpResp<List<JmChapterImageResp>> chapterImages(@PathVariable("aid") Long aid,
                                                            @PathVariable("chapterId") Long chapterId) {
        return HttpResp.success(jmcomicSqliteService.listChapterImages(aid, chapterId));
    }

    @PostMapping("/manage/album/search")
    public HttpResp<IPage<JmAlbumManageResp>> searchAlbums(@RequestBody JmAlbumQueryReq request) {
        return HttpResp.success(jmcomicSqliteService.searchAlbums(request));
    }

    @PostMapping("/manage/album/request/{aid}")
    public HttpResp<Album> requestAlbum(@PathVariable("aid") String aid) {
        BaseResp<Album> resp = jmcomicService.requestAlbum(aid);
        if (!resp.isSuccess()) {
            return HttpResp.fail(resp.getMsg(), null);
        }
        return HttpResp.success(resp.getData());
    }

    @PostMapping("/manage/album/deleteBatch")
    public HttpResp deleteAlbums(@RequestBody JmAlbumDeleteReq request) {
        jmcomicSqliteService.deleteAlbums(request);
        return HttpResp.success("删除完成", null);
    }

    @PostMapping("/manage/chapter-image/search")
    public HttpResp<IPage<JmChapterImageManageResp>> searchChapterImages(@RequestBody JmChapterImageQueryReq request) {
        return HttpResp.success(jmcomicSqliteService.searchChapterImages(request));
    }

    @PostMapping("/manage/chapter-image/request")
    public HttpResp requestChapterImages(@RequestBody JmChapterImageQueryReq request) {
        if (request == null || request.getAlbumId() == null || request.getChapterId() == null) {
            return HttpResp.fail("缺少JM ID或章节ID", null);
        }
        try {
            Chapter chapter = jmcomicService.requestChapter(String.valueOf(request.getChapterId()));
            jmcomicSqliteService.saveOrUpdateChapterImages(request.getAlbumId(), chapter);
            return HttpResp.success("拉取完成", null);
        } catch (Exception e) {
            return HttpResp.fail("拉取章节异常：" + e.getMessage(), null);
        }
    }

    @PostMapping("/manage/chapter-image/deleteBatch")
    public HttpResp deleteChapterImages(@RequestBody JmChapterImageDeleteReq request) {
        jmcomicSqliteService.deleteChapterImages(request);
        return HttpResp.success("删除完成", null);
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

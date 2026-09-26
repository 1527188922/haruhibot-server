package com.haruhi.botserver.features.jmcomic.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botserver.bootstrap.SysConstants;
import com.haruhi.botserver.shared.model.BaseResp;
import com.haruhi.botserver.features.jmcomic.client.model.Album;
import com.haruhi.botserver.features.jmcomic.client.model.Chapter;
import com.haruhi.botserver.features.jmcomic.service.JmcomicService;
import com.haruhi.botserver.features.jmcomic.service.JmcomicSqliteService;
import com.haruhi.botserver.shared.model.HttpResp;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumCollectReq;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumDeleteReq;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumManageResp;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumOnlineSearchReq;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumOnlineSearchResp;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumQueryReq;
import com.haruhi.botserver.features.jmcomic.model.JmOnlineSearchHistory;
import com.haruhi.botserver.features.jmcomic.model.JmOnlineSearchHistoryDeleteReq;
import com.haruhi.botserver.features.jmcomic.model.JmChapterImageDeleteReq;
import com.haruhi.botserver.features.jmcomic.model.JmChapterImageManageResp;
import com.haruhi.botserver.features.jmcomic.model.JmChapterImageQueryReq;
import com.haruhi.botserver.features.jmcomic.model.JmChapterImageResp;
import com.haruhi.botserver.features.jmcomic.model.JmChapterInfoResp;
import com.haruhi.botserver.features.jmcomic.model.JmTaskSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(SysConstants.CONTEXT_PATH+"/jmcomic")
public class JmcomicController {


    @Autowired
    private JmcomicService jmcomicService;

    @Autowired
    private JmcomicSqliteService jmcomicSqliteService;

    /**
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
    }*/

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

    @GetMapping("/manage/tags")
    public HttpResp<List<String>> allTag() {
        return HttpResp.success(jmcomicSqliteService.allTag());
    }

    @GetMapping("/manage/authors")
    public HttpResp<List<String>> allAuthor() {
        return HttpResp.success(jmcomicSqliteService.allAuthor());
    }

    /**
     * JM在线搜索(调用JM服务器/search接口)，支持排序与分页，返回结果会带上是否已入库
     */
    @PostMapping("/manage/album/searchOnline")
    public HttpResp<JmAlbumOnlineSearchResp> searchOnline(@RequestBody JmAlbumOnlineSearchReq request) {
        try {
            return HttpResp.success(jmcomicService.onlineSearch(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return HttpResp.fail(e.getMessage(), null);
        } catch (Exception e) {
            log.error("JM在线搜索异常", e);
            return HttpResp.fail("JM在线搜索异常：" + e.getMessage(), null);
        }
    }

    /**
     * JM在线搜索历史列表(不含结果快照)，按时间倒序
     */
    @GetMapping("/manage/album/searchOnline/history")
    public HttpResp<List<JmOnlineSearchHistory>> searchOnlineHistory() {
        return HttpResp.success(jmcomicService.listSearchHistory());
    }

    /**
     * JM在线搜索历史详情，含当时那一页的结果快照
     */
    @GetMapping("/manage/album/searchOnline/history/{id}")
    public HttpResp<JmOnlineSearchHistory> searchOnlineHistoryDetail(@PathVariable("id") Long id) {
        JmOnlineSearchHistory history = jmcomicService.getSearchHistory(id);
        if (history == null) {
            return HttpResp.fail("搜索历史不存在或已被清理", null);
        }
        return HttpResp.success(history);
    }

    /**
     * 删除JM在线搜索历史(清空或按id删除)
     */
    @PostMapping("/manage/album/searchOnline/history/delete")
    public HttpResp deleteSearchOnlineHistory(@RequestBody JmOnlineSearchHistoryDeleteReq request) {
        if (request == null) {
            return HttpResp.fail("缺少参数", null);
        }
        if (Boolean.TRUE.equals(request.getClearAll())) {
            jmcomicService.clearSearchHistory();
            return HttpResp.success("清空完成", null);
        }
        if (CollectionUtils.isEmpty(request.getIds())) {
            return HttpResp.fail("缺少要删除的记录id", null);
        }
        jmcomicService.deleteSearchHistory(request.getIds());
        return HttpResp.success("删除完成", null);
    }

    @PostMapping("/manage/album/request/{aid}")
    public HttpResp<Album> requestAlbum(@PathVariable("aid") String aid) {
        BaseResp<Album> resp = jmcomicService.requestAlbum(aid);
        if (!resp.isSuccess()) {
            return HttpResp.fail(resp.getMsg(), null);
        }
        return HttpResp.success(resp.getData());
    }

    @PostMapping("/manage/album/download/{aid}")
    public HttpResp<String> downloadAlbumManage(@PathVariable("aid") String aid) {
        return toHttpResp(jmcomicService.manageDownloadAlbum(aid));
    }

    @PostMapping("/manage/album/generateZip/{aid}")
    public HttpResp<String> generateZip(@PathVariable("aid") String aid) {
        return toHttpResp(jmcomicService.manageGenerateZip(aid));
    }

    @PostMapping("/manage/album/generatePdf/{aid}")
    public HttpResp<String> generatePdf(@PathVariable("aid") String aid) {
        return toHttpResp(jmcomicService.manageGeneratePdf(aid));
    }

    /**
     * 内存中的JM任务列表(运行中/排队中/最近完成)，供管理端轮询展示
     * <p>
     * 只反映当前进程的内存状态，不持久化，重启即清空
     */
    @GetMapping("/manage/task/list")
    public HttpResp<JmTaskSnapshot> listTasks() {
        return HttpResp.success(jmcomicService.listTasks());
    }

    /**
     * 取消排队中的任务。正在执行的任务不支持取消
     */
    @PostMapping("/manage/task/cancel/{taskId}")
    public HttpResp<String> cancelTask(@PathVariable("taskId") String taskId) {
        String error = jmcomicService.cancelTask(taskId);
        if (error != null) {
            return HttpResp.fail(error, null);
        }
        return HttpResp.success("已取消排队任务", null);
    }

    /**
     * 取消全部排队中的任务
     */
    @PostMapping("/manage/task/cancelQueued")
    public HttpResp<String> cancelQueuedTasks() {
        int count = jmcomicService.cancelQueuedTasks();
        return HttpResp.success(count > 0 ? "已取消" + count + "个排队任务" : "没有排队中的任务", null);
    }

    @PostMapping("/manage/album/deleteBatch")
    public HttpResp deleteAlbums(@RequestBody JmAlbumDeleteReq request) {
        jmcomicSqliteService.deleteAlbums(request);
        return HttpResp.success("删除完成", null);
    }

    /**
     * 收藏/取消收藏JM主记录
     */
    @PostMapping("/manage/album/collect")
    public HttpResp collectAlbums(@RequestBody JmAlbumCollectReq request) {
        if (request == null || CollectionUtils.isEmpty(request.getIds())) {
            return HttpResp.fail("缺少JM ID", null);
        }
        jmcomicSqliteService.collectAlbums(request);
        return HttpResp.success(Boolean.TRUE.equals(request.getCollected()) ? "收藏完成" : "取消收藏完成", null);
    }
    @PostMapping("/manage/album/deleteAllFile")
    public HttpResp deleteAllFile(@RequestBody JmAlbumDeleteReq request) {
        jmcomicSqliteService.deleteAllFile(request);
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

    private HttpResp<String> toHttpResp(BaseResp<String> resp) {
        if (!resp.isSuccess() && !resp.isQueued()) {
            return HttpResp.fail(resp.getMsg(), resp.getData());
        }
        return HttpResp.success(resp.getMsg(), resp.getData());
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

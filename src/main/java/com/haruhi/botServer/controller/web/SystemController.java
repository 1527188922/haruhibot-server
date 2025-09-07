package com.haruhi.botServer.controller.web;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.Tailer;
import cn.hutool.core.text.StrFormatter;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.haruhi.botServer.annotation.IgnoreAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.WebuiConfig;
import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.constant.RootTypeEnum;
import com.haruhi.botServer.constant.SqlTypeEnum;
import com.haruhi.botServer.dto.SqlExecuteResult;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.service.SqliteDatabaseService;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.vo.*;
import com.haruhi.botServer.dto.qqclient.RequestBox;
import com.haruhi.botServer.dto.qqclient.SyncResponse;
import com.haruhi.botServer.exception.BusinessException;
import com.haruhi.botServer.service.SystemService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.ws.Bot;
import com.haruhi.botServer.ws.BotContainer;
import com.haruhi.botServer.ws.BotServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestController
@Slf4j
@RequestMapping(BotConfig.CONTEXT_PATH+"/sys")
public class SystemController {

    @Autowired
    private SystemService systemService;
    @Autowired
    private WebuiConfig webuiConfig;

    @Autowired
    private BotServer botServer;
    @Autowired
    private SqliteDatabaseService sqliteDatabaseService;
    @Autowired
    private DictionarySqliteService dictionarySqliteService;


    @IgnoreAuthentication
    @PostMapping("/bot/action/{action}")
    public HttpResp sendMessage(@RequestBody JSONObject params,
                                @RequestParam(value = "botId",required = false) Long botId,
                                @RequestParam(value = "async",required = false) String async,
                                @RequestParam(value = "echo",required = false) String echo,
                                @RequestParam(value = "timeout",required = false,defaultValue = "10000") Long timeout,
                                @PathVariable(value = "action") String action) {
        RequestBox<JSONObject> request = new RequestBox<>();
        request.setAction(action);
        request.setParams(params);
        request.setEcho(echo);

        Bot bot = null;
        if (Objects.isNull(botId)) {
            bot = BotContainer.getBotFirst();
        }else{
            bot = BotContainer.getBotById(botId);
        }
        if (bot == null) {
            return HttpResp.fail("无连接",null);
        }
        if("1".equals(async)){
            // 异步发送
            request.setEcho(null);
            bot.sendMessage(JSONObject.toJSONString(request));
            return HttpResp.success("已发送",null);
        }

        try {
            request.setEcho(StringUtils.isNotBlank(request.getEcho()) ? request.getEcho() : CommonUtil.uuid());
            SyncResponse<Object> syncResponse = bot.sendSyncRequest(JSONObject.toJSONString(request),request.getEcho(), timeout, new TypeReference<SyncResponse<Object>>() {
            });
            return HttpResp.success("发送成功",syncResponse);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            return HttpResp.fail("发送异常",e.getMessage());
        }

    }

    @IgnoreAuthentication
    @RequestMapping(value = "/bot/restart",method = {RequestMethod.POST,RequestMethod.GET})
    public HttpResp restartBot() {
        try {
            systemService.restartBot();
            return HttpResp.success("重启命令已执行",null);
        }catch (BusinessException e){
            return HttpResp.fail(e.getErrorMsg(),null);
        }
    }

    /**
     * 找出父级路径下的所有文件和目录
     * @param request {parentPath}
     * @return
     */
    @PostMapping("/file/nodes")
    public HttpResp fileNodes(@RequestBody FileNode request,@RequestParam String rootType) {
        String rootDir = systemService.calcRootPath(rootType);

        String parentPath = request.getAbsolutePath();
        if (StringUtils.isBlank(parentPath)) {
            parentPath = rootDir;
        }

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("rootDir", rootDir);
            map.put("rootDirTotalSize", systemService.calcRootSize(rootType));
            List<FileNode> nodes = systemService.findNodesByParentPath(parentPath,rootType);
            map.put("nodes", nodes);
            return HttpResp.success(map);
        }catch (Exception e) {
            log.error("获取fileNodes异常 ：{}",parentPath, e);
            return HttpResp.fail(e.getMessage(),null);
        }
    }

    @PostMapping("/file/readContent")
    public HttpResp<String> readFileContent(@RequestBody FileNode request) {
        String path = request.getAbsolutePath();//绝对路径
        if (StringUtils.isBlank(path)) {
            return HttpResp.fail("缺少文件路径","");
        }
        return HttpResp.success(systemService.readFileContent(path));
    }

    @PostMapping("/file/delete")
    public HttpResp<String> deleteFile(@RequestBody FileNode request,
                                       @RequestParam String rootType,
                                       @RequestParam String password) {
        if (!webuiConfig.getLoginPassword().equals(password)) {
            return HttpResp.fail("密码错误",null);
        }
        String path = request.getAbsolutePath();//绝对路径
        if (StringUtils.isBlank(path)) {
            return HttpResp.fail("缺少路径",null);
        }

        if (!RootTypeEnum.BOT_TOOT.getType().equals(rootType)
                || !path.startsWith(FileUtil.getAppDir())) {
            return HttpResp.fail("禁止删除",null);
        }
        boolean delete = cn.hutool.core.io.FileUtil.del(new File(path));
        if (delete) {
            return HttpResp.success("删除成功",null);
        }
        return HttpResp.fail("删除失败",null);
    }

    @IgnoreAuthentication
    @GetMapping("/file/download")
    public void downloadFile(@RequestParam String path, HttpServletResponse response) {
        if (StringUtils.isBlank(path)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader("content-Type", "application/json; charset=utf-8");
            try (ServletOutputStream outputStream = response.getOutputStream()){
                outputStream.write(JSONObject.toJSONString(HttpResp.fail("参数错误",null)).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {  }
            return;
        }
        File file = new File(path);
        if(!file.exists() || !file.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setHeader("content-Type", "application/json; charset=utf-8");
            try (ServletOutputStream outputStream = response.getOutputStream()){
                outputStream.write(JSONObject.toJSONString(HttpResp.fail("文件不存在",path)).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) { }
            return;
        }

        try (InputStream in = Files.newInputStream(file.toPath());
             ServletOutputStream outputStream = response.getOutputStream()){
            response.setCharacterEncoding("UTF-8");
            response.setHeader("content-Type", "*");
            response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(file.getName(), "UTF-8"));
            response.setHeader("access-control-expose-headers", "*");
            response.setHeader("overwrite-response-data", "true");
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setContentLength((int) file.length());
            IoUtil.copy(in, outputStream);
        } catch (Exception e) {
            log.error("下载文件异常",e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 内容写入文件
     * 不存在文件则创建
     * @param request
     * @return
     */
    @PostMapping("/file/save")
    public HttpResp<String> saveFile(@RequestBody ContentFileNode request) {
        try {
            FileUtil.writeText(new File(request.getAbsolutePath()), request.getContent());
            return HttpResp.success("保存成功",null);
        } catch (IOException e) {
            log.error("保存异常",e);
            return HttpResp.success("保存异常："+e.getMessage(),null);
        }
    }

    @GetMapping("/botws/info")
    public HttpResp<BotWebSocketInfo> botWebSocketInfo() {

        return HttpResp.success(systemService.getBotWebSocketInfo());
    }

    @PostMapping("/botws/opt")
    public HttpResp botWebSocketOperation(@RequestParam String command) {
        if ("1".equals(command)) {
            botServer.start();
            return HttpResp.success("已启动",systemService.getBotWebSocketInfo());
        }
        if ("2".equals(command)) {
            botServer.stop();
            return HttpResp.success("已逻辑停止",systemService.getBotWebSocketInfo());
        }
        return HttpResp.fail("不支持的操作",null);
    }

    @PostMapping("/db/info")
    public HttpResp databaseInfo(@RequestBody(required = false) DatabaseInfoNode request) {
        return HttpResp.success(systemService.databaseInfo(request));
    }

    @GetMapping("/db/ddl")
    public HttpResp databaseDDL(@RequestParam String tableName) {
        return HttpResp.success(systemService.tableDDL(tableName));
    }

    @PostMapping("/db/execute")
    public HttpResp<List<SqlExecuteResult>> executeSql(@RequestBody Map<String,String> request) {
        String sql = request.get("sql");
        try {
            return HttpResp.success(sqliteDatabaseService.executeSql(sql));
        } catch (Exception e) {
            log.error("executeSql异常",e);
            SqlExecuteResult result = new SqlExecuteResult();
            result.setSql(sql);
            result.setType(SqlTypeEnum.ERROR.name());
            result.setErrorMessage(e.getMessage());
            return HttpResp.success(Collections.singletonList(result));
        }
    }

    @GetMapping("/db/sql")
    public HttpResp<String> getSqlCache() {
        String s = dictionarySqliteService.get(DictionaryEnum.DATABASE_DB_SQL_CACHE.getKey());
        return HttpResp.success(s);
    }

    @PostMapping("/db/sql")
    public HttpResp<String> saveSqlCache(@RequestBody Map<String,String> request) {
        String sql = request.get("sql");
        dictionarySqliteService.put(DictionaryEnum.DATABASE_DB_SQL_CACHE.getKey(),sql);
        return HttpResp.success();
    }

    @PostMapping("/db/export")
    public HttpResp<String> export(@RequestBody ExportDatabaseReq request,HttpServletResponse response) {
        String sql = request.getSql();
        SqlExecuteResult data = request.getData();
        String tableName = request.getTableName();
        String filename = null;
        if (StringUtils.isNotBlank(tableName)) {
            filename = StrFormatter.format("{}_{}.xlsx",tableName, DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss2));
        }else{
            filename = StrFormatter.format("db_export_{}.xlsx", DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss2));
        }


        File file = new File(FileUtil.getAppTempDir() + File.separator + filename);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)){

            if(Objects.nonNull(data)){
                sqliteDatabaseService.exportResult(Collections.singletonList(data), fileOutputStream);
            }else{
                sqliteDatabaseService.executeAndExport(sql, fileOutputStream);
            }
            response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
            return HttpResp.success(file.getAbsolutePath());
        }catch (BusinessException e){
            return HttpResp.fail(e.getErrorMsg(),null);
        } catch (Exception e) {
            return HttpResp.fail("导出异常："+e.getMessage(),null);
        }

    }

    @PostMapping("/db/import")
    public HttpResp importData(@RequestParam MultipartFile file,@RequestParam String tableName) {
        try (InputStream inputStream = file.getInputStream()){

            sqliteDatabaseService.importData(inputStream, tableName);
            return HttpResp.success("导入成功",null);
        } catch (Exception e) {
            log.error("导入数据异常:{}",tableName,e);
            return HttpResp.fail(e.getMessage(),null);
        }
    }



    @GetMapping(value = "/log/tail", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter tailLog(@RequestParam(required = false) Integer initLine) {
        // 设置超时时间为30分钟
        SseEmitter emitter = new SseEmitter(-1L);
        LineHandler listener = line -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("log")
                        .data(line));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        };
        String file = "D:\\JavaProject\\haruhibot-server\\logs\\haruhibot.log";
        Tailer tailer = new Tailer(new File(file),
                StandardCharsets.UTF_8,
                listener,
                initLine == null ? 30 : initLine,
                50L);
        tailer.start(true);


        emitter.onCompletion(() -> tailer.stop());
        emitter.onTimeout(() -> tailer.stop());
        emitter.onError((e) -> tailer.stop());

        return emitter;
    }

}

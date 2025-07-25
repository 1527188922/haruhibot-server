package com.haruhi.botServer.controller.web;

import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.haruhi.botServer.annotation.IgnoreAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.WebuiConfig;
import com.haruhi.botServer.constant.RootTypeEnum;
import com.haruhi.botServer.controller.HttpResp;
import com.haruhi.botServer.dto.qqclient.RequestBox;
import com.haruhi.botServer.dto.qqclient.SyncResponse;
import com.haruhi.botServer.exception.BusinessException;
import com.haruhi.botServer.service.SystemService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.vo.ContentFileNode;
import com.haruhi.botServer.vo.FileNode;
import com.haruhi.botServer.ws.Bot;
import com.haruhi.botServer.ws.BotContainer;
import com.haruhi.botServer.ws.BotServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

//    @IgnoreAuthentication
    @GetMapping("/bot/restart")
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



    @PostMapping("/file/download")
    public void downloadFile(@RequestBody FileNode request, HttpServletResponse response) {
        String path = request.getAbsolutePath();//绝对路径
        if (StringUtils.isBlank(path)) {
            return;
        }

        File file = new File(path);
        if(!file.exists() || !file.isFile()) {
            return;
        }

        response.setCharacterEncoding("UTF-8");
//        response.setHeader("content-Type", "application/vnd.ms-excel");
//        response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        response.setHeader("access-control-expose-headers", "*");
        response.setHeader("overwrite-response-data", "true");
        response.setContentLength((int) file.length());

        try (InputStream in = Files.newInputStream(file.toPath());
             ServletOutputStream outputStream = response.getOutputStream()){
            outputStream.write(IoUtil.readBytes(in));
        } catch (IOException e) {
            throw new RuntimeException(e);
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
    public HttpResp<SystemService.BotWebSocketInfo> botWebSocketInfo() {

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


}

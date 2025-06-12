package com.haruhi.botServer.controller.web;

import cn.hutool.core.io.IoUtil;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.WebuiConfig;
import com.haruhi.botServer.constant.RootTypeEnum;
import com.haruhi.botServer.controller.HttpResp;
import com.haruhi.botServer.service.SystemService;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.vo.ContentFileNode;
import com.haruhi.botServer.vo.FileNode;
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

@RestController
@Slf4j
@RequestMapping(BotConfig.CONTEXT_PATH+"/sys")
public class SystemController {

    @Autowired
    private SystemService systemService;
    @Autowired
    private WebuiConfig webuiConfig;

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
    @PostMapping("/file/write")
    public HttpResp<String> writeFile(@RequestBody ContentFileNode request) {
        try {
            FileUtil.writeText(new File(request.getAbsolutePath()), request.getContent());
            return HttpResp.success("保存成功",null);
        } catch (IOException e) {
            log.error("保存异常",e);
            return HttpResp.success("保存异常："+e.getMessage(),null);
        }
    }


}

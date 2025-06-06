package com.haruhi.botServer.controller.web;

import cn.hutool.core.io.IoUtil;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.controller.HttpResp;
import com.haruhi.botServer.service.SystemService;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.vo.FileNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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

    /**
     * 找出父级路径下的所有文件和目录
     * @param request {parentPath}
     * @return
     */
    @PostMapping("/fileNodes")
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

    @PostMapping("/readFileContent")
    public HttpResp<String> readFileContent(@RequestBody FileNode request) {
        String path = request.getAbsolutePath();//绝对路径
        if (StringUtils.isBlank(path)) {
            return HttpResp.fail("缺少文件路径","");
        }
        return HttpResp.success(systemService.readFileContent(path));
    }


    @PostMapping("/downloadFile")
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

}

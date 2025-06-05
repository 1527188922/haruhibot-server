package com.haruhi.botServer.controller.web;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.controller.HttpResp;
import com.haruhi.botServer.service.SystemService;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.vo.FileNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public HttpResp fileNodes(@RequestBody Map<String,String> request) {
        String parentPath = request.get("parentPath");
        if (StringUtils.isBlank(parentPath)) {
            parentPath = FileUtil.getAppDir();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("appDir", FileUtil.getAppDir());
        List<FileNode> nodes = systemService.findNodesByParentPath(parentPath);
        map.put("nodes", nodes);
        return HttpResp.success(map);

    }
}

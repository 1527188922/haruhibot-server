package com.haruhi.botServer.controller.web;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.controller.HttpResp;
import com.haruhi.botServer.entity.sqlite.WordStripSqlite;
import com.haruhi.botServer.service.sqlite.WordStripSqliteService;
import com.haruhi.botServer.vo.WordStripQueryReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/wordStrip")
public class WordStripController {


    @Autowired
    private WordStripSqliteService wordStripService;


    @PostMapping("/search")
    public HttpResp<IPage<WordStripSqlite>> search(@RequestBody WordStripQueryReq request) {
        IPage<WordStripSqlite> list = wordStripService.search(request, true);
        return HttpResp.success(list);
    }

    @PostMapping("/deleteBatch")
    public HttpResp search(@RequestBody List<WordStripSqlite> request) {
        List<Long> ids = request.stream()
                .map(WordStripSqlite::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ids)) {
            return HttpResp.fail("无数据",null);
        }
        try {
            wordStripService.removeByIds(ids);
            return HttpResp.success("删除完成",null);
        }catch (Exception e){
            log.error("[webui][/wordStrip]删除词条异常：{}", JSONObject.toJSONString(request),e);
            return HttpResp.fail("删除异常："+e.getMessage(),null);
        }
    }

    @PostMapping("/refresh")
    public HttpResp refreshCache() {
        long l = System.currentTimeMillis();
        wordStripService.clearCache();
        wordStripService.loadWordStrip();
        long l1 = System.currentTimeMillis() - l;
        return HttpResp.success("刷新完成，耗时："+l1,null);
    }


}

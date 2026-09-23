package com.haruhi.botserver.dictionary.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botserver.bootstrap.SysConstants;
import com.haruhi.botserver.dictionary.model.DictQueryReq;
import com.haruhi.botserver.dictionary.persistence.entity.DictionarySqlite;
import com.haruhi.botserver.dictionary.service.DictionaryService;
import com.haruhi.botserver.shared.model.HttpResp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(SysConstants.CONTEXT_PATH+"/dict")
public class DictionaryController {

    @Autowired
    private DictionaryService dictionaryService;


    @PostMapping("/search")
    public HttpResp<IPage<DictionarySqlite>> list(@RequestBody DictQueryReq request) {
        IPage<DictionarySqlite> page = dictionaryService.search(request, true);
        return HttpResp.success(page);
    }

    @PostMapping("/refresh")
    public HttpResp refreshCache() {
        long l = System.currentTimeMillis();
        dictionaryService.refreshCache();
        return HttpResp.success("刷新完成，耗时：" + (System.currentTimeMillis() - l), null);
    }

    @PostMapping("/add")
    public HttpResp add(@RequestBody DictionarySqlite request) {
        if (StringUtils.isBlank(request.getKey())) {
            return HttpResp.fail("key不能为空", null);
        }

        dictionaryService.add(request);
        return HttpResp.success("新增成功", request);
    }

    @PostMapping("/update")
    public HttpResp update(@RequestBody DictionarySqlite request) {
        if (request.getId() == null) {
            return HttpResp.fail("参数错误", null);
        }
        if (StringUtils.isBlank(request.getKey())) {
            return HttpResp.fail("key不能为空", null);
        }
        dictionaryService.update(request);
        return HttpResp.success("修改成功", request);
    }

    @PostMapping("/deleteBatch")
    public HttpResp deleteBatch(@RequestBody List<DictionarySqlite> request) {
        if (CollectionUtils.isEmpty(request)) {
            return HttpResp.fail("缺少参数", null);
        }
        dictionaryService.deleteBatch(request);
        return HttpResp.success("删除成功", request);
    }

}
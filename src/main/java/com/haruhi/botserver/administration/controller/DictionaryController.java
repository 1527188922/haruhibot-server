package com.haruhi.botserver.administration.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botserver.bootstrap.SysConstants;
import com.haruhi.botserver.infrastructure.kvstore.model.KvQuery;
import com.haruhi.botserver.infrastructure.kvstore.persistence.entity.KvEntry;
import com.haruhi.botserver.infrastructure.kvstore.service.KvStoreService;
import com.haruhi.botserver.shared.model.HttpResp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(SysConstants.CONTEXT_PATH+"/dict")
public class DictionaryController {

    private final KvStoreService kvStoreService;


    @PostMapping("/search")
    public HttpResp<IPage<KvEntry>> list(@RequestBody KvQuery request) {
        IPage<KvEntry> page = kvStoreService.search(request, true);
        return HttpResp.success(page);
    }

    @PostMapping("/refresh")
    public HttpResp refreshCache() {
        long l = System.currentTimeMillis();
        kvStoreService.refreshCache();
        return HttpResp.success("刷新完成，耗时：" + (System.currentTimeMillis() - l), null);
    }

    @PostMapping("/add")
    public HttpResp add(@RequestBody KvEntry request) {
        if (StringUtils.isBlank(request.getKey())) {
            return HttpResp.fail("key不能为空", null);
        }

        kvStoreService.add(request);
        return HttpResp.success("新增成功", request);
    }

    @PostMapping("/update")
    public HttpResp update(@RequestBody KvEntry request) {
        if (request.getId() == null) {
            return HttpResp.fail("参数错误", null);
        }
        if (StringUtils.isBlank(request.getKey())) {
            return HttpResp.fail("key不能为空", null);
        }
        kvStoreService.update(request);
        return HttpResp.success("修改成功", request);
    }

    @PostMapping("/deleteBatch")
    public HttpResp deleteBatch(@RequestBody List<KvEntry> request) {
        if (CollectionUtils.isEmpty(request)) {
            return HttpResp.fail("缺少参数", null);
        }
        kvStoreService.deleteBatch(request);
        return HttpResp.success("删除成功", request);
    }

}

package com.haruhi.botServer.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.BilibiliSubscribeTypeEnum;
import com.haruhi.botServer.entity.BilibiliSubscribeSqlite;
import com.haruhi.botServer.service.BilibiliSubscribeSqliteService;
import com.haruhi.botServer.utils.PushTargetUtil;
import com.haruhi.botServer.vo.BilibiliSubscribeQueryReq;
import com.haruhi.botServer.vo.BilibiliSubscribeResp;
import com.haruhi.botServer.vo.BilibiliSubscribeTargetReq;
import com.haruhi.botServer.vo.HttpResp;
import com.haruhi.botServer.vo.PushTargetInfo;
import com.haruhi.botServer.vo.PushTargetQueryReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * bilibili订阅管理
 */
@Slf4j
@RestController
@RequestMapping(BotConfig.CONTEXT_PATH + "/bilibili/subscribe")
public class BilibiliSubscribeController {

    @Autowired
    private BilibiliSubscribeSqliteService bilibiliSubscribeSqliteService;

    @PostMapping("/search")
    public HttpResp<IPage<BilibiliSubscribeResp>> search(@RequestBody BilibiliSubscribeQueryReq request) {
        return HttpResp.success(bilibiliSubscribeSqliteService.search(request, true));
    }

    @PostMapping("/add")
    public HttpResp add(@RequestBody BilibiliSubscribeSqlite request) {
        String error = validate(request);
        if (Objects.nonNull(error)) {
            return HttpResp.fail(error, null);
        }
        if (bilibiliSubscribeSqliteService.existsSubscribe(request.getUid(), request.getSelfId(), request.getSubType(), null)) {
            return HttpResp.fail("该机器人已订阅此主播，无需重复添加", null);
        }
        try {
            bilibiliSubscribeSqliteService.saveSubscribe(request);
            return HttpResp.success("新增成功", request);
        } catch (Exception e) {
            log.error("[webui][/bilibili/subscribe]新增订阅异常：{}", JSONObject.toJSONString(request), e);
            return HttpResp.fail("新增异常：" + e.getMessage(), null);
        }
    }

    @PostMapping("/update")
    public HttpResp update(@RequestBody BilibiliSubscribeSqlite request) {
        if (Objects.isNull(request.getId())) {
            return HttpResp.fail("缺少订阅id", null);
        }
        String error = validate(request);
        if (Objects.nonNull(error)) {
            return HttpResp.fail(error, null);
        }
        if (bilibiliSubscribeSqliteService.existsSubscribe(request.getUid(), request.getSelfId(), request.getSubType(), request.getId())) {
            return HttpResp.fail("该机器人已订阅此主播，无需重复添加", null);
        }
        try {
            bilibiliSubscribeSqliteService.updateSubscribe(request);
            return HttpResp.success("修改成功", request);
        } catch (Exception e) {
            log.error("[webui][/bilibili/subscribe]修改订阅异常：{}", JSONObject.toJSONString(request), e);
            return HttpResp.fail("修改异常：" + e.getMessage(), null);
        }
    }

    /**
     * 修改推送的群与好友
     */
    @PostMapping("/updateTargets")
    public HttpResp updateTargets(@RequestBody BilibiliSubscribeTargetReq request) {
        if (Objects.isNull(request.getId())) {
            return HttpResp.fail("缺少订阅id", null);
        }
        try {
            boolean update = bilibiliSubscribeSqliteService.updateTargets(request.getId(), request.getGroupIds(), request.getFriendIds());
            return update ? HttpResp.success("修改成功", null) : HttpResp.fail("修改失败，订阅不存在", null);
        } catch (Exception e) {
            log.error("[webui][/bilibili/subscribe]修改推送目标异常：{}", JSONObject.toJSONString(request), e);
            return HttpResp.fail("修改异常：" + e.getMessage(), null);
        }
    }

    @PostMapping("/deleteBatch")
    public HttpResp deleteBatch(@RequestBody List<BilibiliSubscribeSqlite> request) {
        List<Long> ids = request.stream()
                .map(BilibiliSubscribeSqlite::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ids)) {
            return HttpResp.fail("无数据", null);
        }
        try {
            bilibiliSubscribeSqliteService.removeByIds(ids);
            return HttpResp.success("删除完成", null);
        } catch (Exception e) {
            log.error("[webui][/bilibili/subscribe]删除订阅异常：{}", JSONObject.toJSONString(request), e);
            return HttpResp.fail("删除异常：" + e.getMessage(), null);
        }
    }

    /**
     * 查询可选的推送目标(群/好友)
     */
    @PostMapping("/target/list")
    public HttpResp<List<PushTargetInfo>> targetList(@RequestBody PushTargetQueryReq request) {
        try {
            return HttpResp.success(bilibiliSubscribeSqliteService.listTargetCandidates(request));
        } catch (Exception e) {
            log.error("[webui][/bilibili/subscribe]查询推送目标异常：{}", JSONObject.toJSONString(request), e);
            return HttpResp.fail(e.getMessage(), null);
        }
    }

    private String validate(BilibiliSubscribeSqlite request) {
        if (Objects.isNull(request.getUid())) {
            return "主播uid不能为空";
        }
        if (Objects.isNull(request.getSelfId())) {
            return "机器人QQ号不能为空";
        }
        if (StringUtils.isBlank(request.getSubType())) {
            request.setSubType(BilibiliSubscribeTypeEnum.LIVE.getType());
        }
        if (Objects.isNull(BilibiliSubscribeTypeEnum.getEnumByType(request.getSubType()))) {
            return "不支持的订阅类型：" + request.getSubType();
        }
        if (StringUtils.isNotBlank(request.getGroupIds()) && PushTargetUtil.parseIds(request.getGroupIds()).isEmpty()) {
            return "推送群号格式错误";
        }
        if (StringUtils.isNotBlank(request.getFriendIds()) && PushTargetUtil.parseIds(request.getFriendIds()).isEmpty()) {
            return "推送好友QQ号格式错误";
        }
        return null;
    }
}

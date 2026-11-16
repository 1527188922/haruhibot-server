package com.haruhi.botserver.features.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botserver.bootstrap.SysConstants;
import com.haruhi.botserver.features.ai.model.admin.AiIdReq;
import com.haruhi.botserver.features.ai.model.admin.AiOverviewResp;
import com.haruhi.botserver.features.ai.model.admin.AiRolePreviewReq;
import com.haruhi.botserver.features.ai.model.admin.AiRolePreviewResp;
import com.haruhi.botserver.features.ai.model.admin.AiRoleView;
import com.haruhi.botserver.features.ai.model.admin.AiSessionDetailResp;
import com.haruhi.botserver.features.ai.model.admin.AiSessionQueryReq;
import com.haruhi.botserver.features.ai.model.admin.AiSessionView;
import com.haruhi.botserver.features.ai.persistence.entity.AiMemoryEntity;
import com.haruhi.botserver.features.ai.persistence.entity.AiMessageEntity;
import com.haruhi.botserver.features.ai.persistence.entity.AiUsageEntity;
import com.haruhi.botserver.features.ai.service.AiAdminService;
import com.haruhi.botserver.shared.model.HttpResp;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WebUI AI 管理接口（P0 只读）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SysConstants.CONTEXT_PATH + "/ai")
public class AiAdminController {

    private final AiAdminService aiAdminService;

    @PostMapping("/overview")
    public HttpResp<AiOverviewResp> overview() {
        return HttpResp.success(aiAdminService.overview());
    }

    @PostMapping("/session/search")
    public HttpResp<IPage<AiSessionView>> searchSessions(@RequestBody AiSessionQueryReq request) {
        return HttpResp.success(aiAdminService.searchSessions(request));
    }

    @PostMapping("/session/detail")
    public HttpResp<AiSessionDetailResp> detail(@RequestBody AiIdReq request) {
        return HttpResp.success(aiAdminService.detail(request.getId()));
    }

    @PostMapping("/session/messages")
    public HttpResp<List<AiMessageEntity>> messages(@RequestBody AiIdReq request) {
        return HttpResp.success(aiAdminService.messages(request.getId(), request.getLimit()));
    }

    @PostMapping("/session/memories")
    public HttpResp<List<AiMemoryEntity>> memories(@RequestBody AiIdReq request) {
        return HttpResp.success(aiAdminService.memories(request.getId()));
    }

    @PostMapping("/session/usage")
    public HttpResp<List<AiUsageEntity>> usage(@RequestBody AiIdReq request) {
        return HttpResp.success(aiAdminService.usage(request.getId(), request.getLimit()));
    }

    @PostMapping("/role/list")
    public HttpResp<List<AiRoleView>> roleList() {
        return HttpResp.success(aiAdminService.roleList());
    }

    @PostMapping("/role/preview")
    public HttpResp<AiRolePreviewResp> rolePreview(@RequestBody AiRolePreviewReq request) {
        return HttpResp.success(aiAdminService.rolePreview(request.getName()));
    }

    @PostMapping("/role/rescan")
    public HttpResp<List<AiRoleView>> roleRescan() {
        return HttpResp.success(aiAdminService.roleRescan());
    }
}

package com.haruhi.botserver.features.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botserver.bootstrap.SysConstants;
import com.haruhi.botserver.features.ai.model.admin.AiIdReq;
import com.haruhi.botserver.features.ai.model.admin.AiOverviewResp;
import com.haruhi.botserver.features.ai.model.admin.AiPromptPreviewReq;
import com.haruhi.botserver.features.ai.model.admin.AiPromptPreviewResp;
import com.haruhi.botserver.features.ai.model.admin.AiRoleCopyReq;
import com.haruhi.botserver.features.ai.model.admin.AiRoleDetailResp;
import com.haruhi.botserver.features.ai.model.admin.AiRoleNameCheckReq;
import com.haruhi.botserver.features.ai.model.admin.AiRoleNameCheckResp;
import com.haruhi.botserver.features.ai.model.admin.AiRolePreviewReq;
import com.haruhi.botserver.features.ai.model.admin.AiRolePreviewResp;
import com.haruhi.botserver.features.ai.model.admin.AiRoleSaveReq;
import com.haruhi.botserver.features.ai.model.admin.AiRoleSaveResp;
import com.haruhi.botserver.features.ai.model.admin.AiRoleView;
import com.haruhi.botserver.features.ai.model.admin.AiSessionDetailResp;
import com.haruhi.botserver.features.ai.model.admin.AiSessionQueryReq;
import com.haruhi.botserver.features.ai.model.admin.AiSessionRoleReq;
import com.haruhi.botserver.features.ai.model.admin.AiSessionStatusReq;
import com.haruhi.botserver.features.ai.model.admin.AiSessionView;
import com.haruhi.botserver.features.ai.model.admin.AiUsageBreakdownResp;
import com.haruhi.botserver.features.ai.model.admin.AiUsageRangeReq;
import com.haruhi.botserver.features.ai.model.admin.AiUsageTrendResp;
import com.haruhi.botserver.features.ai.persistence.entity.AiMemoryEntity;
import com.haruhi.botserver.features.ai.persistence.entity.AiMessageEntity;
import com.haruhi.botserver.features.ai.persistence.entity.AiUsageEntity;
import com.haruhi.botserver.features.ai.service.AiAdminService;
import com.haruhi.botserver.shared.error.BusinessException;
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

    @PostMapping("/role/detail")
    public HttpResp<AiRoleDetailResp> roleDetail(@RequestBody AiRolePreviewReq request) {
        return HttpResp.success(aiAdminService.roleDetail(request.getName()));
    }

    @PostMapping("/role/save")
    public HttpResp<AiRoleSaveResp> roleSave(@RequestBody AiRoleSaveReq request) {
        try {
            return HttpResp.success(aiAdminService.roleSave(request));
        } catch (BusinessException e) {
            return HttpResp.fail(e.getErrorMsg(), null);
        }
    }

    @PostMapping("/role/copy")
    public HttpResp<AiRoleDetailResp> roleCopy(@RequestBody AiRoleCopyReq request) {
        try {
            return HttpResp.success(aiAdminService.roleCopy(request));
        } catch (BusinessException e) {
            return HttpResp.fail(e.getErrorMsg(), null);
        }
    }

    @PostMapping("/role/nameCheck")
    public HttpResp<AiRoleNameCheckResp> roleNameCheck(@RequestBody AiRoleNameCheckReq request) {
        return HttpResp.success(aiAdminService.roleNameCheck(request));
    }

    @PostMapping("/session/status")
    public HttpResp<AiSessionDetailResp> updateStatus(@RequestBody AiSessionStatusReq request) {
        try {
            return HttpResp.success(aiAdminService.updateStatus(request.getId(), request.getStatus()));
        } catch (BusinessException e) {
            return HttpResp.fail(e.getErrorMsg(), null);
        }
    }

    @PostMapping("/session/role")
    public HttpResp<AiSessionDetailResp> updateRole(@RequestBody AiSessionRoleReq request) {
        try {
            return HttpResp.success(aiAdminService.updateRole(request.getId(), request.getRoleName()));
        } catch (BusinessException e) {
            return HttpResp.fail(e.getErrorMsg(), null);
        }
    }

    @PostMapping("/session/reset")
    public HttpResp<AiSessionDetailResp> resetSession(@RequestBody AiIdReq request) {
        try {
            return HttpResp.success(aiAdminService.resetSession(request.getId()));
        } catch (BusinessException e) {
            return HttpResp.fail(e.getErrorMsg(), null);
        }
    }

    @PostMapping("/usage/trend")
    public HttpResp<AiUsageTrendResp> trend(@RequestBody AiUsageRangeReq request) {
        return HttpResp.success(aiAdminService.trend(request.getDays()));
    }

    @PostMapping("/usage/breakdown")
    public HttpResp<AiUsageBreakdownResp> breakdown(@RequestBody AiUsageRangeReq request) {
        return HttpResp.success(aiAdminService.breakdown(request.getDays()));
    }

    @PostMapping("/prompt/preview")
    public HttpResp<AiPromptPreviewResp> promptPreview(@RequestBody AiPromptPreviewReq request) {
        try {
            return HttpResp.success(aiAdminService.promptPreview(request));
        } catch (BusinessException e) {
            return HttpResp.fail(e.getErrorMsg(), null);
        }
    }
}

<template>
  <div class="ai-prompt">
    <basic-container>
      <div class="page-header">
        <div>
          <div class="page-title">提示词预览</div>
          <div class="page-desc">查看最终注入模型的 system 与 messages，不调用 API、不发送 QQ、不写库。</div>
        </div>
        <el-tag type="info" effect="plain">P1 实现</el-tag>
      </div>

      <el-alert title="该页面将在 P1 阶段实现"
                type="info"
                :closable="false"
                show-icon
                class="page-alert">
        <div slot="default">
          P0 先提供菜单与路由骨架；接口契约将在 P1 与页面一起落地。
        </div>
      </el-alert>

      <el-card shadow="never" class="plan-card">
        <div slot="header" class="plan-title">规划中的能力</div>
        <div class="plan-grid">
          <div class="plan-section">
            <div class="plan-label">输入</div>
            <ul>
              <li>场景：群聊 / 私聊</li>
              <li>机器人、目标群号 / QQ</li>
              <li>角色：自动解析或手动指定</li>
              <li>模拟消息、环境上下文条数</li>
              <li>工具 / 表情 / 视觉开关</li>
            </ul>
          </div>
          <div class="plan-section">
            <div class="plan-label">输出</div>
            <ul>
              <li>System 分段：安全 / 协议 / 工具 / 表情 / 场景 / 角色卡</li>
              <li>Messages：system + 历史 + 当前消息</li>
              <li>Token 估算：system / 历史 / 当前 / 合计</li>
              <li>只读结果，不提供发送测试消息</li>
            </ul>
          </div>
        </div>
      </el-card>
    </basic-container>
  </div>
</template>

<script>
export default {
  name: 'AiPrompt'
};
</script>

<style lang="scss" scoped>
.ai-prompt {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  .page-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }

  .page-desc {
    margin-top: 4px;
    font-size: 12px;
    color: #909399;
  }

  .page-alert {
    margin-bottom: 16px;
  }

  .plan-card {
    border: 1px solid #ebeef5;
  }

  .plan-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .plan-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 24px;
  }

  .plan-label {
    margin-bottom: 8px;
    font-size: 13px;
    font-weight: 600;
    color: #606266;
  }

  ul {
    margin: 0;
    padding-left: 18px;
  }

  li {
    font-size: 13px;
    line-height: 1.9;
    color: #606266;
  }

  @media (max-width: 768px) {
    .plan-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>

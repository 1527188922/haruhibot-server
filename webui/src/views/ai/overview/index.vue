<template>
  <div class="ai-overview">
    <basic-container>
      <div class="page-header">
        <div>
          <div class="page-title">AI 概览</div>
          <div class="page-desc">今日用量、运行状态与账户余额</div>
        </div>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button>
      </div>

      <el-alert v-if="!runtime.enabled"
                title="AI 聊天总开关未开启"
                type="warning"
                :closable="false"
                show-icon
                class="page-alert">
        <div slot="default">当前不会接管群聊/私聊。可前往「系统管理 → 配置管理 → ai.properties」开启。</div>
      </el-alert>

      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-label">今日请求数</div>
          <div class="stat-value">{{ today.requests || 0 }}</div>
          <div class="stat-sub">失败 {{ today.failures || 0 }} 次</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">今日 Token</div>
          <div class="stat-value">{{ today.totalTokens || 0 }}</div>
          <div class="stat-sub">输入 {{ today.promptTokens || 0 }} / 输出 {{ today.completionTokens || 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">缓存命中率</div>
          <div class="stat-value">{{ cacheHitRate }}</div>
          <div class="stat-sub">命中 {{ today.cacheHitTokens || 0 }} tokens</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">平均延迟</div>
          <div class="stat-value">{{ avgLatency }}</div>
          <div class="stat-sub">毫秒 / 次</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">每日限额</div>
          <div class="stat-value">{{ dailyLimitText }}</div>
          <div class="stat-sub">0 表示不限制</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">账户余额</div>
          <div class="stat-value small">{{ balanceText }}</div>
          <div class="stat-sub">{{ balance.error ? balance.error : '来自官方 /user/balance' }}</div>
        </div>
      </div>

      <el-row :gutter="16">
        <el-col :xs="24" :sm="24" :md="12">
          <div class="panel">
            <div class="panel-title">运行状态</div>
            <div class="tag-list">
              <el-tag size="small" :type="runtime.enabled ? 'success' : 'info'" effect="plain">
                总开关：{{ runtime.enabled ? '开' : '关' }}
              </el-tag>
              <el-tag size="small" type="info" effect="plain">模型：{{ runtime.model || '-' }}</el-tag>
              <el-tag size="small" :type="runtime.thinking ? 'warning' : 'info'" effect="plain">
                思考模式：{{ runtime.thinking ? '开' : '关' }}
              </el-tag>
              <el-tag size="small" type="info" effect="plain">群白名单：{{ runtime.groupWhitelistCount || 0 }}</el-tag>
              <el-tag size="small" :type="runtime.privateEnabled ? 'success' : 'info'" effect="plain">
                私聊：{{ runtime.privateEnabled ? '开' : '关' }}
              </el-tag>
              <el-tag size="small" type="info" effect="plain">角色卡：{{ runtime.roleCount || 0 }}</el-tag>
              <el-tag size="small" type="info" effect="plain">表情：{{ runtime.emojiCount || 0 }}</el-tag>
              <el-tag size="small" :type="runtime.searchEnabled ? 'success' : 'info'" effect="plain">
                联网搜索：{{ runtime.searchEnabled ? '可用' : '关闭' }}
              </el-tag>
              <el-tag size="small" :type="runtime.visionEnabled ? 'success' : 'info'" effect="plain">
                图片理解：{{ runtime.visionEnabled ? '可用' : '关闭' }}
              </el-tag>
              <el-tag size="small" :type="runtime.agentEnabled ? 'success' : 'info'" effect="plain">
                工具调用：{{ runtime.agentEnabled ? '开' : '关' }}
              </el-tag>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="24" :md="12">
          <div class="panel">
            <div class="panel-title">账户余额</div>
            <template v-if="balanceItems.length">
              <div v-for="item in balanceItems" :key="item.currency" class="balance-row">
                <div class="balance-currency">{{ item.currency }}</div>
                <div class="balance-main">{{ item.totalBalance }}</div>
                <div class="balance-sub">赠送 {{ item.grantedBalance }} / 充值 {{ item.toppedUpBalance }}</div>
              </div>
            </template>
            <div v-else class="empty-tip">{{ balance.error || '暂无余额信息' }}</div>
          </div>
        </el-col>
      </el-row>

      <div class="panel">
        <div class="panel-title">今日用量明细</div>
        <el-table :data="usageRows" size="small" border stripe>
          <el-table-column prop="label" label="指标" min-width="160"></el-table-column>
          <el-table-column prop="value" label="数值" min-width="160"></el-table-column>
          <el-table-column prop="remark" label="说明" min-width="260"></el-table-column>
        </el-table>
      </div>
    </basic-container>
  </div>
</template>

<script>
import { overview } from '@/api/ai';

export default {
  name: 'AiOverview',
  data() {
    return {
      loading: false,
      data: {
        today: {},
        runtime: {},
        balance: {}
      }
    };
  },
  computed: {
    today() {
      return this.data.today || {};
    },
    runtime() {
      return this.data.runtime || {};
    },
    balance() {
      return this.data.balance || {};
    },
    balanceItems() {
      return this.balance.items || [];
    },
    cacheHitRate() {
      const prompt = Number(this.today.promptTokens || 0);
      const hit = Number(this.today.cacheHitTokens || 0);
      if (!prompt) {
        return '--';
      }
      return ((hit / prompt) * 100).toFixed(1) + '%';
    },
    avgLatency() {
      const value = Number(this.today.avgLatencyMs || 0);
      return value ? value.toFixed(0) : '0';
    },
    dailyLimitText() {
      const limit = Number(this.runtime.dailyTokenLimit || 0);
      return limit > 0 ? limit.toLocaleString() : '不限';
    },
    balanceText() {
      if (this.balance.error) {
        return '查询失败';
      }
      if (!this.balanceItems.length) {
        return '--';
      }
      return this.balanceItems
        .map(item => (item.currency || '') + ' ' + (item.totalBalance || '0'))
        .join(' / ');
    },
    usageRows() {
      return [
        { label: '请求数', value: this.today.requests || 0, remark: '今日调用 DeepSeek 的次数' },
        { label: '输入 Token', value: this.today.promptTokens || 0, remark: 'system + 历史 + 当前消息' },
        { label: '输出 Token', value: this.today.completionTokens || 0, remark: '模型本次生成' },
        { label: '总 Token', value: this.today.totalTokens || 0, remark: '输入 + 输出' },
        { label: '缓存命中 Token', value: this.today.cacheHitTokens || 0, remark: '命中缓存可降低成本' },
        { label: '失败次数', value: this.today.failures || 0, remark: '解析失败、超时、限流等' },
        { label: '平均延迟', value: (Number(this.today.avgLatencyMs || 0)).toFixed(0) + ' ms', remark: '从发起请求到收到响应' }
      ];
    }
  },
  created() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      overview().then(({ data: { code, message, data } }) => {
        if (code !== 200) {
          return this.$message.error(message || '加载失败');
        }
        this.data = data || { today: {}, runtime: {}, balance: {} };
      }).catch(() => {
        this.$message.error('加载 AI 概览失败');
      }).finally(() => {
        this.loading = false;
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.ai-overview {
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

  .stat-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 16px;
  }

  .stat-card {
    padding: 16px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 6px;
  }

  .stat-label {
    font-size: 12px;
    color: #909399;
  }

  .stat-value {
    margin-top: 8px;
    font-size: 26px;
    font-weight: 600;
    line-height: 1.2;
    color: #303133;
    word-break: break-all;

    &.small {
      font-size: 18px;
    }
  }

  .stat-sub {
    margin-top: 8px;
    font-size: 12px;
    color: #a8abb2;
  }

  .panel {
    margin-bottom: 16px;
    padding: 16px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 6px;
  }

  .panel-title {
    margin-bottom: 12px;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .balance-row {
    padding: 10px 0;
    border-bottom: 1px dashed #ebeef5;

    &:last-child {
      border-bottom: none;
    }
  }

  .balance-currency {
    font-size: 12px;
    color: #909399;
  }

  .balance-main {
    margin-top: 4px;
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }

  .balance-sub {
    margin-top: 4px;
    font-size: 12px;
    color: #a8abb2;
  }

  .empty-tip {
    padding: 24px 0;
    text-align: center;
    font-size: 13px;
    color: #909399;
  }

  @media (max-width: 768px) {
    .stat-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
}
</style>

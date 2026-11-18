<template>
  <div class="ai-overview">
    <basic-container>
      <div class="page-header">
        <div>
          <div class="page-title">AI 概览</div>
          <div class="page-desc">今日用量、运行状态与账户余额</div>
        </div>
        <el-button size="small" icon="el-icon-refresh" :loading="loading || usageLoading" @click="refresh">刷新</el-button>
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
        <div class="panel-title">每日限额</div>
        <div class="limit-row">
          <el-progress :percentage="dailyPercent"
                       :status="dailyStatus"
                       :stroke-width="12"
                       class="limit-progress"></el-progress>
          <div class="limit-text">{{ limitProgressText }}</div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-toolbar">
          <div class="panel-title">用量趋势</div>
          <el-radio-group v-model="rangeDays" size="mini" @change="loadUsage">
            <el-radio-button :label="1">今日</el-radio-button>
            <el-radio-button :label="7">7 天</el-radio-button>
            <el-radio-button :label="30">30 天</el-radio-button>
          </el-radio-group>
        </div>
        <el-table v-loading="usageLoading" :data="trend.points" size="small" border stripe max-height="360">
          <el-table-column prop="date" label="日期" width="120"></el-table-column>
          <el-table-column prop="requests" label="请求" width="80" align="center"></el-table-column>
          <el-table-column label="Token" min-width="200">
            <template slot-scope="{row}">
              <div class="bar-cell">
                <span class="bar-value">{{ row.totalTokens }}</span>
                <div class="bar">
                  <div class="bar-inner" :style="{ width: tokenPercent(row) }"></div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="promptTokens" label="输入" width="90" align="center"></el-table-column>
          <el-table-column prop="completionTokens" label="输出" width="90" align="center"></el-table-column>
          <el-table-column prop="cacheHitTokens" label="缓存命中" width="100" align="center"></el-table-column>
          <el-table-column prop="failures" label="失败" width="70" align="center"></el-table-column>
          <el-table-column label="平均延迟" width="100" align="center">
            <template slot-scope="{row}">{{ (row.avgLatencyMs || 0).toFixed(0) }} ms</template>
          </el-table-column>
        </el-table>
      </div>

      <el-row :gutter="16">
        <el-col :xs="24" :sm="24" :md="8">
          <div class="panel">
            <div class="panel-title">按场景</div>
            <el-table v-loading="usageLoading" :data="breakdown.byScene" size="small" border stripe>
              <el-table-column prop="label" label="场景" min-width="70"></el-table-column>
              <el-table-column prop="requests" label="请求" width="70" align="center"></el-table-column>
              <el-table-column prop="tokens" label="Token" width="100" align="center"></el-table-column>
              <el-table-column prop="failures" label="失败" width="60" align="center"></el-table-column>
            </el-table>
          </div>
        </el-col>
        <el-col :xs="24" :sm="24" :md="8">
          <div class="panel">
            <div class="panel-title">按模型</div>
            <el-table v-loading="usageLoading" :data="breakdown.byModel" size="small" border stripe>
              <el-table-column prop="label" label="模型" min-width="130"></el-table-column>
              <el-table-column prop="requests" label="请求" width="70" align="center"></el-table-column>
              <el-table-column prop="tokens" label="Token" width="100" align="center"></el-table-column>
              <el-table-column prop="failures" label="失败" width="60" align="center"></el-table-column>
            </el-table>
          </div>
        </el-col>
        <el-col :xs="24" :sm="24" :md="8">
          <div class="panel">
            <div class="panel-toolbar">
              <div class="panel-title">{{ rankScene === 'group' ? '按群 Top 10' : '私聊 Top 10' }}</div>
              <el-radio-group v-model="rankScene" size="mini">
                <el-radio-button label="group">群聊</el-radio-button>
                <el-radio-button label="private">私聊</el-radio-button>
              </el-radio-group>
            </div>
            <el-table v-loading="usageLoading" :data="rankList" size="small" border stripe>
              <el-table-column :label="rankScene === 'group' ? '群' : '用户'" min-width="120">
                <template slot-scope="{row}">
                  <div class="rank-target">
                    <el-avatar :size="24" :src="rankAvatar(row)" class="rank-avatar">
                      <i :class="rankScene === 'group' ? 'el-icon-chat-dot-round' : 'el-icon-user-solid'"></i>
                    </el-avatar>
                    <span class="rank-label">{{ row.key || row.label }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="requests" label="请求" width="70" align="center"></el-table-column>
              <el-table-column prop="tokens" label="Token" width="100" align="center"></el-table-column>
              <el-table-column prop="failures" label="失败" width="60" align="center"></el-table-column>
            </el-table>
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
import { overview, usageTrend, usageBreakdown } from '@/api/ai';
import { sessionAvatarUrl } from '@/util/avatar';

export default {
  name: 'AiOverview',
  data() {
    return {
      loading: false,
      usageLoading: false,
      rangeDays: 7,
      rankScene: 'group',
      data: {
        today: {},
        runtime: {},
        balance: {}
      },
      trend: {
        points: []
      },
      breakdown: {
        byScene: [],
        byModel: [],
        byGroup: [],
        byPrivate: []
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
    dailyPercent() {
      const limit = Number(this.runtime.dailyTokenLimit || 0);
      const used = Number(this.today.totalTokens || 0);
      if (!limit) {
        return 0;
      }
      return Math.min(100, Math.round((used * 1000) / limit) / 10);
    },
    dailyStatus() {
      if (!Number(this.runtime.dailyTokenLimit || 0)) {
        return '';
      }
      if (this.dailyPercent >= 100) {
        return 'exception';
      }
      return this.dailyPercent >= 80 ? 'warning' : '';
    },
    limitProgressText() {
      const used = Number(this.today.totalTokens || 0).toLocaleString();
      const limit = Number(this.runtime.dailyTokenLimit || 0);
      return limit > 0 ? used + ' / ' + limit.toLocaleString() : used + '（不限）';
    },
    maxTrendTokens() {
      return Math.max(1, ...this.trend.points.map(point => Number(point.totalTokens || 0)));
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
    },
    rankList() {
      const list = this.rankScene === 'group' ? this.breakdown.byGroup : this.breakdown.byPrivate;
      return list || [];
    }
  },
  created() {
    this.load();
    this.loadUsage();
  },
  methods: {
    refresh() {
      this.load();
      this.loadUsage();
    },
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
    },
    loadUsage() {
      this.usageLoading = true;
      Promise.all([
        usageTrend({ days: this.rangeDays }),
        usageBreakdown({ days: this.rangeDays })
      ]).then(([trendRes, breakdownRes]) => {
        this.trend = this.unwrap(trendRes) || { points: [] };
        this.breakdown = this.unwrap(breakdownRes) || { byScene: [], byModel: [], byGroup: [], byPrivate: [] };
      }).catch(() => {
        this.$message.error('加载用量统计失败');
      }).finally(() => {
        this.usageLoading = false;
      });
    },
    unwrap(res) {
      const body = res && res.data ? res.data : {};
      if (body.code !== 200) {
        throw new Error(body.message || '接口返回失败');
      }
      return body.data;
    },
    tokenPercent(row) {
      const value = Number(row.totalTokens || 0);
      return Math.min(100, Math.round((value * 1000) / this.maxTrendTokens) / 10) + '%';
    },
    rankAvatar(row) {
      return sessionAvatarUrl(this.rankScene, row.key);
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

  .panel-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;

    .panel-title {
      margin-bottom: 0;
    }
  }

  .limit-row {
    display: flex;
    align-items: center;
  }

  .limit-progress {
    flex: 1;
    margin-right: 16px;
  }

  .limit-text {
    min-width: 150px;
    text-align: right;
    font-size: 12px;
    color: #606266;
  }

  .bar-cell {
    display: flex;
    align-items: center;
  }

  .bar-value {
    width: 80px;
    flex-shrink: 0;
    font-size: 12px;
    color: #606266;
  }

  .bar {
    flex: 1;
    height: 8px;
    overflow: hidden;
    background: #f0f2f5;
    border-radius: 4px;
  }

  .bar-inner {
    height: 100%;
    background: linear-gradient(90deg, #79bbff, #409eff);
    border-radius: 4px;
    transition: width 0.3s;
  }

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .rank-target {
    display: flex;
    align-items: center;
  }

  .rank-avatar {
    margin-right: 8px;
    flex-shrink: 0;
    background: #f0f2f5;
  }

  .rank-label {
    color: #606266;
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

<template>
  <div class="ai-session">
    <basic-container>
      <div class="page-header">
        <div>
          <div class="page-title">会话管理</div>
          <div class="page-desc">查看群聊 / 私聊会话与最近对话（P0 只读）</div>
        </div>
        <el-button size="small" icon="el-icon-refresh" :loading="loading" @click="load">刷新</el-button>
      </div>

      <el-form :inline="true" size="small" class="filter-bar">
        <el-form-item label="场景">
          <el-select v-model="filters.scene" clearable placeholder="全部" style="width: 110px">
            <el-option label="群聊" value="group"></el-option>
            <el-option label="私聊" value="private"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="目标ID">
          <el-input v-model="filters.targetId" clearable placeholder="群号 / QQ" style="width: 150px"></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width: 110px">
            <el-option label="运行中" value="active"></el-option>
            <el-option label="已暂停" value="paused"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="最后活跃">
          <el-date-picker v-model="filters.datetimerange"
                          type="datetimerange"
                          value-format="yyyy-MM-dd HH:mm:ss"
                          range-separator="至"
                          start-placeholder="开始时间"
                          end-placeholder="结束时间"
                          style="width: 330px">
          </el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="search">查询</el-button>
          <el-button icon="el-icon-refresh-left" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" size="small" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center"></el-table-column>
        <el-table-column label="场景" width="90" align="center">
          <template slot-scope="{row}">
            <el-tag size="mini" :type="row.scene === 'group' ? 'success' : 'warning'" effect="plain">
              {{ sceneText(row.scene) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="目标" min-width="170">
          <template slot-scope="{row}">
            <div class="target-cell">
              <el-avatar :size="28" :src="sessionAvatar(row)" class="target-avatar">
                <i :class="row.scene === 'group' ? 'el-icon-chat-dot-round' : 'el-icon-user-solid'"></i>
              </el-avatar>
              <span class="target-id">{{ row.targetId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="120">
          <template slot-scope="{row}">{{ row.roleName || '默认角色' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{row}">
            <el-tag size="mini" :type="row.status === 'paused' ? 'info' : 'success'" effect="plain">
              {{ row.status === 'paused' ? '已暂停' : '运行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="messageCount" label="消息数" width="90" align="center"></el-table-column>
        <el-table-column prop="todayTokens" label="今日 Token" width="110" align="center"></el-table-column>
        <el-table-column prop="lastActiveAt" label="最后活跃" min-width="160"></el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template slot-scope="{row}">
            <el-button size="mini" type="text" @click="open(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager"
                     background
                     :current-page.sync="page.currentPage"
                     :page-size="page.pageSize"
                     :page-sizes="[10, 20, 50, 100]"
                     layout="total, sizes, prev, pager, next"
                     :total="total"
                     @size-change="onSizeChange"
                     @current-change="load">
      </el-pagination>
    </basic-container>

    <el-drawer :visible.sync="drawer" size="72%" direction="rtl" :with-header="false">
      <div v-loading="detailLoading" class="drawer-body">
        <template v-if="detail">
          <div class="drawer-header">
            <div class="drawer-title-wrap">
              <el-avatar :size="42" :src="sessionAvatar(detail.session)" class="drawer-avatar">
                <i :class="detail.session.scene === 'group' ? 'el-icon-chat-dot-round' : 'el-icon-user-solid'"></i>
              </el-avatar>
              <div>
                <div class="drawer-title">
                  {{ sceneText(detail.session.scene) }} {{ detail.session.targetId }}
                  <el-tag size="mini" :type="detail.session.status === 'paused' ? 'info' : 'success'" effect="plain">
                    {{ detail.session.status === 'paused' ? '已暂停' : '运行中' }}
                  </el-tag>
                </div>
                <div class="drawer-sub">
                  实际角色：{{ detail.effectiveRole || '无角色' }} ·
                  会话角色：{{ detail.session.roleName || '默认' }} ·
                  最后活跃：{{ detail.session.lastActiveAt || '-' }}
                </div>
              </div>
            </div>
            <div class="drawer-actions">
              <el-button size="small"
                         :type="detail.session.status === 'paused' ? 'success' : 'warning'"
                         :loading="actionLoading"
                         @click="toggleStatus">
                {{ detail.session.status === 'paused' ? '恢复' : '暂停' }}
              </el-button>
              <el-button size="small" @click="openRoleDialog">切换角色</el-button>
              <el-button size="small" type="danger" plain :loading="actionLoading" @click="resetSession">重置</el-button>
              <el-button size="small" @click="drawer = false">关闭</el-button>
            </div>
          </div>

          <div class="summary-grid">
            <div class="summary-card">
              <div class="summary-label">消息数</div>
              <div class="summary-value">{{ detail.session.messageCount }}</div>
            </div>
            <div class="summary-card">
              <div class="summary-label">今日 Token</div>
              <div class="summary-value">{{ detail.session.todayTokens }}</div>
            </div>
            <div class="summary-card">
              <div class="summary-label">累计请求</div>
              <div class="summary-value">{{ detail.totalRequests }}</div>
            </div>
            <div class="summary-card">
              <div class="summary-label">累计 Token</div>
              <div class="summary-value">{{ detail.totalTokens }}</div>
            </div>
            <div class="summary-card">
              <div class="summary-label">失败次数</div>
              <div class="summary-value">{{ detail.failures }}</div>
            </div>
            <div class="summary-card">
              <div class="summary-label">平均延迟</div>
              <div class="summary-value">{{ (detail.avgLatencyMs || 0).toFixed(0) }} ms</div>
            </div>
            <div class="summary-card">
              <div class="summary-label">长期记忆</div>
              <div class="summary-value">{{ detail.memoryCount }}</div>
            </div>
          </div>

          <el-tabs v-model="activeTab" class="detail-tabs">
            <el-tab-pane label="对话记录" name="messages">
              <div v-if="!messages.length" class="empty-tip">暂无对话记录</div>
              <div v-for="msg in messages" :key="msg.id" class="chat-row"
                   :class="msg.role === 'assistant' ? 'is-bot' : 'is-user'">
                <el-avatar :size="34" :src="messageAvatar(msg)" class="chat-avatar">
                  <i :class="msg.role === 'assistant' ? 'el-icon-service' : 'el-icon-user-solid'"></i>
                </el-avatar>
                <div class="chat-main">
                  <div class="chat-meta">
                    {{ msg.role === 'assistant' ? '你' : (msg.speakerName || msg.speakerId || '用户') }}
                  </div>
                  <div class="chat-bubble">{{ msg.content }}</div>
                  <div class="chat-time">{{ msg.createdAt }}</div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="长期记忆" name="memories">
              <div v-if="!memories.length" class="empty-tip">暂无长期记忆</div>
              <el-table v-else :data="memories" size="small" border stripe>
                <el-table-column prop="memKey" label="Key" min-width="140"></el-table-column>
                <el-table-column prop="memValue" label="内容" min-width="260"></el-table-column>
                <el-table-column prop="updatedAt" label="更新时间" min-width="160"></el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="调用记录" name="usage">
              <div v-if="!usage.length" class="empty-tip">暂无调用记录</div>
              <el-table v-else :data="usage" size="small" border stripe>
                <el-table-column prop="model" label="模型" min-width="130"></el-table-column>
                <el-table-column prop="promptTokens" label="输入" width="90" align="center"></el-table-column>
                <el-table-column prop="completionTokens" label="输出" width="90" align="center"></el-table-column>
                <el-table-column prop="totalTokens" label="总 Token" width="100" align="center"></el-table-column>
                <el-table-column prop="cacheHitTokens" label="缓存命中" width="100" align="center"></el-table-column>
                <el-table-column prop="latencyMs" label="延迟(ms)" width="100" align="center"></el-table-column>
                <el-table-column label="结果" width="80" align="center">
                  <template slot-scope="{row}">
                    <el-tag size="mini" :type="row.success ? 'success' : 'danger'" effect="plain">
                      {{ row.success ? '成功' : '失败' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="时间" min-width="160"></el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="Prompt 预览" name="prompt">
              <div class="empty-tip">P1 实现：按当前会话上下文生成 system + messages，不调用 API。</div>
            </el-tab-pane>
          </el-tabs>
        </template>
        <div v-else class="empty-tip">没有找到会话</div>
      </div>
    </el-drawer>

    <el-dialog title="切换角色" :visible.sync="roleDialogVisible" width="420px">
      <el-select v-model="selectedRole" clearable placeholder="恢复默认解析（群映射 / 全局默认）" style="width: 100%">
        <el-option v-for="role in roleOptions" :key="role.name" :label="role.name" :value="role.name"></el-option>
      </el-select>
      <div class="dialog-tip">
        只影响当前会话；留空表示按「会话 > 群/私聊映射 > 全局默认」重新解析。
      </div>
      <div slot="footer">
        <el-button size="small" @click="roleDialogVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="actionLoading" @click="saveRole">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  sessionSearch,
  sessionDetail,
  sessionMessages,
  sessionMemories,
  sessionUsage,
  sessionStatus,
  sessionRole,
  sessionReset,
  roleList
} from '@/api/ai';
import { sessionAvatarUrl, userAvatarUrl } from '@/util/avatar';

export default {
  name: 'AiSession',
  data() {
    return {
      loading: false,
      filters: {
        scene: '',
        targetId: '',
        status: '',
        datetimerange: []
      },
      page: {
        currentPage: 1,
        pageSize: 20
      },
      total: 0,
      rows: [],
      drawer: false,
      detailLoading: false,
      detail: null,
      activeTab: 'messages',
      messages: [],
      memories: [],
      usage: [],
      actionLoading: false,
      roleDialogVisible: false,
      roleOptions: [],
      selectedRole: ''
    };
  },
  created() {
    this.load();
  },
  methods: {
    search() {
      this.page.currentPage = 1;
      this.load();
    },
    reset() {
      this.filters = {
        scene: '',
        targetId: '',
        status: '',
        datetimerange: []
      };
      this.search();
    },
    load() {
      this.loading = true;
      sessionSearch({
        currentPage: this.page.currentPage,
        pageSize: this.page.pageSize,
        scene: this.filters.scene || undefined,
        targetId: this.filters.targetId || undefined,
        status: this.filters.status || undefined,
        datetimerange: this.filters.datetimerange && this.filters.datetimerange.length
          ? this.filters.datetimerange
          : undefined
      }).then(({ data: { code, message, data } }) => {
        if (code !== 200) {
          return this.$message.error(message || '查询失败');
        }
        this.rows = (data && data.records) || [];
        this.total = (data && data.total) || 0;
      }).catch(() => {
        this.$message.error('查询会话失败');
      }).finally(() => {
        this.loading = false;
      });
    },
    onSizeChange(size) {
      this.page.pageSize = size;
      this.page.currentPage = 1;
      this.load();
    },
    open(row) {
      this.drawer = true;
      this.detailLoading = true;
      this.activeTab = 'messages';
      this.detail = null;
      this.messages = [];
      this.memories = [];
      this.usage = [];
      const id = row.id;
      Promise.all([
        sessionDetail({ id }),
        sessionMessages({ id, limit: 50 }),
        sessionMemories({ id }),
        sessionUsage({ id, limit: 20 })
      ]).then(([detailRes, messageRes, memoryRes, usageRes]) => {
        this.detail = this.unwrap(detailRes);
        this.messages = this.unwrap(messageRes) || [];
        this.memories = this.unwrap(memoryRes) || [];
        this.usage = this.unwrap(usageRes) || [];
      }).catch(() => {
        this.$message.error('加载会话详情失败');
      }).finally(() => {
        this.detailLoading = false;
      });
    },
    unwrap(res) {
      const body = res && res.data ? res.data : {};
      if (body.code !== 200) {
        throw new Error(body.message || '接口返回失败');
      }
      return body.data;
    },
    toggleStatus() {
      if (!this.detail) {
        return;
      }
      const next = this.detail.session.status === 'paused' ? 'active' : 'paused';
      this.actionLoading = true;
      sessionStatus({ id: this.detail.session.id, status: next }).then((res) => {
        this.detail = this.unwrap(res);
        this.$message.success(next === 'paused' ? '已暂停当前会话' : '已恢复当前会话');
        this.load();
      }).catch((e) => {
        this.$message.error((e && e.message) || '操作失败');
      }).finally(() => {
        this.actionLoading = false;
      });
    },
    openRoleDialog() {
      if (!this.detail) {
        return;
      }
      this.selectedRole = this.detail.session.roleName || '';
      this.roleDialogVisible = true;
      if (this.roleOptions.length) {
        return;
      }
      roleList().then((res) => {
        this.roleOptions = this.unwrap(res) || [];
      }).catch(() => {
        this.$message.error('加载角色卡失败');
      });
    },
    saveRole() {
      if (!this.detail) {
        return;
      }
      this.actionLoading = true;
      sessionRole({
        id: this.detail.session.id,
        roleName: this.selectedRole || ''
      }).then((res) => {
        this.detail = this.unwrap(res);
        this.roleDialogVisible = false;
        this.$message.success('角色已切换');
        this.load();
      }).catch((e) => {
        this.$message.error((e && e.message) || '切换角色失败');
      }).finally(() => {
        this.actionLoading = false;
      });
    },
    resetSession() {
      if (!this.detail) {
        return;
      }
      this.$confirm('将清空当前会话的全部历史消息，且不可恢复，是否继续？', '重置会话', {
        confirmButtonText: '重置',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.actionLoading = true;
        return sessionReset({ id: this.detail.session.id }).then((res) => {
          this.detail = this.unwrap(res);
          this.messages = [];
          this.$message.success('会话已重置');
          this.load();
        });
      }).catch((e) => {
        if (e !== 'cancel') {
          this.$message.error((e && e.message) || '重置失败');
        }
      }).finally(() => {
        this.actionLoading = false;
      });
    },
    sessionAvatar(session) {
      if (!session) {
        return '';
      }
      return sessionAvatarUrl(session.scene, session.targetId);
    },
    messageAvatar(msg) {
      if (msg.role === 'assistant') {
        const session = this.detail ? this.detail.session : null;
        return userAvatarUrl(session ? session.selfId : null);
      }
      if (msg.speakerId) {
        return userAvatarUrl(msg.speakerId);
      }
      const session = this.detail ? this.detail.session : null;
      return session && session.scene === 'private' ? userAvatarUrl(session.targetId) : '';
    },
    sceneText(scene) {
      return scene === 'group' ? '群聊' : '私聊';
    }
  }
};
</script>

<style lang="scss" scoped>
.ai-session {
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

  .filter-bar {
    padding: 12px 12px 0;
    margin-bottom: 12px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 6px;
  }

  .pager {
    margin-top: 16px;
    text-align: right;
  }

  .target-cell {
    display: flex;
    align-items: center;
  }

  .target-avatar {
    flex-shrink: 0;
    margin-right: 8px;
    color: #909399;
    background: #f0f2f5;
  }

  .target-id {
    color: #303133;
  }

  .drawer-body {
    min-height: 100%;
    padding: 20px;
    box-sizing: border-box;
  }

  .drawer-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding-bottom: 16px;
    border-bottom: 1px solid #ebeef5;
  }

  .drawer-title-wrap {
    display: flex;
    align-items: center;
  }

  .drawer-avatar {
    flex-shrink: 0;
    margin-right: 12px;
    color: #909399;
    background: #f0f2f5;
  }

  .drawer-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }

  .drawer-sub {
    margin-top: 6px;
    font-size: 12px;
    color: #909399;
  }

  .drawer-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .dialog-tip {
    margin-top: 10px;
    font-size: 12px;
    line-height: 1.6;
    color: #909399;
  }

  .summary-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 10px;
    margin: 16px 0;
  }

  .summary-card {
    padding: 12px;
    background: #fafafa;
    border-radius: 6px;
  }

  .summary-label {
    font-size: 12px;
    color: #909399;
  }

  .summary-value {
    margin-top: 6px;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .detail-tabs {
    margin-top: 8px;
  }

  .empty-tip {
    padding: 32px 0;
    text-align: center;
    font-size: 13px;
    color: #909399;
  }

  .chat-row {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 16px;

    &.is-user {
      flex-direction: row-reverse;
    }
  }

  .chat-avatar {
    flex-shrink: 0;
    color: #909399;
    background: #f0f2f5;
  }

  .chat-main {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    max-width: 72%;
  }

  .chat-row.is-user .chat-main {
    align-items: flex-end;
    text-align: right;
  }

  .chat-meta {
    margin-bottom: 4px;
    font-size: 12px;
    color: #909399;
  }

  .chat-time {
    margin-top: 4px;
    font-size: 11px;
    color: #c0c4cc;
  }

  .chat-bubble {
    display: inline-block;
    max-width: 100%;
    padding: 8px 12px;
    border-radius: 8px;
    font-size: 13px;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-word;
    text-align: left;
  }

  .is-user .chat-bubble {
    background: #ecf5ff;
    color: #303133;
  }

  .is-bot .chat-bubble {
    background: #f4f4f5;
    color: #303133;
  }

  @media (max-width: 768px) {
    .summary-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .chat-main {
      max-width: 78%;
    }
  }
}
</style>

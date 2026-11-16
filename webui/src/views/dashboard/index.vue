<template>
  <div class="dashboard">
    <bot-status-bar></bot-status-bar>

    <div class="kpi-grid">
      <button
        v-for="item in kpiList"
        :key="item.key"
        type="button"
        class="kpi-card"
        @click="handleKpi(item.key)"
      >
        <div class="kpi-head">
          <span class="kpi-label">{{ item.label }}</span>
          <i :class="item.icon"></i>
        </div>
        <div class="kpi-value">{{ item.value }}</div>
        <div class="kpi-foot">
          <span>{{ item.foot }}</span>
          <i class="el-icon-arrow-right kpi-arrow"></i>
        </div>
      </button>
    </div>

    <div class="dashboard-grid">
      <el-card ref="botCard" class="dash-card dashboard-card" shadow="never">
        <div class="card-head">
          <span class="card-title">Bot 实例</span>
          <span class="card-extra">{{ botLoaded ? bots.length + ' 个在线' : '—' }}</span>
        </div>

        <div v-if="botLoading" class="card-empty">加载中...</div>
        <div v-else-if="botError && !botLoaded" class="card-empty">
          Bot 列表加载失败
          <el-button type="text" size="mini" @click="loadBots()">重试</el-button>
        </div>
        <div v-else-if="bots.length === 0" class="card-empty">
          暂无 Bot 连接，请在 NapCat 中配置反向 WebSocket 地址与 Token
        </div>
        <ul v-else class="bot-list">
          <li v-for="bot in visibleBots" :key="bot.id" class="bot-item">
            <el-avatar :size="36" :src="bot.avatarUrl" class="bot-avatar">{{ avatarText(bot) }}</el-avatar>
            <div class="bot-main">
              <div class="bot-name">{{ bot.name || '未命名 Bot' }}</div>
              <div class="bot-meta">QQ {{ bot.id }}</div>
            </div>
            <span class="bot-state"><i></i>在线</span>
          </li>
        </ul>

        <div class="card-foot">
          <router-link to="/group-list">群聊查询</router-link>
          <router-link to="/friend-list">好友列表</router-link>
        </div>
      </el-card>

      <realtime-task-card ref="taskCard" @snapshot="handleTaskSnapshot" @error="handleTaskError"></realtime-task-card>

      <recent-log-card></recent-log-card>

      <el-card class="dash-card dashboard-card" shadow="never">
        <div class="card-head">
          <span class="card-title">快捷入口</span>
        </div>
        <div class="quick-grid">
          <router-link v-for="entry in quickEntries" :key="entry.path" :to="entry.path" class="quick-item">
            <i :class="entry.icon"></i>
            <span>{{ entry.label }}</span>
          </router-link>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import BotStatusBar from './bot-status-bar.vue'
import RealtimeTaskCard from './realtime-task-card.vue'
import RecentLogCard from './recent-log-card.vue'
import { botList } from '@/api/system'
import { search as searchGroup } from '@/api/group'
import { search as searchFriend } from '@/api/friend'

const OVERVIEW_REFRESH_MILLIS = 30000

export default {
  name: 'Dashboard',
  components: {
    BotStatusBar,
    RealtimeTaskCard,
    RecentLogCard
  },
  data() {
    return {
      bots: [],
      botLoaded: false,
      botLoading: false,
      botError: false,
      groupTotal: 0,
      groupLoaded: false,
      groupError: false,
      friendTotal: 0,
      friendLoaded: false,
      friendError: false,
      taskLoaded: false,
      taskError: false,
      taskStats: { jmRunning: 0, jmQueued: 0, biliRunning: 0 },
      overviewTimer: null,
      overviewLoadedOnce: false,
      refreshing: false,
      quickEntries: [
        { label: '聊天记录', path: '/chat-record', icon: 'el-icon-chat-dot-round' },
        { label: '群聊查询', path: '/group-list', icon: 'el-icon-user' },
        { label: '好友列表', path: '/friend-list', icon: 'el-icon-user-solid' },
        { label: 'JM 漫画', path: '/jmcomic', icon: 'el-icon-picture-outline' },
        { label: 'B站订阅', path: '/bilibili-subscribe', icon: 'el-icon-video-camera' },
        { label: 'B站视频', path: '/bilibili-video', icon: 'el-icon-film' },
        { label: '配置管理', path: '/config', icon: 'el-icon-setting' },
        { label: '日志监控', path: '/log-monitor', icon: 'el-icon-monitor' }
      ]
    }
  },
  computed: {
    visibleBots() {
      return this.bots.slice(0, 4)
    },
    kpiList() {
      const taskCount = this.taskLoaded
        ? this.taskStats.jmRunning + this.taskStats.biliRunning
        : null
      let taskFoot = this.taskError ? '获取失败' : '统计中'
      if (this.taskLoaded) {
        taskFoot = taskCount > 0
          ? `JM ${this.taskStats.jmRunning} · B站 ${this.taskStats.biliRunning}`
          : '暂无进行中任务'
      }
      return [
        {
          key: 'bots',
          label: '在线 Bot',
          value: this.botLoaded ? this.formatNumber(this.bots.length) : '—',
          foot: this.botLoaded ? 'WebSocket 已连接' : (this.botError ? '加载失败' : '统计中'),
          icon: 'el-icon-cpu'
        },
        {
          key: 'groups',
          label: '群聊总数',
          value: this.groupLoaded ? this.formatNumber(this.groupTotal) : '—',
          foot: this.groupLoaded ? '已同步群聊' : (this.groupError ? '获取失败' : '统计中'),
          icon: 'el-icon-chat-dot-round'
        },
        {
          key: 'friends',
          label: '好友总数',
          value: this.friendLoaded ? this.formatNumber(this.friendTotal) : '—',
          foot: this.friendLoaded ? '已同步好友' : (this.friendError ? '获取失败' : '统计中'),
          icon: 'el-icon-user'
        },
        {
          key: 'tasks',
          label: '进行中任务',
          value: taskCount === null ? '—' : this.formatNumber(taskCount),
          foot: taskFoot,
          icon: 'el-icon-tickets'
        }
      ]
    }
  },
  activated() {
    this.loadOverview(this.overviewLoadedOnce)
    this.overviewLoadedOnce = true
    this.startAutoRefresh()
  },
  deactivated() {
    this.stopAutoRefresh()
  },
  beforeDestroy() {
    this.stopAutoRefresh()
  },
  methods: {
    loadOverview(silent = false) {
      if (this.refreshing) return
      this.refreshing = true
      Promise.allSettled([
        this.loadBots(silent),
        this.loadGroupTotal(),
        this.loadFriendTotal()
      ]).finally(() => {
        this.refreshing = false
      })
    },
    loadBots(silent = false) {
      if (!silent) {
        this.botLoading = true
      }
      return botList()
        .then(({ data: { data, code } }) => {
          if (code !== 200) {
            if (!this.botLoaded) this.botError = true
            return
          }
          this.bots = data || []
          this.botLoaded = true
          this.botError = false
        })
        .catch(() => {
          if (!this.botLoaded) this.botError = true
        })
        .finally(() => {
          this.botLoading = false
        })
    },
    loadGroupTotal() {
      return searchGroup({ currentPage: 1, pageSize: 1 })
        .then(({ data: { data, code } }) => {
          if (code !== 200 || !data) {
            if (!this.groupLoaded) this.groupError = true
            return
          }
          this.groupTotal = data.total || 0
          this.groupLoaded = true
          this.groupError = false
        })
        .catch(() => {
          if (!this.groupLoaded) this.groupError = true
        })
    },
    loadFriendTotal() {
      return searchFriend({ currentPage: 1, pageSize: 1 })
        .then(({ data: { data, code } }) => {
          if (code !== 200 || !data) {
            if (!this.friendLoaded) this.friendError = true
            return
          }
          this.friendTotal = data.total || 0
          this.friendLoaded = true
          this.friendError = false
        })
        .catch(() => {
          if (!this.friendLoaded) this.friendError = true
        })
    },
    handleTaskSnapshot(stats) {
      this.taskStats = stats || { jmRunning: 0, jmQueued: 0, biliRunning: 0 }
      this.taskLoaded = true
      this.taskError = false
    },
    handleTaskError() {
      if (!this.taskLoaded) this.taskError = true
    },
    handleKpi(key) {
      if (key === 'groups') {
        this.$router.push('/group-list')
      } else if (key === 'friends') {
        this.$router.push('/friend-list')
      } else if (key === 'bots') {
        this.scrollToCard('botCard')
      } else if (key === 'tasks') {
        this.scrollToCard('taskCard')
      }
    },
    scrollToCard(ref) {
      const target = this.$refs[ref]
      const el = target && (target.$el || target)
      if (el && el.scrollIntoView) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    },
    startAutoRefresh() {
      this.stopAutoRefresh()
      this.overviewTimer = setInterval(() => {
        if (document.hidden) return
        this.loadOverview(true)
      }, OVERVIEW_REFRESH_MILLIS)
    },
    stopAutoRefresh() {
      if (this.overviewTimer) {
        clearInterval(this.overviewTimer)
        this.overviewTimer = null
      }
    },
    avatarText(bot) {
      const name = bot.name || String(bot.id || '?')
      return name.slice(0, 1)
    },
    formatNumber(value) {
      if (value === null || value === undefined || value === '') return '—'
      const number = Number(value)
      return Number.isNaN(number) ? String(value) : number.toLocaleString('zh-CN')
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard {
  padding: 10px 6px 16px;
  box-sizing: border-box;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.kpi-card {
  display: block;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #ffffff;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;

  &:hover {
    border-color: #c6e2ff;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    transform: translateY(-1px);
  }

  &:focus-visible {
    outline: 2px solid rgba(64, 158, 255, 0.4);
    outline-offset: 2px;
  }
}

.kpi-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  color: #909399;

  i {
    font-size: 16px;
    color: #c0c4cc;
  }
}

.kpi-value {
  margin-top: 8px;
  font-size: 28px;
  line-height: 1.1;
  font-weight: 600;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.kpi-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.kpi-arrow {
  color: #c0c4cc;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 12px;

  > * {
    min-width: 0;
  }
}

.dash-card {
  border-radius: 4px;
}

.dashboard-card {
  ::v-deep .el-card__body {
    padding: 16px;
  }
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-extra {
  font-size: 12px;
  color: #909399;
}

.card-empty {
  padding: 28px 8px;
  text-align: center;
  font-size: 13px;
  line-height: 1.6;
  color: #909399;
}

.bot-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bot-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.bot-avatar {
  flex: none;
  background: #f5f7fa;
  color: #909399;
}

.bot-main {
  flex: 1;
  min-width: 0;
}

.bot-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.bot-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.bot-state {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #67c23a;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #67c23a;
  }
}

.card-foot {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
  font-size: 12px;

  a {
    color: #409eff;
    text-decoration: none;
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 64px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  text-decoration: none;
  transition: border-color 0.2s, background-color 0.2s, color 0.2s;

  i {
    font-size: 20px;
    color: #409eff;
  }

  &:hover {
    border-color: #c6e2ff;
    background: #ecf5ff;
    color: #409eff;
  }
}

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 992px) {
  .dashboard-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 768px) {
  .dashboard {
    padding: 8px 4px 12px;
  }

  .kpi-grid {
    gap: 8px;
  }

  .kpi-card {
    padding: 12px;
  }

  .kpi-value {
    font-size: 22px;
  }

  .dashboard-grid {
    gap: 8px;
  }
}
</style>

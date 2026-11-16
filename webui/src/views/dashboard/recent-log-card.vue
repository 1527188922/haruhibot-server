<template>
  <el-card class="dash-card log-card" shadow="never">
    <div class="card-head">
      <span class="card-title">最近日志</span>
      <span class="log-head-right">
        <el-tag size="mini" :type="stateTagType">{{ stateText }}</el-tag>
        <router-link to="/log-monitor" class="card-link">查看全部</router-link>
      </span>
    </div>

    <div v-if="logs.length === 0" class="card-empty">
      {{ state === 'error' ? '日志连接失败，正在重试...' : '正在等待日志...' }}
    </div>
    <ul v-else class="log-list">
      <li v-for="(item, index) in logs" :key="item.raw + index" class="log-item" :class="'is-' + item.level.toLowerCase()">
        <span class="log-time">{{ item.time || '--:--:--' }}</span>
        <span class="log-level">{{ item.level }}</span>
        <span class="log-text" :title="item.raw">{{ item.text }}</span>
      </li>
    </ul>
  </el-card>
</template>

<script>
import { createLogSSEClient } from '@/api/log'

const MAX_LOGS = 6

export default {
  name: 'RecentLogCard',
  data() {
    return {
      state: 'connecting',
      logs: [],
      sseClient: null
    }
  },
  computed: {
    stateTagType() {
      if (this.state === 'live') return 'success'
      if (this.state === 'error') return 'danger'
      return 'info'
    },
    stateText() {
      if (this.state === 'live') return '实时'
      if (this.state === 'error') return '重连中'
      return '连接中'
    }
  },
  activated() {
    this.connect()
  },
  deactivated() {
    this.close()
  },
  beforeDestroy() {
    this.close()
  },
  methods: {
    connect() {
      if (this.sseClient) return
      this.logs = []
      this.state = 'connecting'
      this.sseClient = createLogSSEClient(20, this.handleMessage, {
        onOpen: () => {
          this.state = 'live'
        },
        onError: () => {
          this.state = 'error'
        }
      })
      this.sseClient.connect()
    },
    close() {
      if (this.sseClient) {
        this.sseClient.close()
        this.sseClient = null
      }
      this.logs = []
      this.state = 'connecting'
    },
    handleMessage({ data }) {
      if (!data || data.length === 0) return
      const parsed = []
      data.forEach((raw) => {
        if (!raw) return
        try {
          const obj = JSON.parse(raw)
          if (obj && typeof obj.data === 'string') {
            const item = this.parseLine(obj.data)
            if (item) parsed.push(item)
          }
        } catch (e) {
          // 单行解析失败不影响后续日志
        }
      })
      if (parsed.length === 0) return
      this.state = 'live'
      this.appendLogs(parsed)
    },
    appendLogs(lines) {
      const all = this.logs.concat(lines)
      const seen = {}
      const result = []
      for (let i = all.length - 1; i >= 0 && result.length < MAX_LOGS; i--) {
        const item = all[i]
        if (seen[item.raw]) continue
        seen[item.raw] = true
        result.unshift(item)
      }
      this.logs = result
    },
    parseLine(raw) {
      const cleaned = raw.replace(/\u001b\[[0-9;]*m/g, '')
      if (!cleaned) return null
      const timeMatch = cleaned.match(/(\d{2}:\d{2}:\d{2}(?:[.,]\d{1,3})?)/)
      const levelMatch = cleaned.match(/\b(TRACE|DEBUG|INFO|WARN|ERROR)\b/)
      let text = cleaned
        .replace(/^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}[.,]?\d*\s*/, '')
        .replace(/^(TRACE|DEBUG|INFO|WARN|ERROR)\s*/, '')
      if (!text) text = cleaned
      return {
        time: timeMatch ? timeMatch[1].replace(',', '.') : '',
        level: levelMatch ? levelMatch[1] : 'LOG',
        text,
        raw: cleaned
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.log-card {
  border-radius: 4px;

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

.log-head-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-link {
  font-size: 12px;
  color: #409eff;
  text-decoration: none;
}

.card-empty {
  padding: 40px 0;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.log-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.log-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 18px;
}

.log-time {
  flex: none;
  color: #909399;
}

.log-level {
  flex: none;
  width: 44px;
  padding: 0 4px;
  border-radius: 2px;
  text-align: center;
  font-size: 11px;
  font-weight: 600;
  color: #909399;
  background: #f4f4f5;
}

.log-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #606266;
}

.is-error {
  .log-level {
    color: #f56c6c;
    background: rgba(245, 108, 108, 0.1);
  }

  .log-text {
    color: #f56c6c;
  }
}

.is-warn {
  .log-level {
    color: #e6a23c;
    background: rgba(230, 162, 60, 0.1);
  }

  .log-text {
    color: #b88230;
  }
}

.is-info {
  .log-level {
    color: #409eff;
    background: rgba(64, 158, 255, 0.1);
  }
}

.is-debug,
.is-trace {
  .log-level,
  .log-text {
    color: #a8abb2;
  }
}
</style>

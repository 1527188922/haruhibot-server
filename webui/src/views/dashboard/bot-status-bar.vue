<template>
  <el-card class="dash-card status-card" shadow="never" v-loading="loading">
    <div class="status-row">
      <div class="status-main">
        <span class="status-dot" :class="statusDotClass"></span>
        <span class="status-name">Bot 反向 WebSocket</span>
        <el-tag v-if="loaded" effect="dark" size="mini" :type="running ? 'success' : 'danger'">
          {{ running ? '运行中' : '已停止' }}
        </el-tag>
        <span class="status-divider"></span>
        <span class="status-meta">连接数 <b>{{ connectionText }}</b></span>
        <span class="status-meta">控制台实时通道 <b :class="wsStatusClass">{{ wsStatusText }}</b></span>
      </div>
      <div class="status-actions">
        <el-button size="mini" icon="el-icon-refresh" :loading="loading" @click="loadInfo">刷新</el-button>
        <el-button
          size="mini"
          type="success"
          plain
          icon="el-icon-video-play"
          :disabled="running"
          :loading="operating === 'start'"
          @click="start"
        >启动</el-button>
        <el-button
          size="mini"
          type="warning"
          plain
          icon="el-icon-refresh-right"
          :disabled="!running"
          :loading="operating === 'restart'"
          @click="restart"
        >重启</el-button>
        <el-button
          size="mini"
          type="danger"
          plain
          icon="el-icon-switch-button"
          :disabled="!running"
          :loading="operating === 'stop'"
          @click="stop"
        >停止</el-button>
      </div>
    </div>
    <div class="status-row status-row--detail">
      <span class="status-meta">
        WebSocket 路径
        <b class="mono">{{ info.path || '-' }}</b>
        <el-button v-if="info.path" type="text" size="mini" icon="el-icon-document-copy" @click="copy(info.path)">复制</el-button>
      </span>
      <span class="status-meta">
        Token
        <b class="mono">{{ tokenText }}</b>
        <el-button v-if="info.accessToken" type="text" size="mini" @click="tokenVisible = !tokenVisible">
          {{ tokenVisible ? '隐藏' : '显示' }}
        </el-button>
        <el-button v-if="info.accessToken" type="text" size="mini" icon="el-icon-document-copy" @click="copy(info.accessToken)">复制</el-button>
        <span v-if="loaded && !info.accessToken" class="status-hint">未配置</span>
      </span>
    </div>
  </el-card>
</template>

<script>
import { botWsInfo as botWsInfoApi, botWsOperation, restartBot } from '@/api/system'

export default {
  name: 'BotStatusBar',
  data() {
    return {
      loading: false,
      loaded: false,
      operating: '',
      info: {},
      tokenVisible: false,
      wsStatus: this.$ws.getStatus(),
      wsStatusOff: null,
      restartTimer: null
    }
  },
  computed: {
    running() {
      return !!this.info.running
    },
    statusDotClass() {
      if (!this.loaded) return 'is-unknown'
      return this.running ? 'is-on' : 'is-off'
    },
    connectionText() {
      if (!this.loaded) return '—'
      return `${this.info.connections == null ? 0 : this.info.connections} / ${this.info.maxConnections == null ? 0 : this.info.maxConnections}`
    },
    tokenText() {
      if (!this.loaded) return '—'
      if (!this.info.accessToken) return ''
      return this.tokenVisible ? this.info.accessToken : '••••••••'
    },
    wsStatusText() {
      if (this.wsStatus === 'open') return '已连接'
      if (this.wsStatus === 'connecting') return '连接中'
      if (this.wsStatus === 'closed') return '已断开'
      return '未连接'
    },
    wsStatusClass() {
      if (this.wsStatus === 'open') return 'is-success'
      if (this.wsStatus === 'connecting') return 'is-warning'
      if (this.wsStatus === 'closed') return 'is-danger'
      return 'is-muted'
    }
  },
  mounted() {
    this.wsStatusOff = this.$ws.onStatus((status) => {
      this.wsStatus = status
    })
  },
  activated() {
    this.loadInfo()
  },
  beforeDestroy() {
    if (this.wsStatusOff) {
      this.wsStatusOff()
      this.wsStatusOff = null
    }
    if (this.restartTimer) {
      clearTimeout(this.restartTimer)
      this.restartTimer = null
    }
  },
  methods: {
    loadInfo() {
      if (this.loading) return
      this.loading = true
      botWsInfoApi()
        .then(({ data: { data, code, message } }) => {
          if (code !== 200) {
            this.$message.error(message)
            return
          }
          this.info = data || {}
          this.loaded = true
        })
        .catch(() => {
          // 异常已由 http 拦截器统一提示，这里只吞掉 rejection
        })
        .finally(() => {
          this.loading = false
        })
    },
    start() {
      this.operate('1', '启动')
    },
    stop() {
      this.$confirm('确认停止 Bot WebSocket 服务？将清空并断开所有 Bot 连接。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.operate('2', '停止')
      }).catch(() => {})
    },
    operate(command, actionName) {
      if (this.operating) return
      this.operating = actionName === '启动' ? 'start' : 'stop'
      botWsOperation(command)
        .then(({ data: { data, code, message } }) => {
          if (code !== 200) {
            this.$message.error(message)
            return
          }
          this.info = data || this.info
          this.loaded = true
          this.$message.success(message || `${actionName}成功`)
        })
        .catch(() => {})
        .finally(() => {
          this.operating = ''
        })
    },
    restart() {
      this.$confirm('确认重启 Bot 程序？WebUI 可能会短暂断开连接。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        if (this.operating) return
        this.operating = 'restart'
        restartBot()
          .then(({ data: { code, message } }) => {
            if (code !== 200) {
              this.$message.error(message)
              return
            }
            this.$message.success(message || '重启命令已执行')
            this.restartTimer = setTimeout(() => {
              this.loadInfo()
            }, 3000)
          })
          .catch(() => {})
          .finally(() => {
            this.operating = ''
          })
      }).catch(() => {})
    },
    copy(text) {
      if (!text) return
      const success = () => this.$message.success('已复制')
      const fallback = () => {
        const textarea = document.createElement('textarea')
        textarea.value = text
        textarea.style.position = 'fixed'
        textarea.style.opacity = '0'
        document.body.appendChild(textarea)
        textarea.select()
        try {
          document.execCommand('copy')
          success()
        } catch (e) {
          this.$message.error('复制失败，请手动选择')
        }
        document.body.removeChild(textarea)
      }
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(text).then(success).catch(fallback)
      } else {
        fallback()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.status-card {
  margin-bottom: 12px;
  border-radius: 4px;

  ::v-deep .el-card__body {
    padding: 14px 16px;
  }
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.status-row--detail {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
  gap: 20px;
}

.status-main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  flex: none;

  &.is-on {
    background: #67c23a;
    box-shadow: 0 0 0 4px rgba(103, 194, 58, 0.12);
  }

  &.is-off {
    background: #f56c6c;
    box-shadow: 0 0 0 4px rgba(245, 108, 108, 0.12);
  }
}

.status-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.status-divider {
  width: 1px;
  height: 12px;
  background: #e4e7ed;
}

.status-meta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;

  b {
    font-weight: 600;
    color: #606266;
  }

  .mono {
    font-family: Consolas, Monaco, 'Courier New', monospace;
    font-weight: 400;
  }

  .is-success {
    color: #67c23a;
  }

  .is-warning {
    color: #e6a23c;
  }

  .is-danger {
    color: #f56c6c;
  }

  .is-muted {
    color: #c0c4cc;
  }
}

.status-hint {
  color: #c0c4cc;
}

.status-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;

  ::v-deep .el-button + .el-button {
    margin-left: 0;
  }
}

@media (max-width: 768px) {
  .status-actions {
    width: 100%;
  }

  .status-row--detail {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
}
</style>

<template>
  <el-card class="dash-card status-card" shadow="never" v-loading="loading">
    <div class="status-row">
      <div class="status-main">
        <span class="status-dot" :class="statusDotClass"></span>
        <span class="status-name">Bot 接入</span>
        <span class="status-sub">反向 WebSocket</span>
        <el-tag v-if="loaded" effect="dark" size="mini" :type="running ? 'success' : 'danger'">
          {{ running ? '接入中' : '已关闭' }}
        </el-tag>
        <span class="status-divider"></span>
        <span class="status-meta">连接数 <b>{{ connectionText }}</b></span>
        <span class="status-meta">控制台实时通道 <b :class="wsStatusClass">{{ wsStatusText }}</b></span>
      </div>
      <div class="status-actions">
        <div class="access-control">
          <el-switch
            :value="accessEnabled"
            :disabled="!loaded || !!operating"
            active-color="#67c23a"
            inactive-color="#c0c4cc"
            @change="handleAccessChange"
          ></el-switch>
          <i v-if="operating === 'start' || operating === 'stop'" class="el-icon-loading access-loading"></i>
          <el-tooltip content="仅控制反向 WebSocket 接入，Bot 进程不受影响" placement="top">
            <span class="access-state" :class="{ 'is-on': running }">{{ accessStateText }}</span>
          </el-tooltip>
          <el-tooltip content="刷新接入状态" placement="top">
            <el-button
              type="text"
              size="mini"
              icon="el-icon-refresh"
              :disabled="!!operating"
              :loading="loading"
              @click="loadInfo"
            ></el-button>
          </el-tooltip>
        </div>
        <span class="action-divider"></span>
        <el-tooltip content="重启 Bot 程序，WebUI 会短暂断开" placement="top">
          <el-button
            type="text"
            size="mini"
            class="restart-btn"
            icon="el-icon-refresh-right"
            :disabled="!!operating"
            :loading="operating === 'restart'"
            @click="restart"
          >重启程序</el-button>
        </el-tooltip>
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
      accessEnabled: false,
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
    accessStateText() {
      if (!this.loaded) return '未知'
      if (this.operating === 'start' || this.operating === 'stop') return '切换中...'
      return this.running ? '已开启' : '已关闭'
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
          this.accessEnabled = !!this.info.running
        })
        .catch(() => {
          // 异常已由 http 拦截器统一提示，这里只吞掉 rejection
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleAccessChange(value) {
      if (value) {
        this.accessEnabled = true
        this.operate('1')
        return
      }
      this.$confirm('关闭后 NapCat 将无法连接，Bot 不会响应消息，但进程不会退出。确认关闭接入？', '关闭 Bot 接入', {
        confirmButtonText: '确认关闭',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.accessEnabled = false
        this.operate('2')
      }).catch(() => {
        this.accessEnabled = this.running
      })
    },
    operate(command) {
      if (this.operating) return
      const actionName = command === '1' ? '开启接入' : '关闭接入'
      this.operating = command === '1' ? 'start' : 'stop'
      botWsOperation(command)
        .then(({ data: { data, code, message } }) => {
          if (code !== 200) {
            this.$message.error(message)
            this.accessEnabled = this.running
            return
          }
          this.info = data || this.info
          this.loaded = true
          this.accessEnabled = !!this.info.running
          this.$message.success(message || `${actionName}成功`)
        })
        .catch(() => {
          this.accessEnabled = this.running
        })
        .finally(() => {
          this.operating = ''
        })
    },
    restart() {
      this.$confirm('将重启整个 Bot 程序进程，WebUI 可能会短暂断开连接。确认继续？', '重启 Bot 程序', {
        confirmButtonText: '确认重启',
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

.status-sub {
  font-size: 12px;
  color: #909399;
}

.access-control {
  display: inline-flex;
  align-items: center;
  gap: 6px;

  ::v-deep .el-button--text {
    color: #909399;
    font-size: 12px;

    &:hover,
    &:focus {
      color: #409eff;
    }
  }
}

.access-state {
  font-size: 12px;
  color: #909399;
  cursor: default;

  &.is-on {
    color: #67c23a;
  }
}

.access-loading {
  font-size: 12px;
  color: #909399;
}

.restart-btn {
  color: #606266;
  font-size: 12px;

  &:hover,
  &:focus {
    color: #e6a23c;
  }
}

.action-divider {
  width: 1px;
  height: 14px;
  background: #e4e7ed;
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

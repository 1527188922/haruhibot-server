<template>
  <el-card class="dash-card usage-card" shadow="never" v-loading="loading && !loaded">
    <div class="card-head">
      <span class="card-title">资源占用</span>
      <span class="card-extra">{{ loadError ? '更新失败' : (updatedAt ? updatedAt + ' 更新' : '') }}</span>
    </div>

    <div v-if="loadError && !loaded" class="card-empty">
      资源占用获取失败
      <el-button type="text" size="mini" @click="load">重试</el-button>
    </div>
    <div v-else-if="!loaded" class="card-empty">正在获取资源占用...</div>
    <div v-else class="usage-body">
      <div class="usage-list">
        <div class="usage-row">
          <div class="usage-top">
            <span class="usage-label">CPU 使用率</span>
            <span class="usage-value">{{ cpuText }}</span>
          </div>
          <el-progress :percentage="cpuPercent" :stroke-width="6" :show-text="false" :color="progressColor"></el-progress>
        </div>
        <div class="usage-row">
          <div class="usage-top">
            <span class="usage-label">系统内存</span>
            <span class="usage-value">{{ memoryText }}</span>
          </div>
          <el-progress :percentage="memoryPercent" :stroke-width="6" :show-text="false" :color="progressColor"></el-progress>
        </div>
        <div class="usage-row">
          <div class="usage-top">
            <span class="usage-label">程序内存</span>
            <span class="usage-value">{{ jvmMemoryText }}</span>
          </div>
          <el-progress :percentage="jvmMemoryPercent" :stroke-width="6" :show-text="false" :color="progressColor"></el-progress>
        </div>
        <div class="usage-row">
          <div class="usage-top">
            <span class="usage-label">磁盘占用</span>
            <span class="usage-value">{{ diskText }}</span>
          </div>
          <el-progress :percentage="diskPercent" :stroke-width="6" :show-text="false" :color="progressColor"></el-progress>
        </div>
      </div>
      <div class="system-info">
        <div class="info-row">
          <span class="info-label">系统</span>
          <span class="info-value">{{ osName || '—' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">版本</span>
          <span class="info-value">{{ osVersion || '—' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">CPU 核心</span>
          <span class="info-value">{{ coresText }}</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script>
import { systemUsage } from '@/api/system'

const USAGE_REFRESH_MILLIS = 10000

export default {
  name: 'ResourceUsageCard',
  data() {
    return {
      loading: false,
      loaded: false,
      loadError: false,
      cpuUsage: null,
      memUsed: null,
      memTotal: null,
      jvmMemUsed: null,
      jvmMemMax: null,
      diskUsed: null,
      diskTotal: null,
      osName: '',
      osVersion: '',
      cpuCores: null,
      updatedAt: '',
      timer: null
    }
  },
  computed: {
    cpuPercent() {
      return this.cpuUsage === null ? 0 : this.clampPercent(this.cpuUsage)
    },
    cpuText() {
      return this.cpuUsage === null ? '—' : `${Math.round(this.cpuUsage)}%`
    },
    memoryPercent() {
      if (this.memTotal === null || this.memTotal <= 0 || this.memUsed === null) return 0
      return this.clampPercent(this.memUsed * 100 / this.memTotal)
    },
    memoryText() {
      if (this.memUsed === null || this.memTotal === null) return '—'
      return `${this.memUsed.toFixed(1)} / ${this.memTotal.toFixed(1)} GB`
    },
    jvmMemoryPercent() {
      if (this.jvmMemMax === null || this.jvmMemMax <= 0 || this.jvmMemUsed === null) return 0
      return this.clampPercent(this.jvmMemUsed * 100 / this.jvmMemMax)
    },
    jvmMemoryText() {
      if (this.jvmMemUsed === null || this.jvmMemMax === null) return '—'
      return `${this.jvmMemUsed.toFixed(1)} / ${this.jvmMemMax.toFixed(1)} GB`
    },
    diskPercent() {
      if (this.diskTotal === null || this.diskTotal <= 0 || this.diskUsed === null) return 0
      return this.clampPercent(this.diskUsed * 100 / this.diskTotal)
    },
    diskText() {
      if (this.diskUsed === null || this.diskTotal === null) return '—'
      return `${this.diskUsed.toFixed(1)} / ${this.diskTotal.toFixed(1)} GB`
    },
    coresText() {
      return this.cpuCores === null ? '—' : `${this.cpuCores} 核`
    }
  },
  activated() {
    this.load()
    this.startTimer()
  },
  deactivated() {
    this.stopTimer()
  },
  beforeDestroy() {
    this.stopTimer()
  },
  methods: {
    async load() {
      if (this.loading) return
      this.loading = true
      try {
        const { data: body } = await systemUsage()
        if (!body || body.code !== 200 || !body.data) {
          this.loadError = true
          return
        }
        const data = body.data
        this.cpuUsage = this.toNumber(data.cpuUsage)
        this.memUsed = this.toNumber(data.memUsed)
        this.memTotal = this.toNumber(data.memTotal)
        this.jvmMemUsed = this.toNumber(data.jvmMemUsed)
        this.jvmMemMax = this.toNumber(data.jvmMemMax)
        this.diskUsed = this.toNumber(data.diskUsed)
        this.diskTotal = this.toNumber(data.diskTotal)
        this.osName = data.osName || ''
        this.osVersion = data.osVersion || ''
        this.cpuCores = this.toNumber(data.cpuCores)
        if (this.cpuUsage === null && this.memUsed === null && this.jvmMemUsed === null) {
          this.loadError = true
          return
        }
        this.loaded = true
        this.loadError = false
        this.updatedAt = this.formatTime(data.timestamp)
      } catch (e) {
        this.loadError = true
      } finally {
        this.loading = false
      }
    },
    startTimer() {
      this.stopTimer()
      this.timer = setInterval(() => {
        if (document.hidden) return
        this.load()
      }, USAGE_REFRESH_MILLIS)
    },
    stopTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },
    toNumber(value) {
      if (value === null || value === undefined || value === '') return null
      const number = Number(value)
      return Number.isFinite(number) && number >= 0 ? number : null
    },
    clampPercent(value) {
      return Math.max(0, Math.min(100, Math.round(value)))
    },
    progressColor(percentage) {
      return percentage > 90 ? '#f56c6c' : ''
    },
    formatTime(timestamp) {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      if (Number.isNaN(date.getTime())) return ''
      const pad = (value) => String(value).padStart(2, '0')
      return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    }
  }
}
</script>

<style lang="scss" scoped>
.usage-card {
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

.card-extra {
  font-size: 12px;
  color: #909399;
}

.card-empty {
  padding: 28px 0;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.usage-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.usage-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.usage-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.usage-label {
  font-size: 12px;
  color: #909399;
}

.usage-value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.system-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
}

.info-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
}

.info-label {
  flex: none;
  color: #909399;
}

.info-value {
  color: #606266;
  text-align: right;
  word-break: break-all;
}
</style>

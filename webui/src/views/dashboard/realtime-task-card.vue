<template>
  <el-card class="dash-card task-card" shadow="never" v-loading="loading && !loaded">
    <div class="card-head">
      <span class="card-title">实时任务</span>
      <span class="task-tags">
        <el-tag v-if="jmRunning" size="mini">JM {{ jmRunning }}</el-tag>
        <el-tag v-if="jmQueued" size="mini" type="info">排队 {{ jmQueued }}</el-tag>
        <el-tag v-if="biliRunning" size="mini" type="success">B站 {{ biliRunning }}</el-tag>
        <el-tag v-if="loaded && !hasActive" size="mini" type="info">空闲</el-tag>
      </span>
    </div>

    <div v-if="loadError && !loaded" class="card-empty">
      任务状态获取失败
      <el-button type="text" size="mini" @click="loadAll">重试</el-button>
    </div>
    <div v-else-if="!loaded" class="card-empty">正在获取任务状态...</div>
    <ul v-else-if="visibleTasks.length" class="task-list">
      <li v-for="task in visibleTasks" :key="task.key" class="task-item">
        <div class="task-line">
          <span class="task-type" :class="'is-' + task.typeKey">{{ task.type }}</span>
          <span class="task-name" :title="task.name">{{ task.name }}</span>
          <el-tag size="mini" :type="task.tagType">{{ task.status }}</el-tag>
        </div>
        <el-progress
          v-if="task.percent !== null"
          :percentage="task.percent"
          :stroke-width="4"
          :show-text="false"
          class="task-progress"
        ></el-progress>
        <div class="task-meta">{{ task.meta || '—' }}</div>
      </li>
    </ul>
    <div v-else class="card-empty">当前没有进行中的任务</div>

    <div class="card-foot">
      <router-link to="/jmcomic">JM 漫画</router-link>
      <router-link to="/bilibili-video">B站视频</router-link>
    </div>
  </el-card>
</template>

<script>
import { listJmTasks } from '@/api/jmcomic'
import { downloadTasks } from '@/api/bilibili-video'

const JM_TASK_TOPIC = 'jm.task'
const JM_TASK_SNAPSHOT = 'jm.task.snapshot'
const JM_TASK_LIST_COMMAND = 'jm.task.list'

const BILI_DOWNLOAD_TOPIC = 'bilibili.video.download'
const BILI_DOWNLOAD_SNAPSHOT = 'bilibili.video.download.snapshot'
const BILI_DOWNLOAD_LIST_COMMAND = 'bilibili.video.download.list'

const FALLBACK_POLL_MILLIS = 10000

export default {
  name: 'RealtimeTaskCard',
  data() {
    return {
      loading: false,
      loaded: false,
      loadError: false,
      bound: false,
      wsStatus: this.$ws.getStatus(),
      jm: {
        counters: { running: 0, queued: 0 },
        runningList: [],
        queuedList: []
      },
      bili: {
        counters: { running: 0, success: 0, fail: 0 },
        runningList: []
      },
      jmSnapshotOff: null,
      biliSnapshotOff: null,
      wsStatusOff: null,
      fallbackTimer: null
    }
  },
  computed: {
    jmRunning() {
      return Number(this.jm.counters.running) || 0
    },
    jmQueued() {
      return Number(this.jm.counters.queued) || 0
    },
    biliRunning() {
      return Number(this.bili.counters.running) || 0
    },
    hasActive() {
      return this.jmRunning > 0 || this.jmQueued > 0 || this.biliRunning > 0
    },
    visibleTasks() {
      const tasks = []
      this.jm.runningList.slice(0, 2).forEach((task) => {
        tasks.push({
          key: `jm-running-${task.taskId || task.aid}`,
          type: 'JM',
          typeKey: 'jm',
          name: task.albumName || `JM${task.aid || ''}`,
          status: task.statusName || '进行中',
          tagType: '',
          percent: this.jmPercent(task),
          meta: this.jmMeta(task)
        })
      })
      if (tasks.length < 3) {
        this.jm.queuedList.slice(0, 1).forEach((task) => {
          tasks.push({
            key: `jm-queued-${task.taskId || task.aid}`,
            type: 'JM',
            typeKey: 'jm',
            name: task.albumName || `JM${task.aid || ''}`,
            status: task.statusName || '排队中',
            tagType: 'info',
            percent: null,
            meta: task.queuePosition ? `排队第 ${task.queuePosition} 位` : ''
          })
        })
      }
      if (tasks.length < 3) {
        this.bili.runningList.slice(0, 3 - tasks.length).forEach((task) => {
          tasks.push({
            key: `bili-${task.taskId || task.bvid}`,
            type: 'B站',
            typeKey: 'bili',
            name: task.title || task.bvid || 'B站视频',
            status: task.statusName || '下载中',
            tagType: 'success',
            percent: null,
            meta: [task.bvid, task.fileName].filter(Boolean).join(' · ')
          })
        })
      }
      return tasks
    }
  },
  activated() {
    this.bind()
  },
  deactivated() {
    this.unbind()
  },
  beforeDestroy() {
    this.unbind()
  },
  methods: {
    bind() {
      if (this.bound) return
      this.bound = true
      this.wsStatus = this.$ws.getStatus()
      this.jmSnapshotOff = this.$ws.on(JM_TASK_SNAPSHOT, this.applyJm)
      this.biliSnapshotOff = this.$ws.on(BILI_DOWNLOAD_SNAPSHOT, this.applyBili)
      this.wsStatusOff = this.$ws.onStatus(this.handleWsStatus)
      this.$ws.subscribe(JM_TASK_TOPIC)
      this.$ws.subscribe(BILI_DOWNLOAD_TOPIC)
      if (this.wsStatus !== 'open') {
        // WS 尚未连上时先走 HTTP 轮询，等连接建立后 handleWsStatus 会自动停止兜底
        this.startFallback()
      }
      this.loadAll()
    },
    unbind() {
      if (!this.bound) return
      this.bound = false
      if (this.jmSnapshotOff) {
        this.jmSnapshotOff()
        this.jmSnapshotOff = null
      }
      if (this.biliSnapshotOff) {
        this.biliSnapshotOff()
        this.biliSnapshotOff = null
      }
      if (this.wsStatusOff) {
        this.wsStatusOff()
        this.wsStatusOff = null
      }
      this.$ws.unsubscribe(JM_TASK_TOPIC)
      this.$ws.unsubscribe(BILI_DOWNLOAD_TOPIC)
      this.stopFallback()
    },
    handleWsStatus(status) {
      if (status === this.wsStatus) return
      this.wsStatus = status
      if (status === 'open') {
        this.stopFallback()
        this.loadAll()
      } else {
        this.startFallback()
      }
    },
    async loadAll() {
      if (this.loading) return
      this.loading = true
      this.loadError = false
      try {
        if (this.$ws.isOpen()) {
          const results = await Promise.allSettled([
            this.$ws.send(JM_TASK_LIST_COMMAND),
            this.$ws.send(BILI_DOWNLOAD_LIST_COMMAND)
          ])
          if (results[0].status === 'fulfilled' && results[0].value && results[0].value.data) {
            this.applyJm(results[0].value.data)
          }
          if (results[1].status === 'fulfilled' && results[1].value && results[1].value.data) {
            this.applyBili(results[1].value.data)
          }
        } else {
          await this.loadHttp()
        }
      } finally {
        this.loading = false
        if (!this.loaded) {
          this.loadError = true
          this.$emit('error')
        }
      }
    },
    async loadHttp() {
      const results = await Promise.allSettled([listJmTasks(), downloadTasks()])
      if (results[0].status === 'fulfilled') {
        const body = results[0].value.data || {}
        if (body.code === 200 && body.data) {
          this.applyJm(body.data)
        }
      }
      if (results[1].status === 'fulfilled') {
        const body = results[1].value.data || {}
        if (body.code === 200 && body.data) {
          this.applyBili(body.data)
        }
      }
    },
    applyJm(data) {
      if (!data) return
      this.jm = {
        counters: data.counters || { running: 0, queued: 0 },
        runningList: data.runningList || [],
        queuedList: data.queuedList || []
      }
      this.loaded = true
      this.loadError = false
      this.emitStats()
    },
    applyBili(data) {
      if (!data) return
      this.bili = {
        counters: data.counters || { running: 0, success: 0, fail: 0 },
        runningList: data.runningList || []
      }
      this.loaded = true
      this.loadError = false
      this.emitStats()
    },
    emitStats() {
      this.$emit('snapshot', {
        jmRunning: this.jmRunning,
        jmQueued: this.jmQueued,
        biliRunning: this.biliRunning
      })
    },
    jmPercent(task) {
      const imageTotal = Number(task.imageTotal) || 0
      if (imageTotal > 0) {
        const downloaded = Number(task.imageDownloaded) || 0
        return Math.max(0, Math.min(100, Math.round(downloaded * 100 / imageTotal)))
      }
      const chapterTotal = Number(task.chapterTotal) || 0
      if (chapterTotal > 0) {
        const chapterIndex = Number(task.chapterIndex) || 0
        return Math.max(0, Math.min(100, Math.round(chapterIndex * 100 / chapterTotal)))
      }
      return null
    },
    jmMeta(task) {
      const parts = []
      if (task.chapterTotal) {
        parts.push(`第 ${task.chapterIndex || 1}/${task.chapterTotal} 话`)
      }
      if (task.imageTotal) {
        parts.push(`${task.imageDownloaded || 0}/${task.imageTotal} 张`)
      }
      return parts.join(' · ') || task.stage || ''
    },
    startFallback() {
      if (this.fallbackTimer) return
      this.fallbackTimer = setInterval(() => {
        if (document.hidden) return
        this.loadAll()
      }, FALLBACK_POLL_MILLIS)
    },
    stopFallback() {
      if (this.fallbackTimer) {
        clearInterval(this.fallbackTimer)
        this.fallbackTimer = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.task-card {
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

.task-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.card-empty {
  padding: 28px 0;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.task-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  min-width: 0;
}

.task-line {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.task-type {
  flex: none;
  font-size: 11px;
  font-weight: 600;
  padding: 1px 5px;
  border-radius: 2px;

  &.is-jm {
    color: #409eff;
    background: rgba(64, 158, 255, 0.1);
  }

  &.is-bili {
    color: #67c23a;
    background: rgba(103, 194, 58, 0.1);
  }
}

.task-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #303133;
}

.task-progress {
  margin-top: 6px;
}

.task-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
</style>

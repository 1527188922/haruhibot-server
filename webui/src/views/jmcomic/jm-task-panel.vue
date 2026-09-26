<template>
  <el-drawer :title="drawerTitle" :visible.sync="visibleProxy" size="660px" direction="rtl" custom-class="jm-task-drawer">
    <div class="jm-task-panel">
      <div class="jm-task-toolbar">
        <span class="jm-task-summary">
          <el-tag size="mini" type="primary">进行中 {{counters.running}}</el-tag>
          <el-tag size="mini" type="info">排队中 {{counters.queued}}</el-tag>
          <el-tag v-if="counters.success" size="mini" type="success">成功 {{counters.success}}</el-tag>
          <el-tag v-if="counters.fail" size="mini" type="danger">失败 {{counters.fail}}</el-tag>
          <el-tag v-if="counters.cancelled" size="mini" type="warning">已取消 {{counters.cancelled}}</el-tag>
        </span>
        <span class="jm-task-toolbar-ops">
          <el-tag size="mini" effect="plain" :type="wsConnected ? 'success' : 'info'">
            {{wsConnected ? '实时推送' : '未连接 · 轮询兜底'}}
          </el-tag>
          <el-switch v-model="autoRefresh" class="jm-task-switch" size="mini" active-text="自动更新"></el-switch>
          <el-button type="text" size="mini" :loading="loading" @click="loadTasks()">刷新</el-button>
          <el-button type="text" size="mini" :disabled="counters.queued === 0" @click="cancelAllQueued">取消全部排队</el-button>
        </span>
      </div>

      <el-alert v-if="parallel" class="jm-task-alert" type="info" :closable="false" show-icon
                title="当前为并行执行模式，任务不进入等待队列，因此不提供取消"></el-alert>

      <div v-loading="loading && !loaded" class="jm-task-body">
        <div class="jm-task-section">
          <div class="jm-task-section-title">
            进行中
            <el-tag size="mini" type="primary">{{runningList.length}}</el-tag>
            <span class="jm-task-section-hint">正在执行的任务不支持取消</span>
          </div>
          <div v-if="runningList.length === 0" class="jm-task-empty">没有正在执行的任务</div>
          <div v-else class="jm-task-list">
            <div v-for="task in runningList" :key="task.taskId" class="jm-task-card">
              <div class="jm-task-cover">
                <el-image v-if="task.coverUrl" class="jm-task-cover-img" :src="task.coverUrl" fit="cover">
                  <div slot="error" class="jm-task-cover-fallback">JM{{task.aid}}</div>
                </el-image>
                <div v-else class="jm-task-cover-fallback">JM{{task.aid}}</div>
              </div>
              <div class="jm-task-main">
                <div class="jm-task-line">
                  <span class="jm-task-name" :title="taskDisplayName(task)">{{taskDisplayName(task)}}</span>
                  <el-tag size="mini" type="primary"><i class="el-icon-loading"></i> {{task.statusName}}</el-tag>
                </div>
                <div class="jm-task-meta-line">
                  <el-tag size="mini" effect="plain" :type="actionTagType(task)">{{task.actionName}}</el-tag>
                  <span class="jm-task-meta">JM{{task.aid}}</span>
                  <span v-if="task.stage" class="jm-task-stage">{{task.stage}}</span>
                  <span class="jm-task-meta">已运行 {{formatDuration(elapsedMillis(task))}}</span>
                </div>
                <div v-if="showChapterProgress(task)" class="jm-task-progress">
                  <span class="jm-task-chapter">第 {{task.chapterIndex || 1}}/{{task.chapterTotal || 1}} 话</span>
                  <template v-if="hasImageProgress(task)">
                    <el-progress class="jm-task-bar" :percentage="imagePercent(task)" :stroke-width="6"
                                 :show-text="false" color="#409eff"></el-progress>
                    <span class="jm-task-count">{{task.imageDownloaded}}/{{task.imageTotal}} 张</span>
                  </template>
                  <span v-else class="jm-task-meta">正在获取本话图片列表…</span>
                </div>
              </div>
              <div class="jm-task-ops">
                <el-tooltip content="正在执行的任务不支持取消" placement="left">
                  <i class="el-icon-lock jm-task-lock"></i>
                </el-tooltip>
              </div>
            </div>
          </div>
        </div>

        <div class="jm-task-section">
          <div class="jm-task-section-title">
            排队中
            <el-tag size="mini" type="info">{{queuedList.length}}</el-tag>
            <span class="jm-task-section-hint">按提交顺序执行，可取消</span>
          </div>
          <div v-if="queuedList.length === 0" class="jm-task-empty">没有排队中的任务</div>
          <div v-else class="jm-task-list">
            <div v-for="task in queuedList" :key="task.taskId" class="jm-task-card">
              <div class="jm-task-cover">
                <el-image v-if="task.coverUrl" class="jm-task-cover-img" :src="task.coverUrl" fit="cover">
                  <div slot="error" class="jm-task-cover-fallback">JM{{task.aid}}</div>
                </el-image>
                <div v-else class="jm-task-cover-fallback">JM{{task.aid}}</div>
              </div>
              <div class="jm-task-main">
                <div class="jm-task-line">
                  <span class="jm-task-name" :title="taskDisplayName(task)">{{taskDisplayName(task)}}</span>
                  <el-tag size="mini" type="info">{{task.statusName}}</el-tag>
                </div>
                <div class="jm-task-meta-line">
                  <el-tag size="mini" effect="plain" :type="actionTagType(task)">{{task.actionName}}</el-tag>
                  <span class="jm-task-meta">JM{{task.aid}}</span>
                  <span class="jm-task-meta">第 {{task.queuePosition}} 位 · 已等待 {{formatDuration(waitMillis(task))}}</span>
                </div>
              </div>
              <div class="jm-task-ops">
                <el-button type="danger" size="mini" plain :loading="cancellingMap[task.taskId]" @click="cancelTask(task)">取消</el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="jm-task-section">
          <div class="jm-task-section-title jm-task-section-title-clickable" @click="finishedCollapsed = !finishedCollapsed">
            最近完成
            <el-tag size="mini" type="success">{{counters.success}}</el-tag>
            <el-tag v-if="counters.fail" size="mini" type="danger">{{counters.fail}}</el-tag>
            <el-tag v-if="counters.cancelled" size="mini" type="warning">{{counters.cancelled}}</el-tag>
            <span class="jm-task-section-hint">{{finishedCollapsed ? '展开' : '收起'}}</span>
          </div>
          <template v-if="!finishedCollapsed">
            <div v-if="finishedList.length === 0" class="jm-task-empty">本次运行还没有完成的任务</div>
            <div v-else class="jm-task-list">
              <div v-for="task in finishedList" :key="task.taskId" class="jm-task-card jm-task-card-finished">
                <div class="jm-task-main">
                  <div class="jm-task-line">
                    <span class="jm-task-name" :title="taskDisplayName(task)">{{taskDisplayName(task)}}</span>
                    <el-tag size="mini" :type="statusTagType(task)">{{task.statusName}}</el-tag>
                  </div>
                  <div class="jm-task-meta-line">
                    <el-tag size="mini" effect="plain" :type="actionTagType(task)">{{task.actionName}}</el-tag>
                    <span class="jm-task-meta">JM{{task.aid}}</span>
                    <span class="jm-task-meta">用时 {{formatDuration(task.costMillis)}}</span>
                  </div>
                  <div v-if="task.message" class="jm-task-message" :title="task.message">{{task.message}}</div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import { cancelJmTask, cancelQueuedJmTasks, listJmTasks } from "@/api/jmcomic";

// 任务主题与命令，与后端 JmTaskPushService 保持一致
const JM_TASK_TOPIC = 'jm.task'
const JM_TASK_LIST_COMMAND = 'jm.task.list'
// 全局WebSocket未连通时的轮询兜底间隔
const FALLBACK_POLL_MILLIS = 3000

export default {
  name: 'JmTaskPanel',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false,
      loaded: false,
      autoRefresh: true,
      // 全局WebSocket是否连通，未连通时降级为轮询
      wsConnected: false,
      parallel: false,
      runningList: [],
      queuedList: [],
      finishedList: [],
      counters: this.defCounters(),
      finishedCollapsed: false,
      cancellingMap: {},
      tickTimer: null,
      fallbackTimer: null,
      nowTick: Date.now(),
      // 是否已向总线订阅(避免重复订阅造成引用计数泄漏)
      pushBound: false,
      snapshotOff: null,
      statusOff: null
    }
  },
  computed: {
    visibleProxy: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    },
    drawerTitle() {
      const active = this.counters.running + this.counters.queued
      return active > 0 ? `JM任务队列（${active}）` : 'JM任务队列'
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.startTick()
        this.bindPush()
        this.loadTasks()
      } else {
        this.stopTick()
        this.unbindPush()
      }
    },
    autoRefresh(value) {
      if (value) {
        this.bindPush()
      } else {
        this.unbindPush()
      }
    }
  },
  beforeDestroy() {
    this.stopTick()
    this.unbindPush()
  },
  methods: {
    defCounters() {
      return { running: 0, queued: 0, success: 0, fail: 0, cancelled: 0 }
    },
    /**
     * 已运行/已等待是本地走表的，不依赖推送
     */
    startTick() {
      this.stopTick()
      this.tickTimer = setInterval(() => {
        this.nowTick = Date.now()
      }, 1000)
    },
    stopTick() {
      if (this.tickTimer) {
        clearInterval(this.tickTimer)
        this.tickTimer = null
      }
    },
    /**
     * 订阅全局WebSocket的任务推送；未连通时启动轮询兜底
     */
    bindPush() {
      if (!this.visible || !this.autoRefresh || this.pushBound) {
        return
      }
      this.pushBound = true
      this.snapshotOff = this.$ws.on('jm.task.snapshot', this.applySnapshot)
      this.statusOff = this.$ws.onStatus(this.handleWsStatus)
      this.$ws.subscribe(JM_TASK_TOPIC)
      this.syncFallbackTimer()
    },
    unbindPush() {
      if (!this.pushBound) {
        this.clearFallbackTimer()
        return
      }
      this.pushBound = false
      if (this.snapshotOff) {
        this.snapshotOff()
        this.snapshotOff = null
      }
      if (this.statusOff) {
        this.statusOff()
        this.statusOff = null
      }
      this.$ws.unsubscribe(JM_TASK_TOPIC)
      this.clearFallbackTimer()
    },
    handleWsStatus(status) {
      this.wsConnected = status === 'open'
      this.syncFallbackTimer()
    },
    /**
     * WebSocket断开时（或未支持时）用HTTP轮询兜底，保证抽屉里始终有数据
     */
    syncFallbackTimer() {
      this.clearFallbackTimer()
      if (!this.visible || this.wsConnected || document.hidden) {
        return
      }
      this.fallbackTimer = setInterval(() => this.loadTasks(), FALLBACK_POLL_MILLIS)
    },
    clearFallbackTimer() {
      if (this.fallbackTimer) {
        clearInterval(this.fallbackTimer)
        this.fallbackTimer = null
      }
    },
    /**
     * 手动刷新：WebSocket连通时走总线命令，否则回退HTTP接口
     */
    async loadTasks() {
      this.loading = true
      try {
        if (this.$ws.isOpen()) {
          const message = await this.$ws.send(JM_TASK_LIST_COMMAND)
          if (message && message.data) {
            this.applySnapshot(message.data)
          }
        } else {
          const { data: { code, data } } = await listJmTasks()
          if (code === 200 && data) {
            this.applySnapshot(data)
          }
        }
      } catch (e) {
        // 静默处理，避免轮询失败刷屏
      } finally {
        this.loading = false
      }
    },
    /**
     * 推送、轮询、手动刷新三条来源都汇总到这里
     */
    applySnapshot(data) {
      if (!data) {
        return
      }
      this.parallel = !!data.parallel
      this.runningList = data.runningList || []
      this.queuedList = data.queuedList || []
      this.finishedList = data.finishedList || []
      this.counters = data.counters || this.defCounters()
      this.loaded = true
      // 把快照回传给页面，页面据此刷新徽标与列表行内状态
      this.$emit('snapshot', data)
    },
    taskDisplayName(task) {
      return task && task.albumName ? task.albumName : `JM${task.aid}`
    },
    statusTagType(task) {
      const map = { QUEUED: 'info', RUNNING: 'primary', SUCCESS: 'success', FAIL: 'danger', CANCELLED: 'warning' }
      return map[task && task.status] || 'info'
    },
    actionTagType(task) {
      const map = { download: 'primary', zip: 'success', pdf: 'warning' }
      return map[task && task.action] || 'info'
    },
    showChapterProgress(task) {
      return !!(task && task.chapterTotal && task.chapterIndex)
    },
    hasImageProgress(task) {
      return !!(task && Number(task.imageTotal) > 0)
    },
    /**
     * 进度条按"已下载/总图片数"等比展示，文字单独显示 已下载/总数，不显示百分比
     */
    imagePercent(task) {
      const total = Number(task && task.imageTotal) || 0
      const downloaded = Number(task && task.imageDownloaded) || 0
      if (total <= 0) {
        return 0
      }
      return Math.max(0, Math.min(100, Math.round(downloaded * 100 / total)))
    },
    elapsedMillis(task) {
      if (!task || !task.startTime) {
        return 0
      }
      if (task.endTime) {
        return task.endTime - task.startTime
      }
      return Math.max(this.nowTick - task.startTime, 0)
    },
    waitMillis(task) {
      if (!task) {
        return 0
      }
      if (task.waitMillis !== null && task.waitMillis !== undefined) {
        return task.waitMillis
      }
      return Math.max(this.nowTick - task.enqueueTime, 0)
    },
    formatDuration(millis) {
      if (millis === null || millis === undefined || millis < 0) {
        return '-'
      }
      const totalSeconds = Math.floor(millis / 1000)
      const pad = value => `${value}`.padStart(2, '0')
      const hours = Math.floor(totalSeconds / 3600)
      const minutes = Math.floor((totalSeconds % 3600) / 60)
      const seconds = totalSeconds % 60
      return hours > 0 ? `${hours}:${pad(minutes)}:${pad(seconds)}` : `${pad(minutes)}:${pad(seconds)}`
    },
    cancelTask(task) {
      this.$confirm(`确认取消排队中的任务「${this.taskDisplayName(task)} · ${task.actionName}」？`, '取消排队任务', {
        confirmButtonText: '确定',
        cancelButtonText: '关闭',
        type: 'warning'
      }).then(() => {
        this.$set(this.cancellingMap, task.taskId, true)
        cancelJmTask(task.taskId).then(({data: {code, message}}) => {
          if (code !== 200) {
            this.$message.error(message || '取消失败')
            return
          }
          this.$message.success(message || '已取消')
          this.loadTasks()
        }).catch(() => {
          this.$message.error('取消失败')
        }).finally(() => {
          this.$delete(this.cancellingMap, task.taskId)
        })
      }).catch(() => {})
    },
    cancelAllQueued() {
      this.$confirm(`确认取消全部 ${this.counters.queued} 个排队中的任务？`, '取消全部排队任务', {
        confirmButtonText: '确定',
        cancelButtonText: '关闭',
        type: 'warning'
      }).then(() => {
        cancelQueuedJmTasks().then(({data: {code, message}}) => {
          if (code !== 200) {
            this.$message.error(message || '取消失败')
            return
          }
          this.$message.success(message || '已取消')
          this.loadTasks()
        }).catch(() => {
          this.$message.error('取消失败')
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.jm-task-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0 16px 16px;
}

.jm-task-toolbar {
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: space-between;
  padding: 0 0 10px;
}

.jm-task-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.jm-task-toolbar-ops {
  align-items: center;
  display: flex;
  gap: 12px;
}

.jm-task-switch {
  margin-right: 4px;
}

.jm-task-alert {
  margin-top: 10px;
}

.jm-task-body {
  flex: 1 1 auto;
  margin-top: 12px;
  min-height: 200px;
  overflow: auto;
}

.jm-task-section + .jm-task-section {
  margin-top: 16px;
}

.jm-task-section-title {
  align-items: center;
  color: #303133;
  display: flex;
  font-size: 13px;
  font-weight: 600;
  gap: 6px;
  margin-bottom: 8px;

  &.jm-task-section-title-clickable {
    cursor: pointer;
  }
}

.jm-task-section-hint {
  color: #c0c4cc;
  font-size: 12px;
  font-weight: 400;
}

.jm-task-empty {
  background: #fafafa;
  border: 1px dashed #ebeef5;
  border-radius: 4px;
  color: #c0c4cc;
  font-size: 12px;
  line-height: 32px;
  text-align: center;
}

.jm-task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.jm-task-card {
  align-items: center;
  background: #fff;
  border: 1px solid #ebeef5;
  border-left: 3px solid #409eff;
  border-radius: 4px;
  display: flex;
  gap: 10px;
  padding: 8px 10px;

  &.jm-task-card-finished {
    border-left-color: #dcdfe6;
  }
}

.jm-task-cover {
  flex: 0 0 auto;
}

.jm-task-cover-img,
.jm-task-cover-fallback {
  border-radius: 3px;
  display: block;
  height: 62px;
  width: 46px;
}

.jm-task-cover-fallback {
  align-items: center;
  background: #f5f7fa;
  color: #909399;
  display: flex;
  font-size: 10px;
  justify-content: center;
  text-align: center;
}

.jm-task-main {
  flex: 1 1 auto;
  min-width: 0;
}

.jm-task-line {
  align-items: center;
  display: flex;
  gap: 6px;
}

.jm-task-name {
  color: #303133;
  flex: 1 1 auto;
  font-size: 13px;
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.jm-task-meta-line {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.jm-task-meta {
  color: #909399;
  font-size: 12px;
}

.jm-task-stage {
  color: #409eff;
  font-size: 12px;
}

.jm-task-progress {
  align-items: center;
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

.jm-task-chapter {
  color: #606266;
  flex: 0 0 auto;
  font-size: 12px;
}

.jm-task-bar {
  flex: 1 1 auto;
  min-width: 80px;
}

.jm-task-count {
  color: #606266;
  flex: 0 0 auto;
  font-size: 12px;
  min-width: 72px;
  text-align: right;
}

.jm-task-message {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.jm-task-ops {
  align-items: center;
  display: flex;
  flex: 0 0 auto;
}

.jm-task-lock {
  color: #c0c4cc;
  font-size: 14px;
}
</style>

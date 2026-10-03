<template>
  <!--
    下载任务抽屉。
    任务状态由父组件订阅WebSocket主题后通过 snapshot 传入（父组件同一份快照还要更新卡片上的状态），
    这里只负责展示、以及失败任务的重试。
    下载过程不统计总字节数/已下载字节数，所以这里没有进度条，只有状态。
  -->
  <el-drawer :title="drawerTitle" :visible.sync="visibleProxy" :size="drawerSize"
             :direction="drawerDirection" custom-class="bili-download-drawer">
    <div class="bili-download-panel">
      <div class="bili-download-toolbar">
        <span class="bili-download-summary">
          <el-tag size="mini" type="primary">下载中 {{counters.running}}</el-tag>
          <el-tag v-if="counters.success" size="mini" type="success">成功 {{counters.success}}</el-tag>
          <el-tag v-if="counters.fail" size="mini" type="danger">失败 {{counters.fail}}</el-tag>
        </span>
        <span class="bili-download-ops">
<!--          <el-tag size="mini" effect="plain" :type="wsConnected ? 'success' : 'info'">-->
<!--            {{wsConnected ? '实时推送' : '未连接 · 轮询兜底'}}-->
<!--          </el-tag>-->
<!--          <el-button type="text" size="mini" :loading="loading" @click="$emit('refresh')">刷新</el-button>-->
        </span>
      </div>

      <div class="bili-download-body">
        <div class="bili-download-section">
          <div class="bili-download-section-title">
            下载中
            <el-tag size="mini" type="primary">{{runningList.length}}</el-tag>
            <span class="bili-download-hint">只推送状态，不展示下载进度</span>
          </div>
          <div v-if="runningList.length === 0" class="bili-download-empty">没有正在下载的视频</div>
          <div v-else class="bili-download-list">
            <div v-for="task in runningList" :key="task.taskId" class="bili-download-card bili-download-card-running">
              <div class="bili-download-cover">
                <img v-if="coverUrl(task)" :src="coverUrl(task)" referrerpolicy="no-referrer" alt="">
                <div v-else class="bili-download-cover-fallback">{{task.bvid}}</div>
              </div>
              <div class="bili-download-main">
                <div class="bili-download-line">
                  <span class="bili-download-name" :title="task.title">{{task.title || task.bvid}}</span>
                  <el-tag size="mini" type="primary"><i class="el-icon-loading"></i> {{task.statusName}}</el-tag>
                </div>
                <div class="bili-download-meta">
                  <span>{{task.bvid}}</span>
                  <span>cid:{{task.cid}}</span>
                  <span>已运行 {{formatDuration(elapsedMillis(task))}}</span>
                </div>
                <div v-if="task.fileName" class="bili-download-file" :title="task.fileName">{{task.fileName}}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="bili-download-section">
          <div class="bili-download-section-title bili-download-section-title-clickable"
               @click="finishedCollapsed = !finishedCollapsed">
            最近完成
            <el-tag size="mini" type="success">{{counters.success}}</el-tag>
            <el-tag v-if="counters.fail" size="mini" type="danger">{{counters.fail}}</el-tag>
            <span class="bili-download-hint">{{finishedCollapsed ? '展开' : '收起'}}</span>
          </div>
          <template v-if="!finishedCollapsed">
            <div v-if="finishedList.length === 0" class="bili-download-empty">本次运行还没有完成的下载</div>
            <div v-else class="bili-download-list">
              <div v-for="task in finishedList" :key="task.taskId"
                   class="bili-download-card bili-download-card-finished">
                <div class="bili-download-cover">
                  <img v-if="coverUrl(task)" :src="coverUrl(task)" referrerpolicy="no-referrer" alt="">
                  <div v-else class="bili-download-cover-fallback">{{task.bvid}}</div>
                </div>
                <div class="bili-download-main">
                  <div class="bili-download-line">
                    <span class="bili-download-name" :title="task.title">{{task.title || task.bvid}}</span>
                    <el-tag size="mini" :type="task.status === 'success' ? 'success' : 'danger'">
                      {{task.statusName}}
                    </el-tag>
                  </div>
                  <div class="bili-download-meta">
                    <span>{{task.bvid}}</span>
                    <span>cid:{{task.cid}}</span>
                    <span>用时 {{formatDuration(task.costMillis)}}</span>
                  </div>
                  <div v-if="task.message" class="bili-download-message" :title="task.message">
                    <i class="el-icon-warning-outline"></i>
                    {{task.message}}
                  </div>
                </div>
                <div v-if="isRetryable(task)" class="bili-download-ops">
                  <el-button type="primary" size="mini" plain icon="el-icon-refresh-right"
                             :loading="isRetrying(task)" @click="retryTask(task)">重试</el-button>
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
import { download as downloadApi } from '@/api/bilibili-video';

export default {
  name: 'BiliDownloadPanel',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    snapshot: {
      type: Object,
      default: null
    },
    loading: {
      type: Boolean,
      default: false
    },
    wsConnected: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      finishedCollapsed: false,
      // 正在重试的任务，key=taskId
      retryingMap: {},
      tickTimer: null,
      nowTick: Date.now()
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
    runningList() {
      return (this.snapshot && this.snapshot.runningList) || []
    },
    finishedList() {
      return (this.snapshot && this.snapshot.finishedList) || []
    },
    counters() {
      return (this.snapshot && this.snapshot.counters) || {running: 0, success: 0, fail: 0}
    },
    drawerTitle() {
      return this.counters.running > 0 ? `下载任务（${this.counters.running}）` : '下载任务'
    },
    // 移动端改用底部抽屉并铺满宽度，桌面端保持右侧 620px 面板
    drawerDirection() {
      return this.isMobileView ? 'btt' : 'rtl'
    },
    drawerSize() {
      return this.isMobileView ? '92%' : '620px'
    }
  },
  watch: {
    visible(value) {
      // 抽屉收起后不再走表，避免无意义的定时器常驻
      if (value) {
        this.startTick()
      } else {
        this.stopTick()
      }
    }
  },
  beforeDestroy() {
    this.stopTick()
  },
  methods: {
    /**
     * "已运行"是本地走表的，不依赖推送
     */
    startTick() {
      this.stopTick()
      this.nowTick = Date.now()
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
    elapsedMillis(task) {
      if (!task || !task.startTime) {
        return task && task.costMillis ? task.costMillis : 0
      }
      if (task.endTime) {
        return task.endTime - task.startTime
      }
      return Math.max(this.nowTick - task.startTime, 0)
    },
    coverUrl(task) {
      return task.coverUrl ? String(task.coverUrl).replace(/^http:/, 'https:') : null
    },
    formatDuration(millis) {
      const total = Math.max(0, Math.floor((Number(millis) || 0) / 1000))
      const hour = Math.floor(total / 3600)
      const minute = Math.floor((total % 3600) / 60)
      const second = total % 60
      const pad = value => String(value).padStart(2, '0')
      return hour > 0 ? `${hour}:${pad(minute)}:${pad(second)}` : `${pad(minute)}:${pad(second)}`
    },
    /**
     * 失败的任务才能重试（后端会重新提交同一个视频的下载任务）
     */
    isRetryable(task) {
      return !!task && task.status === 'fail'
    },
    isRetrying(task) {
      return !!(task && this.retryingMap[task.taskId])
    },
    retryTask(task) {
      if (!this.isRetryable(task) || this.isRetrying(task)) {
        return
      }
      if (!task.videoId) {
        return this.$message.error('该任务没有关联的视频记录，无法重试')
      }
      this.$set(this.retryingMap, task.taskId, true)
      // 重试就是重新调用下载接口，失败任务会被新的进行中任务替换
      downloadApi({id: task.videoId}).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message || '重试失败')
        }
        this.$message.success(message || '已重新提交下载')
        this.$emit('refresh')
      }).catch(e => {
        this.$message.error((e && e.message) || '重试失败')
      }).finally(() => {
        this.$delete(this.retryingMap, task.taskId)
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.bili-download-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0 12px 12px;
}

.bili-download-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;

  .bili-download-summary,
  .bili-download-ops {
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.bili-download-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-top: 10px;
}

.bili-download-section {
  & + & {
    margin-top: 14px;
  }
}

.bili-download-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;

  &-clickable {
    cursor: pointer;
  }
}

.bili-download-hint {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.bili-download-empty {
  font-size: 12px;
  color: #909399;
  padding: 8px 0;
}

.bili-download-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bili-download-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 1px solid #ebeef5;
  border-left: 3px solid #dcdfe6;
  border-radius: 6px;
  background-color: #fff;
}

.bili-download-card-running {
  border-left-color: #409eff;
}

.bili-download-cover {
  flex: 0 0 72px;
  width: 72px;
  height: 45px;
  border-radius: 4px;
  overflow: hidden;
  background-color: #f5f7fa;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .bili-download-cover-fallback {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    color: #c0c4cc;
    text-align: center;
    word-break: break-all;
    padding: 2px;
  }
}

.bili-download-main {
  flex: 1;
  min-width: 0;
}

.bili-download-line {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;

  .bili-download-name {
    flex: 1;
    min-width: 0;
    font-size: 13px;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.bili-download-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;

  span {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

/* 下载中任务的文件名，单独一行，过长省略 */
.bili-download-file {
  margin-top: 4px;
  font-size: 12px;
  color: #c0c4cc;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 失败原因：整行可换行展示，便于排错 */
.bili-download-message {
  margin-top: 4px;
  font-size: 12px;
  color: #f56c6c;
  overflow-wrap: anywhere;
  word-break: break-all;

  i {
    margin-right: 2px;
  }
}

.bili-download-ops {
  display: flex;
  align-items: center;
  flex: none;
}
</style>

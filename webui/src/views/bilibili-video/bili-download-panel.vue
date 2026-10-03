<template>
  <!--
    下载任务抽屉。
    进度由父组件订阅WebSocket主题后通过 snapshot 传入（父组件同一份快照还要更新卡片上的进度条），
    这里只负责展示与手动刷新。
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
            <span class="bili-download-hint">同一个视频只会有一个下载任务</span>
          </div>
          <div v-if="runningList.length === 0" class="bili-download-empty">没有正在下载的视频</div>
          <div v-else class="bili-download-list">
            <div v-for="task in runningList" :key="task.taskId" class="bili-download-card">
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
                  <span v-if="task.fileName" :title="task.fileName">{{task.fileName}}</span>
                </div>
                <div class="bili-download-progress">
                  <el-progress class="bili-download-bar"
                               :percentage="Number(task.percent) || 0"
                               :stroke-width="6" :show-text="false" color="#409eff"></el-progress>
                  <span class="bili-download-count">
                    {{task.percent === null || task.percent === undefined ? '下载中' : task.percent + '%'}}
                    · {{formatSize(task.downloadedBytes)}}<template v-if="task.totalBytes"> / {{formatSize(task.totalBytes)}}</template>
                    <template v-if="task.speed"> · {{formatSize(task.speed)}}/s</template>
                  </span>
                </div>
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
              <div v-for="task in finishedList" :key="task.taskId" class="bili-download-card">
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
                    <span v-if="task.status === 'success'">用时 {{formatDuration(task.costMillis)}}</span>
                    <span v-if="task.status === 'success' && task.totalBytes">大小 {{formatSize(task.totalBytes)}}</span>
                  </div>
                  <div v-if="task.message" class="bili-download-message" :title="task.message">{{task.message}}</div>
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
      finishedCollapsed: false
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
  methods: {
    coverUrl(task) {
      return task.coverUrl ? String(task.coverUrl).replace(/^http:/, 'https:') : null
    },
    formatSize(bytes) {
      const size = Number(bytes) || 0
      if (size <= 0) {
        return '0B'
      }
      const units = ['B', 'KB', 'MB', 'GB']
      let index = 0
      let value = size
      while (value >= 1024 && index < units.length - 1) {
        value = value / 1024
        index += 1
      }
      return `${value.toFixed(index === 0 ? 0 : 1)}${units[index]}`
    },
    formatDuration(millis) {
      const total = Math.max(0, Math.floor((Number(millis) || 0) / 1000))
      const hour = Math.floor(total / 3600)
      const minute = Math.floor((total % 3600) / 60)
      const second = total % 60
      const pad = value => String(value).padStart(2, '0')
      return hour > 0 ? `${hour}:${pad(minute)}:${pad(second)}` : `${pad(minute)}:${pad(second)}`
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
  gap: 8px;
  padding: 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background-color: #fff;
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

.bili-download-progress {
  margin-top: 6px;

  .bili-download-count {
    display: block;
    margin-top: 2px;
    font-size: 11px;
    color: #909399;
  }
}

.bili-download-message {
  margin-top: 4px;
  font-size: 12px;
  color: #f56c6c;
  word-break: break-all;
}
</style>

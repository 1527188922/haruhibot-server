<template>
  <div id="BilibiliVideo">
    <!-- 查询条件 -->
    <basic-container>
      <el-row>
        <el-form :model="queryFormObj" label-width="70px" inline ref="queryForm" size="small"
                 @submit.native.prevent>
          <el-form-item label="bv号" prop="bvid">
            <el-input v-model.trim="queryFormObj.bvid" class="form-input" clearable maxlength="30"
                      placeholder="bv号" @keyup.enter.native="search"></el-input>
          </el-form-item>
          <el-form-item label="标题" prop="title">
            <el-input v-model.trim="queryFormObj.title" class="form-input" clearable maxlength="50"
                      @keyup.enter.native="search"></el-input>
          </el-form-item>
<!--          <el-form-item label="作者" prop="ownerName">-->
<!--            <el-input v-model.trim="queryFormObj.ownerName" class="form-input" clearable maxlength="50"-->
<!--                      @keyup.enter.native="search"></el-input>-->
<!--          </el-form-item>-->
<!--          <el-form-item label="标签" prop="tag">-->
<!--            <el-input v-model.trim="queryFormObj.tag" class="form-input" clearable maxlength="30"-->
<!--                      @keyup.enter.native="search"></el-input>-->
<!--          </el-form-item>-->
        </el-form>
      </el-row>
      <el-row class="query-form-option-buts">
        <el-button type="primary" size="small" plain icon="el-icon-search" @click="search">查询</el-button>
        <el-button type="primary" size="small" plain icon="el-icon-refresh-right" @click="resetQueryForm">重置</el-button>
      </el-row>
    </basic-container>

    <!-- 添加视频 + 工具栏 -->
    <basic-container>
      <div class="bili-toolbar">
        <div class="bili-add">
          <el-input v-model.trim="addText" class="bili-add-input" size="small" clearable
                    placeholder="粘贴b站视频链接 / bv号 / av号，回车添加"
                    @keyup.enter.native="addVideo"></el-input>
          <el-button type="primary" size="small" plain icon="el-icon-plus"
                     :loading="adding" @click="addVideo">添加视频</el-button>
        </div>
        <div class="bili-view-mode">
          <el-badge :value="runningTaskCount" :hidden="runningTaskCount === 0" type="warning"
                    class="bili-task-badge">
            <el-button size="small" plain icon="el-icon-s-operation"
                       @click="openDownloadPanel">下载任务</el-button>
          </el-badge>
          <el-button type="danger" size="small" plain icon="el-icon-delete"
                     :disabled="selectedIds.length === 0"
                     @click="deleteSelected">删除选中{{selectedIds.length ? `(${selectedIds.length})` : ''}}</el-button>
          <el-button size="small" plain icon="el-icon-refresh" :loading="loading" @click="search">刷新</el-button>
        </div>
      </div>
      <div class="bili-summary">
        共 {{allTotal}} 个视频，已加载 {{list.length}} 个
        <template v-if="activeGroup.key !== ALL_KEY">，当前分组：{{activeGroupName}}（{{total}} 个）</template>
      </div>
    </basic-container>

    <!-- 左侧分组列表 + 右侧视频卡片 -->
    <basic-container>
      <div class="bili-layout">
        <div class="bili-side">
          <bili-group-panel :mode.sync="groupMode" :authors="authors" :tags="tags" :total="allTotal"
                            :active-key="activeGroup.key" :loading="groupLoading"
                            @select="selectGroup" @refresh="loadGroups"></bili-group-panel>
        </div>
        <div class="bili-main">
          <div v-loading="loading && list.length === 0" class="bili-body">
            <div v-if="list.length > 0" class="bili-grid">
              <bili-video-card v-for="row in list" :key="row.id" :row="row" :selected="isSelected(row.id)"
                               @select="toggleSelect" @open-video="openVideo" @play="openPlayer"
                               @download="downloadVideo" @refresh="refreshVideo"
                               @delete="deleteVideo"></bili-video-card>
            </div>
            <el-empty v-else-if="!loading" description="该分组下暂无视频，可粘贴b站链接添加"></el-empty>
            <div ref="loadMoreTrigger" class="bili-load-more">
              <span v-if="loading && list.length > 0"><i class="el-icon-loading"></i> 加载中...</span>
              <span v-else-if="hasMore && list.length > 0">向下滑动加载更多</span>
              <span v-else-if="list.length > 0">没有更多了</span>
            </div>
          </div>
        </div>
      </div>
    </basic-container>

    <!-- 服务器本地视频播放，不新开浏览器tab -->
    <el-dialog :title="player.title || '视频播放'" :visible.sync="playerVisible" width="70%" top="6vh"
               append-to-body custom-class="bili-player-dialog" @closed="closePlayer">
      <video v-if="player.src" class="bili-player" :src="player.src" controls autoplay
             controlslist="nodownload"></video>
      <div class="bili-player-meta">
        <span>{{player.fileName}}</span>
        <span v-if="player.bvid" class="bili-player-meta-link"
              @click="openVideo(player)">{{player.bvid}}</span>
      </div>
    </el-dialog>

    <bili-download-panel :visible.sync="downloadPanelVisible" :snapshot="downloadSnapshot"
                         :loading="downloadLoading" :ws-connected="wsConnected"
                         @refresh="refreshDownloadTasks"></bili-download-panel>
  </div>
</template>

<script>
import {
  search as searchApi,
  authors as authorsApi,
  tags as tagsApi,
  add as addApi,
  refresh as refreshApi,
  download as downloadApi,
  downloadTasks as downloadTasksApi,
  deleteBatch
} from '@/api/bilibili-video';
import BiliGroupPanel from './bili-group-panel.vue';
import BiliVideoCard from './bili-video-card.vue';
import BiliDownloadPanel from './bili-download-panel.vue';

const PAGE_SIZE = 20;
// 左侧"全部视频"这一项的分组key，与 bili-group-panel 内部保持一致
const ALL_KEY = 'all';
// 下载任务主题与命令，与后端 BilibiliVideoDownloadPushService 保持一致
const DOWNLOAD_TOPIC = 'bilibili.video.download';
const DOWNLOAD_SNAPSHOT_TYPE = 'bilibili.video.download.snapshot';
const DOWNLOAD_LIST_COMMAND = 'bilibili.video.download.list';
// WebSocket未连通时的轮询兜底间隔
const FALLBACK_POLL_MILLIS = 3000;

export default {
  name: 'BilibiliVideo',
  components: {
    BiliGroupPanel,
    BiliVideoCard,
    BiliDownloadPanel
  },
  data() {
    return {
      ALL_KEY,
      queryFormObj: {
        bvid: '',
        title: '',
        ownerName: '',
        tag: ''
      },
      addText: '',
      adding: false,
      // 左侧分组的浏览方式：owner-按作者 / tag-按标签
      groupMode: 'owner',
      // 当前选中的分组，key=all 表示全部视频
      activeGroup: {key: ALL_KEY},
      authors: [],
      tags: [],
      groupLoading: false,
      list: [],
      // 当前查询条件下的总数（选中分组时是该分组的数量）
      total: 0,
      // 全部视频总数，左侧"全部视频"用
      allTotal: 0,
      currentPage: 1,
      hasMore: false,
      loading: false,
      selectedIds: [],
      player: {title: '', src: null, fileName: '', bvid: ''},
      playerVisible: false,
      downloadPanelVisible: false,
      downloadSnapshot: null,
      downloadLoading: false,
      wsConnected: false,
      observer: null,
      scrollParent: null,
      scrollHandler: null,
      fallbackTimer: null,
      snapshotOff: null,
      statusOff: null,
      pushBound: false
    }
  },
  computed: {
    activeGroupKey() {
      return this.activeGroup.key
    },
    activeGroupName() {
      if (this.activeGroup.key === ALL_KEY) {
        return '全部视频'
      }
      return this.activeGroup.ownerName || this.activeGroup.tag || ''
    },
    runningTaskCount() {
      return (this.downloadSnapshot && this.downloadSnapshot.counters
        && this.downloadSnapshot.counters.running) || 0
    }
  },
  watch: {
    groupMode() {
      // 切换浏览方式后回到"全部视频"，避免留下上一种分组方式的过滤条件
      this.activeGroup = {key: ALL_KEY}
      this.search()
    },
    // 列表数据变化后重新挂载哨兵，保证首屏不足一屏时也能继续加载
    'list.length'() {
      this.$nextTick(() => this.setupObserver())
    }
  },
  mounted() {
    this.loadGroups()
    this.search()
    this.$nextTick(() => this.setupObserver())
    this.bindDownloadPush()
  },
  activated() {
    this.$nextTick(() => this.setupObserver())
  },
  deactivated() {
    this.destroyObserver()
  },
  beforeDestroy() {
    this.destroyObserver()
    this.unbindDownloadPush()
    this.clearFallbackTimer()
  },
  methods: {
    /* ==================== 分组列表 ==================== */
    loadGroups() {
      this.groupLoading = true
      Promise.all([authorsApi(), tagsApi()]).then(([authorResp, tagResp]) => {
        if (authorResp.data.code === 200) {
          this.authors = authorResp.data.data || []
        }
        if (tagResp.data.code === 200) {
          this.tags = tagResp.data.data || []
        }
      }).catch(e => {
        this.$message.error(e.message)
      }).finally(() => {
        this.groupLoading = false
      })
    },
    selectGroup(group) {
      this.activeGroup = group
      this.search()
    },

    /* ==================== 查询 ==================== */
    search() {
      this.currentPage = 1
      this.hasMore = true
      this.selectedIds = []
      this.list = []
      this.loadPage()
    },
    resetQueryForm() {
      this.$refs.queryForm.resetFields()
      this.activeGroup = {key: ALL_KEY}
      this.search()
    },
    buildQuery() {
      const query = Object.assign({}, this.queryFormObj)
      if (this.activeGroup.key !== ALL_KEY) {
        if (this.activeGroup.ownerMid) {
          query.ownerMid = this.activeGroup.ownerMid
        }
        if (this.activeGroup.tag) {
          // 精确匹配左侧选中的标签，"财经"不能命中"财经商业"
          query.tagExact = this.activeGroup.tag
        }
      }
      return query
    },
    loadPage() {
      if (this.loading || !this.hasMore) {
        return
      }
      this.loading = true
      searchApi(Object.assign(this.buildQuery(), {
        currentPage: this.currentPage,
        pageSize: PAGE_SIZE
      })).then(({data: {code, message, data}}) => {
        if (code !== 200) {
          this.$message.error(message)
          this.hasMore = false
          return
        }
        const records = (data && data.records) || []
        this.total = Number((data && data.total) || 0)
        if (this.activeGroup.key === ALL_KEY) {
          this.allTotal = this.total
        }
        const merged = this.list.concat(records.map(row => Object.assign({}, row)))
        this.list = merged
        this.currentPage += 1
        this.hasMore = records.length > 0 && merged.length < this.total
        // 新加载的数据可能带着进行中的下载
        this.syncFallbackPoll()
      }).catch(e => {
        this.$message.error(e.message)
        this.hasMore = false
      }).finally(() => {
        this.loading = false
      })
    },

    /* ==================== 添加 ==================== */
    addVideo() {
      if (!this.addText) {
        return this.$message.warning('请输入b站视频链接或bv号')
      }
      this.adding = true
      addApi({text: this.addText}).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.addText = ''
        this.$message.success(message || '添加成功')
        this.loadGroups()
        this.search()
      }).catch(e => {
        this.$message.error(e.message)
      }).finally(() => {
        this.adding = false
      })
    },

    /* ==================== 下载 ==================== */
    downloadVideo(row) {
      this.$confirm(`确认下载视频【${row.title || row.bvid}】到服务器本地？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return downloadApi({id: row.id})
      }).then(({data: {code, message, data}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$message.success(message || '已开始下载')
        this.$set(row, 'downloading', true)
        this.$set(row, 'downloadState', 'running')
        this.$set(row, 'downloadMessage', null)
        this.$set(row, 'downloadPercent', 0)
        if (data) {
          this.applyTasks([data])
        }
        this.refreshDownloadTasks()
      }).catch(e => {
        if (e !== 'cancel' && e && e.message) {
          this.$message.error(e.message)
        }
      })
    },
    openDownloadPanel() {
      this.downloadPanelVisible = true
      this.refreshDownloadTasks()
    },
    /**
     * 拉取一次下载任务快照：WebSocket连通时走总线命令，否则回退HTTP
     */
    refreshDownloadTasks() {
      this.downloadLoading = true
      if (this.$ws.isOpen()) {
        this.$ws.send(DOWNLOAD_LIST_COMMAND).then(message => {
          if (message && message.data) {
            this.applyDownloadSnapshot(message.data)
          }
        }).catch(() => {
          // 忽略：推送会补上最新状态
        }).finally(() => {
          this.downloadLoading = false
        })
        return
      }
      downloadTasksApi().then(({data: {code, data}}) => {
        if (code === 200 && data) {
          this.applyDownloadSnapshot(data)
        }
      }).catch(() => {
        // 忽略：轮询失败不刷屏
      }).finally(() => {
        this.downloadLoading = false
      })
    },
    bindDownloadPush() {
      if (this.pushBound) {
        return
      }
      this.pushBound = true
      this.snapshotOff = this.$ws.on(DOWNLOAD_SNAPSHOT_TYPE, this.applyDownloadSnapshot)
      this.statusOff = this.$ws.onStatus(this.handleWsStatus)
      this.$ws.subscribe(DOWNLOAD_TOPIC)
    },
    unbindDownloadPush() {
      if (!this.pushBound) {
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
      this.$ws.unsubscribe(DOWNLOAD_TOPIC)
    },
    handleWsStatus(status) {
      this.wsConnected = status === 'open'
      this.syncFallbackPoll()
    },
    /**
     * WebSocket未连通且存在进行中的下载时，用HTTP轮询兜底
     */
    syncFallbackPoll() {
      this.clearFallbackTimer()
      if (this.wsConnected || this.runningTaskCount === 0 || document.hidden) {
        return
      }
      this.fallbackTimer = setInterval(() => this.refreshDownloadTasks(), FALLBACK_POLL_MILLIS)
    },
    clearFallbackTimer() {
      if (this.fallbackTimer) {
        clearInterval(this.fallbackTimer)
        this.fallbackTimer = null
      }
    },
    applyDownloadSnapshot(snapshot) {
      if (!snapshot) {
        return
      }
      this.downloadSnapshot = snapshot
      this.applyTasks([].concat(snapshot.runningList || [], snapshot.finishedList || []))
      this.syncFallbackPoll()
    },
    /**
     * 把下载任务进度同步到卡片上（按 bvid+cid 匹配）
     */
    applyTasks(tasks) {
      if (!tasks || tasks.length === 0) {
        return
      }
      const taskMap = {}
      tasks.forEach(task => {
        if (task && task.bvid && task.cid !== undefined && task.cid !== null) {
          taskMap[`${task.bvid}_${task.cid}`] = task
        }
      })
      this.list.forEach(row => {
        const task = taskMap[`${row.bvid}_${row.cid}`]
        if (!task) {
          return
        }
        this.$set(row, 'downloading', task.status === 'running')
        this.$set(row, 'downloadState', task.status)
        this.$set(row, 'downloadMessage', task.message)
        this.$set(row, 'downloadPercent', task.percent)
        this.$set(row, 'downloadSpeed', task.speed)
        this.$set(row, 'downloadedBytes', task.downloadedBytes)
        if (task.status === 'success') {
          this.$set(row, 'downloaded', true)
        }
      })
    },

    /* ==================== 播放 ==================== */
    openPlayer(row) {
      this.player = {
        title: row.title || row.bvid,
        src: row.videoPath || `/video/bilibili/${row.videoFileName}`,
        fileName: row.videoFileName,
        bvid: row.bvid
      }
      this.playerVisible = true
    },
    closePlayer() {
      // 清掉src，关掉弹窗时停止播放
      this.player = {title: '', src: null, fileName: '', bvid: ''}
    },
    openVideo(row) {
      window.open(`https://www.bilibili.com/video/${row.bvid}`, '_blank')
    },

    /* ==================== 刷新 / 删除 ==================== */
    refreshVideo(row) {
      refreshApi({id: row.id}).then(({data: {code, message, data}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        const index = this.list.findIndex(item => item.id === row.id)
        if (index >= 0 && data) {
          this.$set(this.list, index, Object.assign({}, data))
        }
        this.$message.success(message || '刷新成功')
      }).catch(e => {
        this.$message.error(e.message)
      })
    },
    deleteVideo(row) {
      this.deleteRows([row.id], `确认删除视频【${row.title || row.bvid}】的记录？本地已下载的视频文件不会被删除`)
    },
    deleteSelected() {
      if (this.selectedIds.length === 0) {
        return
      }
      this.deleteRows(this.selectedIds, `确认删除选中的 ${this.selectedIds.length} 条视频记录？本地已下载的视频文件不会被删除`)
    },
    deleteRows(ids, text) {
      this.$confirm(text, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return deleteBatch({ids})
      }).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$message.success(message || '删除完成')
        this.loadGroups()
        this.search()
      }).catch(e => {
        if (e !== 'cancel' && e && e.message) {
          this.$message.error(e.message)
        }
      })
    },

    /* ==================== 勾选 ==================== */
    isSelected(id) {
      return this.selectedIds.indexOf(id) >= 0
    },
    toggleSelect(row) {
      const index = this.selectedIds.indexOf(row.id)
      if (index >= 0) {
        this.selectedIds.splice(index, 1)
      } else {
        this.selectedIds.push(row.id)
      }
    },

    /* ==================== 下滑翻页 ==================== */
    setupObserver() {
      this.destroyObserver()
      const trigger = this.$refs.loadMoreTrigger
      if (!trigger) {
        return
      }
      const scrollParent = this.getScrollParent(this.$el)
      if (!window.IntersectionObserver) {
        // 兜底：不支持IntersectionObserver时监听滚动
        this.scrollParent = scrollParent
        this.scrollHandler = () => {
          const el = scrollParent === window ? document.documentElement : scrollParent
          if (el.scrollTop + el.clientHeight >= el.scrollHeight - 200) {
            this.loadPage()
          }
        }
        scrollParent.addEventListener('scroll', this.scrollHandler, {passive: true})
        return
      }
      this.observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            this.loadPage()
          }
        })
      }, {
        root: scrollParent === window ? null : scrollParent,
        rootMargin: '200px 0px'
      })
      this.observer.observe(trigger)
    },
    destroyObserver() {
      if (this.observer) {
        this.observer.disconnect()
        this.observer = null
      }
      if (this.scrollHandler && this.scrollParent) {
        this.scrollParent.removeEventListener('scroll', this.scrollHandler)
        this.scrollHandler = null
        this.scrollParent = null
      }
    },
    /**
     * 页面真正的滚动容器是 .avue-view，向上找到第一个可滚动的祖先
     */
    getScrollParent(el) {
      let parent = el && el.parentElement
      while (parent) {
        const style = window.getComputedStyle(parent)
        if (/(auto|scroll|overlay)/.test(style.overflowY) && parent.scrollHeight > parent.clientHeight) {
          return parent
        }
        parent = parent.parentElement
      }
      return window
    }
  }
}
</script>

<style lang="scss" scoped>
#BilibiliVideo {
  .bili-toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
  }

  .bili-add {
    display: flex;
    align-items: center;
    gap: 8px;

    .bili-add-input {
      width: 360px;
      max-width: 100%;
    }
  }

  .bili-view-mode {
    display: flex;
    align-items: center;
    gap: 8px;

    .el-button + .el-button {
      margin-left: 0;
    }
  }

  .bili-task-badge {
    ::v-deep .el-badge__content {
      top: 12px;
    }
  }

  .bili-summary {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
  }

  /* 左侧分组 + 右侧卡片 */
  .bili-layout {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    min-width: 0;
  }

  .bili-side {
    flex: 0 0 190px;
    width: 190px;
    min-width: 0;
    border-right: 1px solid #ebeef5;
    padding-right: 12px;
  }

  .bili-main {
    flex: 1 1 auto;
    min-width: 0;
  }

  .bili-body {
    min-height: 120px;
  }

  .bili-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
    gap: 12px;
  }

  .bili-load-more {
    text-align: center;
    padding: 12px 0;
    font-size: 12px;
    color: #909399;
  }

  .bili-player {
    width: 100%;
    max-height: 70vh;
    background-color: #000;
    display: block;
  }

  .bili-player-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 8px;
    font-size: 12px;
    color: #909399;

    .bili-player-meta-link {
      color: #409eff;
      cursor: pointer;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  /* 移动端：左侧分组收成顶部横向条 */
  @media screen and (max-width: 768px) {
    .bili-layout {
      display: block;
    }

    .bili-side {
      flex: none;
      width: 100%;
      border-right: none;
      padding-right: 0;
      margin-bottom: 8px;
    }
  }
}
</style>

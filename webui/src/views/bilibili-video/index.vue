<template>
  <div id="BilibiliVideo">
    <!-- 查询条件 -->
    <basic-container>
      <el-row>
        <query-form :model="queryFormObj" inline ref="queryForm" size="small"
                 @submit.native.prevent>
          <el-form-item prop="bvid">
            <el-input v-model.trim="queryFormObj.bvid" class="form-input" clearable maxlength="30"
                      placeholder="bv号" @keyup.enter.native="search"></el-input>
          </el-form-item>
          <el-form-item prop="title">
            <el-input placeholder="标题" v-model.trim="queryFormObj.title" class="form-input" clearable maxlength="50"
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
        </query-form>
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
<!--          <el-button size="small" plain icon="el-icon-refresh" :loading="loading" @click="search">刷新</el-button>-->
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
                               @select="toggleSelect" @play="openPlayer"
                               @download="downloadVideo" @retry="retryDownload" @refresh="refreshVideo"
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

    <!-- 服务器本地视频播放，不新开浏览器tab；窄屏/手机端直接满屏，让视频尽量宽 -->
    <el-dialog :title="player.title || '视频播放'" :visible.sync="playerVisible" width="860px" top="6vh"
               :fullscreen="isMobileView"
               append-to-body custom-class="bili-player-dialog"
               :dialog-drag-enabled="!isMobileView" :close-on-click-modal="false"
               @closed="closePlayer">
      <div class="bili-player-stage">
        <video v-if="player.src && !playerError" class="bili-player" :src="player.src"
               controls autoplay playsinline preload="metadata" controlslist="nodownload"
               @error="playerError = true"></video>
        <div v-else class="bili-player-empty">
          <i class="el-icon-warning-outline"></i>
          <span>{{player.src ? '视频加载失败，服务器本地文件可能已被删除' : '没有可播放的视频'}}</span>
        </div>
      </div>
      <div class="bili-player-meta">
        <div class="bili-player-file" :title="player.fileName">
          <i class="el-icon-document"></i>
          <span class="bili-player-file-name">{{player.fileName || '-'}}</span>
        </div>
        <div class="bili-player-ops">
          <el-button v-if="player.bvid" type="text" size="mini" icon="el-icon-link"
                     @click="openVideo(player)">在B站打开</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 删除：两个勾选项互相独立，勾了哪个删哪个 -->
    <el-dialog :title="deleteDialogTitle" :visible.sync="deleteDialogVisible" width="420px"
               @closed="deleteDialogClosed">
      <div class="bili-delete-tip">{{deleteTip}}</div>
      <div class="bili-delete-options">
        <el-checkbox v-model="deleteOptions.deleteData">删除数据库记录</el-checkbox>
        <el-checkbox v-model="deleteOptions.deleteFile">删除视频文件</el-checkbox>
      </div>
<!--      <div class="bili-delete-hint">-->
<!--        两个勾选项互相独立；只删记录时本地视频文件会保留，视频文件删除后无法恢复。-->
<!--      </div>-->
      <span slot="footer">
        <el-button size="small" @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" size="small" :loading="deleteLoading" :disabled="deleteDisabled"
                   @click="submitDelete">确定</el-button>
      </span>
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
      // 删除弹框：待删除的行 + 两个勾选项
      deleteDialogVisible: false,
      deleteRows: [],
      deleteOptions: {deleteData: true, deleteFile: false},
      deleteLoading: false,
      player: {title: '', src: null, fileName: '', bvid: ''},
      playerVisible: false,
      // 视频标签加载失败（本地文件已被删除等）
      playerError: false,
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
    },
    deleteDialogTitle() {
      return this.deleteRows.length > 1 ? '删除选中视频' : '删除视频'
    },
    deleteTip() {
      if (this.deleteRows.length === 1) {
        const row = this.deleteRows[0]
        return `确认删除视频【${row.title || row.bvid}】？`
      }
      return `确认删除选中的 ${this.deleteRows.length} 个视频？`
    },
    // 一项都没勾时不允许提交
    deleteDisabled() {
      return !this.deleteOptions.deleteData && !this.deleteOptions.deleteFile
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
        this.submitDownload(row)
      }).catch(() => {
      })
    },
    /**
     * 下载失败后的重试：不再二次确认，直接重新提交同一个视频的下载任务
     */
    retryDownload(row) {
      this.submitDownload(row)
    },
    /**
     * 提交下载任务。下载过程不展示进度，只按WebSocket推送的状态更新卡片
     */
    submitDownload(row) {
      downloadApi({id: row.id}).then(({data: {code, message, data}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$message.success(message || '已开始下载')
        this.$set(row, 'downloading', true)
        this.$set(row, 'downloadState', 'running')
        this.$set(row, 'downloadMessage', null)
        if (data) {
          this.applyTasks([data])
        }
        this.refreshDownloadTasks()
      }).catch(e => {
        if (e && e.message) {
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
     * 把下载任务状态同步到卡片上（按 bvid+cid 匹配）
     * <p>
     * 注意：任务列表是后端内存里的历史记录（已完成任务最多留20条），它并不知道文件后来
     * 有没有被删掉。所以只有"这一行这次确实在下载中、任务刚刚结束"时才用任务状态去改
     * 卡片的已下载标记；其余情况一律以列表接口返回的 downloaded（磁盘真实情况）为准——
     * 否则删过文件的视频会被几十秒前那条历史成功任务重新标成"已下载"。
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
        // 覆盖状态之前先记下：这一行是不是正处在"这次下载"中
        // （downloading 是前端自己标的；downloadState=running 来自接口，说明后端确实有在跑的任务）
        const justFinished = (!!row.downloading || row.downloadState === 'running') && task.status !== 'running'
        this.$set(row, 'downloading', task.status === 'running')
        this.$set(row, 'downloadState', task.status)
        this.$set(row, 'downloadMessage', task.message)
        if (justFinished) {
          // 失败时临时文件已经被删掉，成功时文件刚落盘
          this.$set(row, 'downloaded', task.status === 'success')
        }
      })
    },

    /* ==================== 播放 ==================== */
    openPlayer(row) {
      this.playerError = false
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
      this.playerError = false
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
      this.openDeleteDialog([row])
    },
    deleteSelected() {
      if (this.selectedIds.length === 0) {
        return
      }
      // 勾选状态会被 search() 清掉，这里先把要删的行取出来
      this.openDeleteDialog(this.list.filter(row => this.selectedIds.indexOf(row.id) >= 0))
    },
    /**
     * 打开删除弹框：删哪些内容由弹框里的两个勾选项决定
     */
    openDeleteDialog(rows) {
      if (!rows || rows.length === 0) {
        return
      }
      this.deleteRows = rows
      this.deleteDialogVisible = true
    },
    /**
     * 弹框关闭后复位，避免上次的勾选带到下一次
     */
    deleteDialogClosed() {
      this.deleteRows = []
      this.deleteOptions = {deleteData: true, deleteFile: false}
    },
    submitDelete() {
      if (this.deleteDisabled) {
        return this.$message.warning('请至少勾选一项要删除的内容')
      }
      this.deleteLoading = true
      deleteBatch({
        ids: this.deleteRows.map(row => row.id),
        deleteData: this.deleteOptions.deleteData,
        deleteFile: this.deleteOptions.deleteFile
      }).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.deleteDialogVisible = false
        this.$message.success(message || '删除完成')
        this.loadGroups()
        this.search()
      }).catch(e => {
        this.$message.error((e && e.message) || '删除失败')
      }).finally(() => {
        this.deleteLoading = false
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
    flex-wrap: wrap;
    gap: 8px;
    min-width: 0;

    .bili-add-input {
      width: 360px;
      max-width: 100%;
    }
  }

  .bili-view-mode {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;

    .el-button + .el-button {
      margin-left: 0;
    }
  }

  .bili-task-badge {
    // 不要覆盖 element-ui 的 .el-badge__content 定位(top:0 + translateY(-50%))，
    // 那样角标会落到按钮右上角的下方；默认就是压在按钮右上角上的
    margin-right: 8px;
  }

  .bili-summary {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
  }

  /* 删除弹框 */
  .bili-delete-tip {
    font-size: 13px;
    line-height: 20px;
    color: #303133;
    word-break: break-all;
  }

  .bili-delete-options {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    margin-top: 12px;

    .el-checkbox {
      margin-right: 0;
    }
  }

  //.bili-delete-hint {
  //  margin-top: 10px;
  //  font-size: 12px;
  //  line-height: 18px;
  //  color: #909399;
  //}

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

  /* 移动端：左侧分组收成顶部横向条 */
  @media screen and (max-width: 767.98px) {
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

    /*
      添加视频：输入框写死了 360px，加上按钮在手机上会顶出屏幕，
      这里改成上下两行、各自占满一行，工具栏与操作按钮也允许换行。
    */
    .bili-toolbar {
      flex-direction: column;
      align-items: stretch;
    }

    .bili-add {
      align-items: stretch;

      .bili-add-input {
        flex: 1 1 100%;
        width: 100%;
      }

      .el-button {
        flex: 1 1 100%;
        width: 100%;
      }
    }

    .bili-view-mode {
      justify-content: flex-start;
    }
  }
}
</style>

<!--
  视频播放弹窗的样式。

  弹窗带 append-to-body，运行时会被挪到 body 下，既不再是 #BilibiliVideo 的后代，
  而 custom-class(bili-player-dialog) 挂在 el-dialog 上、拿不到组件的 scoped 属性，
  所以这一块必须用非 scoped 的全局样式，否则规则会静默失效
  （表现为视频按原始尺寸撑满弹窗、四周一圈默认留白，非常难看）。
-->
<style lang="scss">
.bili-player-dialog.el-dialog {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, .24);

  .el-dialog__header {
    padding: 12px 44px 12px 16px;
    border-bottom: 1px solid #ebeef5;
  }

  .el-dialog__title {
    font-size: 14px;
    font-weight: 600;
    line-height: 20px;
    color: #303133;
    // 标题可能很长，最多占两行
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    overflow-wrap: anywhere;
  }

  .el-dialog__headerbtn {
    top: 12px;
    right: 12px;
    width: 24px;
    height: 24px;
    line-height: 24px;
  }

  // 视频区自己撑满，不要 element-ui 默认的 30px 20px 内边距
  .el-dialog__body {
    padding: 0;
  }
}

.bili-player-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #000;
  min-height: 120px;
}

.bili-player {
  width: 100%;
  max-height: 68vh;
  display: block;
  background-color: #000;
  outline: none;
}

.bili-player-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 220px;
  background-color: #1f1f1f;
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 0 16px;
  box-sizing: border-box;

  i {
    font-size: 26px;
    color: #606266;
  }
}

.bili-player-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 14px;
  border-top: 1px solid #ebeef5;
  background-color: #fafafa;
  font-size: 12px;
  color: #909399;
}

.bili-player-file {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1 1 auto;
  min-width: 0;

  .bili-player-file-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.bili-player-ops {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  white-space: nowrap;

  .el-button {
    margin: 0;
    padding: 0 4px;
    white-space: nowrap;
  }
}

/*
  满屏形态：窄屏/手机端（isMobileView 打开 el-dialog 的 fullscreen）用整个视口播放，
  视频在剩余空间里居中铺满，底部信息条固定在下面。

  这里用 position:fixed 铺满视口，而不是只靠 element-ui 的 .is-fullscreen（height:100%）：
  el-dialog__wrapper 在移动端带 20px 的 padding-bottom（见 styles/media.scss），
  只写 100% 的话底部会露出一条缝。宽度也要 !important，否则会被 media.scss 的
  "窄屏弹窗 98%/92vw" 规则压回去。
*/
.bili-player-dialog.el-dialog.is-fullscreen {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  width: 100% !important;
  max-width: 100% !important;
  height: 100% !important;
  margin: 0 !important;
  border-radius: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .el-dialog__header {
    flex: 0 0 auto;
  }

  // 视频区自己撑满，不要 element-ui 默认的 30px 20px 内边距和限高
  .el-dialog__body {
    display: flex;
    flex-direction: column;
    flex: 1 1 auto;
    min-height: 0;
    max-height: none;
    overflow: hidden;
    padding: 0;
  }

  .bili-player-stage {
    flex: 1 1 auto;
    min-height: 0;
  }

  // 视频元素铺满舞台，画面比例由浏览器自己加黑边保持
  .bili-player {
    width: 100%;
    height: 100%;
    max-height: none;
  }

  .bili-player-empty {
    height: auto;
    flex: 1 1 auto;
  }

  .bili-player-meta {
    flex: 0 0 auto;
  }
}

/*
  窄屏/手机：标题压到一行，把纵向空间留给视频；
  文件名与"在B站打开"仍排一行，文件名过长就省略，按钮不换行。
*/
@media screen and (max-width: 767.98px) {
  .bili-player-dialog.el-dialog {
    .el-dialog__header {
      padding: 10px 40px 10px 12px;
    }

    .el-dialog__title {
      font-size: 13px;
      -webkit-line-clamp: 1;
    }

    .el-dialog__headerbtn {
      top: 10px;
      right: 8px;
    }
  }

  .bili-player-meta {
    gap: 8px;
    padding: 8px 12px;
  }
}
</style>

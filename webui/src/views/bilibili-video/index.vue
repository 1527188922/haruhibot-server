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
          <el-form-item label="作者" prop="ownerName">
            <el-input v-model.trim="queryFormObj.ownerName" class="form-input" clearable maxlength="50"
                      @keyup.enter.native="search"></el-input>
          </el-form-item>
          <el-form-item label="标签" prop="tag">
            <el-input v-model.trim="queryFormObj.tag" class="form-input" clearable maxlength="30"
                      @keyup.enter.native="search"></el-input>
          </el-form-item>
        </el-form>
      </el-row>
      <el-row class="query-form-option-buts">
        <el-button type="primary" size="small" plain icon="el-icon-search" @click="search">查询</el-button>
        <el-button type="primary" size="small" plain icon="el-icon-refresh-right" @click="resetQueryForm">重置</el-button>
      </el-row>
    </basic-container>

    <!-- 添加视频 + 展示方式 -->
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
          <span class="bili-mode-label">分组方式：</span>
          <el-radio-group v-model="groupBy" size="mini">
            <el-radio-button label="none">不分组</el-radio-button>
            <el-radio-button label="owner">按作者</el-radio-button>
            <el-radio-button label="tag">按标签</el-radio-button>
          </el-radio-group>
          <el-button type="danger" size="mini" plain icon="el-icon-delete"
                     :disabled="selectedIds.length === 0"
                     @click="deleteSelected">删除选中{{selectedIds.length ? `(${selectedIds.length})` : ''}}</el-button>
          <el-button size="mini" plain icon="el-icon-refresh" :loading="loading" @click="search">刷新</el-button>
        </div>
      </div>
      <div class="bili-summary">
        共 {{total}} 个视频，已加载 {{list.length}} 个<span v-if="groupBy !== 'none'">，共 {{groupedList.length}} 组</span>
      </div>
    </basic-container>

    <!-- 视频卡片 -->
    <basic-container>
      <div v-loading="loading && list.length === 0" class="bili-body">
        <template v-if="groupedList.length > 0">
          <div v-for="group in groupedList" :key="group.key" class="bili-group">
            <div v-if="groupBy !== 'none'" class="bili-group-header">
              <img v-if="group.face" class="bili-group-avatar" :src="group.face" referrerpolicy="no-referrer">
              <i v-else-if="groupBy === 'owner'" class="el-icon-user-solid bili-group-avatar bili-group-avatar-icon"></i>
              <i v-else class="el-icon-price-tag bili-group-avatar bili-group-avatar-icon"></i>
              <span class="bili-group-title" :title="group.title">{{group.title}}</span>
              <span class="bili-group-count">{{group.cards.length}}</span>
              <el-button v-if="groupBy === 'owner' && group.mid" type="text" size="mini"
                         @click="openSpaceUrl(group.mid)">b站主页</el-button>
            </div>
            <div class="bili-grid">
              <div v-for="row in group.cards" :key="`${group.key}-${row.id}`" class="bili-card"
                   :class="{'bili-card-downloaded': row.downloaded}">
                <div class="bili-cover" @click="openVideo(row)">
                  <img v-if="coverUrl(row)" :src="coverUrl(row)" referrerpolicy="no-referrer" alt="">
                  <div v-else class="bili-cover-empty"><i class="el-icon-picture-outline"></i></div>
                  <span v-if="row.duration" class="bili-cover-duration">{{durationText(row.duration)}}</span>
                  <span class="bili-cover-flag" :class="row.downloaded ? 'is-downloaded' : 'is-undownloaded'">
                    {{row.downloaded ? '已下载' : (row.downloading ? '下载中' : '未下载')}}
                  </span>
                </div>
                <div class="bili-card-body">
                  <div class="bili-title" :title="row.title">{{row.title}}</div>
                  <div class="bili-meta-line">
                    <a class="bili-bvid"  target="_blank" rel="noopener noreferrer"
                       @click.stop>{{row.bvid}}</a>
<!--                    <span class="bili-cid" :title="`cid：${row.cid}，av号：${row.avid || ''}`">cid:{{row.cid}}</span>-->
                    <el-checkbox class="bili-card-select" :value="isSelected(row.id)"
                                 @change="toggleSelect(row.id)"></el-checkbox>
                  </div>
                  <div class="bili-owner" :title="`点击进入b站个人主页(uid: ${row.ownerMid})`"
                       @click="openSpace(row)">
                    <img v-if="row.ownerFace" class="bili-owner-face" :src="row.ownerFace"
                         referrerpolicy="no-referrer" alt="">
                    <i v-else class="el-icon-user-solid bili-owner-face bili-owner-face-icon"></i>
                    <span class="bili-owner-name">{{row.ownerName || '未知作者'}}</span>
                  </div>
                  <div class="bili-tags">
                    <el-tag v-for="tag in visibleTags(row)" :key="tag" size="mini" type="success"
                            :title="tag">{{tag}}</el-tag>
                    <el-popover v-if="tagList(row).length > MAX_TAG_DISPLAY" placement="top" trigger="click"
                                width="280">
                      <div class="bili-tag-popover">
                        <el-tag v-for="tag in tagList(row)" :key="tag" size="mini" type="success">{{tag}}</el-tag>
                      </div>
                      <el-button slot="reference" type="text" size="mini">
                        +{{tagList(row).length - MAX_TAG_DISPLAY}}
                      </el-button>
                    </el-popover>
                    <span v-if="tagList(row).length === 0" class="bili-empty-text">无标签</span>
                  </div>
                  <div class="bili-times">
                    <div title="视频发布时间(pubdate)">发布：{{formatTs(row.pubdate)}}</div>
                  </div>
                  <div class="bili-actions">
                    <el-tooltip content="视频下载到服务器本地" placement="top">
                      <el-button v-if="!row.downloaded" type="primary" size="mini" plain icon="el-icon-download"
                                 :loading="row.downloading" @click="downloadVideo(row)">下载视频</el-button>
                    </el-tooltip>
                    <el-button size="mini" plain icon="el-icon-refresh" @click="refreshVideo(row)">刷新</el-button>
                    <el-button size="mini" type="danger" plain icon="el-icon-delete" @click="deleteVideo(row)">删除</el-button>
                  </div>
                  <div class="bili-file">
                    <template v-if="row.downloaded">
                      文件：{{row.videoFileName}}
                    </template>
                    <template v-else>
                       &nbsp;
                    </template>
                  </div>
                  <div v-if="row.downloadState === 'fail' && row.downloadMessage" class="bili-download-error"
                       :title="row.downloadMessage">下载失败：{{row.downloadMessage}}</div>
                </div>
              </div>
            </div>
          </div>
        </template>
        <el-empty v-else-if="!loading" description="暂无视频数据，可粘贴b站链接添加"></el-empty>
        <div ref="loadMoreTrigger" class="bili-load-more">
          <span v-if="loading && list.length > 0"><i class="el-icon-loading"></i> 加载中...</span>
          <span v-else-if="hasMore && list.length > 0">向下滑动加载更多</span>
          <span v-else-if="list.length > 0">没有更多了</span>
        </div>
      </div>
    </basic-container>
  </div>
</template>

<script>
import {search as searchApi, add as addApi, refresh as refreshApi, download as downloadApi,
  downloadStatus as downloadStatusApi, deleteBatch} from '@/api/bilibili-video';

const PAGE_SIZE = 20;
// 卡片上最多直接展示几个标签，剩下的收进popover
const MAX_TAG_DISPLAY = 3;
// 下载进度轮询间隔
const POLL_INTERVAL = 2000;

export default {
  name: 'BilibiliVideo',
  data() {
    return {
      MAX_TAG_DISPLAY,
      queryFormObj: {
        bvid: '',
        title: '',
        ownerName: '',
        tag: ''
      },
      addText: '',
      adding: false,
      groupBy: 'none',
      list: [],
      total: 0,
      currentPage: 1,
      hasMore: false,
      loading: false,
      selectedIds: [],
      pollTimer: null,
      observer: null,
      scrollParent: null,
      scrollHandler: null
    }
  },
  computed: {
    /**
     * 按作者/标签分组。一个视频有多个标签时会出现在多个分组里
     */
    groupedList() {
      if (this.groupBy === 'none') {
        return this.list.length === 0 ? [] : [{key: 'all', title: '全部视频', cards: this.list}]
      }
      const groups = new Map()
      const push = (key, meta, row) => {
        if (!groups.has(key)) {
          groups.set(key, Object.assign({key, cards: []}, meta))
        }
        groups.get(key).cards.push(row)
      }
      this.list.forEach(row => {
        if (this.groupBy === 'owner') {
          const key = `owner-${row.ownerMid || 0}`
          push(key, {title: row.ownerName || '未知作者', face: row.ownerFace, mid: row.ownerMid}, row)
        } else {
          const tags = this.tagList(row)
          if (tags.length === 0) {
            push('tag-__none__', {title: '无标签'}, row)
            return
          }
          tags.forEach(tag => push(`tag-${tag}`, {title: tag}, row))
        }
      })
      return Array.from(groups.values()).sort((a, b) => {
        if (b.cards.length !== a.cards.length) {
          return b.cards.length - a.cards.length
        }
        return String(a.title).localeCompare(String(b.title))
      })
    }
  },
  mounted() {
    this.search()
    this.$nextTick(() => this.setupObserver())
  },
  activated() {
    // 页面被keep-alive缓存，重新进入时恢复滚动监听
    this.$nextTick(() => this.setupObserver())
  },
  deactivated() {
    this.destroyObserver()
  },
  beforeDestroy() {
    this.destroyObserver()
    this.stopPoll()
  },
  methods: {
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
      this.search()
    },
    loadPage() {
      if (this.loading || !this.hasMore) {
        return
      }
      this.loading = true
      searchApi(Object.assign({}, this.queryFormObj, {
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
        const merged = this.list.concat(records.map(row => Object.assign({}, row)))
        this.list = merged
        this.currentPage += 1
        this.hasMore = records.length > 0 && merged.length < this.total
        // 新加载的数据可能带着进行中的下载
        this.syncDownloadState(merged)
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
      }).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$message.success(message || '已开始下载')
        this.$set(row, 'downloading', true)
        this.$set(row, 'downloadState', 'running')
        this.$set(row, 'downloadMessage', null)
        this.startPoll()
      }).catch(e => {
        if (e !== 'cancel' && e && e.message) {
          this.$message.error(e.message)
        }
      })
    },
    startPoll() {
      if (this.pollTimer) {
        return
      }
      this.pollTimer = setInterval(() => this.pollDownloadStatus(), POLL_INTERVAL)
      this.pollDownloadStatus()
    },
    stopPoll() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    pollDownloadStatus() {
      downloadStatusApi({}).then(({data: {code, data}}) => {
        if (code !== 200 || !data) {
          return
        }
        this.applyDownloadStates(data)
        const hasRunning = Object.keys(data).some(key => data[key] && data[key].state === 'running')
        if (!hasRunning) {
          this.stopPoll()
        }
      }).catch(() => {
        this.stopPoll()
      })
    },
    /**
     * 列表里存在正在下载的视频时开始轮询进度（首次加载/翻页时调用）
     */
    syncDownloadState(rows) {
      if (rows.some(row => row.downloading)) {
        this.startPoll()
      }
    },
    applyDownloadStates(states) {
      this.list.forEach(row => {
        const state = states[row.id]
        // 后台没有该记录的状态，说明不是本次会话发起的下载，保持接口返回值
        if (!state) {
          return
        }
        this.$set(row, 'downloading', state.state === 'running')
        this.$set(row, 'downloadState', state.state)
        this.$set(row, 'downloadMessage', state.message)
        if (state.state === 'success') {
          this.$set(row, 'downloaded', true)
        }
      })
    },

    /* ==================== 刷新 ==================== */
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

    /* ==================== 删除 ==================== */
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
    toggleSelect(id) {
      const index = this.selectedIds.indexOf(id)
      if (index >= 0) {
        this.selectedIds.splice(index, 1)
      } else {
        this.selectedIds.push(id)
      }
    },

    /* ==================== 展示 ==================== */
    videoUrl(row) {
      return `https://www.bilibili.com/video/${row.bvid}`
    },
    spaceUrl(uid) {
      return uid ? `https://space.bilibili.com/${uid}` : null
    },
    openVideo(row) {
      window.open(this.videoUrl(row), '_blank')
    },
    openSpace(row) {
      this.openSpaceUrl(row.ownerMid)
    },
    openSpaceUrl(uid) {
      const url = this.spaceUrl(uid)
      if (url) {
        window.open(url, '_blank')
      }
    },
    coverUrl(row) {
      // b站图片是http的，https页面下会被浏览器拦截，统一换成https
      return row.pic ? String(row.pic).replace(/^http:/, 'https:') : null
    },
    tagList(row) {
      if (!row.tag) {
        return []
      }
      return String(row.tag).split(/[,，]/).map(tag => tag.trim()).filter(tag => tag)
    },
    visibleTags(row) {
      return this.tagList(row).slice(0, MAX_TAG_DISPLAY)
    },
    formatTs(timestamp) {
      if (!timestamp) {
        return '-'
      }
      const value = String(timestamp).length === 10 ? Number(timestamp) * 1000 : Number(timestamp)
      return this.$dayjs(value).format('YYYY-MM-DD HH:mm:ss')
    },
    durationText(seconds) {
      const total = Math.max(0, Math.floor(Number(seconds) || 0))
      const hour = Math.floor(total / 3600)
      const minute = Math.floor((total % 3600) / 60)
      const second = total % 60
      const pad = value => String(value).padStart(2, '0')
      return hour > 0 ? `${hour}:${pad(minute)}:${pad(second)}` : `${pad(minute)}:${pad(second)}`
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
  },
  watch: {
    // 列表数据变化后重新挂载哨兵，保证首屏不足一屏时也能继续加载
    'list.length'() {
      this.$nextTick(() => this.setupObserver())
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
    .bili-mode-label {
      font-size: 12px;
      color: #606266;
    }
    .el-button + .el-button {
      margin-left: 0;
    }
  }
  .bili-summary {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
  }
  .bili-body {
    min-height: 120px;
  }
  .bili-group {
    &:not(:first-child) {
      margin-top: 14px;
    }
  }
  .bili-group-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 0;
    border-bottom: 1px solid #ebeef5;
    margin-bottom: 10px;
    .bili-group-avatar {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      flex: none;
      object-fit: cover;
      background-color: #f5f7fa;
    }
    .bili-group-avatar-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      color: #c0c4cc;
      font-size: 16px;
    }
    .bili-group-title {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
      max-width: 320px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .bili-group-count {
      font-size: 12px;
      color: #909399;
      background-color: #f4f4f5;
      border-radius: 8px;
      padding: 0 8px;
      line-height: 18px;
    }
  }
  .bili-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
    gap: 12px;
  }
  .bili-card {
    display: flex;
    flex-direction: column;
    background-color: #fff;
    border: 1px solid #ebeef5;
    border-radius: 6px;
    overflow: hidden;
    transition: box-shadow .2s;
    &:hover {
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, .1);
    }
  }
  .bili-card-downloaded {
    border-color: #b3e19d;
  }
  .bili-cover {
    position: relative;
    width: 100%;
    padding-top: 56.25%;
    background-color: #f5f7fa;
    cursor: pointer;
    img {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .bili-cover-empty {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #c0c4cc;
      font-size: 22px;
    }
    .bili-cover-duration {
      position: absolute;
      right: 6px;
      bottom: 6px;
      padding: 0 4px;
      font-size: 11px;
      line-height: 16px;
      color: #fff;
      background-color: rgba(0, 0, 0, .6);
      border-radius: 3px;
    }
    .bili-cover-flag {
      position: absolute;
      left: 6px;
      top: 6px;
      padding: 0 5px;
      font-size: 11px;
      line-height: 16px;
      border-radius: 3px;
      color: #fff;
      &.is-downloaded {
        background-color: rgba(103, 194, 58, .9);
      }
      &.is-undownloaded {
        background-color: rgba(144, 147, 153, .85);
      }
    }
  }
  .bili-card-body {
    display: flex;
    flex-direction: column;
    flex: 1;
    padding: 8px;
  }
  .bili-title {
    font-size: 13px;
    line-height: 18px;
    color: #303133;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    word-break: break-all;
    min-height: 36px;
  }
  .bili-meta-line {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 4px;
    min-width: 0;
    .bili-bvid {
      color: #409eff;
      font-size: 12px;
      flex: none;
      cursor: pointer;
      &:hover {
        text-decoration: underline;
      }
    }
    .bili-cid {
      font-size: 12px;
      color: #c0c4cc;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 1;
      min-width: 0;
    }
    .bili-card-select {
      flex: none;
      margin-right: 0;
    }
  }
  .bili-owner {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 6px;
    min-width: 0;
    cursor: pointer;
    .bili-owner-face {
      width: 20px;
      height: 20px;
      border-radius: 50%;
      flex: none;
      object-fit: cover;
      background-color: #f5f7fa;
    }
    .bili-owner-face-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      color: #c0c4cc;
      font-size: 12px;
    }
    .bili-owner-name {
      font-size: 12px;
      color: #606266;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      &:hover {
        color: #409eff;
      }
    }
  }
  .bili-tags {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
    margin-top: 6px;
    .el-tag {
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .bili-empty-text {
      font-size: 12px;
      color: #c0c4cc;
    }
  }
  .bili-times {
    margin-top: 6px;
    font-size: 12px;
    line-height: 18px;
    color: #909399;
  }
  .bili-actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
    margin-top: auto;
    padding-top: 8px;
    .el-button {
      margin: 0;
      padding: 5px 7px;
    }
    .bili-downloaded-tag {
      line-height: 22px;
    }
  }
  .bili-file {
    margin-top: 6px;
    font-size: 11px;
    color: #c0c4cc;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .bili-download-error {
    margin-top: 4px;
    font-size: 12px;
    color: #f56c6c;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .bili-load-more {
    text-align: center;
    padding: 12px 0;
    font-size: 12px;
    color: #909399;
  }
  .bili-tag-popover {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }
}
</style>

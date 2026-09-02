<template>
  <el-drawer :title="previewTitle" :visible.sync="visibleProxy" size="78%" direction="rtl" custom-class="jm-preview-drawer">
    <div v-loading="previewPreparing" class="jm-preview">
      <el-empty v-if="previewChapters.length === 0" description="暂无章节信息"></el-empty>
      <el-tabs v-else v-model="activePreviewChapterId" tab-position="left" @tab-click="handlePreviewTabClick">
        <el-tab-pane v-for="chapter in previewChapters" :key="chapter.chapterId" :label="formatPreviewChapterLabel(chapter)" :name="`${chapter.chapterId}`">
          <div v-if="activePreviewChapterId === `${chapter.chapterId}`" v-loading="currentPreviewLoading" class="jm-preview-content">
            <div v-if="currentPreviewImages.length > 0" class="jm-preview-toolbar">
              <div class="jm-preview-summary">
                已加载 {{currentPreviewImages.length}} / {{currentPreviewTotal || currentPreviewImages.length}} 张
              </div>
              <div class="jm-preview-width-control">
                <span>宽度</span>
                <el-slider v-model="previewImageWidth" :min="40" :max="100" :step="2" :show-tooltip="true"></el-slider>
                <span>{{previewImageWidth}}%</span>
              </div>
            </div>
            <el-empty v-if="currentPreviewImages.length === 0 && !currentPreviewLoading" description="暂无图片"></el-empty>
            <div v-else class="jm-preview-images">
              <div v-for="image in currentPreviewImages" :key="previewImageKey(image)" ref="previewImageBox" :data-preview-image-key="previewImageKey(image)" class="jm-preview-image-box" :style="previewImageDisplayStyle">
                <div class="jm-preview-image-meta">
                  <span>{{formatPreviewImageIndex(image)}}</span>
                  <span :title="formatPreviewImageFileName(image)" class="jm-preview-image-name">{{formatPreviewImageFileName(image)}}</span>
                </div>
                <img v-if="isPreviewImageAvailable(image) && image.lazyVisible" :src="image.serverImgUrl" :alt="image.imageFile" class="jm-preview-image" @error="markPreviewImageLoadFailed(image)">
                <div v-else-if="isPreviewImageAvailable(image)" class="jm-preview-image-pending">
                  <i class="el-icon-loading"></i>
                  <div>图片进入可视区域后加载</div>
                  <div class="jm-preview-image-file">{{image.imageFile}}</div>
                </div>
                <div v-else class="jm-preview-image-missing">
                  <i class="el-icon-picture-outline"></i>
                  <div>图片文件不存在</div>
                  <div class="jm-preview-image-file">{{image.imageFile}}</div>
                </div>
              </div>
              <div ref="previewLoadMoreTrigger" class="jm-preview-load-more">
                <span v-if="currentPreviewLoading">加载中...</span>
                <span v-else-if="currentPreviewHasMore">继续下滑加载更多</span>
                <span v-else-if="currentPreviewImages.length > 0">已加载全部图片</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-drawer>
</template>

<script>
import { searchChapterImages } from "@/api/jmcomic";

export default {
  name: 'JmPreviewDrawer',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    album: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      activePreviewChapterId: '',
      previewChapterImagesMap: {},
      previewChapterNextPageMap: {},
      previewChapterTotalMap: {},
      previewLoadingMap: {},
      previewImageObserver: null,
      previewLoadMoreObserver: null,
      previewScrollRoot: null,
      previewImageWidth: 72,
      lastPreviewAlbumId: null,
      previewPreparing: false,
      previewPageSize: 10
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
    previewTitle() {
      if (!this.album) {
        return '漫画预览'
      }
      return `JM${this.album.id} ${this.album.name || ''}`
    },
    previewChapters() {
      return this.album && this.album.chapterList ? this.album.chapterList : []
    },
    currentPreviewTotal() {
      return this.previewChapterTotalMap[this.activePreviewChapterId] || 0
    },
    currentPreviewLoading() {
      return Boolean(this.previewLoadingMap[this.activePreviewChapterId])
    },
    currentPreviewImages() {
      return this.previewChapterImagesMap[this.activePreviewChapterId] || []
    },
    currentPreviewHasMore() {
      return this.currentPreviewTotal > this.currentPreviewImages.length
    },
    previewImageDisplayStyle() {
      return {
        width: `${this.previewImageWidth}%`,
        maxWidth: '100%'
      }
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.initPreview()
      } else {
        this.closePreview()
      }
    },
    album() {
      if (this.visible) {
        this.initPreview()
      }
    }
  },
  methods: {
    async initPreview() {
      this.disconnectPreviewObservers()
      const albumId = this.album ? this.album.id : null
      if (!albumId) {
        this.resetPreviewScrollPosition()
        this.resetPreviewState()
        this.lastPreviewAlbumId = null
        return
      }
      if (albumId === this.lastPreviewAlbumId && this.hasAnyPreviewState()) {
        this.previewPreparing = false
        this.$nextTick(this.setupPreviewObservers)
        return
      }
      this.previewPreparing = true
      this.resetPreviewScrollPosition()
      this.resetPreviewState()
      this.lastPreviewAlbumId = albumId
      const firstChapter = this.previewChapters[0]
      this.activePreviewChapterId = firstChapter ? `${firstChapter.chapterId}` : ''
      try {
        if (this.activePreviewChapterId) {
          this.$set(this.previewChapterNextPageMap, this.activePreviewChapterId, 1)
          await this.loadPreviewChapterImages(this.activePreviewChapterId)
        }
      } finally {
        this.previewPreparing = false
      }
    },
    closePreview() {
      this.disconnectPreviewObservers()
      this.previewLoadingMap = {}
      this.previewPreparing = false
    },
    resetPreviewState() {
      this.activePreviewChapterId = ''
      this.previewChapterImagesMap = {}
      this.previewChapterNextPageMap = {}
      this.previewChapterTotalMap = {}
      this.previewLoadingMap = {}
    },
    hasAnyPreviewState() {
      return Boolean(
        this.activePreviewChapterId ||
        Object.keys(this.previewChapterImagesMap).length ||
        Object.keys(this.previewChapterNextPageMap).length ||
        Object.keys(this.previewChapterTotalMap).length
      )
    },
    resetPreviewScrollPosition() {
      const root = this.getPreviewScrollRoot() || this.previewScrollRoot
      if (root) {
        root.scrollTop = 0
      }
    },
    handlePreviewTabClick(tab) {
      if (!this.previewChapterNextPageMap[tab.name]) {
        this.$set(this.previewChapterNextPageMap, tab.name, 1)
      }
      if (this.hasPreviewChapterRequested(tab.name)) {
        this.$nextTick(this.setupPreviewObservers)
        return
      }
      this.loadPreviewChapterImages(tab.name)
    },
    hasPreviewChapterRequested(chapterId) {
      const images = this.previewChapterImagesMap[chapterId] || []
      return images.length > 0 || this.previewChapterTotalMap[chapterId] > 0 || this.previewChapterNextPageMap[chapterId] > 1
    },
    formatPreviewChapterLabel(chapter) {
      return chapter.title || chapter.name || `章节${chapter.chapterId}`
    },
    handleRequestError(error) {
      const message = error && error.data && error.data.message
        ? error.data.message
        : error && error.message
          ? error.message
          : '请求失败'
      this.$message.error(message)
    },
    async loadPreviewChapterImages(chapterId) {
      if (!this.album || !chapterId) {
        return
      }
      if (this.previewLoadingMap[chapterId]) {
        return
      }
      const nextPage = this.previewChapterNextPageMap[chapterId] || 1
      const currentImages = this.previewChapterImagesMap[chapterId] || []
      const total = this.previewChapterTotalMap[chapterId] || 0
      if (total > 0 && currentImages.length >= total) {
        this.$nextTick(this.setupPreviewObservers)
        return
      }
      this.$set(this.previewLoadingMap, chapterId, true)
      try {
        const { data: { code, message, data } } = await searchChapterImages({
          albumId: this.album.id,
          chapterId,
          currentPage: nextPage,
          pageSize: this.previewPageSize
        })
        if (code !== 200) {
          this.$message.error(message)
          return
        }
        const records = (data.records || []).map(record => ({
          ...record,
          lazyVisible: false
        }))
        const mergedRecords = currentImages.concat(records)
        this.$set(this.previewChapterTotalMap, chapterId, data.total || records.length)
        this.$set(this.previewChapterImagesMap, chapterId, mergedRecords)
        this.$set(this.previewChapterNextPageMap, chapterId, nextPage + 1)
        this.$nextTick(this.setupPreviewObservers)
      } catch (error) {
        this.handleRequestError(error)
      } finally {
        this.$delete(this.previewLoadingMap, chapterId)
      }
    },
    previewImageKey(image) {
      return `${image.chapterId}-${image.imageFile}`
    },
    formatPreviewImageIndex(image) {
      const index = this.currentPreviewImages.findIndex(item => this.previewImageKey(item) === this.previewImageKey(image))
      const total = this.currentPreviewTotal || this.currentPreviewImages.length
      return `第 ${index + 1} / ${total} 张`
    },
    formatPreviewImageFileName(image) {
      return image && image.imageFile ? image.imageFile : '未知文件'
    },
    isPreviewImageAvailable(image) {
      return image && image.imageFileExists && image.serverImgUrl && !image.loadFailed
    },
    markPreviewImageLoadFailed(image) {
      this.$set(image, 'loadFailed', true)
    },
    setupPreviewObservers() {
      this.setupPreviewScrollListener()
      this.setupPreviewImageObserver()
      this.setupPreviewLoadMoreObserver()
    },
    getPreviewScrollRoot() {
      return this.$el ? this.$el.querySelector('.el-tabs__content') : null
    },
    getFirstRef(refValue) {
      return Array.isArray(refValue) ? refValue[0] : refValue
    },
    setupPreviewScrollListener() {
      const root = this.getPreviewScrollRoot()
      if (!root || root === this.previewScrollRoot) {
        return
      }
      this.disconnectPreviewScrollListener()
      if (!root.getAttribute('tabindex')) {
        root.setAttribute('tabindex', '0')
      }
      root.addEventListener('scroll', this.handlePreviewScroll, { passive: true })
      root.addEventListener('keydown', this.handlePreviewKeydown)
      this.previewScrollRoot = root
      if (root.focus) {
        root.focus({ preventScroll: true })
      }
    },
    isEditablePreviewKeyTarget(target) {
      if (!target || target === this.previewScrollRoot) {
        return false
      }
      const tagName = target.tagName ? target.tagName.toLowerCase() : ''
      return tagName === 'input' || tagName === 'textarea' || tagName === 'select' || target.isContentEditable
    },
    setPreviewScrollTop(root, top) {
      const maxTop = Math.max(root.scrollHeight - root.clientHeight, 0)
      const nextTop = Math.max(Math.min(top, maxTop), 0)
      if (root.scrollTo) {
        root.scrollTo({ top: nextTop })
      } else {
        root.scrollTop = nextTop
      }
      this.handlePreviewScroll({ target: root })
    },
    handlePreviewKeydown(event) {
      const root = this.getPreviewScrollRoot()
      if (!root || this.isEditablePreviewKeyTarget(event.target)) {
        return
      }
      const smallStep = 80
      const pageStep = Math.floor(root.clientHeight * 0.9)
      const keyScrollMap = {
        ArrowUp: root.scrollTop - smallStep,
        ArrowDown: root.scrollTop + smallStep,
        PageUp: root.scrollTop - pageStep,
        PageDown: root.scrollTop + pageStep,
        Home: 0,
        End: root.scrollHeight
      }
      if (!Object.prototype.hasOwnProperty.call(keyScrollMap, event.key)) {
        return
      }
      event.preventDefault()
      this.setPreviewScrollTop(root, keyScrollMap[event.key])
    },
    handlePreviewScroll(event) {
      if (!this.currentPreviewHasMore || this.currentPreviewLoading) {
        return
      }
      const target = event && event.target ? event.target : this.getPreviewScrollRoot()
      if (!target) {
        return
      }
      const distanceToBottom = target.scrollHeight - target.scrollTop - target.clientHeight
      if (distanceToBottom <= 240) {
        this.loadPreviewChapterImages(this.activePreviewChapterId)
      }
    },
    setupPreviewImageObserver() {
      this.disconnectPreviewImageObserver()
      const boxes = Array.isArray(this.$refs.previewImageBox)
        ? this.$refs.previewImageBox
        : this.$refs.previewImageBox ? [this.$refs.previewImageBox] : []
      if (boxes.length === 0) {
        return
      }
      if (!window.IntersectionObserver) {
        this.currentPreviewImages.forEach(image => this.$set(image, 'lazyVisible', true))
        return
      }
      const root = this.getPreviewScrollRoot()
      this.previewImageObserver = new IntersectionObserver(entries => {
        entries.forEach(entry => {
          if (!entry.isIntersecting) {
            return
          }
          const imageKey = entry.target.dataset.previewImageKey
          const image = this.currentPreviewImages.find(item => this.previewImageKey(item) === imageKey)
          if (image) {
            this.$set(image, 'lazyVisible', true)
          }
          this.previewImageObserver.unobserve(entry.target)
        })
      }, {
        root,
        threshold: 0.01
      })
      boxes.forEach(box => this.previewImageObserver.observe(box))
    },
    setupPreviewLoadMoreObserver() {
      this.disconnectPreviewLoadMoreObserver()
      const trigger = this.getFirstRef(this.$refs.previewLoadMoreTrigger)
      if (!trigger || !this.currentPreviewHasMore || !window.IntersectionObserver) {
        return
      }
      const root = this.getPreviewScrollRoot()
      this.previewLoadMoreObserver = new IntersectionObserver(entries => {
        const shouldLoadMore = entries.some(entry => entry.isIntersecting)
        if (shouldLoadMore) {
          this.loadPreviewChapterImages(this.activePreviewChapterId)
        }
      }, {
        root,
        rootMargin: '300px 0px',
        threshold: 0.01
      })
      this.previewLoadMoreObserver.observe(trigger)
    },
    disconnectPreviewImageObserver() {
      if (this.previewImageObserver) {
        this.previewImageObserver.disconnect()
        this.previewImageObserver = null
      }
    },
    disconnectPreviewLoadMoreObserver() {
      if (this.previewLoadMoreObserver) {
        this.previewLoadMoreObserver.disconnect()
        this.previewLoadMoreObserver = null
      }
    },
    disconnectPreviewScrollListener() {
      if (this.previewScrollRoot) {
        this.previewScrollRoot.removeEventListener('scroll', this.handlePreviewScroll)
        this.previewScrollRoot.removeEventListener('keydown', this.handlePreviewKeydown)
        this.previewScrollRoot = null
      }
    },
    disconnectPreviewObservers() {
      this.disconnectPreviewImageObserver()
      this.disconnectPreviewLoadMoreObserver()
      this.disconnectPreviewScrollListener()
    }
  },
  beforeDestroy() {
    this.disconnectPreviewObservers()
  }
}
</script>

<style lang="scss" scoped>
.jm-preview {
  height: 100%;
  padding: 0 16px 16px;

  ::v-deep .el-tabs {
    height: 100%;
  }

  ::v-deep .el-tabs__content {
    height: 100%;
    overflow: auto;
    padding-left: 16px;
  }
}

.jm-preview-content {
  min-height: 360px;
}

.jm-preview-toolbar {
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  gap: 20px;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 0 0 12px;
  position: sticky;
  top: 0;
  z-index: 2;
}

.jm-preview-summary {
  color: #606266;
  flex: 0 0 auto;
  font-size: 13px;
}

.jm-preview-width-control {
  align-items: center;
  color: #606266;
  display: grid;
  flex: 0 1 360px;
  font-size: 13px;
  gap: 10px;
  grid-template-columns: auto minmax(160px, 1fr) 44px;
}

.jm-preview-images {
  align-items: center;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.jm-preview-image {
  background: #f5f7fa;
  box-shadow: 0 1px 4px rgba(0, 0, 0, .12);
  display: block;
  width: 100%;
  min-height: 80px;
}

.jm-preview-image-box {
  align-items: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.jm-preview-image-meta {
  align-items: center;
  color: #606266;
  display: flex;
  font-size: 13px;
  gap: 12px;
  justify-content: space-between;
  line-height: 24px;
  width: 100%;
}

.jm-preview-image-name {
  color: #909399;
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.jm-preview-image-missing {
  align-items: center;
  background: #f5f7fa;
  border: 1px dashed #c0c4cc;
  color: #909399;
  display: flex;
  flex-direction: column;
  font-size: 13px;
  gap: 6px;
  justify-content: center;
  min-height: 160px;
  padding: 20px;
  width: 100%;

  i {
    font-size: 28px;
  }
}

.jm-preview-image-pending {
  align-items: center;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  color: #909399;
  display: flex;
  flex-direction: column;
  font-size: 13px;
  gap: 6px;
  justify-content: center;
  min-height: 220px;
  padding: 20px;
  width: 100%;

  i {
    font-size: 24px;
  }
}

.jm-preview-image-file {
  color: #606266;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.jm-preview-load-more {
  color: #909399;
  font-size: 13px;
  line-height: 32px;
  min-height: 32px;
  text-align: center;
  width: 100%;
}
</style>

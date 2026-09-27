<template>
  <el-drawer :title="previewTitle" :visible.sync="visibleProxy" :direction="drawerDirection" :size="drawerSize" custom-class="jm-preview-drawer" @opened="handlePreviewDrawerOpened">
    <div v-loading="previewPreparing" class="jm-preview" :class="{'jm-preview--mobile': isMobileView}">
      <el-empty v-if="previewChapters.length === 0" description="暂无章节信息"></el-empty>
      <el-tabs v-else v-model="activePreviewChapterId" :tab-position="previewTabPosition" @tab-click="handlePreviewTabClick">
        <el-tab-pane v-for="chapter in previewChapters" :key="chapter.chapterId" :label="formatPreviewChapterLabel(chapter)" :name="`${chapter.chapterId}`">
          <div v-if="activePreviewChapterId === `${chapter.chapterId}`" v-loading="currentPreviewLoading" class="jm-preview-content">
            <div v-if="currentPreviewImages.length > 0" class="jm-preview-toolbar">
              <div class="jm-preview-summary">
                已加载 {{currentPreviewImages.length}} / {{currentPreviewTotal || currentPreviewImages.length}} 张
              </div>
              <div class="jm-preview-width-control">
                <span class="jm-preview-width-label">宽度</span>
                <el-slider class="jm-preview-width-slider" v-model="previewImageWidth" :min="40" :max="100" :step="2" :show-tooltip="true"></el-slider>
                <span class="jm-preview-width-value">{{previewImageWidth}}%</span>
                <!-- 移动端触屏缩放：双指捏合或双击切换 -->
                <el-button v-if="isMobileView" class="jm-preview-zoom-btn" type="text" size="mini" :title="zoomButtonTitle" @click="togglePreviewZoom">
                  <i :class="previewZoom > 1 ? 'el-icon-zoom-out' : 'el-icon-zoom-in'"></i>
                  {{previewZoom > 1 ? previewZoom.toFixed(1) + 'x' : '放大'}}
                </el-button>
              </div>
            </div>
            <el-empty v-if="currentPreviewImages.length === 0 && !currentPreviewLoading" description="暂无图片"></el-empty>
            <div v-else class="jm-preview-images" ref="previewImageStage" @touchstart="handlePreviewTouchStart" @touchmove="handlePreviewTouchMove">
              <div v-for="image in currentPreviewImages" :key="previewImageKey(image)" ref="previewImageBox" :data-preview-image-key="previewImageKey(image)" class="jm-preview-image-box" :style="previewImageDisplayStyle" @dblclick="togglePreviewZoom">
                <div class="jm-preview-image-meta">
                  <span>{{formatPreviewImageIndex(image)}}</span>
                  <span :title="formatPreviewImageFileName(image)" class="jm-preview-image-name">{{formatPreviewImageFileName(image)}}</span>
                </div>
                <img v-if="isPreviewImageAvailable(image) && image.lazyVisible" :src="$localUrl(image.serverImgUrl)" :alt="image.imageFile" class="jm-preview-image" @error="markPreviewImageLoadFailed(image)">
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

/**
 * 移动端图片基准宽度下限。
 * 低于该值说明测量时机不对（布局未稳定），此类结果视为无效并丢弃，
 * 否则图片会按错误的像素宽度渲染成一条细线。
 */
const MOBILE_MIN_IMAGE_WIDTH = 200;

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
      previewPageSize: 10,
      // 触屏缩放：previewZoom 为缩放倍数，changePreviewZoom 负责重置
      previewZoom: 1,
      previewBaseWidth: 0,
      previewPinchStartDistance: 0,
      previewPinchStartZoom: 1,
      previewPinchActive: false,
      // 图片舞台尺寸监听（移动端基准宽度的权威来源）
      previewStageObserver: null,
      previewStageEl: null
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
    /**
     * 移动端：抽屉铺满全屏，章节 Tab 放到底部（拇指可达），桌面端保持右侧 78% + 左侧章节栏。
     */
    drawerDirection() {
      return this.isMobileView ? 'btt' : 'rtl'
    },
    drawerSize() {
      return this.isMobileView ? '100%' : '78%'
    },
    previewTabPosition() {
      return this.isMobileView ? 'bottom' : 'left'
    },
    zoomButtonTitle() {
      return this.previewZoom > 1 ? '还原为适应宽度' : '放大图片（也可双指捏合或双击）'
    },
    /**
     * 图片显示尺寸：
     * - 桌面端沿用"占内容区百分比"，受宽度滑块控制；
     * - 移动端基础宽度固定为内容区满宽，宽度只由缩放倍数决定，
     *   用像素值表达才能让缩放后的图片正确撑出横向滚动区域。
     */
    previewImageDisplayStyle() {
      if (this.isMobileView) {
        // 基准宽度未知时退化为"容器满宽"，绝不用可疑的小值去定尺寸
        if (this.previewBaseWidth <= 0) {
          return {
            width: '100%',
            maxWidth: '100%'
          }
        }
        return {
          width: `${Math.round(this.previewBaseWidth * this.previewZoom)}px`,
          // 上限给足，避免放大后又被 maxWidth 卡回去
          maxWidth: `${this.previewBaseWidth * 10}px`
        }
      }
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
    },
    // 移动端视口变化（如旋转屏幕）后，缩放基准宽度需要重新测量
    isMobileView() {
      this.resetPreviewZoom()
      this.$nextTick(this.measurePreviewBaseWidth)
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
        this.$nextTick(() => {
          this.setupPreviewObservers()
          this.setupPreviewStageObserver()
          this.measurePreviewBaseWidth()
        })
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
      this.resetPreviewZoom()
    },
    /* ==================== 移动端触屏缩放 ==================== */
    /**
     * 缩放基准宽度：移动端图片按"容器满宽 × 缩放倍数"以像素表达。
     *
     * 这里必须避免量到偏小的值：一旦基准宽度取到小数值（布局未稳定、容器瞬时宽度等），
     * 图片就会按那个像素宽度渲染成一条细线。因此：
     * 1. 用 getBoundingClientRect().width（不受 transform 影响，抽屉 translate 动画不影响结果）；
     * 2. 小于 MOBILE_MIN_IMAGE_WIDTH 的值一律视为无效，宁可不更新也不用坏值；
     * 3. 用 ResizeObserver 监听容器，布局稳定后会自动纠正。
     */
    getPreviewStage() {
      return this.$refs.previewImageStage
        || (this.$el && this.$el.querySelector('.jm-preview-images'))
    },
    measurePreviewBaseWidth() {
      if (!this.isMobileView) {
        this.previewBaseWidth = 0
        return
      }
      const stage = this.getPreviewStage()
      if (!stage) {
        return
      }
      const width = stage.getBoundingClientRect
        ? stage.getBoundingClientRect().width
        : stage.clientWidth
      if (width >= MOBILE_MIN_IMAGE_WIDTH) {
        this.previewBaseWidth = Math.round(width)
      }
      // 量不到合理宽度时保持原值（或 0 → 图片走 100% 兜底），不使用坏值覆盖
    },
    /**
     * 监听图片舞台尺寸：抽屉打开、旋转屏幕、缩放窗口后自动重新测量。
     * 这是对"抽屉动画期间测量不可靠"最稳的兜底。
     */
    setupPreviewStageObserver() {
      this.disconnectPreviewStageObserver()
      const stage = this.getPreviewStage()
      if (!stage) {
        return
      }
      this.previewStageEl = stage
      if (typeof window !== 'undefined' && window.ResizeObserver) {
        this.previewStageObserver = new ResizeObserver(() => {
          this.measurePreviewBaseWidth()
        })
        this.previewStageObserver.observe(stage)
      }
    },
    disconnectPreviewStageObserver() {
      if (this.previewStageObserver) {
        this.previewStageObserver.disconnect()
        this.previewStageObserver = null
      }
      this.previewStageEl = null
    },
    resetPreviewZoom() {
      this.previewZoom = 1
      this.previewPinchActive = false
      this.previewPinchStartDistance = 0
      this.previewPinchStartZoom = 1
    },
    /** 抽屉展开动画结束（element-ui 的 opened 事件）后，布局已稳定，做一次权威测量 */
    handlePreviewDrawerOpened() {
      this.$nextTick(() => {
        this.setupPreviewStageObserver()
        this.measurePreviewBaseWidth()
      })
    },
    zoomStep() {
      return 0.5
    },
    togglePreviewZoom() {
      if (!this.isMobileView) {
        return
      }
      this.previewZoom = this.previewZoom > 1 ? 1 : 2
      this.previewPinchActive = false
    },
    getPreviewTouchDistance(touches) {
      const dx = touches[0].clientX - touches[1].clientX
      const dy = touches[0].clientY - touches[1].clientY
      return Math.sqrt(dx * dx + dy * dy)
    },
    handlePreviewTouchStart(event) {
      if (!this.isMobileView || !event.touches || event.touches.length !== 2) {
        this.previewPinchActive = false
        return
      }
      this.previewPinchActive = true
      this.previewPinchStartDistance = this.getPreviewTouchDistance(event.touches)
      this.previewPinchStartZoom = this.previewZoom
    },
    /**
     * 双指捏合缩放。
     * 单指滑动不拦截，交给 el-tabs__content 的纵向滚动处理（含触底加载更多）。
     */
    handlePreviewTouchMove(event) {
      if (!this.previewPinchActive || !event.touches || event.touches.length !== 2) {
        return
      }
      const distance = this.getPreviewTouchDistance(event.touches)
      if (!this.previewPinchStartDistance) {
        this.previewPinchStartDistance = distance
        return
      }
      const nextZoom = this.previewPinchStartZoom * (distance / this.previewPinchStartDistance)
      this.previewZoom = Math.min(5, Math.max(1, Math.round(nextZoom * 100) / 100))
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
      // 切换章节后重置缩放，避免上一话的放大倍数带到新章节
      this.resetPreviewZoom()
      if (!this.previewChapterNextPageMap[tab.name]) {
        this.$set(this.previewChapterNextPageMap, tab.name, 1)
      }
      if (this.hasPreviewChapterRequested(tab.name)) {
        this.$nextTick(() => {
          this.setupPreviewObservers()
          this.setupPreviewStageObserver()
          this.measurePreviewBaseWidth()
        })
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
        this.$nextTick(() => {
          this.setupPreviewObservers()
          // 首屏图片渲染后再测量一次：此时 .jm-preview-images 才真实存在
          this.setupPreviewStageObserver()
          this.measurePreviewBaseWidth()
        })
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
    /**
     * 可展示性只看字段本身；地址按当前访问站点替换由 $localUrl 在渲染时完成
     */
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
      this.disconnectPreviewStageObserver()
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

  /**
   * 移动端：抽屉铺满全屏、章节 Tab 移到底部、内容区铺满。
   * 关闭浏览器对图片区域的双击缩放，避免与自定义双击/捏合缩放冲突。
   */
  &--mobile {
    padding: 0 6px 6px;

    ::v-deep .el-tabs__content {
      padding-left: 0;
      padding-right: 0;
      overscroll-behavior: contain;
    }

    ::v-deep .el-tabs--bottom .el-tabs__header.is-bottom {
      margin-top: 4px;
      margin-bottom: 0;
    }

    ::v-deep .el-tabs__nav-wrap {
      padding: 0 6px;
    }

    ::v-deep .el-tabs__item {
      font-size: 13px;
      height: 36px;
      line-height: 36px;
      padding: 0 12px;
    }

    // 章节可能很多，底部 Tab 条允许横向滑动
    ::v-deep .el-tabs__nav-scroll {
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
    }
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

.jm-preview-zoom-btn {
  padding: 0 4px;
  color: #409eff;
}

.jm-preview-images {
  align-items: center;
  display: flex;
  flex-direction: column;
  gap: 12px;
  // 缩放后允许整张图片横向滚动查看
  min-width: 0;
}

/**
 * 移动端样式：全部包在媒体查询里，桌面端行为完全不变
 */
@media screen and (max-width: 768px) {
  .jm-preview {
    padding: 0 4px 4px;

    ::v-deep .el-tabs__content {
      padding-left: 0;
      // 顶部工具条是 sticky 的，横向缩放后需要能滚到边缘
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      overscroll-behavior: contain;
    }
  }

  .jm-preview-content {
    // 手机上把纵向空间尽量留给图片
    min-height: 200px;
  }

  .jm-preview-toolbar {
    gap: 6px;
    padding: 6px 4px;
    // 缩放后图片比可视区宽，工具条保持铺满视口宽度
    position: sticky;
    left: 0;
    width: 100%;
    box-sizing: border-box;
  }

  // 手机上省掉"已加载 x / y 张"文案，把宽度留给缩放控件
  .jm-preview-summary {
    display: none;
  }

  .jm-preview-width-control {
    flex: 1 1 auto;
    gap: 8px;
    // 手机上隐藏了"宽度"文字，列数同步减一：滑块 + 百分比 + 缩放按钮
    grid-template-columns: minmax(0, 1fr) 38px auto;
    width: 100%;
  }

  .jm-preview-width-label {
    display: none;
  }

  .jm-preview-image-meta {
    font-size: 12px;
    gap: 6px;
  }

  // 缩放状态下图片可能超出视口，图片盒也不能被压缩
  .jm-preview-image-box {
    align-items: center;
    flex: 0 0 auto;
    // 取消双击缩放延迟，让 @dblclick 更快响应，同时保留纵向滚动
    touch-action: manipulation;
  }

  .jm-preview-image-missing,
  .jm-preview-image-pending {
    min-height: 120px;
    padding: 12px;
  }

  .jm-preview-load-more {
    // 底部 Tab 已占据底部空间，这里留出一点间距即可
    padding-bottom: 4px;
  }
}

.jm-preview-image {
  background: #f5f7fa;
  box-shadow: 0 1px 4px rgba(0, 0, 0, .12);
  display: block;
  width: 100%;
  min-height: 80px;
  // 长按图片不再弹出系统图片菜单，便于连续阅读；
  // 用 manipulation 保留滚动/捏合，同时让双击缩放快速触发
  -webkit-touch-callout: none;
  -webkit-user-select: none;
  user-select: none;
  touch-action: manipulation;
}

.jm-preview-image-box {
  align-items: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  // 缩放后由 width 撑开宽度，禁止被 flex 容器压缩
  flex: 0 0 auto;
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

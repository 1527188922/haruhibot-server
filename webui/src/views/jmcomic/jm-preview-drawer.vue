<template>
  <el-drawer :title="previewTitle" :visible.sync="visibleProxy" size="78%" direction="rtl" custom-class="jm-preview-drawer">
    <div class="jm-preview">
      <el-empty v-if="previewChapters.length === 0" description="暂无章节信息"></el-empty>
      <el-tabs v-else v-model="activePreviewChapterId" tab-position="left" @tab-click="handlePreviewTabClick">
        <el-tab-pane v-for="chapter in previewChapters" :key="chapter.chapterId" :label="formatPreviewChapterLabel(chapter)" :name="`${chapter.chapterId}`">
          <div v-loading="previewLoadingMap[chapter.chapterId]" class="jm-preview-content">
            <el-empty v-if="currentPreviewImages.length === 0 && !previewLoadingMap[chapter.chapterId]" description="暂无图片"></el-empty>
            <div v-else class="jm-preview-images">
              <div v-for="image in currentPreviewImages" :key="`${image.chapterId}-${image.imageFile}`" class="jm-preview-image-box">
                <img v-if="isPreviewImageAvailable(image)" :src="image.serverImgUrl" :alt="image.imageFile" loading="lazy" class="jm-preview-image" @error="markPreviewImageLoadFailed(image)">
                <div v-else class="jm-preview-image-missing">
                  <i class="el-icon-picture-outline"></i>
                  <div>图片文件不存在</div>
                  <div class="jm-preview-image-file">{{image.imageFile}}</div>
                </div>
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
      previewLoadingMap: {}
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
    currentPreviewImages() {
      return this.previewChapterImagesMap[this.activePreviewChapterId] || []
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.initPreview()
      }
    },
    album() {
      if (this.visible) {
        this.initPreview()
      }
    }
  },
  methods: {
    initPreview() {
      this.previewChapterImagesMap = {}
      this.previewLoadingMap = {}
      const firstChapter = this.previewChapters[0]
      this.activePreviewChapterId = firstChapter ? `${firstChapter.chapterId}` : ''
      if (this.activePreviewChapterId) {
        this.loadPreviewChapterImages(this.activePreviewChapterId)
      }
    },
    handlePreviewTabClick(tab) {
      this.loadPreviewChapterImages(tab.name)
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
      if (!this.album || !chapterId || this.previewChapterImagesMap[chapterId]) {
        return
      }
      this.$set(this.previewLoadingMap, chapterId, true)
      try {
        const pageSize = 200
        let currentPage = 1
        let records = []
        let total = 0
        do {
          const { data: { code, message, data } } = await searchChapterImages({
            albumId: this.album.id,
            chapterId,
            currentPage,
            pageSize
          })
          if (code !== 200) {
            this.$message.error(message)
            return
          }
          const pageRecords = data.records || []
          total = data.total || pageRecords.length
          records = records.concat(pageRecords)
          currentPage += 1
        } while (records.length < total)
        this.$set(this.previewChapterImagesMap, chapterId, records)
      } catch (error) {
        this.handleRequestError(error)
      } finally {
        this.$delete(this.previewLoadingMap, chapterId)
      }
    },
    isPreviewImageAvailable(image) {
      return image && image.imageFileExists && image.serverImgUrl && !image.loadFailed
    },
    markPreviewImageLoadFailed(image) {
      this.$set(image, 'loadFailed', true)
    }
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
  max-width: 100%;
  min-height: 80px;
}

.jm-preview-image-box {
  display: flex;
  justify-content: center;
  width: 100%;
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
  width: 320px;

  i {
    font-size: 28px;
  }
}

.jm-preview-image-file {
  color: #606266;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

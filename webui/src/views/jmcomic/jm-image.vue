<template>
  <el-image v-if="preview && displaySrc && !failed" class="jm-image" :src="displaySrc" :fit="fit"
            :preview-src-list="resolvedPreviewList" referrerpolicy="no-referrer" @error="handleLoadError">
    <div slot="placeholder" class="jm-image-state">
      <slot name="placeholder"><i class="el-icon-loading"></i></slot>
    </div>
    <div slot="error" class="jm-image-state">
      <slot name="error"><i class="el-icon-picture-outline"></i></slot>
    </div>
  </el-image>
  <img v-else-if="!preview && displaySrc && !failed" class="jm-image" :src="displaySrc" :alt="alt"
       referrerpolicy="no-referrer" @error="handleLoadError">
  <div v-else-if="failed" class="jm-image jm-image-state">
    <slot name="error"><i class="el-icon-picture-outline"></i></slot>
  </div>
  <div v-else class="jm-image jm-image-state">
    <slot name="placeholder"><i class="el-icon-loading"></i></slot>
  </div>
</template>

<script>
import { isLocalResourceUrl } from '@/util/url'
import { acquireJmImageBlobUrl, releaseJmImageBlobUrl } from '@/util/jm-image-crypto'

/**
 * JM图片展示组件。
 *
 * 后端开启图片加密后，本地封面/漫画图片是加密文件，不能直接交给 <img>。
 * 本组件负责：明文或远程地址直接展示，加密地址先取字节解密成 blob url 再展示。
 *
 * props:
 * - src：图片地址（后端返回的本地地址或JM远程地址）
 * - encrypted：后端返回的加密标记。false=确定明文直接展示；true=确定加密；null/undefined=自动探测
 * - preview：是否启用 element-ui 的点击大图预览（封面使用）
 * - previewList：大图预览地址列表，缺省为当前图片
 */
export default {
  name: 'JmImage',
  props: {
    src: {
      type: String,
      default: ''
    },
    encrypted: {
      type: Boolean,
      default: null
    },
    preview: {
      type: Boolean,
      default: false
    },
    previewList: {
      type: Array,
      default: null
    },
    fit: {
      type: String,
      default: 'cover'
    },
    alt: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      displaySrc: '',
      failed: false,
      resolveToken: 0,
      // 当前占用的blob缓存地址，切换图片/卸载时要释放
      acquiredSrc: ''
    }
  },
  computed: {
    resolvedPreviewList() {
      if (this.previewList && this.previewList.length > 0) {
        return this.previewList
      }
      return this.displaySrc ? [this.displaySrc] : []
    }
  },
  watch: {
    src: {
      handler() {
        this.resolveSrc()
      },
      immediate: true
    },
    encrypted() {
      this.resolveSrc()
    }
  },
  methods: {
    async resolveSrc() {
      const token = ++this.resolveToken
      this.releaseAcquired()
      this.failed = false
      const url = this.$localUrl(this.src)
      if (!url) {
        this.displaySrc = ''
        return
      }
      // 远程JM地址不加密；后端明确标记为明文时也直接交给浏览器加载
      if (!isLocalResourceUrl(url) || this.encrypted === false) {
        this.displaySrc = url
        return
      }
      this.displaySrc = ''
      this.acquiredSrc = url
      try {
        const blobUrl = await acquireJmImageBlobUrl(url)
        if (token === this.resolveToken) {
          this.displaySrc = blobUrl
        }
      } catch (error) {
        if (token === this.resolveToken) {
          this.failed = true
          this.$emit('error', error)
        }
      }
    },
    releaseAcquired() {
      if (this.acquiredSrc) {
        releaseJmImageBlobUrl(this.acquiredSrc)
        this.acquiredSrc = ''
      }
    },
    handleLoadError(event) {
      this.failed = true
      this.$emit('error', event)
    }
  },
  beforeDestroy() {
    this.resolveToken++
    this.releaseAcquired()
  }
}
</script>

<style lang="scss" scoped>
.jm-image {
  display: block;
}

.jm-image-state {
  align-items: center;
  display: flex;
  justify-content: center;
}
</style>

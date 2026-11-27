<template>
  <div class="chat-image-viewer" @click.self="close" @wheel.prevent>
    <span class="viewer-btn viewer-close" title="关闭" @click="close">
      <i class="el-icon-close"></i>
    </span>
    <span class="viewer-btn viewer-prev" :class="{ 'is-disabled': index <= 0 }" title="上一张" @click="handlePrevClick">
      <i class="el-icon-arrow-left"></i>
    </span>
    <span class="viewer-btn viewer-next" :class="{ 'is-disabled': index >= urlList.length - 1 }" title="下一张" @click="handleNextClick">
      <i class="el-icon-arrow-right"></i>
    </span>
    <div class="viewer-canvas">
      <img v-if="!imgError" :src="urlList[index]" alt="" @error="imgError = true">
      <span v-else class="viewer-error">[图片]</span>
    </div>
    <div class="viewer-index" v-if="urlList.length > 1">{{ index + 1 }} / {{ urlList.length }}</div>
  </div>
</template>
<script>
/**
 * 聊天图片预览：左右切换但不循环，翻到首尾后继续翻动无反应
 */
export default {
  name: 'ChatImageViewer',
  props: {
    urlList: {
      type: Array,
      default: () => []
    },
    initialIndex: {
      type: Number,
      default: 0
    }
  },
  data () {
    return {
      index: Math.min(Math.max(this.initialIndex, 0), Math.max(this.urlList.length - 1, 0)),
      imgError: false
    }
  },
  watch: {
    index () {
      this.imgError = false
    }
  },
  mounted () {
    document.addEventListener('keydown', this.handleKeydown)
  },
  beforeDestroy () {
    document.removeEventListener('keydown', this.handleKeydown)
  },
  methods: {
    handleKeydown (e) {
      if (e.key === 'Escape') {
        this.close()
      } else if (e.key === 'ArrowLeft') {
        e.preventDefault()
        this.prev()
      } else if (e.key === 'ArrowRight') {
        e.preventDefault()
        this.next()
      }
    },
    prev () {
      if (this.index > 0) {
        this.index--
      }
    },
    next () {
      if (this.index < this.urlList.length - 1) {
        this.index++
      }
    },
    // 按钮翻到边界时给出提示，键盘方向键保持无反应
    handlePrevClick () {
      if (this.index > 0) {
        this.index--
      } else {
        this.$message.info('暂无更多图片')
      }
    },
    handleNextClick () {
      if (this.index < this.urlList.length - 1) {
        this.index++
      } else {
        this.$message.info('暂无更多图片')
      }
    },
    close () {
      this.$emit('close')
    }
  }
}
</script>
<style scoped lang="scss">
.chat-image-viewer{
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.65);
  .viewer-canvas{
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    pointer-events: none;
    img{
      max-width: 90vw;
      max-height: 85vh;
      pointer-events: auto;
      user-select: none;
    }
  }
  .viewer-error{
    color: #fff;
    font-size: 16px;
  }
  .viewer-btn{
    position: absolute;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: #606266;
    color: #fff;
    font-size: 24px;
    opacity: 0.8;
    cursor: pointer;
    &:hover{
      opacity: 1;
    }
    // 已是首/尾时按钮保留，仅置灰，点击给出提示
    &.is-disabled{
      opacity: 0.35;
      cursor: default;
      &:hover{
        opacity: 0.35;
      }
    }
  }
  .viewer-close{
    top: 30px;
    right: 30px;
  }
  .viewer-prev{
    top: 50%;
    left: 30px;
    transform: translateY(-50%);
  }
  .viewer-next{
    top: 50%;
    right: 30px;
    transform: translateY(-50%);
  }
  .viewer-index{
    position: absolute;
    bottom: 24px;
    left: 50%;
    transform: translateX(-50%);
    padding: 4px 12px;
    border-radius: 12px;
    background: rgba(0, 0, 0, 0.4);
    color: #fff;
    font-size: 14px;
  }
}
</style>

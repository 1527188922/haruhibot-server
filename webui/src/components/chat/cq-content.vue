<script>
import { mapGetters } from 'vuex'
import { parseCqContent } from '@/util/cq'

export default {
  name: 'CqContent',
  props: {
    content: {
      type: String,
      default: ''
    },
    //当前消息的图片在预览列表中的起始下标
    previewOffset: {
      type: Number,
      default: 0
    }
  },
  data () {
    return {
      //记录加载失败的图片片段下标，失败后直接渲染占位文本，不再保留图片组件
      imageErrorMap: {}
    }
  },
  computed: {
    ...mapGetters(['colorName']),
    segments () {
      return parseCqContent(this.content)
    }
  },
  watch: {
    content () {
      this.imageErrorMap = {}
    }
  },
  methods: {
    handleImageError (index, seg) {
      this.$set(this.imageErrorMap, index, true)
      this.$emit('image-error', seg)
    }
  },
  // 使用渲染函数而不是模板：模板中的换行/缩进会在 pre-wrap 下变成多余空白，导致原文空格被撑宽
  render (h) {
    let imageIndex = 0
    const children = this.segments.map((seg, index) => {
      if (seg.type === 'text') {
        return h('span', { key: 't' + index }, seg.text)
      }
      if (seg.type === 'image') {
        const previewIndex = this.previewOffset + imageIndex
        imageIndex++
        // 加载失败的图片直接渲染占位文本，避免图片组件错误态下尺寸异常
        if (this.imageErrorMap[index]) {
          return h('span', {
            key: 'i' + index,
            class: 'cq-tag',
            style: { color: this.colorName },
            attrs: { title: seg.raw }
          }, '[图片]')
        }
        return h('el-image', {
          key: 'i' + index,
          class: 'cq-image',
          attrs: {
            src: seg.url,
            referrerpolicy: 'no-referrer'
          },
          on: {
            error: () => this.handleImageError(index, seg),
            click: () => this.$emit('image-preview', previewIndex)
          }
        }, [
          h('span', {
            slot: 'error',
            class: 'cq-tag',
            style: { color: this.colorName },
            attrs: { title: seg.raw }
          }, '[图片]')
        ])
      }
      return h('span', {
        key: 'g' + index,
        class: 'cq-tag',
        style: { color: this.colorName },
        attrs: { title: seg.raw }
      }, seg.text)
    })
    return h('span', { class: 'cq-content' }, children)
  }
}
</script>
<style scoped lang="scss">
.cq-content {
  // 只在内容层保留原消息的空格与换行，气泡本身不再使用 pre-wrap
  white-space: pre-wrap;
  .cq-image {
    display: inline-block;
    vertical-align: bottom;
    max-width: 200px;
    max-height: 200px;
    margin: 2px;
    cursor: zoom-in;
    // 覆盖 el-image 默认的 100% 宽高，按图片原始比例展示并限制最大尺寸
    ::v-deep .el-image__inner {
      display: block;
      width: auto;
      height: auto;
      max-width: 200px;
      max-height: 200px;
      border-radius: 4px;
    }
    ::v-deep .el-image__placeholder {
      width: 80px;
      height: 80px;
      border-radius: 4px;
    }
  }
  // 占位标签颜色由当前主题色动态指定（内联 style），不加内外边距和背景，保证与原消息的空格宽度一致
  .cq-tag {
    color: #409EFF;
  }
}
</style>

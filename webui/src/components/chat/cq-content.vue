<script>
import { mapGetters } from 'vuex'
import { parseCqContent } from '@/util/cq'

export default {
  name: 'CqContent',
  props: {
    content: {
      type: String,
      default: ''
    }
  },
  computed: {
    ...mapGetters(['colorName']),
    segments () {
      return parseCqContent(this.content)
    }
  },
  // 使用渲染函数而不是模板：模板中的换行/缩进会在 pre-wrap 下变成多余空白，导致原文空格被撑宽
  render (h) {
    const children = this.segments.map((seg, index) => {
      if (seg.type === 'text') {
        return h('span', { key: 't' + index }, seg.text)
      }
      if (seg.type === 'image') {
        return h('el-image', {
          key: 'i' + index,
          class: 'cq-image',
          attrs: {
            src: seg.url,
            referrerpolicy: 'no-referrer'
          },
          on: {
            error: () => this.$emit('image-error', seg)
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
  // 占位标签颜色由当前主题色动态指定（内联 style），不加内外边距，保证与原消息的空格宽度一致
  .cq-tag {
    color: #409EFF;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 2px;
  }
}
</style>

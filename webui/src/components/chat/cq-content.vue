<template>
  <span class="cq-content">
    <template v-for="(seg, index) in segments">
      <span v-if="seg.type === 'text'" :key="'t' + index">
        {{ seg.text }}
      </span>
      <el-image v-else-if="seg.type === 'image'" :key="'i' + index" class="cq-image" :src="seg.url" referrerpolicy="no-referrer">
        <span slot="error" class="cq-tag">[图片]</span>
      </el-image>
      <span v-else :key="'g' + index" class="cq-tag">
        {{ seg.text }}
      </span>
    </template>
  </span>
</template>
<script>
import { parseCqContent } from '@/util/cq'

// 片段之间不能出现换行空白（模板已写成单行），否则气泡的 pre-wrap 会在图文之间渲染出多余空格
export default {
  name: 'CqContent',
  props: {
    content: {
      type: String,
      default: ''
    }
  },
  computed: {
    segments () {
      return parseCqContent(this.content)
    }
  }
}
</script>
<style scoped lang="scss">
.cq-content {
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
  // 占位标签统一主题色；加浅色底保证在蓝色自己气泡上也清晰可见
  .cq-tag {
    color: #409EFF;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 4px;
    padding: 0 3px;
    margin: 0 1px;
  }
}
</style>

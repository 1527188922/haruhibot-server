<template>
  <basic-container class="sql-editor">
    <div class="editor-container" :class="theme">
      <!-- 左侧资源管理器 -->
      <left-panel :left-width-holder="leftWidthHolder"></left-panel>

      <!-- 拖拽条 -->
      <div class="resize-bar" @mousedown="startResize" :style="{width: resizeBarWeight+'px'}"></div>

      <!-- 右侧工作区 -->
      <right-panel ref="rightPanel"></right-panel>
    </div>
  </basic-container>

</template>

<script>
import LeftPanel from "./left-panel.vue";
import RightPanel from "./right-panel.vue";

export default {
  components: {
    LeftPanel,
    RightPanel
  },
  data() {
    return {
      theme: 'light',
      resizeBarWeight:5,
      leftWidthHolder:{
        leftWidth: 280,
      },
      dbTree: [],
      treeProps: {
        children: 'children',
        label: 'label'
      },
    }
  },
  async created() {
  },
  methods: {
    // 拖拽调整宽度
    startResize(e) {
      const startX = e.clientX
      const startWidth = this.leftWidthHolder.leftWidth
      document.onmousemove = (e) => {
        this.leftWidthHolder.leftWidth = startWidth + (e.clientX - startX)
      }
      document.onmouseup = () => {
        document.onmousemove = null
      }
    },

  }
}
</script>

<style scoped lang="scss">
.sql-editor{
  display: flex;
  height: 100%;
  padding-bottom: 0;
  ::v-deep .el-card__body{
    height: 100%;
    padding: 0;
  }
  .editor-container {
    display: flex;
    height: 100%;
    &.dark {
      background: #1a1a1a;
      color: #fff;
    }

    .resize-bar {
      background: #ddd;
      cursor: col-resize;
      z-index: 1;
      transition: background 0.3s;
      &:hover {
        background: #409EFF;
      }
    }


  }
}

</style>
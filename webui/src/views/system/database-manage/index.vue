<template>
  <basic-container class="db-manage">
    <div class="db-manage-container" :class="theme">
      <!-- 移动端资源树以抽屉形式覆盖在编辑区之上 -->
      <div v-if="mobilePanelOpen"
           class="db-manage-mask"
           @click="closeMobilePanel"></div>

      <!-- 左侧资源管理器 -->
      <left-panel ref="leftPanel" :left-width-holder="leftWidthHolder"></left-panel>

      <!-- 拖拽条：移动端无鼠标拖拽，由样式隐藏 -->
      <div class="resize-bar" @mousedown="startResize" :style="{width: resizeBarWeight+'px'}"></div>

      <!-- 右侧工作区 -->
      <right-panel ref="rightPanel" @toggle-left-panel="toggleLeftPanel"></right-panel>
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
      leftMinWidth:200,//左侧最小宽度
      // 桌面端资源树宽度；移动端由 leftWidthHolder 计算属性固定收窄为 0/280
      desktopLeftWidth: 400,
      mobileLeftWidth: 0,
      dbTree: [],
      treeProps: {
        children: 'children',
        label: 'label'
      },
    }
  },
  computed: {
    /**
     * left-panel 通过该对象读取宽度。
     * 用计算属性而不是在 mounted 里赋值，是为了避免"子组件 mounted 早于父组件提交断点"
     * 造成的移动端误判（挂载瞬间 screen 还是初始值 -1）。
     */
    leftWidthHolder() {
      return {
        leftWidth: this.isMobileView ? this.mobileLeftWidth : this.desktopLeftWidth
      }
    },
    // 移动端下资源树以抽屉形式展开
    mobilePanelOpen() {
      return this.isMobileView && this.mobileLeftWidth > 0
    }
  },
  async created() {
  },
  methods: {
    // 右侧按钮栏的"资源树"入口：移动端抽屉开关（桌面端资源树常驻，开关用于临时收起）
    toggleLeftPanel() {
      if (this.isMobileView) {
        this.mobileLeftWidth = this.mobileLeftWidth > 0 ? 0 : 280
        return
      }
      this.desktopLeftWidth = this.desktopLeftWidth > 0 ? 0 : 400
    },
    closeMobilePanel() {
      this.mobileLeftWidth = 0
    },
    // 拖拽调整宽度（仅桌面端可用）
    startResize(e) {
      const startX = e.clientX
      const startWidth = this.desktopLeftWidth
      document.onmousemove = (e) => {
        const w = startWidth + (e.clientX - startX)
        if (w > this.leftMinWidth) {
          this.desktopLeftWidth = startWidth + (e.clientX - startX)
        }
      }
      document.onmouseup = () => {
        document.onmousemove = null
      }
    },

  }
}
</script>

<style scoped lang="scss">
.db-manage{
  display: flex;
  height: 100%;
  padding-bottom: 0;
  ::v-deep .el-card__body{
    height: 100%;
    padding: 0;
  }
  .db-manage-container {
    display: flex;
    height: 100%;
    &.dark {
      background: #1a1a1a;
      color: #fff;
    }

    .resize-bar {
      //background: #ddd;
      background-color: #f0f2f5;
      cursor: col-resize;
      z-index: 1;
      transition: background 0.3s;
      &:hover {
        background: #409EFF;
      }
    }


  }

  .db-manage-mask {
    display: none;
  }

  /**
   * 移动端：资源树改为覆盖式抽屉，SQL 编辑区获得全部宽度。
   * 拖拽条依赖 mousedown，移动端无意义直接隐藏。
   */
  @media screen and (max-width: 768px) {
    .db-manage-container {
      position: relative;

      .resize-bar {
        display: none;
      }

      ::v-deep .left-panel {
        position: absolute;
        top: 0;
        bottom: 0;
        left: 0;
        z-index: 12;
        max-width: 84vw;
        background: #fff;
        box-shadow: 2px 0 12px rgba(0, 0, 0, .25);
        transition: width .2s;
      }

      // 资源树收起后不再占据布局宽度（宽度由行内 style 控制为 0）
      &.dark ::v-deep .left-panel {
        background: #1a1a1a;
      }
    }

    .db-manage-mask {
      display: block;
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      left: 0;
      z-index: 11;
      background: rgba(0, 0, 0, .35);
    }
  }
}

</style>
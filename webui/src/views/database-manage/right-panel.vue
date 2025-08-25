<template>
  <div class="right-panel">
    <el-container>
      <el-aside>
        <div class="btn-dev" title="执行">
          <el-button type="text" @click="exec">▶</el-button>
        </div>
        <div class="btn-dev" title="执行选中的" @mousedown.prevent="(e)=>{}">
          <template v-if="selectedText && selectedText.trim().length !== 0">
            <el-button type="text" class="success-text-btn" @click="execSelected">▶</el-button>
          </template>
          <template v-else>
            <el-button type="text" class="info-text-btn" disabled>▶</el-button>
          </template>
        </div>
      </el-aside>
      <el-container>
        <el-main ref="main" :style="{ height: mainHeight + 'px' }">
          <sql-textarea v-model="content" @selection-change="handleSelection" @contextmenu="openMenu"></sql-textarea>
        </el-main>

        <!-- 拖动条 -->
        <div class="drag-bar" @mousedown="startDrag" :style="{ height: dragBarHeight + 'px'}"></div>

        <el-footer ref="footer" :style="{ height: footerHeight + 'px' }">
          Footer Content
        </el-footer>
      </el-container>

    </el-container>
    <context-menu ref="contextMenu" :items="menuItems" @menu-click="handleMenuClick"></context-menu>
  </div>
</template>
<script>
import ContextMenu from "@/components/context-menu.vue";
import SqlTextarea from "./sql-textarea.vue";
export default {
  components:{
    ContextMenu,
    SqlTextarea,
  },
  data(){
    return{
      dragBarHeight: 5,//拖动条高度
      mainHeight: 400,      // main初始高度
      footerHeight: 250,    // footer初始高度
      minMainHeight: 0,   // main最小高度
      minFooterHeight: 30, // footer最小高度
      isDragging: false,
      content:'',
      selectedText:'',
      menuItems: [
        { text: '▶执行', action: 'execAll' },
        { text: '▶执行选中的', action: 'execSelected' }
      ]
    }
  },
  mounted() {
    this.mainHeight = this.$refs.main.$el.offsetHeight; // main初始高度
    document.addEventListener('mousemove', this.handleDrag);
    document.addEventListener('mouseup', this.stopDrag);
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.handleDrag);
    document.removeEventListener('mouseup', this.stopDrag);
  },
  methods:{
    exec(){
      console.log(this.content)
    },
    execSelected(){
      console.log(this.selectedText)
    },
    // 右键事件
    openMenu(e){
      this.$refs.contextMenu.open(e)
    },
    handleMenuClick({ action }){
      if (action === 'execAll') {
        this.exec()
      }else if(action === 'execSelected'){
        this.execSelected()
      }
    },
    prependSql(text){
      let t = this.content
      this.content = text + t
    },
    handleSelection(v){
      this.selectedText = v;
    },
    startDrag(e) {
      this.isDragging = true;
      this.startY = e.clientY;
      this.startMainHeight = this.mainHeight;
      document.body.style.cursor = 'ns-resize';
      document.body.style.userSelect = 'none';
    },
    handleDrag(e) {
      if (!this.isDragging) {
        return
      }
      const deltaY = e.clientY - this.startY;
      const newMainHeight = this.startMainHeight + deltaY;

      // 计算可用空间
      const containerHeight = this.$el.clientHeight - this.dragBarHeight;
      const maxMainHeight = containerHeight - this.minFooterHeight;

      // 应用高度限制
      if (newMainHeight > this.minMainHeight && newMainHeight < maxMainHeight) {
        this.mainHeight = newMainHeight;
        this.footerHeight = containerHeight - newMainHeight;
      }
    },
    stopDrag() {
      this.isDragging = false;
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    }
  }
}
</script>
<style scoped lang="scss">
.right-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  ::v-deep .el-container{
    height: calc(100vh - 110px) !important;
  }

  ::v-deep .el-aside{
    // 减去
    //height: calc(100vh - 110px) !important;
    width: 30px !important;
    padding: 5px;
    text-align: center;
    border-top: 1px solid #DCDFE6;
    border-left: 1px solid #DCDFE6;
    border-bottom: 1px solid #DCDFE6;
    .btn-dev{
      .el-button{
        padding-top: 10px;
        padding-bottom: 10px;
        span{
          font-size: 20px;
        }
      }
    }
    .btn-dev:not(:last-child) {
      border-bottom: 1px solid #DCDFE6;
    }

  }

  ::v-deep .el-footer{
    border:  1px solid #DCDFE6;
  }


  .drag-bar {
    background-color: #f0f2f5;
    cursor: ns-resize;
    position: relative;
    z-index: 1;
    transition: background 0.3s;
  }
  .drag-bar:hover {
    background: #409EFF;
  }

}
</style>
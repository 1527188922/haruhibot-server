<template>
  <div class="right-panel">
    <el-container>
      <el-main ref="main" :style="{ height: mainHeight + 'px' }">
          <sql-textarea v-model="content" @selection-change="handleSelection"></sql-textarea>

      </el-main>

      <!-- 拖动条 -->
      <div class="drag-bar" @mousedown="startDrag" :style="{ height: dragBarHeight + 'px'}"></div>

      <el-footer ref="footer" :style="{ height: footerHeight + 'px' }">
        Footer Content
      </el-footer>
    </el-container>
  </div>
</template>
<script>
import SqlTextarea from "./sql-textarea.vue";
export default {
  components:{
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
      selectedText:''
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
    handleSelection(v){
      this.selectedText = v;
      console.log(this.content)
      console.log(this.selectedText)
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
}

.drag-bar {
  background: #e8e8e8;
  cursor: ns-resize;
  position: relative;
  z-index: 1;
  transition: background 0.3s;
}

.drag-bar:hover {
  background: #409EFF;
}

.el-main {
  border-bottom: 1px solid #ebeef5;
  overflow: auto;
}

.el-footer {
  border-top: 1px solid #ebeef5;
  //background: #f5f7fa;
  overflow: auto;
}
</style>
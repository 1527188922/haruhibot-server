<template>
  <div class="left-panel" :style="{width: leftWidthHolder.leftWidth + 'px'}">
    <el-tree
        :data="dbTree"
        node-key="id"
        :props="treeProps"
        @node-click="handleNodeClick"
        @node-contextmenu="handleContextMenu"
    >
      <div slot-scope="{node, data}" class="custom-tree-node">
        <span>{{ data.label }}</span>
        <el-dropdown v-if="data.type === 'table'" trigger="contextmenu">
          <!-- 右键菜单内容 -->
        </el-dropdown>
      </div>
    </el-tree>
  </div>
</template>
<script>
export default {
  props:{
    leftWidthHolder:{
      type: Object,
      default: {leftWidth:100}
    }
  },
  data(){
    return{
      dbTree: [],
      treeProps: {
        children: 'children',
        label: 'label'
      },
    }
  },
  methods:{
    handleContextMenu(event,data,node,component){
      console.log(event,data,node,component)
    },
    handleNodeClick(data,node,component){
      console.log(data,node,component)
    },
  }
}
</script>
<style scoped lang="scss">
.left-panel {
  height: 100%;
  overflow: auto;
  border-right: 1px solid #ebeef5;
}
</style>
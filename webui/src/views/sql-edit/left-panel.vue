<template>
  <div class="left-panel" :style="{width: leftWidthHolder.leftWidth + 'px'}">
    <div class="filter-input-container">
      <el-input size="small" clearable placeholder="输入关键字进行过滤" v-model="filterText">
      </el-input>
    </div>
    <div class="tree-container">
      <el-tree ref="tree" :data="fileNodes" :props="props" :load="loadNode" node-key="name" lazy
               highlight-current @node-click="handleNodeClick"  :filter-node-method="filterNode">
            <span class="alignment" slot-scope="{ node, data }">
               <span>
                 <div class="node-name">{{ data.name }}</div>
                 <span class="node-attribute" v-if="isColumn(data) && data.columnType">{{ data.columnType }}</span>
                 <span class="node-attribute" v-if="isColumn(data) && data.notnull === 1">NOT NULL</span>
                 <span class="node-attribute" v-if="isColumn(data) && data.defaultValue">{{ `DEFAULT ${data.defaultValue}` }}</span>
                 <span class="node-attribute" v-if="isColumn(data) && data.pk === 1">PK</span>
                 <span class="node-attribute" v-if="isIndex(data) && data.unique === 1">UNIQUE</span>
               </span>
              <span class="node-operation-btns">
                <el-button type="text"  size="mini" v-if="isTable(data)"
                           @click.stop="showDDL(data)">DDL</el-button>
                <!--                <el-button type="text" class="danger-text-btn" size="mini"-->
                <!--                           @click.stop="deleteClick(data,node)">删除</el-button>-->
              </span>
            </span>
      </el-tree>
    </div>
  </div>
</template>
<script>
import {databaseInfoNode} from "@/api/database";
export default {
  props:{
    leftWidthHolder:{
      type: Object,
      default: {leftWidth:0}
    }
  },
  data(){
    return{
      filterText:'',
      fileNodes: [],
      props: {
        label: 'name',
        isLeaf: 'leaf'
      },
    }
  },
  watch: {
    filterText(val) {
      this.$refs.tree.filter(val);
    }
  },
  methods:{
    async loadNode(node, resolve){
      // console.log(node)
      let {data:{data}} = await databaseInfoNode(node && node.data && node.data.length !== 0 ? node.data : {})
      resolve(data)
      this.$refs.tree.filter(this.filterText);
    },
    handleNodeClick(data,node,component){
      // console.log(data,node,component)
    },
    showDDL(data){

    },
    deleteClick(nodeData, node){

    },
    filterNode(value, data) {
      if (!value || value === '') return true;
      let fn = (str)=> {
        return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      }
      const regex = new RegExp(fn(value), 'i');
      return regex.test(data.name);
    },
    isColumn(data){
      if(!data){
        return false
      }
      return data.type === 'column'
    },
    isIndex(data){
      if(!data){
        return false
      }
      return data.type === 'index'
    },
    isTable(data){
      if(!data){
        return false
      }
      return data.type === 'table'
    }
  },
}
</script>
<style scoped lang="scss">
.left-panel {
  overflow: auto;
  border-right: 1px solid #ebeef5;
  padding-bottom: 10px;
  padding-left: 10px;
  padding-right: 10px;
}

.filter-input-container{
  position: sticky;
  top: 0;
  z-index: 2;
  background: white;
  padding-top: 10px;
}

.tree-container{

  .alignment {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    //padding-right: 8px;
  }
  .node-name{
    float: left;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 520px;
  }
  .node-attribute{
    margin-left: 10px;
    font-size: 12px;
    color: #C0C4CC;
  }
}


</style>
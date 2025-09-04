<template>
  <div class="left-panel" :style="{width: leftWidthHolder.leftWidth + 'px'}">
    <div class="filter-input-container">
      <el-input placeholder="输入关键字进行过滤" v-model="filterText" size="small">
        <el-button title="刷新" @click="refreshTable" slot="prepend" icon="el-icon-refresh"></el-button>
      </el-input>
    </div>
    <div class="tree-container">
      <el-tree ref="tree" :data="tableNodes" :props="props" :load="loadNode" node-key="key" lazy
               highlight-current @node-click="handleNodeClick"  :filter-node-method="filterNode"
               @node-contextmenu="nodeContextmenu">
            <span class="alignment" slot-scope="{ node, data }">
               <span>
                 <div class="node-name">{{ data.name }}</div>
                 <span class="node-attribute" v-if="isColumn(data) && data.columnType">{{ data.columnType }}</span>
                 <span class="node-attribute" v-if="isColumn(data) && data.notnull === 1">NOT NULL</span>
                 <span class="node-attribute" v-if="isColumn(data) && data.defaultValue">{{ `DEFAULT ${data.defaultValue}` }}</span>
                 <span class="node-attribute" v-if="isColumn(data) && data.pk === 1">PK</span>
                 <span class="node-attribute" v-if="isIndex(data) && data.unique === 1">UNIQUE</span>
               </span>
<!--              <span class="node-operation-btns">-->
<!--                <el-button type="text"  size="mini" v-if="isTable(data)"-->
<!--                           @click.stop="showDDL(data)">DDL</el-button>-->
<!--                <el-button type="text" class="danger-text-btn" size="mini"-->
<!--                           @click.stop="deleteClick(data,node)">删除</el-button>-->
<!--              </span>-->
            </span>
      </el-tree>
    </div>
    <context-menu ref="contextMenu" :items="menuItems" @menu-click="handleMenuClick"></context-menu>
    <import-data-dialog ref="importDataDialog"></import-data-dialog>
  </div>
</template>
<script>
import {databaseInfoNode, databaseDDL, execAndExport as execAndExportApi} from "@/api/database";
import ContextMenu from "@/components/context-menu.vue";
import {downloadFileUrl} from "@/api/system";
import {downloadLink} from "@/util/util";
import ImportDataDialog from "./import-data-dialog.vue";
export default {
  components:{
    ContextMenu,
    ImportDataDialog
  },
  props:{
    leftWidthHolder:{
      type: Object,
      default: {leftWidth:0}
    }
  },
  data(){
    return{
      filterText:'',
      tableNodes: [],
      menuItems: [
        this.ddlMenuItem(),
        this.refreshMenuItem(),
        this.exportMenuItem(),
        this.importMenuItem()
      ],
      props: {
        label: 'name',
        isLeaf: 'leaf'
      },
    }
  },
  computed: {
    rightPanel(){
      return this.$parent.$parent.$parent.$refs.rightPanel
    }
  },
  watch: {
    filterText(val) {
      this.$refs.tree.filter(val);
    }
  },
  methods:{
    async refreshTable(){
      this.tableNodes = await this.requestNodes(null)
    },
    importMenuItem(){
      return { text: '导入数据', action: 'import',icon:'el-icon-upload2' }
    },
    exportMenuItem(){
      return { text: '导出数据', action: 'export',icon:'el-icon-download' }
    },
    ddlMenuItem(){
      return { text: 'DDL', action: 'DDL',icon:'el-icon-view' }
    },
    refreshMenuItem(){
      return { text: '刷新', action: 'refresh',icon:'el-icon-refresh' }
    },
    // 右键节点 点击item事件
    handleMenuClick({ action }, {data,node}){
      if (action === 'DDL') {
        this.showDDL(data)
      }else if(action === 'refresh'){
        node.loaded = false
        node.expand()
      }else if(action === 'export'){
        this.execAndExport(`SELECT * FROM ${data.tableName};`,data.tableName)
      } else if(action === 'import'){
        this.$refs.importDataDialog.open(data)
      }
    },
    execAndExport(sql, tableName){
      const loading = this.$loading({ lock: true,  text: 'Loading', spinner: 'el-icon-loading'});
      execAndExportApi({sql,tableName}).then(({data:{code,data,message},headers})=>{
        if (code !== 200) {
          return this.$message.error(message)
        }
        const contentDisposition = headers['content-disposition']
        let fileName = `${tableName}_`+new Date().getTime()+'.xlsx'
        if (contentDisposition) {
          const fileNameMatch = contentDisposition.match(/filename=(.+)/)
          if (fileNameMatch.length > 1) {
            fileName = decodeURIComponent(fileNameMatch[1].replace(/"/g, ''))
          }
        }
        let h = downloadFileUrl(encodeURI(data))
        downloadLink(h, fileName)
      }).catch(e =>{
        if(e.message){
          return this.$message.error(e.message)
        }
        this.$message.error('导出异常')
      }).finally(()=>{
        loading.close()
      })
    },
    async loadNode(node, resolve){
      let data = await this.requestNodes(node)
      resolve(data)
      this.$refs.tree.filter(this.filterText);
    },
    async requestNodes(node){
      let {data:{data}} = await databaseInfoNode(node && node.data && node.data.length !== 0 ? node.data : {})
      return data
    },
    // 右键节点事件
    nodeContextmenu(event,data,node,component){
      this.$refs.tree.setCurrentKey(data.key);
      if(data.leaf){
        this.$refs.contextMenu.close()
        return
      }
      if(data.type === 'table'){
        this.$refs.contextMenu.open({event, data:{data,node}})
      }
      if (data.type === 'fixed') {
        this.$refs.contextMenu.open({event, data:{data,node}},[this.refreshMenuItem()])
      }
    },
    handleNodeClick(data,node,component){
      this.$refs.contextMenu.close()
    },
    showDDL(data){
      // this.rightPanel.prependSql(data.sql+'\n\n')

      const loading = this.$loading({ lock: true,  text: 'Loading', spinner: 'el-icon-loading'});
      databaseDDL(data.tableName).then(({data:{data}})=>{
        this.rightPanel.prependSql(data+'\n\n')
      }).catch(e=>{
        console.log('err',e)
      }).finally(()=>{
        loading.close()
      })
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
  //overflow: auto;
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
<template>
  <div id="SystemFile">
    <el-row>
      <el-col>
        <basic-container :show-header="true">
          <div slot="header" class="">
            <div>
              <el-input size="small" clearable class="filter-input" placeholder="输入关键字进行过滤" v-model="filterText">
                <el-select v-model="rootType" slot="prepend" placeholder="请选择" @change="rootTypeChange">
                  <el-option v-for="(value,key) in rootTypeMap" :key="key" :label="value" :value="key"></el-option>
                </el-select>
<!--                <el-button slot="append" icon="el-icon-search"></el-button>-->
              </el-input>

            </div>
            <span class="app-dir alignment">
              <span class="">
                {{rootDir}}
              <span class="file-attribute" v-if="showSize(rootDirTotalSize)">{{rootDirTotalSize | fileSizeFormatter}}</span>
              </span>
            </span>
          </div>
          <el-tree ref="tree" :data="fileNodes" :props="props" :load="loadNode" node-key="absolutePath" lazy
              highlight-current @node-click="handleNodeClick"  :filter-node-method="filterNode"
                   @node-contextmenu="nodeContextmenu">
            <span class="alignment" slot-scope="{ node, data }">
               <span>
                 <div :title="node.label" class="file-name">{{ node.label }}</div>
                 <span class="file-attribute" v-if="showSize(data.size)">{{ data.size | fileSizeFormatter }}</span>
                 <span class="file-attribute" v-if="showChildCount(data)">{{ data.childCount | childCountFormatter }}</span>
               </span>
              <span class="node-operation-btns">
<!--                <el-button type="text" size="mini" @click.stop="preview(data)" v-if="data.showPreview">编辑</el-button>-->
<!--                <el-button type="text" size="mini" @click.stop="downloadFile(data)" v-if="!data.isDirectory">下载</el-button>-->
<!--                <el-button type="text" class="success-text-btn" size="mini"-->
<!--                           @click.stop="openDetailDialog(data,node)">详情</el-button>-->
                <el-button type="text" class="danger-text-btn" size="mini" v-if="data.showDel"
                           @click.stop="deleteFile(data,node)">删除</el-button>
              </span>
            </span>
          </el-tree>
        </basic-container>
      </el-col>
    </el-row>

    <detail-dialog ref="DetailDialog"></detail-dialog>
    <drawer-preview direction="ltr" ref="drawerPreview"></drawer-preview>
    <context-menu ref="contextMenu" @menu-click="handleMenuClick"></context-menu>
  </div>
</template>
<script>
import {findFileNodes,deleteFile as deleteFileApi,downloadFileUrl} from "@/api/system";
import DrawerPreview from "./drawer-preview";
import DetailDialog from "./detail-dialog";
import ContextMenu from "@/components/context-menu.vue";
import {fileSizeFormatter} from "@/util/util";
import {downloadLink} from "@/util/util";

export default {
  components:{
    DrawerPreview,
    DetailDialog,
    ContextMenu
  },
  name:'SystemFile',
  data(){
    return{
      rootType:'2',
      filterText:'',
      fileNodes:[],
      src:'',
      rootDir:'',
      rootDirTotalSize:0,
      props:{
        label: 'fileName',
        isLeaf: 'leaf'
      },
      rootTypeMap:{
        '1':'系统根目录',
        '2':'BOT程序根目录'
      }
    }
  },
  created() {

  },
  watch: {
    filterText(val) {
      this.$refs.tree.filter(val);
    }
  },
  mounted() {
  },
  methods:{
    refreshMenuItem(){
      return { text: '刷新', action: 'refresh', icon:'el-icon-refresh',style:{color:'#67c23a'}}
    },
    detailMenuItem(){
      return { text: '详情', action: 'detail', icon:'el-icon-info',style:{color:'#909399'} }
    },
    deleteMenuItem(){
      return { text: '删除', action: 'delete', icon:'el-icon-delete',style:{color:'#F56C6CFF'} }
    },
    downloadMenuItem(){
      return { text: '下载', action: 'download', icon:'el-icon-download' }
    },
    previewMenuItem(){
      return { text: '编辑', action: 'preview', icon:'el-icon-edit-outline',style:{color:'#409EFF'} }
    },
    filterNode(value, data) {
      if (!value) return true;
      let fn = (str)=> {
        return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      }
      const regex = new RegExp(fn(value), 'i');
      return regex.test(data.fileName);
    },
    preview(nodeData){
      this.$refs.drawerPreview.open(nodeData)
    },
    async rootTypeChange(v){
      this.$refs.contextMenu.close()
      let data = await this.requestNodes({},v)
      this.rootDir = data.rootDir
      this.fileNodes = data.nodes
      this.rootDirTotalSize = data.rootDirTotalSize
    },
    async loadNode(node, resolve){
      let data = await this.requestNodes(node  && node.data && node.data.length !== 0 ? node.data : {},this.rootType)
      this.rootDir = data.rootDir
      this.rootDirTotalSize = data.rootDirTotalSize
      resolve(data.nodes)
    },
    async requestNodes(request,rootType){
      let {data:{data,code}} = await findFileNodes(request,rootType)
      return data
    },
    handleMenuClick({action}, {data,node}){
      if (action === 'refresh') {
        node.loaded = false
        node.expand()
      }else if(action === 'preview'){
        this.preview(data)
      }else if(action === 'download'){
        this.downloadFile(data)
      }else if(action === 'detail'){
        this.openDetailDialog(data,node)
      }else if(action === 'delete'){
        this.deleteFile(data,node)
      }
    },
    // 右键节点事件
    nodeContextmenu(event,data,node,component){
      this.$refs.tree.setCurrentKey(data.absolutePath);
      let v = {event,data:{data,node}}
      let menuItems = []
      if (!data.leaf) {
        menuItems.push(this.refreshMenuItem())
      }
      if (data.showPreview) {
        menuItems.push(this.previewMenuItem())
      }
      if (!data.isDirectory) {
        menuItems.push(this.downloadMenuItem())
      }
      menuItems.push(this.detailMenuItem())
      if (data.showDel) {
        menuItems.push(this.deleteMenuItem())
      }
      this.$refs.contextMenu.open(v,menuItems)
    },
    handleNodeClick(data, node){
      this.$refs.contextMenu.close()
    },
    showSize(size){
      return size || size === 0
    },
    showChildCount(nodeData){
      return nodeData.isDirectory && (nodeData.childCount || nodeData.childCount === 0)
    },
    removeNode(nodeData,node){
      let parent = node.parent
      if(parent){
        let children = parent.childNodes
        if(children){
          let index = children.findIndex(d => d.data.absolutePath === nodeData.absolutePath)
          if(index !== -1){
            children.splice(index, 1)
          }
        }
      }
    },
    openDetailDialog(nodeData,node){
      this.$refs.DetailDialog.open(nodeData)
    },
    downloadFile(data){
      downloadLink(downloadFileUrl(encodeURI(data.absolutePath)), data.fileName)
    },
    deleteFile(nodeData,node){
      let msg = '请输入WEB UI登录密码'
      this.$prompt(`<span style="color: red">确认删除${nodeData.isDirectory ? '目录':'文件'}：</span><br/><div title="${nodeData.absolutePath}" style="white-space: nowrap;overflow: hidden;text-overflow: ellipsis;min-width: 100px">${nodeData.absolutePath}</div>`, '提示', {
        type:'warning',
        dangerouslyUseHTMLString:true,
        closeOnClickModal:false,
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType:'password',
        inputPlaceholder:msg,
        inputErrorMessage:msg,
        inputValidator: (value) => {
          if (!value || !value.trim()) return false
          if(value.length > 100) return '字符过长'
          return true
        },
        beforeClose:(action, instance, done)=>{
          if('cancel' === action) {
            // 取消
            done()
            return
          }
          let inputValue = instance._data.inputValue
          deleteFileApi(nodeData,this.rootType,inputValue).then(({data:{code,message}})=>{
            if(code !== 200){
              return this.$message.error(message)
            }
            this.removeNode(nodeData, node)
            this.$message.success(message)
            done()
          })
        }
      }).then(({ value }) => {
        // this.rootTypeChange(this.rootType)
      })
    }
  },
  filters:{
    fileSizeFormatter(size){
      return fileSizeFormatter(size)
    },
    childCountFormatter(count){
      return `${count}个项目`
    }
  }
}
</script>
<style lang="scss" scoped>
#SystemFile{
  .app-dir{
    font-weight: bold;
    font-size: 14px;
  }
  .alignment {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    //padding-right: 8px;
  }
  .file-name{
    float: left;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    //max-width: 520px;
  }
  .file-attribute{
    margin-left: 10px;
    font-size: 12px;
    color: #C0C4CC;
  }
  //.dir-label{
  //  color: #409EFFFF;
  //}
  .filter-input{
    max-width: 350px;
    ::v-deep .el-input-group__prepend{
      background-color: white;
    }
    ::v-deep .el-input {
      width: 140px;
    }
  }
}
</style>
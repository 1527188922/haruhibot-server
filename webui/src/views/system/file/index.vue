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
            <span class="app-dir custom-tree-node">
              <span class="">
                {{rootDir}}
              <span class="file-size" v-if="showSize(rootDirTotalSize)">{{rootDirTotalSize | fileSizeFormatter}}</span>
              </span>
            </span>
          </div>
          <el-tree ref="tree" :data="fileNodes" :props="props" :load="loadNode" node-key="absolutePath" lazy
              highlight-current @node-click="handleNodeClick"  :filter-node-method="filterNode">
            <span class="custom-tree-node" slot-scope="{ node, data }">
               <span :class="data.isDirectory ? 'dir-label' : ''" :title="node.label">
                 {{ node.label }}
                 <span class="file-size" v-if="showSize(data.size)">{{ data.size | fileSizeFormatter }}</span>
               </span>
              <span class="node-operation-btns">
                <el-button type="text" size="mini" @click.stop="preview(data)" v-if="data.showPreview">预览</el-button>
                <el-button type="text" class="danger-text-btn" size="mini" v-if="data.showDel"
                           @click.stop="deleteFile(data,node)">删除</el-button>
              </span>
            </span>
          </el-tree>
        </basic-container>
      </el-col>
    </el-row>

    <drawer-preview direction="ltr" ref="drawerPreview"></drawer-preview>
  </div>
</template>
<script>
import {findFileNodes,deleteFile as deleteFileApi} from "@/api/system";
import DrawerPreview from "./drawer-preview";
export default {
  components:{
    DrawerPreview
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
    handleNodeClick(data, node){
      // console.log('handleNodeClick',data,node)
    },
    showSize(size){
      return size || size === 0
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
      let kb = size / 1024
      if(kb > 1024){
        let mb = kb / 1024
        if(mb > 1024){
          return (Math.floor((mb / 1024) * 100) / 100) + 'GB';
        }
        return (Math.floor((kb / 1024) * 100) / 100) + 'MB';
      }
      return (Math.floor(kb * 100) / 100)+'KB';
    },
  }
}
</script>
<style lang="scss" scoped>
#SystemFile{
  .app-dir{
    font-weight: bold;
    font-size: 14px;
  }
  .custom-tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    //padding-right: 8px;
  }
  .file-size{
    margin-left: 10px;
    font-size: 12px;
    color: #C0C4CC;
  }
  .dir-label{
    color: #409EFFFF;
  }
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
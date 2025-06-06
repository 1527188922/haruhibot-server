<template>
  <div id="SystemFile">
    <el-row>
      <el-col :span="9">
        <basic-container :show-header="true">
          <div slot="header" class="custom-tree-node">
            <span class="app-dir">
              {{rootDir}}
              <span class="file-size">{{rootDirTotalSize | fileSizeFormatter}}</span>
            </span>
          </div>
          <el-tree ref="tree" :data="fileNodes" :props="props" :load="loadNode" node-key="absolutePath" lazy
              highlight-current @node-click="handleNodeClick">
            <span class="custom-tree-node" slot-scope="{ node, data }">
               <span :class="data.isDirectory ? 'dir-label' : ''">
                 {{ node.label }}
                 <span class="file-size">{{ data.size | fileSizeFormatter }}</span>
               </span>
              <span>
                <el-button type="text" size="mini" @click="preview(data)" v-if="data.showPreview">预览</el-button>
              </span>
            </span>
          </el-tree>
        </basic-container>
      </el-col>
      <el-col :span="15">
        <template v-if="content && content !== ''">
          <basic-container v-loading="readLoading" :show-header="true">
            <div slot="header" class="app-dir">
              {{currentPreviewNodeData.fileName}}
              <span class="file-size">{{ currentPreviewNodeData.size | fileSizeFormatter }}</span>
            </div>
            <pre>{{content}}</pre>
          </basic-container>
        </template>

      </el-col>

    </el-row>


  </div>
</template>
<script>
import {findFileNodes,readFileContent,downloadFile} from "@/api/system";

export default {
  name:'SystemFile',
  data(){
    return{
      fileNodes:[],
      src:'',
      readLoading:false,
      rootDir:'',
      content:'',
      rootDirTotalSize:0,
      currentPreviewNodeData:null,
      props:{
        label: 'fileName',
        isLeaf: 'leaf'
      }
    }
  },
  created() {

  },
  mounted() {
    //
    // fetch('http://127.0.0.1:8090/application.yml')
    //     .then(response => {
    //       // 1. 强制读取为二进制数据
    //       return response.blob();
    //     })
    //     .then(blob => {
    //       // 2. 创建临时 URL
    //       const url = URL.createObjectURL(blob);
    //
    //       // 3. 注入 iframe
    //       const iframe = document.getElementById('previewFrame');
    //       iframe.src = url;
    //     });
  },
  methods:{
    preview(nodeData){
      this.readLoading = true
      readFileContent(nodeData).then(({data:{code,data}})=>{
        this.content = data
        this.currentPreviewNodeData = nodeData
      }).catch(e=>{
        this.content = ''
        this.currentPreviewNodeData = null
      }).finally(()=>{
        this.readLoading = false
      })
    },

    async loadNode(node, resolve){
      let {data:{data,code}} = await findFileNodes(node  && node.data && node.data.length !== 0 ? node.data : {})
      this.rootDir = data.rootDir
      this.rootDirTotalSize = data.rootDirTotalSize
      resolve(data.nodes)
    },
    handleNodeClick(data, node){
      console.log('handleNodeClick',data,node)
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
}
</style>
<template>
  <div id="SystemFile">
    <el-row>
      <el-col :span="9" class="sys-file-left">
        <basic-container>
          <div class="app-dir">
            {{appDir}}
          </div>
          <el-divider/>
          <el-tree ref="tree" :data="fileNodes" :props="props" :load="loadNode" node-key="absolutePath" lazy
              highlight-current @node-click="handleNodeClick">
            <span class="custom-tree-node" slot-scope="{ node, data }">
               <span>{{ node.label }}</span>
              <span>
<!--                <el-button type="text" size="mini">Append</el-button>-->
                <el-button type="text" size="mini" @click="preview(data)" v-if="data.showPreview">预览</el-button>
              </span>
            </span>
          </el-tree>
        </basic-container>
      </el-col>
      <el-col :span="15">
        <basic-container v-loading="readLoading">
          <pre>{{content}}</pre>
        </basic-container>
      </el-col>

    </el-row>


  </div>
</template>
<script>
import {findFileNodes,readFileContent} from "@/api/system";

export default {
  name:'SystemFile',
  data(){
    return{
      fileNodes:[],
      readLoading:false,
      appDir:'',
      content:'',
      props:{
        label: 'fileName',
        isLeaf: 'leaf'
      }
    }
  },
  created() {

  },
  mounted() {

  },
  methods:{
    preview(data){
      this.readLoading = true
      readFileContent({path:data.absolutePath}).then(({data:{code,data}})=>{
        this.content = data
      }).catch(e=>{
        this.content = ''
      }).finally(()=>{
        this.readLoading = false
      })
    },
    async loadNode(node, resolve){
      let {data:{data,code}} = await findFileNodes({parentPath: node && node.data ? node.data.absolutePath : null})
      this.appDir = data.appDir
      resolve(data.nodes)
    },
    handleNodeClick(data, node){
      // console.log('handleNodeClick',data,node)
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
  .sys-file-left{
    //min-width: 300px;
    overflow-x: auto;
  }
  .custom-tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 14px;
    padding-right: 8px;
  }
}
</style>
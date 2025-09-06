<template>
  <div id="DrawerPreview">
    <el-drawer
        :visible.sync="drawer"
        :direction="direction"
        :destroy-on-close="true"
        size="70%"
        @closed="closed">
      <div slot="title" v-loading="saveLoading">
        <div class="" >
          <span>{{nodeData.fileName}}</span>
          <span style="margin-left: 10px">
            <template v-if="editable">
              <el-button type="text" icon="el-icon-close" size="small" @click="cancel">取消</el-button>
              <el-popconfirm title="确认保存文件内容？"
                             @confirm="submit">
                <el-button class="success-text-btn success-text" slot="reference"
                           type="text" icon="el-icon-check" size="small">保存</el-button>
              </el-popconfirm>

            </template>
            <template v-else>
              <el-button type="text" size="small" icon="el-icon-edit" @click="edit">编辑</el-button>
            </template>
          </span>
        </div>
        <div class="info">
          <span>
            <span class="value">{{ content.length }}</span>个字符
          </span>
          <span>
            <span class="value">{{ content | countLines }}</span>行
          </span>
        </div>
      </div>
      <div style="padding: 0 20px;min-height: 100px" v-loading="readLoading" >
        <template v-if="editable">
          <el-input type="textarea" :autosize="{ minRows: 3}" v-model="content"></el-input>
        </template>
        <template v-else>
          <pre style="margin-top: 0">{{content}}</pre>
        </template>
      </div>
    </el-drawer>
  </div>
</template>
<script>
import {readFileContent,saveFile} from "@/api/system";

export default {
  name:'DrawerPreview',
  props:{
    direction:{
      type:String,
      default:'rtl'
    },
  },
  data(){
    return{
      readLoading:false,
      saveLoading:false,
      drawer: false,
      content:'',
      nodeData: {
        fileName:''
      },
      editable:false,
      oldValue:''
    }
  },
  created() {
  },
  filters:{
    countLines(str){
      return !str || str === "" ? 0 : str.split(/\r\n|\r|\n/).length
    }
  },
  methods:{
    submit(){
      this.saveLoading = true
      saveFile({
        ...this.nodeData,
        content:this.content
      }).then(({data:{code,message}})=>{
        if(code !== 200){
          return this.$message.error(message)
        }
        this.$message.success(message)
        this.oldValue = ''
        this.editable = false
      }).catch(e=>{
        this.$message.error(e.message)
      }).finally(()=>{
        this.saveLoading = false
      })
    },
    cancel(){
      this.editable = false
      this.content = this.oldValue
      this.oldValue = ''
    },
    edit(){
      this.editable = true
      this.oldValue = this.content
    },
    open(nodeData){
      this.drawer = true
      this.$nextTick(()=>{
        this.nodeData = nodeData
        this.load(nodeData)
      })
    },
    closed(){
      this.editable = false
      this.content = ''
      this.oldValue = ''
      this.nodeData = {
        fileName:''
      }
    },
    load(nodeData){
      this.readLoading = true
      readFileContent(nodeData).then(({data:{code,data}})=>{
        this.content = data
      }).catch(e=>{
      }).finally(()=>{
        this.readLoading = false
      })
    }
  }
}
</script>
<style lang="scss" scoped>
#DrawerPreview{
  pre{
    font-family: "Consolas", "Courier New", monospace;
  }
  .operation-btns{
    font-weight: bold;
  }
  ::v-deep .el-textarea__inner{
    padding: 0;
    font-family: "Consolas", "Courier New", monospace;
  }
  .info{
    font-size: 12px;
    .value{
      font-weight: bold;
      color: #409EFF;
    }
  }
}
</style>
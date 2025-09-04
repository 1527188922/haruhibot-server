<template>
  <div class="import-data-dialog">
    <el-dialog :title="title" :visible.sync="dialogVisible" width="500px" v-dialogDrag :close-on-click-modal="false"
                @closed="dialogClosed">
      <el-row>
        <el-tooltip class="item" effect="dark" placement="top">
          <div slot="content">
            仅上传xls、xlsx文件
            <br/>数据放置在第一个sheet
          </div>
          <el-button size="small" class="select-btn" @click="btnClick" :title="fileName" :disabled="submitLoading">
            <div class="btn-span">{{fileName || '选择文件'}}</div>
          </el-button>
        </el-tooltip>

        <input type="file" :value="fileName" ref="importFileInput" style="display: none"
                @change="fileInputChange"
                   accept=".csv, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/vnd.ms-excel"/>
      </el-row>
      <el-row v-if="errorMsg">
        <ul class="danger-text err-ul">
          <li v-for="message in errorMsg.split(';')">
            {{message}}
          </li>
        </ul>
      </el-row>

      <span slot="footer">
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button size="small" :loading="submitLoading" type="primary" @click="submit" :disabled="!targetFile">导入</el-button>
      </span>
    </el-dialog>
  </div>
</template>
<script>
import {importExcel} from "@/api/database";
import {deepClone} from "@/util/util";
export default {
  data(){
    return{
      dialogVisible:false,
      title:'',
      errorMsg:'',
      submitLoading:false,
      fileName:'',
      targetFile:null,
      nodeData:null
    }
  },
  methods:{
    open(nodeData){
      this.title = '导入数据：'+nodeData.tableName || '导入数据'
      this.dialogVisible = true
      this.$nextTick(()=>{
        this.nodeData = deepClone(nodeData)
      })
    },
    async submit(){
      if(!this.targetFile){
        this.$message.warning('请选择Excel文件')
        return
      }
      this.submitLoading = true
      try{
        const {data:{code,message}} = await importExcel(this.targetFile, this.nodeData.tableName)
        if(code !== 200){
          this.$message.error('导入异常')
          this.errorMsg = message
          return
        }
        this.$message.success(message)
        this.dialogVisible = false
      }catch (e) {
        this.$message.error('导入异常')
        this.errorMsg = e.message
      }finally {
        this.submitLoading = false
      }
    },
    btnClick(){
      this.targetFile = null
      this.fileName = ''
      this.clearErr()
      this.$refs.importFileInput.click()
    },
    fileInputChange(e){
      this.fileName = e.target.value
      this.targetFile = e.target.files[0]
    },
    clearErr(){
      this.errorMsg = ''
    },
    dialogClosed(){
      this.clearErr()
      this.fileName = ''
      this.title = ''
      this.targetFile = null
      this.nodeData = null
    }
  }
}
</script>
<style scoped lang="scss">
.import-data-dialog{
  ::v-deep .el-dialog__body{
    padding-top: 15px;
    padding-bottom: 0;
  }
  .select-btn{
    max-width: 100%;
    .btn-span{
      white-space: nowrap; /* 超出的空白区域不换行 */
      overflow: hidden; /* 超出隐藏 */
      text-overflow: ellipsis; /* 文本超出显示省略号 */
      max-width: 100% !important;
    }
  }
  .err-ul{
    padding-left: 20px;
  }
}

</style>
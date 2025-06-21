<template>
  <div id="DetailDialog">
    <el-dialog :visible.sync="visible" title="文件详细信息" width="650px" @closed="dialogClosed" v-dialogDrag>
      <el-form :model="nodeData" class="detail-form" label-width="115px">
        <el-form-item label="绝对路径：">
          {{nodeData.absolutePath}}
        </el-form-item>
        <el-form-item label="大小：" v-if="nodeData.size || nodeData.size === 0">
          {{nodeData.size | fileSizeFormatter}}
        </el-form-item>
        <el-form-item label="包含项目数：" v-if="nodeData.isDirectory && (nodeData.childCount || nodeData.childCount === 0)">
          {{ nodeData.childCount }}
        </el-form-item>
        <el-form-item label="最近修改时间：" v-if="nodeData.lastModified">
          {{ $dayjs(nodeData.lastModified).format(pattern) }}
        </el-form-item>
        <el-form-item label="创建时间：" v-if="nodeData.createTime">
          {{ $dayjs(nodeData.createTime).format(pattern) }}
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="visible = false"
                   icon="el-icon-circle-close">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import {fileSizeFormatter} from "@/util/util";
export default {
  name:'DetailDialog',
  data(){
    return{
      pattern:'YYYY-MM-DD HH:mm:ss.SSS',
      visible:false,
      nodeData:{}
    }
  },
  methods:{
    open(nodeData){
      console.log(nodeData)
      this.visible = true
      this.$nextTick(()=>{
        this.nodeData = nodeData
      })
    },
    dialogClosed(){
      this.nodeData = {}
    }
  },
  filters:{
    fileSizeFormatter(size){
      return fileSizeFormatter(size)
    }
  }
}
</script>
<style lang="scss" scoped>
#DetailDialog{
  .detail-form{
    ::v-deep .el-form-item{
      margin-bottom: 0;
    }
  }
}
</style>
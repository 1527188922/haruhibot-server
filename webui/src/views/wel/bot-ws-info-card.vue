<template>
  <basic-container class="block botws-card" shadow="always" :show-header="true" v-loading="botWsInfoLoading">
    <template slot="header">
      <div class="title">
        BOT WebSocket服务
      </div>
    </template>
    <el-form :model="botWsInfo" label-width="85px">
      <el-form-item label="状态"  class="info-form-item">
        <el-tag  effect="dark" size="mini" :type="botWsInfo.running ? 'success' : 'danger'">{{botWsInfo.running ? '运行中' : '已停止'}}</el-tag>
      </el-form-item>
      <el-form-item label="连接数" class="info-form-item">
        {{botWsInfo.connections}}
      </el-form-item>
      <el-form-item label="最大连接数" class="info-form-item">
        {{botWsInfo.maxConnections}}
      </el-form-item>
      <el-form-item label="地址路径" class="info-form-item">
        {{botWsInfo.path}}
      </el-form-item>
      <el-form-item label="token" class="info-form-item">
        {{botWsInfo.accessToken}}
      </el-form-item>
    </el-form>
    <div class="bows-card-footer">
      <el-button type="success" size="small" plain @click="start">启动</el-button>
      <el-button type="danger" size="small" plain @click="stop">停止</el-button>
      <el-button type="primary" size="small" plain @click="getBotWsInfo">刷新</el-button>
    </div>
  </basic-container>
</template>
<script>
import {botWsInfo as botWsInfoApi,botWsOperation} from "@/api/system";

export default {
  data(){
    return{
      botWsInfoLoading:false,
      botWsInfo:{}
    }
  },
  created () {
    this.getBotWsInfo()
  },
  methods:{
    stop(){
      this.$confirm('确认逻辑停止BOT WebSocket服务?（清空并断开所有BOT WebSocket连接）', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async ()=>{
        let {data:{data,code,message}} = await botWsOperation('2')
        if(code !== 200){
          this.$message.error(message)
          return
        }
        this.botWsInfo = data
        this.$message.success(message)
      })
    },
    async start(){
      let {data:{data,code,message}} = await botWsOperation('1')
      if(code !== 200){
        this.$message.error(message)
        return
      }
      this.botWsInfo = data
      this.$message.success(message)
    },
    getBotWsInfo(){
      this.botWsInfoLoading = true
      botWsInfoApi().then(({data:{data,code}})=>{
        this.botWsInfo = data
      }).finally(()=>{
        this.botWsInfoLoading = false
      })
    }
  }
}
</script>
<style lang="scss" scoped>
.botws-card{
  ::v-deep .el-card__body{
    position: relative;
    .bows-card-footer{
      position: absolute;
      /* 抵消父容器padding */
      bottom: 15px;
      left: 15px;
      right: 15px;
      text-align: center;
    }
  }
}
</style>
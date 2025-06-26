<template>
  <div id="index">
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


<!--    <basic-container class="block" shadow="always" :show-header="true" v-loading="botWsInfoLoading">-->
<!--      <template slot="header">-->
<!--        <div class="title">-->
<!--          BOT WebSocket服务-->
<!--        </div>-->
<!--      </template>-->
<!--    </basic-container>-->

  </div>
</template>

<script>
import {botWsInfo as botWsInfoApi,botWsOperation} from "@/api/system";

export default {
  name: "wel",
  data () {
    return {
      botWsInfoLoading:false,
      botWsInfo:{}
    }
  },
  computed: {
  },
  mounted() {

  },
  created () {
    this.getBotWsInfo()
  },
  methods: {
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
};
</script>

<style scoped lang="scss">
#index {
  .block{
    display: inline-block;
    vertical-align: top;
    padding: 0;
    margin: 0 0 10px 15px;
    width: 300px;
    ::v-deep .el-card{
      min-height: 350px;
      .el-card__body{
        min-height: calc(350px - 30px - 60px);
      }
    }
  }
  .title{
    margin-bottom: 0;
    text-align: center;
    font-weight: bold;
    font-size: 15px;
  }
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

}
</style>

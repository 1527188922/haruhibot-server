<template>
  <div class="tail-log" >
    <div class="log-content" ref="logContent">
      <div v-for="(log, index) in logs" :key="index" class="log-line" :class="getLogClass(log)">
        {{ log }}
      </div>
    </div>
  </div>

</template>
<script>

import {createLogSSEClient} from "@/api/log";
import {PATH_LOG_MONITOR} from "@/const/const";
export default {
  data() {
    return {
      sseClient: null,
      logs: []
    }
  },
  mounted() {
    this.initSSEClient()
  },
  watch: {
    logs(newVal) {
      this.autoScrollToBottom()
    },
    // 更精确地只监听 path 的变化
    '$route.path'(newPath, oldPath) {
      if(PATH_LOG_MONITOR === newPath){
        if(!this.sseClient){
          this.initSSEClient()
        }
      }else{
        this.closeSSEClient();
      }
    }
  },
  methods:{
    initSSEClient(){
      this.closeSSEClient()
      this.sseClient = createLogSSEClient(50,this.handleLineLog)
      this.sseClient.connect()
    },
    closeSSEClient(){
      this.logs = []
      if(this.sseClient){
        this.sseClient.close()
      }
      this.sseClient = null
    },
    handleLineLog({data,id,event}){
      if(!data || data.length === 0){
        return
      }
      for (let i = 0; i < data.length; i++) {
        let d = data[i];
        if(!d){
          continue;
        }
        let obj = JSON.parse(d)
        this.logs.push(obj.data)
      }
    },
    // 自动滚动到底部
    autoScrollToBottom() {
      const logContent = this.$refs.logContent;
      if (!logContent) return;

      // 计算滚动条位置是否接近底部（允许(20 + 10)px的误差）
      const isNearBottom = logContent.scrollTop + logContent.clientHeight >=
          logContent.scrollHeight - (20 + 10);
      // const isNearBottom = true;

      if (isNearBottom) {
        this.$nextTick(() => {
          logContent.scrollTop = logContent.scrollHeight;
        });
      }
    },
    getLogClass(log) {
      if (log.includes('ERROR') || log.includes('error')) return 'danger-text';
      if (log.includes('WARN') || log.includes('warn')) return 'warning-text';
      if (log.includes('INFO') || log.includes('info')) return 'info-text2';
      if (log.includes('DEBUG') || log.includes('debug')) return 'log-debug';
      return 'info-text2';
    }
  },
  beforeDestroy() {
    this.closeSSEClient()
  }
}

</script>
<style scoped lang="scss">
.tail-log{
  display: flex;
  min-height: 100%;
  min-width: 100%;
  background-color: #FFF;
  .log-content{

    background-color: #1e1e1e;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 14px;
    line-height: 1.4;

    max-height: calc(100vh - 50px - 40px - 10px - 10px);
    min-width: 100%;
    overflow-y: auto;
    .log-line {
      word-break: break-all;
      padding-left: 10px;
      padding-top: 2px;
      padding-bottom: 2px;
    }

    .log-debug {
      color: #a78bfa;
    }

  }
}
</style>
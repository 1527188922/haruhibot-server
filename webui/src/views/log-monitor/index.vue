<template>
  <basic-container class="tail-log" >
    <code class="log-content">
      <div v-for="(log, index) in logs" :key="index" class="log-line">
        {{ log }}
      </div>
    </code>
  </basic-container>

</template>
<script>

import {createLogSSEClient} from "@/api/log";
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
    }
  },
  methods:{
    initSSEClient(){
      if(this.sseClient){
        this.sseClient.close()
      }
      this.sseClient = createLogSSEClient(120,this.handleLineLog)
      this.sseClient.connect()
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
    }
  },
  beforeDestroy() {
    if(this.sseClient){
      this.sseClient.close()
    }
  }
}

</script>
<style scoped lang="scss">
.tail-log{
  display: flex;
  min-height: 100%;
  padding-bottom: 0;
}
</style>
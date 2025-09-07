<template>
  <basic-container class="tail-log">
    <div class="log-content" ref="logContent">
      <div v-for="(log, index) in logs" :key="index" class="log-line">
        {{ log }}
      </div>
    </div>
  </basic-container>

</template>
<script>

import {createLogSSEClient} from "@/api/log";
export default {
  data() {
    return {
      sse: null,
      logs: [],
      connectionStatus: 'disconnected',
      close:null
    }
  },
  mounted() {
    this.close = createLogSSEClient(10,(s)=>{
      // console.log("qwe",s)
    })
  },
  beforeDestroy() {
    // 移除自定义事件监听
    this.sse.off('notification')
    // 断开连接
    this.sse.disconnect()
  },
  style: `
    .log-viewer {
      display: flex;
      flex-direction: column;
      height: 100%;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      overflow: hidden;
    }

    .log-controls {
      padding: 10px;
      background-color: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;
      display: flex;
      gap: 10px;
      align-items: center;
    }

    .log-path-input {
      flex: 1;
      padding: 8px 12px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 14px;
    }

    .start-btn, .stop-btn, .clear-btn {
      padding: 8px 16px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
    }

    .start-btn {
      background-color: #42b983;
      color: white;
    }

    .start-btn:disabled {
      background-color: #a0d9b9;
      cursor: not-allowed;
    }

    .stop-btn {
      background-color: #f44336;
      color: white;
    }

    .stop-btn:disabled {
      background-color: #f8a69f;
      cursor: not-allowed;
    }

    .clear-btn {
      background-color: #2196f3;
      color: white;
    }

    .log-content {
      flex: 1;
      padding: 10px;
      overflow-y: auto;
      background-color: #2d2d2d;
      color: #f0f0f0;
      font-family: monospace;
      font-size: 14px;
    }

    .log-line {
      margin-bottom: 4px;
      line-height: 1.4;
    }

    .status-info {
      padding: 8px 10px;
      background-color: #f0f0f0;
      border-top: 1px solid #e0e0e0;
      font-size: 13px;
      color: #666;
    }
  `
}

</script>
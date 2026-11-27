<template>
  <div id="chat-context-drawer">
    <el-drawer :visible.sync="visible" :direction="drawerDirection" @closed="closed" :size="drawerSize">
      <template slot="title">
        <span v-if="v">
          <multi-cell v-if="isGroupMsg" :image-url="v.groupAvatarUrl" :text-list="[v.groupName,v.groupId ]" :title-list="[v.groupName ]"></multi-cell>
          <multi-cell v-else :image-url="v.targetAvatarUrl" :text-list="[v.targetId]"></multi-cell>
        </span>
      </template>



      <el-row class="opt-row">
          <span class="num">
            前<el-input-number :step-strictly="true" size="mini"
                               :controls="false"
                               v-model="offset.offset1"
                               :min="0" :max="250"
                               @change="handleNumChange($event,'offset1')"
                               controls-position="right"></el-input-number>条
          </span>
        <el-divider direction="vertical"></el-divider>
        <span class="num">
            后<el-input-number :step-strictly="true" size="mini"
                               :controls="false"
                               v-model="offset.offset2"
                               :min="0" :max="250"
                               @change="handleNumChange($event,'offset2')"
                               controls-position="right"></el-input-number>条
          </span>
        <el-divider direction="vertical"></el-divider>
        <el-button type="text" :disabled="loading"  title="刷新" @click="queryMessageList(false, true)">
          <i v-if="loading" class="el-icon-loading" style="font-size: 20px"></i>
          <i v-else class="el-icon-refresh" style="font-size: 20px"></i>
        </el-button>
        <el-divider direction="vertical"></el-divider>
        <el-button type="text" title="定位"  @click="scrollToMessage(v.id)">
          <i class="el-icon-aim" style="font-size: 20px"></i>
        </el-button>

        <el-divider direction="vertical"></el-divider>
        <el-button type="text" title="最顶部"  @click="scrollToTop()">
          <i class="el-icon-top" style="font-size: 20px"></i>
        </el-button>


        <el-divider direction="vertical"></el-divider>
        <el-button type="text" title="最底部"  @click="scrollToBottom">
          <i class="el-icon-bottom" style="font-size: 20px"></i>
        </el-button>

        <el-divider direction="vertical"></el-divider>
        <el-button type="text" title="打印"  @click="handlePrint" v-print="printParams">
          <i class="el-icon-printer" style="font-size: 20px"></i>
        </el-button>
      </el-row>



      <div class="chat-wrap" v-loading="loading">
        <div id="ChatWindow" class="chat-window" ref="chatWindow">
          <template v-if="errMsg">
            <span class="err-resp">
              {{ errMsg }}
            </span>
          </template>

          <chat-message-list v-else :messages="messageList" :highlight-id="highlightMessageId"/>

        </div>
      </div>
    </el-drawer>
  </div>
</template>
<script>
import {deepClone} from "@/util/util";
import {messageContext} from "@/api/chat-record";
import MultiCell from "@/components/multi-cell.vue";
import ChatMessageList from "@/components/chat/chat-message-list.vue";
export default {
  name:'chat-context-drawer',
  components:{
    MultiCell,
    ChatMessageList
  },
  data(){
    return{
      loading:false,
      visible:false,
      v:null,
      messageList:[],
      errMsg:null,
      offset:{
        offset1:50,
        offset2:50,
      },
      printParams:{
        id:'ChatWindow',
        standard: "html5",
        popTitle: '聊天记录打印'
      }
    }
  },
  created() {
  },
  mounted() {

  },
  computed:{
    isGroupMsg(){
      return this.v.messageType === 'group'
    },
    // 移动端改为底部抽屉并铺满宽度，便于单手滑动浏览聊天上下文
    drawerDirection(){
      return this.isMobileView ? 'btt' : 'rtl'
    },
    drawerSize(){
      return this.isMobileView ? '92%' : '680px'
    },
    // 需要高亮定位的消息id
    highlightMessageId(){
      return this.v ? this.v.id : null
    }
  },
  methods:{
    handlePrint(){
      // 关键：等待DOM更新 → 永远打印最新数据
      // this.$nextTick(() => {
      //   print({
      //     id: '#ChatWindow',
      //     standard: 'html5'
      //   })
      //   this.$message.success('准备打印最新聊天记录')
      // })
    },
    // 回到消息最顶部
    scrollToTop(smooth = true) {
      const container = this.$refs.chatWindow;
      if (!container) return;
      container.scrollTo({
        top: 0,
        behavior: smooth ? "smooth" : "auto"
      });
    },
    // 滚动到聊天最底部
    scrollToBottom(smooth = true) {
      const container = this.$refs.chatWindow;
      if (!container) return;
      container.scrollTo({
        top: container.scrollHeight,
        behavior: smooth ? "smooth" : "auto"
      });
    },

    // 定位到指定消息id
    scrollToMessage(msgId,smooth = true) {
      this.$nextTick(() => {
        const targetDom = document.getElementById(`msg-${msgId}`);
        const container = this.$refs.chatWindow;
        if (!targetDom || !container) return;

        // 计算相对容器的偏移
        const offsetTop = targetDom.offsetTop;
        container.scrollTo({
          top: offsetTop - 140,
          behavior: smooth ? "smooth" : "auto"
        });
      });
    },
    handleNumChange(v,prop){
      if(!v && v !== 0){
        this.offset[prop] = 0
      }
    },
    open(v){
      this.visible = true
      this.$nextTick(()=>{
        this.v = deepClone(v)
        this.queryMessageList(true, false)
      })
    },
    queryMessageList(positioning = true, smooth = true){
      this.loading = true
      messageContext({
        ...this.v,
        offset1:-(this.offset.offset1),
        offset2:this.offset.offset2
      }).then(({data:{data,code,message}})=>{
        this.errMsg = code === 200 ? null : message
        this.messageList = data
      }).finally(()=>{
        this.loading = false

        if (positioning && this.v && this.messageList && this.messageList.length > 0) {
          this.scrollToMessage(this.v.id, smooth)
        }
      })
    },
    closed(){
      this.v = null
      this.offset.offset1 = 50
      this.offset.offset2 = 50
      this.messageList = []
      this.errMsg = null
    }
  }
}
</script>
<style scoped lang="scss">
#chat-context-drawer{
   ::v-deep .el-drawer__header{
     padding: 0 15px ;
     margin: 0;
     height: 90px;
     overflow-y: auto;
     &::-webkit-scrollbar {
       width: 4px;
     }
     &::-webkit-scrollbar-thumb {
       background: #ccc;
       border-radius: 2px;
     }
  }
  ::v-deep .el-drawer__body{
    //padding: 0 15px 15px ;
    .opt-row{
      padding: 0px 15px;
      .num{
        font-size: 14px;
      }
      .el-input-number--mini{
        width: 28px !important;
        .el-input__inner{
          padding-left: 0px;
          padding-right: 0px;
        }
      }
    }


    .chat-wrap {
      //width: 650px;
      margin: 0 auto;
      border: 1px solid #ddd;
      border-radius: 6px;
    }

    .chat-window {
      //height: 550px;
      height: calc(100vh - 90px - 46px - 30px - 2px);//减去 （抽屉title高度90px + opt-row高度46px + 抽屉body内边距30px + chat-wrap上下两条边框厚度2px）
      padding: 15px;
      background: #f7f8fa;
      overflow-y: auto;

      &::-webkit-scrollbar {
        width: 4px;
      }
      &::-webkit-scrollbar-thumb {
        background: #ccc;
        border-radius: 2px;
      }
      .err-resp{
        color: #ec3636;
      }
    }

  }
}
</style>


<style>
@media print {

  @page {
    /* 清除页面默认边距 */
    margin: 0 !important;
    padding: 0 !important;
    size: auto !important;
  }
  body {
    margin: 0 !important;
    padding: 0 !important;
  }

  * {
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }
  #ChatWindow {
    width: 100vw !important;
    min-height: 100vh !important;
    margin: 0 !important;
    padding: 10px !important;
    box-sizing: border-box !important;
    background: #f7f8fa !important;
    font-family: "Microsoft YaHei", sans-serif !important;
  }

  #ChatWindow .message-item {
    margin-bottom: 18px !important;
    overflow: hidden !important;
  }

  #ChatWindow .nick {
    font-size: 12px !important;
    color: #999 !important;
    margin-bottom: 4px !important;
  }

  #ChatWindow .bubble {
    display: inline-block !important;
    padding: 8px 12px !important;
    border-radius: 8px !important;
    max-width: 85% !important;
    word-break: break-all !important;
    font-size: 14px !important;
    white-space: normal !important;
    line-height: 1.4 !important;
  }

  #ChatWindow .other-bubble {
    background: #ffffff !important;
    color: #333 !important;
    text-align: left !important;
  }

  #ChatWindow .self-bubble {
    background: #428dff !important;
    color: #fff !important;
    text-align: left !important;
  }

  /* 单图消息打印时同样不展示气泡 */
  #ChatWindow .bubble-plain {
    padding: 0 !important;
    background: transparent !important;
  }
  #ChatWindow .other-avatar-col{
    width: 45px;
  }

  #ChatWindow .self-avatar-col{
    width: 45px;
  }

  /*#ChatWindow .key-message {
    background-color: #e6a23c !important;
    color: #fff !important;
  }*/

  #ChatWindow .row-other {
    display: flex !important;
    justify-content: flex-start !important;
  }
  #ChatWindow .row-self {
    display: flex !important;
    justify-content: flex-end !important;
    text-align: right !important;
  }

  /* 时间样式 */
  #ChatWindow .alignment {
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    color: #999999;

  }
  #ChatWindow .alignment-self{
    padding-right: 8px;
    span{
      margin-left: 10px;
    }
  }
  #ChatWindow .alignment-other{
    padding-left: 8px;
    span{
      margin-right: 10px;
    }
  }
}
</style>
<template>
  <div id="chat-context-drawer">
    <el-drawer :visible.sync="visible" :direction="direction" @closed="closed" size="680px">
      <template slot="title">
        <span v-if="v">
          <multi-cell v-if="isGroupMsg" :image-url="v.groupAvatarUrl" :text-list="[v.groupName,v.groupId ]" :title-list="[v.groupName ]"></multi-cell>
          <multi-cell v-else :image-url="v.targetAvatarUrl" :text-list="[v.targetId]"></multi-cell>
        </span>
      </template>



      <el-row class="opt-row">
          <span class="num">
            前<el-input-number :step-strictly="true" size="mini"
                               v-model="offset.offset1"
                               :min="0" :max="250"
                               @change="handleNumChange($event,'offset1')"
                               controls-position="right"></el-input-number>条
          </span>
        <el-divider direction="vertical"></el-divider>
        <span class="num">
            后<el-input-number :step-strictly="true" size="mini"
                               v-model="offset.offset2"
                               :min="0" :max="250"
                               @change="handleNumChange($event,'offset2')"
                               controls-position="right"></el-input-number>条
          </span>
        <el-divider direction="vertical"></el-divider>
        <el-button type="text" :loading="loading"  title="刷新" @click="queryMessageList(false, true)">
          <i class="el-icon-refresh" style="font-size: 20px"></i>
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
      </el-row>



      <div class="chat-wrap" v-loading="loading">
        <div class="chat-window" ref="chatWindow">
          <template v-if="errMsg">
            <span class="err-resp">
              {{ errMsg }}
            </span>
          </template>

          <template v-else>
            <div class="message-item" :id="'msg-' + item.id"  v-for="item in messageList" :key="item.id">
              <!-- 别人消息：左 -->
              <el-row v-if="item.userId !== v.selfId"  class="row-other" type="flex"  justify="start">
                <el-col :span="3" class="other-avatar-col">
                  <el-avatar :src="item.userAvatarUrl"></el-avatar>
                </el-col>
                <el-col :span="18">
                  <div class="nick">{{ item.card || item.nickname }}</div>
                  <div class="bubble other-bubble" :class="item.id === v.id ? 'key-message' : null">{{ item.content }}</div>
                </el-col>
              </el-row>

              <!-- 自己消息：右 -->
              <el-row v-else  class="row-self" type="flex"  justify="end">
                <el-col :span="18" class="text-right">
                  <div class="nick">{{ item.card || item.nickname }}</div>
                  <div class="bubble self-bubble" :class="item.id === v.id ? 'key-message' : null">{{ item.content }}</div>
                </el-col>
                <el-col :span="3" class="self-avatar-col">
                  <el-avatar :src="item.userAvatarUrl"></el-avatar>
                </el-col>
              </el-row>
            </div>
          </template>

        </div>
      </div>
    </el-drawer>
  </div>
</template>
<script>
import {deepClone} from "@/util/util";
import {messageContext} from "@/api/chat-record";
import MultiCell from "@/components/multi-cell.vue";
export default {
  name:'chat-context-drawer',
  components:{
    MultiCell
  },
  data(){
    return{
      loading:false,
      visible:false,
      direction: 'rtl',
      v:null,
      messageList:[],
      errMsg:null,
      offset:{
        offset1:100,
        offset2:100,
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
    }
  },
  methods:{
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
        this.errMsg = message
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
      this.offset.offset1 = 100
      this.offset.offset2 = 100
      this.messageList = []
      this.errMsg = null
    }
  }
}
</script>
<style scoped lang="scss">
#chat-context-drawer{
   ::v-deep .el-drawer__header{
    padding: 15px 15px 0;
  }
  ::v-deep .el-drawer__body{
    //padding: 0 15px 15px ;
    .opt-row{
      padding: 0px 15px;
      .num{
        font-size: 14px;
      }
      .el-input-number--mini{
        width: 80px !important;
        .el-input__inner{
          padding-left: 0px;
          padding-right: 29px;
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
      height: calc(100vh - 55px - 32px - 32px - 2px - 46px);//减去 （抽屉title高度55px + 抽屉title内边距32px + 抽屉title外边距32px + chat-wrap上下两条边框厚度2px - opt-row高度46px）
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

    .message-item {
      margin-bottom: 18px;
    }

    .nick {
      font-size: 12px;
      color: #999;
      margin-bottom: 4px;
    }

    .bubble {
      display: inline-block;
      padding: 8px 12px;
      border-radius: 8px;
      max-width: 85%;
      word-break: break-all;
      font-size: 14px;
    }

    .other-bubble {
      background: #ffffff;
    }

    .self-bubble {
      background: #428dff;
      color: #fff;
      text-align: left;
    }
    .other-avatar-col{
      width: 45px;
    }
    .self-avatar-col{
      width: 45px;
      //text-align: left;
    }
    .row-self{
      text-align: right;
    }
    .key-message{
      background-color: #e6a23c;
    }



  }
}
</style>
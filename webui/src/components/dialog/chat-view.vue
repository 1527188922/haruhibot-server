<template>
  <div id="ChatView">
    <el-dialog :visible.sync="visible" :title="title" width="600px" @closed="dialogClosed" v-dialogDrag>
      <div class="chat-wrap">
        <div class="chat-window">
          <div class="message-item" :id="'msg-' + item.id"  v-for="item in messageList" :key="item.id">
            <el-row v-if="item.userId !== item.selfId" class="row-other" type="flex"  justify="start">
              <el-col :span="3" class="other-avatar-col">
                <el-avatar :src="item.userAvatarUrl"></el-avatar>
              </el-col>
              <el-col :span="18">
                <div class="nick">
                  <template v-if="item.card || item.nickname">{{ item.card || item.nickname }}（{{ item.userId }}）</template>
                  <template v-else>{{ item.userId }}</template>
                </div>
                <div class="bubble other-bubble">{{ item.content }}</div>
                <div class="alignment alignment-other" v-if="item.time">
                  <span>{{ item.time }}</span>
                </div>
              </el-col>
            </el-row>


            <el-row v-else  class="row-self" type="flex"  justify="end">
              <el-col :span="18" class="text-right">
                <div class="nick">
                  <template v-if="item.card || item.nickname">{{ item.card || item.nickname }}（{{ item.userId }}）</template>
                  <template v-else>{{ item.userId }}</template>
                </div>
                <div class="bubble self-bubble" >{{ item.content }}</div>
                <div class="alignment alignment-self" v-if="item.time">
                  <span>{{ item.time }}</span>
                </div>
              </el-col>
              <el-col :span="3" class="self-avatar-col">
                <el-avatar :src="item.userAvatarUrl"></el-avatar>
              </el-col>
            </el-row>
          </div>
        </div>
      </div>

    </el-dialog>
  </div>
</template>
<script>

export default {
  name:'ChatViewDialog',
  components: {
  },
  data(){
    return{
      title:'',
      visible:false,
      messageList:[]
    }
  },
  methods:{
    open(list, title = ''){
      this.visible = true
      this.$nextTick(()=>{
        this.title = title
        this.messageList = list
      })
    },
    dialogClosed(){
      this.title = ''
      this.messageList = []
    }
  }
}
</script>
<style scoped lang="scss">
#ChatView{
  .chat-wrap {
    //width: 650px;
    margin: 0 auto;
    border: 1px solid #ddd;
    border-radius: 6px;
  }

  .chat-window {
    //height: 550px;
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
    white-space: pre-wrap;
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
  .alignment {
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    color: #999999;

  }
  .alignment-self{
    padding-right: 8px;
    span{
      margin-left: 10px;
    }
  }
  .alignment-other{
    padding-left: 8px;
    span{
      margin-right: 10px;
    }
  }
}
</style>
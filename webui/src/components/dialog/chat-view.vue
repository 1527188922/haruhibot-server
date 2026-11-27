<template>
  <div id="ChatView">
    <el-dialog :visible.sync="visible" :title="title" width="600px" @closed="dialogClosed" :dialog-drag-enabled="!isMobileView">
      <div class="chat-wrap">
        <div class="chat-window">
          <chat-message-list :messages="messageList"/>
        </div>
      </div>

    </el-dialog>
  </div>
</template>
<script>
import ChatMessageList from '@/components/chat/chat-message-list'

export default {
  name:'ChatViewDialog',
  components: {
    ChatMessageList
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
    margin: 0 auto;
    border: 1px solid #ddd;
    border-radius: 6px;
  }

  .chat-window {
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
}
</style>

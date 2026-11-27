<template>
  <div class="chat-message-list">
    <div class="message-item" :id="'msg-' + item.id" v-for="item in messages" :key="item.id">
      <!-- 别人消息：左 -->
      <el-row v-if="item.userId !== item.selfId" class="row-other" type="flex" justify="start">
        <el-col :span="3" class="other-avatar-col">
          <el-avatar :src="item.userAvatarUrl"></el-avatar>
        </el-col>
        <el-col :span="18">
          <div class="nick">
            <template v-if="item.card || item.nickname">{{ item.card || item.nickname }}（{{ item.userId }}）</template>
            <template v-else>{{ item.userId }}</template>
          </div>
          <div class="bubble other-bubble" :class="isHighlight(item) ? 'key-message' : null">
            <cq-content :content="item.content"/>
          </div>
          <div class="alignment alignment-other" v-if="item.time">
            <span>{{ item.time }}</span>
          </div>
        </el-col>
      </el-row>

      <!-- 自己消息：右 -->
      <el-row v-else class="row-self" type="flex" justify="end">
        <el-col :span="18" class="text-right">
          <div class="nick">
            <template v-if="item.card || item.nickname">{{ item.card || item.nickname }}（{{ item.userId }}）</template>
            <template v-else>{{ item.userId }}</template>
          </div>
          <div class="bubble self-bubble" :class="isHighlight(item) ? 'key-message' : null">
            <cq-content :content="item.content"/>
          </div>
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
</template>
<script>
import CqContent from '@/components/chat/cq-content'

/**
 * 左右区分的聊天消息列表（纯展示）
 * 左右判断沿用 item.userId !== item.selfId，各消息自带 selfId
 */
export default {
  name: 'ChatMessageList',
  components: {
    CqContent
  },
  props: {
    messages: {
      type: Array,
      default: () => []
    },
    //需要高亮的气泡消息id（聊天记录定位场景）
    highlightId: {
      type: [String, Number],
      default: null
    }
  },
  methods: {
    isHighlight (item) {
      return this.highlightId !== null && this.highlightId !== undefined && item.id === this.highlightId
    }
  }
}
</script>
<style scoped lang="scss">
.chat-message-list {
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

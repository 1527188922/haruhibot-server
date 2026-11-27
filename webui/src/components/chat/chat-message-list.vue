<template>
  <div class="chat-message-list">
    <div class="message-item" :id="'msg-' + item.id" v-for="(item, index) in messages" :key="item.id">
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
          <div class="bubble other-bubble" :class="[isPlainImageMessage(item, index) ? 'bubble-plain' : null, isHighlight(item) ? 'key-message' : null]">
            <cq-content :content="item.content" :preview-list="previewData.urls" :preview-offset="previewData.offsets[index] || 0" @image-error="handleImageError(item, index)"/>
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
          <div class="bubble self-bubble" :class="[isPlainImageMessage(item, index) ? 'bubble-plain' : null, isHighlight(item) ? 'key-message' : null]">
            <cq-content :content="item.content" :preview-list="previewData.urls" :preview-offset="previewData.offsets[index] || 0" @image-error="handleImageError(item, index)"/>
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
import { isSingleImageContent, parseCqContent } from '@/util/cq'

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
  data () {
    return {
      //记录图片加载失败的消息，单图消息失败时需要回退成气泡占位
      imageErrorMap: {}
    }
  },
  watch: {
    messages (list) {
      // 消息数组会被增量加载整体替换，保留仍在列表中的图片失败状态，避免气泡回退成无气泡
      const next = {}
      ;(list || []).forEach((item, index) => {
        const key = this.messageKey(item, index)
        if (this.imageErrorMap[key]) {
          next[key] = true
        }
      })
      this.imageErrorMap = next
    }
  },
  computed: {
    // 汇总当前消息列表中的全部图片（按出现顺序），供图片预览左右切换
    previewData () {
      const urls = []
      const offsets = []
      this.messages.forEach(item => {
        offsets.push(urls.length)
        parseCqContent(item.content).forEach(seg => {
          if (seg.type === 'image') {
            urls.push(seg.url)
          }
        })
      })
      return { urls, offsets }
    }
  },
  methods: {
    messageKey (item, index) {
      return item.id !== null && item.id !== undefined ? item.id : 'index-' + index
    },
    isHighlight (item) {
      return this.highlightId !== null && this.highlightId !== undefined && item.id === this.highlightId
    },
    //单图且加载成功时不展示气泡，直接展示图片
    isPlainImageMessage (item, index) {
      return isSingleImageContent(item.content) && !this.imageErrorMap[this.messageKey(item, index)]
    },
    handleImageError (item, index) {
      this.$set(this.imageErrorMap, this.messageKey(item, index), true)
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
    // 这里不能使用 pre-wrap：模板换行会被当作文本渲染，原文空格由内容层 .cq-content 保留
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
  // 单图消息不展示气泡，图片直接呈现
  .bubble-plain{
    padding: 0;
    background: transparent;
    ::v-deep .cq-image{
      margin: 0;
    }
  }
  // 单图消息被定位高亮时，用描边代替气泡背景
  .bubble-plain.key-message ::v-deep .cq-image{
    box-shadow: 0 0 0 2px #e6a23c;
    border-radius: 4px;
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

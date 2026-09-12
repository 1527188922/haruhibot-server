<template>
  <div class="multi-cell">
    <!-- b站等第三方图片存在防盗链，不带Referer才能正常加载 -->
    <img v-if="imageUrl" :src="imageUrl" referrerpolicy="no-referrer">
    <div class="row-box">
      <template v-for="(t,i) in textList">
        <div v-if="t" :key="t+i" class="text-align-left line" :title="titleList.length >= i + 1 ? titleList[i] : null">
          <a v-if="linkList && linkList[i]" class="link-text" :href="linkList[i]" target="_blank"
             rel="noopener noreferrer">{{t}}</a>
          <template v-else>{{t}}</template>
        </div>
      </template>
    </div>
  </div>
</template>
<script>
export default {
  props:{
    imageUrl:{
      type:String,
      default:()=>{
        return null
      }
    },
    textList:{
      type:Array,
      default:()=>{
        return []
      }
    },
    titleList:{
      type:Array,
      default:()=>{
        return []
      }
    },
    // 与textList一一对应，某项不为空时该行渲染为可点击的链接
    linkList:{
      type:Array,
      default:()=>{
        return []
      }
    }
  }
}
</script>
<style scoped lang="scss">
.multi-cell{
  width: fit-content;
  display: flex;
  align-items: center;
  img{
    margin-right: 5px;
    width: 40px;
    border-radius: 50%
  }
  .text-align-left{
    text-align: left;
  }
  .link-text{
    color: #409EFF;
    text-decoration: none;
    cursor: pointer;
    &:hover{
      text-decoration: underline;
    }
  }
  .line{
    &:not(:first-child) {
      border-top: 1px solid #d5dce8;
    }
  }
}
</style>
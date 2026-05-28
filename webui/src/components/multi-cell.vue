<template>
  <div class="multi-cell">
    <img v-if="imageUrl" :src="imageUrl">
    <div>
      <el-row v-for="(t,i) in textList" :key="t+i" class="text-align-left line" :title="titleList.length >= i + 1 ? titleList[i] : null">
        {{t}}
      </el-row>
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
      type:String,
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
  .line{
    &:not(:first-child) {
      border-top: 1px solid #d5dce8;
    }
  }
}
</style>
<template>
  <div class="basic-container"
       :class="{'basic-container--block':block}">
    <el-card class="basic-container__card" :style="styleName" :shadow="shadow">
      <div slot="header" v-if="showHeader">
        <slot name="header"></slot>
      </div>
      <slot></slot>
    </el-card>
  </div>
</template>

<script>
export default {
  name: "basicContainer",
  props: {
    shadow:{
      type: String,
      // never always hover
      default: 'never'
    },
    radius: {
      type: [String, Number],
      default: 4
    },
    background: {
      type: String
    },
    block: {
      type: Boolean,
      default: false
    },
    showHeader:{
      type: Boolean,
      default: false
    }
  },
  computed: {
    styleName () {
      return {
        borderRadius: `${this.radius}px`,
        background: this.background
      }
    }
  }
};
</script>

<style lang="scss">
.basic-container {
  padding: 10px 6px;
  box-sizing: border-box;
  .el-card__body{
    padding: 15px;
  }
  &--block {
    height: 100%;
    .basic-container__card {
      height: 100%;
    }
  }
  &__card {
    width: 100%;
  }
  &:first-child {
    padding-top: 0;
  }
}
</style>

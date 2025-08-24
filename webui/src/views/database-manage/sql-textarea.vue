<template>
  <el-input
      ref="textarea"
      type="textarea"
      :value="value"
      @input="handleInput"
      @select.native="onSelect"
      @mouseup.native="onMouseUp"
      @click.native="onClick"
      @keyup.native="onKeyup"
      @blur="onBlur"
      @focus="onFocus"

      class="full-size-textarea"
  ></el-input>
</template>

<script>
export default {
  props: {
    value: {
      type: String,
      default: ''
    }
  },
  mounted() {
  },
  beforeDestroy() {
  },
  methods: {
    handleInput(value) {
      this.$emit('selection-change', '')
      this.$emit('input', value);
    },
    // —— 关键：延迟到下一帧读取 —— //
    readLater() {
      // 先等 Vue 可能的渲染，再等浏览器把光标/选区状态稳定
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          const el = this.$refs.textarea.$refs.textarea || this.$refs.textarea.$refs.input
          if (!el) return
          const s = el.selectionStart || 0
          const e = el.selectionEnd || 0
          this.start = s
          this.end = e
          let selected = (s === e) ? '' : el.value.slice(s, e)
          this.$emit('selection-change', selected)
        })
      })
    },
    onSelect() { this.readLater() },
    onMouseUp() { this.readLater() },
    onClick() { this.readLater() },
    onKeyup(e) {
      const keys = ['ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Home','End','Escape','Shift']
      if (keys.includes(e.key)) {
        this.readLater()
      }
    },
    onBlur() {
      this.$emit('selection-change', '')
    },
    onFocus(){
      this.$emit('selection-change', '')
    }
  }
};
</script>

<style scoped lang="scss">
//.auto-resize-wrapper {
//  width: 100%;
//  height: 100%;
//}

.full-size-textarea {
  width: 100%;
  height: 100%;
  ::v-deep .el-textarea__inner {
    width: 100% !important;
    height: 100% !important;
    border-radius: 0 !important;
    resize: none;
    padding: 5px;
    box-sizing: border-box;
  }
}
</style>
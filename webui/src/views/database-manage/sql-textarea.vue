<template>
  <div class="sql-editor-container">
    <el-input
        ref="textarea"
        type="textarea"
        v-model="valueObj.value"
        @input="handleInput"
        @select.native="onSelect"
        @mouseup.native="onMouseUp"
        @click.native="onClick"
        @keyup.native="onKeyup"
        @blur="onBlur"
        @focus="onFocus"
        @contextmenu.native.prevent="contextmenu"
        @change="handleChange"
        class="full-size-textarea"
    ></el-input>
    <input-hint
        :all-items="hintItems"
        :position="hintPosition"
        @select="replaceStr"
    ></input-hint>
  </div>

</template>

<script>
import InputHint from './input-hint.vue';

export default {
  components:{
    InputHint
  },
  props: {
    valueObj:{
      type: Object,
      default: ()=>{return {value:''}}
    },
    value: {
      type: String,
      default: ''
    }
  },
  mounted() {
    if (this.allUsers.length === 0) {
      this.getAllUsers();
    }
  },
  beforeDestroy() {
  },
  data(){
    return{
      inputValue:'',
      Seprator: "\n",
      allUsers: [],
      hintItems: [],

      initPosition: {
        left: 15,
        top: 5,
        rowHeight: 20,
        fontSize: 7
      },
      hintPosition: {
        left: 15,
        top: 5
      }
    }
  },
  methods: {
    // 鼠标右键
    contextmenu(e){
      this.$emit('contextmenu', e);
    },
    handleChange(v){
      this.$emit('change', v);
    },
    handleInput(value) {
      // this.inputValue = value
      this.settingHint()
      this.$emit('selection-change', '')
      this.$emit('input', value);
    },
    // 延迟到下一帧读取
    readLater() {
      // 先等 Vue 可能的渲染，再等浏览器把光标/选区状态稳定
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          const el = this.getTextareaEl()
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
    getTextareaEl(){
      return this.$refs.textarea.$refs.textarea || this.$refs.textarea.$refs.input
    },
    onSelect() { this.readLater() },
    onMouseUp() { this.readLater() },
    onClick(e) {
      this.readLater()
      this.settingHint()
    },
    onKeyup(e) {
      const keys = ['ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Home','End','Escape','Shift']
      if (keys.includes(e.key)) {
        this.readLater()
      }
      this.disposeKey(e)
    },
    onBlur() {
      this.closeHint()
      this.$emit('selection-change', '')
    },
    onFocus(){
      this.$emit('selection-change', '')
    },
    disposeKey(e) {
      if ([37, 38, 39, 40].includes(e.keyCode)) {
        this.settingHint();
      }
    },
    settingHint(val) {
      const cursorLocation = this.getTextareaEl().selectionStart;
      const newStr = this.valueObj.value.slice(0, cursorLocation);
      const newArr = newStr.split(this.Seprator);
      const searchKey = newArr.length ? newArr[newArr.length - 1].trim() : "";
      const regExp = new RegExp(searchKey, 'ig');

      this.hintItems = searchKey
          ? this.allUsers.filter(item => item.includes(searchKey))
              .map(item => item.replace(regExp, `<strong>${searchKey}</strong>`))
          : [];
      this.hintPosition.left = this.initPosition.left +
          (this.initPosition.fontSize * Math.max(searchKey.length - 1, 0));
      this.hintPosition.top = this.initPosition.top +
          (this.initPosition.rowHeight * Math.min(newArr.length, 10));
    },
    closeHint() {
      setTimeout(() => {
        this.hintItems = [];
      }, 200);
    },
    replaceStr(val) {
      const cursorLocation = this.getTextareaEl().selectionStart;
      const newStr = this.valueObj.value.slice(0, cursorLocation);
      const row = newStr.split(this.Seprator).length - 1;
      const oriArr = this.valueObj.value.split(this.Seprator);

      oriArr[row] = val;
      let newValue = oriArr.join(this.Seprator);
      // this.value = newValue
      this.valueObj.value = newValue
      this.$emit('input', newValue);
      this.getTextareaEl().focus();
    },
    getAllUsers() {
      this.allUsers = [
        'xiaoming@qq.com', 'daming@163.com', 'liuxioawei@gridsum.com',
        // ...其他邮箱地址保持原有列表
      ];
    }
  }
};
</script>

<style scoped lang="scss">
.sql-editor-container{
  position: relative;
  height: 100%;
  width: 100%;
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
      font-family: monospace, serif;
    }
  }
}

</style>
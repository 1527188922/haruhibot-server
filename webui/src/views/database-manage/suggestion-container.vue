<template>
  <div style="position: relative">
    <el-input
        type="textarea"
        ref="textarea"
        placeholder="请换行输入不同的通知用户"
        :autosize="{minRows: 2, maxRows: 10}"
        v-model="inputValue"
        @blur="closeHint"
        @input="settingHint"
        @click.native="settingHint"
        @keyup.native="disposeKey">
    </el-input>
    <input-hint
        :all-items="hintItems"
        :position = "hintPosition"
        @select = "replaceStr"
    ></input-hint>
  </div>
</template>

<script>
import InputHint from './input-hint.vue';
export default {
  name: 'AdvancedTextarea',
  components: {
    InputHint
  },
  data() {
    return {
      inputValue: '',
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
    };
  },
  mounted() {
    if (this.allUsers.length === 0) {
      this.getAllUsers();
    }
  },
  methods: {
    disposeKey(e) {
      if ([37, 38, 39, 40].includes(e.keyCode)) {
        this.settingHint();
      }
    },
    getE(){
      let e = this.$refs.textarea.$refs.textarea || this.$refs.textarea.$refs.input
      return e.selectionStart
    },
    settingHint(val) {
      const cursorLocation = this.getE();
      const newStr = this.inputValue.slice(0, cursorLocation);
      const newArr = newStr.split(this.Seprator);
      const searchKey = newArr.length ? newArr[newArr.length - 1] : "";
      const regExp = new RegExp(searchKey, 'ig');

      this.hintItems = searchKey
          ? this.allUsers.filter(item => item.includes(searchKey))
              .map(item => item.replace(regExp, `<strong>${searchKey}</strong>`))
          : this.allUsers;

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
      const cursorLocation = this.getE();
      const newStr = this.inputValue.slice(0, cursorLocation);
      const row = newStr.split(this.Seprator).length - 1;
      const oriArr = this.inputValue.split(this.Seprator);

      oriArr[row] = val;
      this.inputValue = oriArr.join(this.Seprator);
      (this.$refs.textarea.$refs.textarea || this.$refs.textarea.$refs.input).focus();
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
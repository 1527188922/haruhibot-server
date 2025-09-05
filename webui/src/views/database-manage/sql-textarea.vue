<template>
  <div class="sql-editor-container">
    <el-input
        ref="textarea"
        type="textarea"
        :value="value"
        @input="handleInput"
        @select.native="onSelect"
        @mouseup.native="onMouseUp"
        @click.native="onClick"
        @keydown.native="onKeydown"
        @keyup.native="onKeyup"
        @blur="onBlur"
        @focus="onFocus"
        @contextmenu.native.prevent="contextmenu"
        @change="handleChange"
        class="full-size-textarea"
    ></el-input>
    <div v-if="showSuggestions"
        class="suggestion-box"
         ref="suggestionBox"
        :style="suggestionStyle">
      <ul>
        <li v-for="(item, index) in filteredSuggestions" :key="index"
            :class="{ 'active': index === activeSuggestionIndex }"
            @click="selectSuggestion(index)">
          {{ item.keyword }}
        </li>
      </ul>
    </div>
  </div>

</template>

<script>
import suggestions from "@/views/database-manage/suggestions";
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
  data() {
    return {
      start: 0,
      end: 0,
      filteredSuggestions: [],
      showSuggestions: false,
      activeSuggestionIndex: -1,
      currentWord: '',
      suggestionStyle:{
        top: `0px`,
        left: `0px`
      }
    }
  },
  computed: {

  },
  methods: {
    countCharacters(str) {
      // 正则表达式匹配英文字符（含字母、数字、英文标点及空格）
      const englishRegex = /^[A-Za-z0-9\s\.,!?;:'"()\-_/\\|\[\]{}~`@#$%^&*<>+=\r\n]$/;

      let englishCount = 0;
      let otherCount = 0;

      for (let char of str) {
        // 测试是否为英文字符
        if (englishRegex.test(char)) {
          englishCount++;
        } else {
          otherCount++;
        }
      }

      return {
        english: englishCount,
        other: otherCount
      };
    },
    // 获取原生textarea元素
    getNativeTextarea() {
      const elInput = this.$refs.textarea;
      if (!elInput) return null;
      // element-ui的textarea实际元素在$refs.textarea中
      return elInput.$refs.textarea || elInput.$refs.input;
    },
    getCursorIndex() {
      const textarea = this.getNativeTextarea();
      if (!textarea) return 0;
      return textarea.selectionStart;
    },
    // 获取光标所在的行号和列号（从1开始计数）
    getCursorRowCol() {
      const textarea = this.getNativeTextarea();
      if (!textarea) return { row: 1, col: 1 };

      const text = textarea.value || '';
      const cursorIndex = this.getCursorIndex();
      const textBeforeCursor = text.substring(0, cursorIndex);

      // 计算行号（根据换行符分割）
      const rows = textBeforeCursor.split('\n');
      const row = rows.length;
      // 计算列号（当前行的长度）
      const col = rows[rows.length - 1].length + 1; // +1是因为列号从1开始

      return { row, col };
    },
    // 获取光标相对于textarea的坐标（left和top，单位px）
    getCursorCoordinates() {
      const textarea = this.getNativeTextarea();
      if (!textarea) return { left: 0, top: 0 };

      // 获取textarea的样式和位置信息
      const rect = textarea.getBoundingClientRect();
      const style = window.getComputedStyle(textarea);
      const paddingLeft = parseInt(style.paddingLeft);
      const paddingTop = parseInt(style.paddingTop);
      const lineHeight = parseInt(style.lineHeight) || 16; // 默认行高

      // 创建临时元素模拟文本布局
      const tempDiv = document.createElement('div');
      // 应用与textarea相同的样式，确保文本布局一致
      tempDiv.style.cssText = `
      position: absolute;
      left: -9999px;
      top: 0;
      font: ${style.font};
      white-space: pre-wrap;
      word-wrap: break-word;
      width: ${rect.width - paddingLeft * 2}px;
      padding: ${paddingTop}px ${paddingLeft}px;
      margin: 0;
      border: none;
      overflow: hidden;
    `;

      // 获取光标前的文本并添加到临时元素
      const cursorIndex = this.getCursorIndex();
      const textBeforeCursor = textarea.value.substring(0, cursorIndex);
      tempDiv.textContent = textBeforeCursor;
      document.body.appendChild(tempDiv);

      // 计算光标位置（临时元素的最后一个字符位置）
      const tempRect = tempDiv.getBoundingClientRect();
      let top = tempRect.height - lineHeight + paddingTop - textarea.scrollTop;
      // const left = (tempRect.width % rect.width) || rect.width - paddingLeft - 2; // 微调光标宽度
      let ts = textBeforeCursor.split('\n')
      let {english,other} = this.countCharacters(ts[ts.length - 1])
      let left = english * 7 + other * 14

      // 清理临时元素
      document.body.removeChild(tempDiv);

      return {
        left: Math.max(0, left), // 确保不小于0
        top: Math.max(0, top)    // 确保不小于0
      };
    },

    // 处理键盘按下事件
    onKeydown(e) {
      // 上下键选择提示
      if (this.showSuggestions) {
        if (e.key === 'ArrowDown') {
          e.preventDefault();
          this.activeSuggestionIndex =
              (this.activeSuggestionIndex + 1) % this.filteredSuggestions.length;

        } else if (e.key === 'ArrowUp') {
          e.preventDefault();
          this.activeSuggestionIndex =
              (this.activeSuggestionIndex - 1 + this.filteredSuggestions.length) % this.filteredSuggestions.length;
        } else if (e.key === 'Enter') {
          // 回车选中当前提示
          if (this.activeSuggestionIndex >= 0) {
            e.preventDefault();
            this.selectSuggestion(this.activeSuggestionIndex);
          }
        } else if (e.key === 'Escape') {
          //  ESC键关闭提示
          this.showSuggestions = false;
        }
      }
    },

    // 处理联想提示
    handleSuggestions() {
      const textarea = this.getNativeTextarea();
      if (!textarea) return;

      const cursorPos = textarea.selectionStart;
      const text = this.value || '';

      // 获取当前输入的单词
      let wordStart = cursorPos;
      while (wordStart > 0 && /[a-zA-Z0-9_]/.test(text[wordStart - 1])) {
        wordStart--;
      }

      this.currentWord = text.substring(wordStart, cursorPos);

      // 过滤提示列表
      if (this.currentWord.length >= 1) {
        const lowerWord = this.currentWord.toLowerCase();
        this.filteredSuggestions = suggestions.filter(item => item.keyword.toLowerCase().includes(lowerWord))
            .sort();

        let showSuggestionsTemp = this.filteredSuggestions.length > 0;
        if (showSuggestionsTemp) {

          let cursorCoordinates = this.getCursorCoordinates()
          this.suggestionStyle.top = `${cursorCoordinates.top + 10}px`
          this.suggestionStyle.left = `${cursorCoordinates.left}px`
        }
        this.showSuggestions = showSuggestionsTemp;
        this.activeSuggestionIndex = this.showSuggestions ? 0 : -1;
      } else {
        this.showSuggestions = false;
      }
    },

    // 选择提示词
    selectSuggestion(index) {
      if (index < 0 || index >= this.filteredSuggestions.length) return;

      const suggestion = this.filteredSuggestions[index].keyword;
      const textarea = this.getNativeTextarea();
      const cursorPos = textarea.selectionStart;
      const text = this.value || '';

      // 计算当前单词的起始位置
      let wordStart = cursorPos;
      while (wordStart > 0 && /[a-zA-Z0-9_]/.test(text[wordStart - 1])) {
        wordStart--;
      }

      // 替换当前单词为选中的提示词
      const newText = text.substring(0, wordStart) + suggestion + text.substring(cursorPos);
      const newCursorPos = wordStart + suggestion.length;

      // 更新文本
      this.$emit('input', newText);

      // 更新光标位置
      this.$nextTick(() => {
        if (textarea) {
          textarea.focus();
          textarea.setSelectionRange(newCursorPos, newCursorPos);
          this.start = newCursorPos;
          this.end = newCursorPos;
        }
      });

      // 关闭提示
      this.showSuggestions = false;
    },

    // 鼠标右键
    contextmenu(e){
      this.$emit('contextmenu', e);
    },
    handleChange(v){
      this.$emit('change', v);
    },
    handleInput(value) {
      this.$emit('selection-change', '')
      this.$emit('input', value);
    },
    // —— 关键：延迟到下一帧读取 —— //
    readLater() {
      return new Promise(resolve => {
        this.$nextTick(() => {
          requestAnimationFrame(() => {
            const el = this.getNativeTextarea();
            if (!el) return
            const s = el.selectionStart || 0
            const e = el.selectionEnd || 0
            this.start = s
            this.end = e
            let selected = (s === e) ? '' : el.value.slice(s, e)
            this.$emit('selection-change', selected)
            resolve();
          })
        })
      });
    },
    onSelect() { this.readLater() },
    onMouseUp() { this.readLater() },
    onClick(e) {
      this.showSuggestions = false
      this.readLater()
    },
    onKeyup(e) {
      const keys = ['ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Home','End','Escape','Shift']
      if (keys.includes(e.key)) {
        this.readLater()
        return
      }

      // 输入字符后处理联想
      if (/^[a-zA-Z0-9_]$/.test(e.key) || e.key === 'Backspace') {
        this.readLater().then(() => {
          this.handleSuggestions();
        });
      } else {
        // 其他特殊键关闭提示
        this.showSuggestions = false;
      }
    },
    onBlur() {
      // this.showSuggestions = false
      this.$emit('selection-change', '')
    },
    onFocus(){
      this.$emit('selection-change', '')
    }
  }
};
</script>

<style scoped lang="scss">
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
.sql-editor-container {
  position: relative;
  height: 100%;
  width: 100%;
}

.suggestion-box {
  z-index: 3100;
  //position: fixed;
  position: absolute;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  min-width: 150px;
  max-height: 200px;
  overflow-y: auto;

  ul {
    list-style: none;
    margin: 0;
    padding: 5px 0;

    li {
      padding: 5px 10px;
      cursor: pointer;
      white-space: nowrap;

      &:hover {
        background-color: #f5f7fa;
      }

      &.active {
        background-color: #e4e8f1;
        color: #1890ff;
      }
    }
  }
}
</style>
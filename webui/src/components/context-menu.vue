<template>
  <ul v-show="visible"
      :style="{left: left + 'px', top: top + 'px'}"
      class="contextmenu"
      @mousedown.prevent="mousedown"
      ref="menu">
    <li v-for="item in items"
        :key="item.action"
        @click.stop="handleClick(item.action)">
      {{ item.text }}
    </li>
  </ul>
</template>

<script>
export default {
  props: {
    items: {
      type: Array,
      default: () => [],
      validator: value => value.every(item =>
          typeof item.text === 'string' &&
          typeof item.action === 'string'
      )
    }
  },
  data() {
    return {
      visible: false,
      left: 0,
      top: 0,
    }
  },
  methods: {
    // 父组件传递右键事件参数
    open(event) {
      this.visible = true
      this.$nextTick(() => {
        const menuEl = this.$refs.menu
        const menuWidth = menuEl.offsetWidth
        const menuHeight = menuEl.offsetHeight
        const maxX = window.innerWidth - menuWidth;
        const maxY = window.innerHeight - menuHeight;
        const x = event.clientX;
        const y = event.clientY;
        this.left = Math.min(x, maxX)
        this.top =  Math.min(y, maxY)

        document.addEventListener('click', this.close)
      })
    },
    close() {
      this.visible = false
      document.removeEventListener('click', this.close)
    },
    handleClick(action) {
      this.$emit('menu-click', {
        action
      })
      this.close()
    },
    mousedown(e){
      // 阻止默认聚焦行为
    }
  },
  mounted() {
  },
  beforeDestroy() {
    document.removeEventListener('click', this.close)
  }
}
</script>

<style scoped lang="scss">
.contextmenu {
  margin: 0;
  background: #fff;
  z-index: 3000;
  position: fixed;
  list-style-type: none;
  padding: 5px 0;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 400;
  color: #333;
  box-shadow:
      /* 主阴影（右下） */
      2px 2px 4px rgba(0, 0, 0, 0.25),
        /* 辅助阴影（左上） */
      -1px -1px 4px rgba(0, 0, 0, 0.1),
        /* 边缘柔化 */
      0 0 2px rgba(0, 0, 0, 0.1);
  li {
    margin: 0;
    padding: 7px 16px;
    cursor: pointer;
  }
  li:hover {
    background: #eee;
  }
}

</style>
<template>
  <ul v-show="visible"
      :style="{left: left + 'px', top: top + 'px',minWidth }"
      class="contextmenu"
      @mousedown.prevent="mousedown"
      ref="menu">
    <li v-for="item in (currentItems && currentItems.length > 0 ? currentItems : items)"
        :key="item.action"
        :title="item.title"
        @click.stop="handleClick(item)"
        :style="liStyle(item)">
      <span>
        <i v-if="item.icon" :class="item.icon"></i>
      {{ item.text }}
      </span>
      <span v-if="item.rightText">
        {{item.rightText}}
      </span>
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
    },
    minWidth:{
      type: String,
      default:() => 'auto',
    }
  },
  data() {
    return {
      visible: false,
      left: 0,
      top: 0,
      data:null,
      currentItems:[]
    }
  },
  methods: {
    // 父组件传递右键事件参数
    open({event,data},items = []) {
      this.currentItems = items
      this.visible = true
      this.$nextTick(() => {
        this.data = data
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
      this.data = null
      this.currentItems = []
      document.removeEventListener('click', this.close)
    },
    handleClick(item) {
      this.$emit('menu-click', item,this.data)
      this.close()
    },
    mousedown(e){
      // 阻止默认聚焦行为
    },
    liStyle(item){
      if (item.style) {
        return item.style
      }
      return {}
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

    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;

    &:hover {
      background: #eee;
    }
  }
}

</style>
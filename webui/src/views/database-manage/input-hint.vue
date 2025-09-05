<template>
  <div v-show="allItems && allItems.length!==0">
    <ul class="el-dropdown-menu el-popper max-height new-scoll-bar" :style="{left: position.left+'px', top: position.top+'px'}">
      <li class="el-dropdown-menu__item" v-for="(item,index) in allItems" :key="index" v-html="item" @click="selectItem(item)"></li>
    </ul>
  </div>
</template>

<style lang="scss" scoped>
.max-height {
  max-height: 250px;
  overflow-y: auto;
}
</style>
<script>
export default {
  name: 'InputHint',
  props: {
    allItems: {
      type: Array,
      default: () => []
    },
    position: {
      type: Object,
      default: () => ({
        left: 0,
        top: 0
      })
    }
  },
  methods: {
    selectItem(item) {
      const regExp = /<strong>|<\/strong>/g;
      const str = item.replace(regExp, '');
      this.$emit('select', str);
    }
  }
};
</script>
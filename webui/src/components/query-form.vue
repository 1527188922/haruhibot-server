<template>
  <div class="query-form" :class="{'query-form--collapsed': isMobileView && !expanded}">
    <el-form ref="form" :model="model" v-bind="$attrs" v-on="$listeners"><slot /></el-form>
    <el-button v-if="isMobileView && fieldCount > 2" class="query-form-toggle" type="text"
               :aria-expanded="String(expanded)" @click="expanded = !expanded">
      {{ expanded ? '收起筛选' : '更多筛选' }}<span v-if="activeCount">（已选 {{ activeCount }} 项）</span>
      <i :class="expanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
    </el-button>
  </div>
</template>
<script>
import { countActiveFilters } from '@/util/mobile-layout'
export default {
  name: 'QueryForm',
  inheritAttrs: false,
  props: { model: { type: Object, required: true } },
  data: () => ({ expanded: false }),
  computed: {
    activeCount() { return countActiveFilters(this.model) },
    fieldCount() { return (this.$slots.default || []).filter(node => node.componentOptions).length }
  },
  methods: {
    resetFields() { this.$refs.form.resetFields() },
    validate(callback) { return this.$refs.form.validate(callback) },
    clearValidate(props) { this.$refs.form.clearValidate(props) }
  }
}
</script>
<style lang="scss">
@media screen and (max-width: 767.98px) {
  .query-form--collapsed > .el-form > .el-form-item:nth-child(n + 3) { display: none; }
  .query-form-toggle { min-height: 40px; padding-top: 0; }
}
</style>

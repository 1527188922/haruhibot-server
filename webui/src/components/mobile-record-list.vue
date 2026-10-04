<template>
  <div class="mobile-record-list" v-loading="loading" :aria-busy="String(loading)">
    <p v-if="!loading && !rows.length" class="mobile-record-empty">暂无数据</p>
    <article v-for="(row, index) in rows" :key="rowKey ? row[rowKey] : index" class="mobile-record">
      <header class="mobile-record-header">
        <img v-if="avatarField && row[avatarField]" :src="row[avatarField]" alt="" referrerpolicy="no-referrer">
        <strong>{{ row[titleField] || row[fallbackField] || '详情' }}</strong>
        <slot name="header" :row="row" />
      </header>
      <slot name="summary" :row="row" />
      <dl class="mobile-record-fields">
        <template v-for="field in fields">
          <dt :key="field.label + '-label'">{{ field.label }}</dt>
          <dd :key="field.label">{{ fieldValue(row, field) }}</dd>
        </template>
      </dl>
      <details v-if="detailFields.length || $scopedSlots.details" class="mobile-record-details">
        <summary>更多详情</summary>
        <dl v-if="detailFields.length" class="mobile-record-fields">
          <template v-for="field in detailFields">
            <dt :key="field.label + '-label'">{{ field.label }}</dt>
            <dd :key="field.label">{{ fieldValue(row, field) }}</dd>
          </template>
        </dl>
        <slot name="details" :row="row" />
      </details>
      <footer v-if="$scopedSlots.actions" class="mobile-record-actions"><slot name="actions" :row="row" /></footer>
    </article>
  </div>
</template>
<script>
export default {
  name: 'MobileRecordList',
  props: {
    rows: { type: Array, default: () => [] },
    loading: Boolean,
    rowKey: String,
    titleField: String,
    fallbackField: String,
    avatarField: String,
    fields: { type: Array, default: () => [] },
    detailFields: { type: Array, default: () => [] }
  },
  methods: {
    fieldValue(row, field) {
      const value = field.format ? field.format(row) : row[field.prop]
      return value === '' || value == null ? '—' : value
    }
  }
}
</script>
<style lang="scss">
.mobile-record-list { min-width: 0; min-height: 60px; }
.mobile-record {
  border: 1px solid #ebeef5; border-radius: 6px; padding: 12px; margin-bottom: 10px;
  overflow-wrap: anywhere; color: #303133;
  &-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px;
    img { width: 36px; height: 36px; border-radius: 50%; flex: none; }
    strong { flex: 1; min-width: 0; }
  }
  &-fields { display: grid; grid-template-columns: 80px minmax(0, 1fr); gap: 8px; font-size: 14px; margin: 10px 0;
    dt { color: #909399; } dd { margin: 0; min-width: 0; white-space: pre-wrap; }
  }
  &-details > summary { min-height: 40px; line-height: 40px; cursor: pointer; color: #409eff; }
  &-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px;
    .el-button { min-height: 40px; margin-left: 0; white-space: normal; }
  }
  &-empty { text-align: center; color: #909399; }
  &-text { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.6; }
}
</style>

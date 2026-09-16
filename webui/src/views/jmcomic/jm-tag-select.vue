<template>
  <el-select
    class="jm-tag-select"
    :value="innerValue"
    multiple
    filterable
    clearable
    collapse-tags
    :loading="loading"
    :disabled="disabled"
    :size="size"
    :placeholder="placeholder"
    :popper-class="popperClass"
    :allow-create="allowCreate"
    @change="handleChange"
    @visible-change="handleVisibleChange">
    <el-option v-for="tag in options" :key="tag" :label="tag" :value="tag"></el-option>
  </el-select>
</template>

<script>
import { allTags } from "@/api/jmcomic";

/**
 * JM标签选择器
 * 候选标签由组件内部调用 /jmcomic/manage/tags 接口获取，使用方只需v-model绑定标签数组
 *
 * 之所以是多选：后端 JmAlbumQueryReq.tags 是 List<String>，多个标签之间按"或"匹配
 *
 * 候选只在首次展开下拉时请求一次，之后(关键字过滤、清空关键字)都只在内存中过滤：
 * 接口不支持分页/关键字，一次返回全部标签(全表扫描)，不适合每次展开都请求
 *
 * 用法：
 * <jm-tag-select v-model="albumQuery.tags" class="form-input"/>
 * 方法：refresh() 重新拉取候选(如新增/删除了JM记录导致标签变化)
 */
export default {
  name: 'JmTagSelect',
  props: {
    /**
     * 已选标签数组，空值为[]或null
     */
    value: {
      type: Array,
      default: () => []
    },
    placeholder: {
      type: String,
      default: '请选择标签'
    },
    disabled: {
      type: Boolean,
      default: false
    },
    size: {
      type: String,
      default: 'small'
    },
    /**
     * 是否允许输入候选中不存在的标签
     */
    allowCreate: {
      type: Boolean,
      default: false
    },
    /**
     * 下拉框样式命名空间，需要单独覆盖下拉样式时传入
     */
    popperClass: {
      type: String,
      default: 'jm-tag-select-popper'
    }
  },
  data() {
    return {
      // 下拉框候选
      options: [],
      loading: false,
      // 候选是否已请求过，请求失败时不置为true，下次展开会重试
      loaded: false,
      // 请求序号，只采用最后一次请求的结果，避免先发出的请求后返回覆盖结果
      loadSeq: 0
    }
  },
  computed: {
    /**
     * el-select多选要求数组，外部传null/undefined时统一成[]
     * 空白项会导致输入框里出现一个空标签，这里统一过滤掉，与后端忽略空白标签的行为一致
     */
    innerValue() {
      if (!Array.isArray(this.value)) {
        return []
      }
      return this.value.filter(e => e !== '' && e !== null && e !== undefined)
    }
  },
  methods: {
    /**
     * 下拉框展开时加载候选，候选只请求一次
     */
    handleVisibleChange(visible) {
      if (visible && !this.loaded) {
        this.loadTags()
      }
    },
    /**
     * 拉取全部标签，接口不支持分页/关键字，所以只在内存中过滤
     */
    loadTags() {
      if (this.loading) {
        return
      }
      const seq = ++this.loadSeq
      this.loading = true
      allTags().then(({data: {code, message, data}}) => {
        if (seq !== this.loadSeq) {
          return
        }
        if (code !== 200) {
          return this.$message.error(message || '标签查询失败')
        }
        this.options = this.normalizeOptions(data)
        this.loaded = true
      }).catch(e => {
        if (seq === this.loadSeq) {
          this.$message.error((e && e.message) || '标签查询失败')
        }
      }).finally(() => {
        if (seq === this.loadSeq) {
          this.loading = false
        }
      })
    },
    /**
     * 去空白、去重、排序
     * 接口返回的是"按入库顺序去重"的结果，顺序随机，排序后更方便在长列表里查找
     */
    normalizeOptions(list) {
      const tags = (list || [])
        .filter(e => e !== null && e !== undefined)
        .map(e => `${e}`.trim())
        .filter(e => e)
      return Array.from(new Set(tags)).sort((a, b) => a.localeCompare(b))
    },
    handleChange(v) {
      this.$emit('input', Array.isArray(v) ? v.slice() : [])
    },
    /**
     * 重新拉取候选，由使用方在数据变更后调用(如新增/删除了JM记录)
     * 候选还没加载过时不必发请求：下次展开下拉会自己拉
     */
    refresh() {
      if (!this.loaded) {
        return
      }
      // 置为未加载，重新拉取失败时下次展开下拉还会重试
      this.loaded = false
      this.loadTags()
    }
  }
}
</script>

<style lang="scss" scoped>
/**
 * 宽度由使用方的form-input等class控制，这里只保证内部input占满宽度
 */
.jm-tag-select {
  ::v-deep .el-input {
    width: 100%;
  }
}
</style>

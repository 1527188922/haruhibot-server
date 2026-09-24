<template>
  <el-select
    class="jm-author-select"
    :value="innerValue"
    filterable
    clearable
    :loading="loading"
    :disabled="disabled"
    :size="size"
    :placeholder="placeholder"
    :popper-class="popperClass"
    :allow-create="allowCreate"
    @change="handleChange"
    @visible-change="handleVisibleChange">
    <el-option v-for="author in options" :key="author" :label="author" :value="author"></el-option>
  </el-select>
</template>

<script>
import { allAuthors } from "@/api/jmcomic";

/**
 * JM作者选择器
 * 候选作者由组件内部调用 /jmcomic/manage/authors 接口获取，使用方只需v-model绑定作者字符串
 *
 * 之所以是单选：后端 JmAlbumQueryReq.author 是 String，按like匹配
 *
 * 候选只在首次展开下拉时请求一次，之后(关键字过滤、清空关键字)都只在内存中过滤：
 * 接口不支持分页/关键字，一次返回全部作者(全表扫描)，不适合每次展开都请求
 *
 * allowCreate默认开启：后端是按like匹配的，除了选中候选，仍需要支持直接输入作者名(或名字的一部分)
 * 然后回车查询，与改造前的el-input输入行为保持一致(回车会先提交输入框内容，再触发展开的keyup.enter)
 *
 * 用法：
 * <jm-author-select v-model="albumQuery.author" class="form-input"/>
 * 方法：refresh() 重新拉取候选(如新增/删除了JM记录导致作者变化)
 */
export default {
  name: 'JmAuthorSelect',
  props: {
    /**
     * 已选作者，空值为''或null
     */
    value: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: '请选择作者'
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
     * 是否允许输入候选中不存在的作者，默认允许，以保留原来的模糊查询能力
     */
    allowCreate: {
      type: Boolean,
      default: true
    },
    /**
     * 下拉框样式命名空间，需要单独覆盖下拉样式时传入
     */
    popperClass: {
      type: String,
      default: 'jm-author-select-popper'
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
     * el-select单选要求字符串，外部传null/undefined时统一成''
     */
    innerValue() {
      return this.value === null || this.value === undefined ? '' : `${this.value}`
    }
  },
  methods: {
    /**
     * 下拉框展开时加载候选，候选只请求一次
     */
    handleVisibleChange(visible) {
      if (visible && !this.loaded) {
        this.loadAuthors()
      }
    },
    /**
     * 拉取全部作者，接口不支持分页/关键字，所以只在内存中过滤
     */
    loadAuthors() {
      if (this.loading) {
        return
      }
      const seq = ++this.loadSeq
      this.loading = true
      allAuthors().then(({data: {code, message, data}}) => {
        if (seq !== this.loadSeq) {
          return
        }
        if (code !== 200) {
          return this.$message.error(message || '作者查询失败')
        }
        this.options = this.normalizeOptions(data)
        this.loaded = true
      }).catch(e => {
        if (seq === this.loadSeq) {
          this.$message.error((e && e.message) || '作者查询失败')
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
      const authors = (list || [])
        .filter(e => e !== null && e !== undefined)
        .map(e => `${e}`.trim())
        .filter(e => e)
      return Array.from(new Set(authors)).sort((a, b) => a.localeCompare(b))
    },
    handleChange(v) {
      this.$emit('input', v === null || v === undefined ? '' : v)
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
      this.loadAuthors()
    }
  }
}
</script>

<style lang="scss" scoped>
/**
 * 宽度由使用方的form-input等class控制，这里只保证内部input占满宽度
 */
.jm-author-select {
  ::v-deep .el-input {
    width: 100%;
  }
}
</style>

<template>
  <el-dialog :title="title" :visible.sync="visibleProxy" width="480px" @closed="handleClosed">
    <div class="jm-fav-picker">
      <div class="jm-fav-picker-tip">
        选择要收藏到的收藏夹。输入不存在的名称会直接新建该收藏夹，并把这
        {{albumCount}} 本漫画收进去。
      </div>

      <el-input
        v-model="keyword"
        size="small"
        clearable
        prefix-icon="el-icon-search"
        placeholder="筛选或输入新收藏夹名称"
        @keyup.enter.native="confirmByKeyword"
      ></el-input>

      <div class="jm-fav-picker-list" v-loading="loading">
        <div v-for="item in filteredFavorites" :key="`picker-${item.id}`"
             class="jm-fav-picker-item"
             :class="{'is-active': !isCreateMode && selectedId === item.id}"
             @click="selectExisting(item)">
          <span class="jm-fav-picker-name" :title="item.name">{{item.name}}</span>
          <el-tag v-if="item.isDefault" size="mini" type="info" effect="plain">默认</el-tag>
          <span class="jm-fav-picker-count">{{item.albumCount}}</span>
          <i v-if="!isCreateMode && selectedId === item.id" class="el-icon-check"></i>
        </div>

        <!-- 输入的名称在现有收藏夹里找不到时，提供"新建并收藏" -->
        <div v-if="isCreateMode" class="jm-fav-picker-item is-create" @click="selectedId = null">
          <i class="el-icon-plus"></i>
          <span class="jm-fav-picker-name">新建收藏夹「{{keywordTrimmed}}」并收藏</span>
          <i v-if="selectedId === null" class="el-icon-check"></i>
        </div>

        <div v-if="!loading && filteredFavorites.length === 0 && !isCreateMode" class="jm-fav-picker-empty">
          没有匹配的收藏夹
        </div>
      </div>
    </div>

    <span slot="footer">
      <el-button size="small" @click="visibleProxy = false">取消</el-button>
      <el-button type="primary" size="small" :disabled="!canConfirm" :loading="submitting" @click="confirm">
        {{ confirmText }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { listFavorites } from "@/api/jmcomic";

/**
 * 收藏到收藏夹的选择弹窗。
 * 支持两种模式：
 * - 已有收藏夹：选中列表中的某项
 * - 新收藏夹：输入的名称不存在时，选中"新建并收藏"，后端会按名称建好再收藏
 */
export default {
  name: 'JmFavoritePicker',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    // 待收藏的漫画id列表
    albumIds: {
      type: Array,
      default: () => []
    },
    // 传入后不再重复请求收藏夹列表
    favoriteOptions: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      loading: false,
      submitting: false,
      favorites: [],
      keyword: '',
      // null 表示"新建"，否则为收藏夹id
      selectedId: null
    }
  },
  computed: {
    visibleProxy: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    },
    albumCount() {
      return this.albumIds.length
    },
    keywordTrimmed() {
      return String(this.keyword || '').trim()
    },
    filteredFavorites() {
      const kw = this.keywordTrimmed.toLowerCase()
      if (!kw) {
        return this.favorites
      }
      return this.favorites.filter(item => String(item.name || '').toLowerCase().includes(kw))
    },
    // 名称与现有收藏夹都不相同（且非空）时，进入"新建"模式
    isCreateMode() {
      if (!this.keywordTrimmed) {
        return false
      }
      const kw = this.keywordTrimmed.toLowerCase()
      return !this.favorites.some(item => String(item.name || '').toLowerCase() === kw)
    },
    canConfirm() {
      return this.isCreateMode || this.selectedId !== null
    },
    confirmText() {
      if (this.isCreateMode) {
        return `新建并收藏（${this.albumCount}）`
      }
      return `收藏（${this.albumCount}）`
    },
    title() {
      return this.albumCount > 1 ? `收藏到收藏夹（${this.albumCount}本）` : '收藏到收藏夹'
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.init()
      }
    },
    keyword() {
      // 手动输入时默认落到"新建"，只有点已有项才切换为既有收藏夹
      if (this.isCreateMode) {
        this.selectedId = null
      } else if (this.selectedId === null) {
        const matched = this.favorites.find(item =>
          String(item.name || '').toLowerCase() === this.keywordTrimmed.toLowerCase())
        this.selectedId = matched ? matched.id : null
      }
    }
  },
  methods: {
    async init() {
      this.keyword = ''
      this.selectedId = null
      await this.loadFavorites()
      // 默认选中默认收藏夹，用户直接点确定即可完成"收藏"
      const defaultFavorite = this.favorites.find(item => item.isDefault) || this.favorites[0]
      if (defaultFavorite) {
        this.selectedId = defaultFavorite.id
      }
    },
    async loadFavorites() {
      if (this.favoriteOptions.length > 0) {
        this.favorites = this.favoriteOptions
        return
      }
      this.loading = true
      try {
        const { data: { code, data } } = await listFavorites()
        if (code === 200) {
          this.favorites = data || []
        }
      } catch (error) {
        this.$emit('error', error)
      } finally {
        this.loading = false
      }
    },
    selectExisting(item) {
      this.selectedId = item.id
      this.keyword = item.name
    },
    confirmByKeyword() {
      if (this.canConfirm) {
        this.confirm()
      }
    },
    confirm() {
      if (!this.canConfirm) {
        return
      }
      if (this.isCreateMode) {
        this.$emit('confirm', { favoriteId: null, favoriteName: this.keywordTrimmed })
        return
      }
      const item = this.favorites.find(fav => fav.id === this.selectedId)
      this.$emit('confirm', { favoriteId: this.selectedId, favoriteName: item ? item.name : null })
    },
    handleClosed() {
      this.keyword = ''
      this.selectedId = null
      this.submitting = false
    }
  }
}
</script>

<style lang="scss" scoped>
.jm-fav-picker {
  &-tip {
    color: #909399;
    font-size: 12px;
    line-height: 1.6;
    margin-bottom: 10px;
  }

  &-list {
    margin-top: 10px;
    max-height: 300px;
    overflow-y: auto;
    border: 1px solid #ebeef5;
    border-radius: 4px;
  }

  &-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    cursor: pointer;
    font-size: 13px;
    border-bottom: 1px solid #f5f7fa;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #f5f7fa;
    }

    &.is-active {
      background: #ecf5ff;
      color: #409eff;
    }

    &.is-create {
      color: #409eff;
    }
  }

  &-name {
    flex: 1 1 auto;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &-count {
    flex: 0 0 auto;
    color: #909399;
    font-size: 12px;
  }

  &-empty {
    padding: 12px;
    text-align: center;
    color: #909399;
    font-size: 12px;
  }
}
</style>

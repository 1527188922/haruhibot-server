<template>
  <el-dialog :title="title" :visible.sync="visibleProxy" width="480px" @closed="handleClosed">
    <div class="jm-fav-picker">
      <div class="jm-fav-picker-tip">{{ tipText }}</div>

      <el-input
        v-model="keyword"
        size="small"
        clearable
        prefix-icon="el-icon-search"
        placeholder="筛选收藏夹，或输入新名称新建"
        @keyup.enter.native="toggleByKeyword"
      ></el-input>

      <div class="jm-fav-picker-list" v-loading="loading">
        <div v-for="item in filteredFavorites" :key="`picker-${item.id}`"
             class="jm-fav-picker-item"
             :class="{'is-active': isSelected(item.id)}"
             @click="toggleFavorite(item)">
          <el-checkbox :value="isSelected(item.id)" @click.native.stop.prevent="toggleFavorite(item)"></el-checkbox>
          <span class="jm-fav-picker-name" :title="item.name">{{item.name}}</span>
          <el-tag v-if="item.isDefault" size="mini" type="info" effect="plain">默认</el-tag>
          <span class="jm-fav-picker-count">{{item.albumCount}}</span>
        </div>

        <!-- 已勾选的新建收藏夹 -->
        <div v-for="(name, index) in newNames" :key="`picker-new-${index}`"
             class="jm-fav-picker-item is-create"
             @click="removeNewName(index)">
          <i class="el-icon-check"></i>
          <span class="jm-fav-picker-name">新建收藏夹「{{name}}」</span>
          <i class="el-icon-close"></i>
        </div>

        <!-- 输入的名称在现有收藏夹里找不到时，提供"新建" -->
        <div v-if="isCreateMode" class="jm-fav-picker-item is-create" @click="addNewName">
          <i class="el-icon-plus"></i>
          <span class="jm-fav-picker-name">新建收藏夹「{{keywordTrimmed}}」</span>
        </div>

        <div v-if="!loading && filteredFavorites.length === 0 && !isCreateMode" class="jm-fav-picker-empty">
          没有匹配的收藏夹
        </div>
      </div>

      <div v-if="isEditMode && selectedTotal === 0" class="jm-fav-picker-warn">
        未勾选任何收藏夹，保存后这 {{albumCount}} 本漫画会从所有收藏夹移出（等于取消收藏）
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
 * 收藏夹选择弹窗，支持多选。
 *
 * 两种模式：
 * - collect（默认）：把漫画收藏到勾选的收藏夹，输入不存在的名称可以顺带新建；
 *                    打开时默认勾选默认收藏夹，直接确定即为"收藏到默认收藏夹"
 * - edit：更改漫画的收藏夹。打开时回显这些漫画当前所属的收藏夹，
 *         保存后以勾选结果为准（缺的补上、多的移出；全不勾=从所有收藏夹移出）
 */
export default {
  name: 'JmFavoritePicker',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    // 待处理（收藏/更改收藏夹）的漫画id列表
    albumIds: {
      type: Array,
      default: () => []
    },
    // 传入后不再重复请求收藏夹列表
    favoriteOptions: {
      type: Array,
      default: () => []
    },
    // collect=收藏到收藏夹；edit=更改收藏夹
    mode: {
      type: String,
      default: 'collect'
    },
    // edit 模式下回显：这些漫画当前所属的收藏夹id
    currentFavoriteIds: {
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
      // 勾选的已有收藏夹id
      selectedIds: [],
      // 勾选的"新建收藏夹"名称
      newNames: []
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
    isEditMode() {
      return this.mode === 'edit'
    },
    albumCount() {
      return this.albumIds.length
    },
    keywordTrimmed() {
      return String(this.keyword || '').trim()
    },
    filteredFavorites() {
      const kw = this.keywordTrimmed.toLowerCase()
      const list = kw
        ? this.favorites.filter(item => String(item.name || '').toLowerCase().includes(kw))
        : this.favorites
      // 已勾选的排在最上方（sort 是稳定排序，同组内保持原顺序）
      return [...list].sort((a, b) => Number(this.isSelected(b.id)) - Number(this.isSelected(a.id)))
    },
    // 名称与现有收藏夹都不相同（且非空）时，可以"新建"
    isCreateMode() {
      if (!this.keywordTrimmed) {
        return false
      }
      const kw = this.keywordTrimmed.toLowerCase()
      const existsInFavorites = this.favorites.some(item => String(item.name || '').toLowerCase() === kw)
      const existsInNewNames = this.newNames.some(name => name.toLowerCase() === kw)
      return !existsInFavorites && !existsInNewNames
    },
    selectedTotal() {
      return this.selectedIds.length + this.newNames.length
    },
    canConfirm() {
      // 更改收藏夹时允许一个都不勾（等于从所有收藏夹移出），收藏时必须至少选一个目标
      return this.isEditMode || this.selectedTotal > 0
    },
    confirmText() {
      if (this.isEditMode) {
        return `保存（${this.albumCount}本 → ${this.selectedTotal}个收藏夹）`
      }
      return `收藏（${this.albumCount}本 → ${this.selectedTotal}个收藏夹）`
    },
    title() {
      if (this.isEditMode) {
        return this.albumCount > 1 ? `更改收藏夹（${this.albumCount}本）` : '更改收藏夹'
      }
      return this.albumCount > 1 ? `收藏到收藏夹（${this.albumCount}本）` : '收藏到收藏夹'
    },
    tipText() {
      if (this.isEditMode) {
        return `勾选这 ${this.albumCount} 本漫画要归属的收藏夹（可多选）。输入不存在的名称会直接新建；` +
          '未勾选的收藏夹会被移出。'
      }
      return `勾选要收藏到的收藏夹（可多选）。输入不存在的名称会直接新建该收藏夹，并把这 ` +
        `${this.albumCount} 本漫画收进去。`
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.init()
      }
    }
  },
  methods: {
    async init() {
      this.keyword = ''
      this.newNames = []
      this.selectedIds = []
      await this.loadFavorites()
      if (this.isEditMode) {
        // 回显当前归属，只保留仍然存在的收藏夹
        const exists = new Set(this.favorites.map(item => item.id))
        this.selectedIds = (this.currentFavoriteIds || []).filter(id => exists.has(id))
        return
      }
      // 收藏默认选中默认收藏夹，用户直接点确定即可完成"收藏"
      const defaultFavorite = this.favorites.find(item => item.isDefault) || this.favorites[0]
      if (defaultFavorite) {
        this.selectedIds = [defaultFavorite.id]
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
    isSelected(id) {
      return this.selectedIds.includes(id)
    },
    toggleFavorite(item) {
      if (this.isSelected(item.id)) {
        this.selectedIds = this.selectedIds.filter(id => id !== item.id)
        return
      }
      this.selectedIds = [...this.selectedIds, item.id]
    },
    addNewName() {
      const name = this.keywordTrimmed
      if (!name || this.newNames.includes(name)) {
        return
      }
      this.newNames = [...this.newNames, name]
      this.keyword = ''
    },
    removeNewName(index) {
      this.newNames = this.newNames.filter((name, i) => i !== index)
    },
    /**
     * 输入框回车：能新建就新建，否则在只剩一个匹配项时直接勾选
     */
    toggleByKeyword() {
      if (this.isCreateMode) {
        this.addNewName()
        return
      }
      if (this.filteredFavorites.length === 1) {
        this.toggleFavorite(this.filteredFavorites[0])
      }
    },
    confirm() {
      if (!this.canConfirm) {
        return
      }
      this.$emit('confirm', {
        favoriteIds: [...this.selectedIds],
        favoriteNames: [...this.newNames]
      })
    },
    handleClosed() {
      this.keyword = ''
      this.selectedIds = []
      this.newNames = []
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

  &-warn {
    margin-top: 10px;
    padding: 6px 8px;
    border-radius: 4px;
    background: #fdf6ec;
    color: #e6a23c;
    font-size: 12px;
    line-height: 1.6;
  }
}
</style>

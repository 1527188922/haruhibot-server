<template>
  <!--
    收藏夹 tab 的内容区。
    查询条件块、内容块这两块 basic-container 由父组件 jmcomic/index.vue 提供：
    它们必须是页面根节点（#JmcomicManage）的直接子节点，才能和 JM主记录 tab 一样，
    在两块卡片之间露出页面基底色；若放在本组件里，就会被 el-tabs 所在的卡片包住，
    露出的全是卡片白底，上下两块看起来连成一片。
    这里只负责内容卡片内部：左侧收藏夹列表 + 右侧漫画列表/瀑布流。
  -->
  <div class="jm-favorite">
    <!-- 移动端：收藏夹列表收成横向子 tab；桌面端为左侧竖列 -->
    <div class="jm-favorite-side" :class="{'jm-favorite-side--collapsed': isMobileView && !sideExpanded}">
      <div class="jm-favorite-side-head">
        <span class="jm-favorite-side-title">收藏夹</span>
        <span class="jm-favorite-side-ops">
          <el-button type="text" size="mini" icon="el-icon-refresh" :loading="favoriteLoading" @click="loadFavorites"></el-button>
          <el-button type="text" size="mini" icon="el-icon-plus" @click="openCreateFavorite">新建</el-button>
          <el-button v-if="isMobileView" type="text" size="mini"
                     :icon="sideExpanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"
                     @click="sideExpanded = !sideExpanded"></el-button>
        </span>
      </div>
      <div v-show="!isMobileView || sideExpanded" class="jm-favorite-side-list">
        <div v-for="item in favorites" :key="`fav-${item.id}`"
             class="jm-favorite-item"
             :class="{'jm-favorite-item--active': item.id === activeFavoriteId}"
             @click="selectFavorite(item)">
          <span class="jm-favorite-item-name" :title="item.name">{{item.name}}</span>
          <span class="jm-favorite-item-count">{{item.albumCount}}</span>
          <span class="jm-favorite-item-ops" @click.stop>
            <el-tooltip v-if="!item.isDefault" content="重命名" placement="top">
              <el-button type="text" size="mini" icon="el-icon-edit" @click="openRenameFavorite(item)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="!item.isDefault" content="删除收藏夹" placement="top">
              <el-button type="text" size="mini" icon="el-icon-delete" class="danger-text-btn" @click="confirmDeleteFavorite(item)"></el-button>
            </el-tooltip>
            <el-tag v-else size="mini" type="info" effect="plain">默认</el-tag>
          </span>
        </div>
        <div v-if="!favoriteLoading && favorites.length === 0" class="jm-favorite-empty">暂无收藏夹</div>
      </div>
    </div>

    <div class="jm-favorite-main">
      <!-- 移动端：收藏夹的重命名/删除收进横向 tab 后单独一行，否则小屏上无法操作 -->
      <div v-if="isMobileView && activeFavorite" class="jm-favorite-current-ops">
        <span class="jm-favorite-current-name" :title="activeFavorite.name">{{activeFavorite.name}}</span>
        <span class="jm-favorite-current-btns">
          <el-button v-if="!activeFavorite.isDefault" type="text" size="mini" icon="el-icon-edit"
                     @click="openRenameFavorite(activeFavorite)">重命名</el-button>
          <el-button v-if="!activeFavorite.isDefault" type="text" size="mini" icon="el-icon-delete"
                     class="danger-text-btn" @click="confirmDeleteFavorite(activeFavorite)">删除</el-button>
        </span>
      </div>

      <!-- 与 JM主记录/在线搜索保持一致：操作按钮靠左，列表/瀑布流切换靠最右 -->
      <div class="data-table-option-buts">
        <el-button type="danger" size="small" plain icon="el-icon-delete" :disabled="selection.length === 0"
                   @click="removeSelectedFromFavorite">移出本收藏夹</el-button>
        <el-button type="primary" size="small" plain icon="el-icon-folder-opened" :disabled="selection.length === 0"
                   @click="changeSelectedFavorites">更改收藏夹</el-button>
        <el-badge class="jm-task-badge" :value="taskActiveCount" :hidden="taskActiveCount === 0" type="warning">
          <el-button type="primary" size="small" plain icon="el-icon-s-operation" @click="$emit('open-tasks')">任务队列</el-button>
        </el-badge>
        <el-dropdown v-if="viewMode === 'list'" trigger="click" :hide-on-click="false">
          <el-button type="primary" size="small" plain icon="el-icon-setting">列设置</el-button>
          <el-dropdown-menu slot="dropdown" class="jm-column-dropdown">
            <el-checkbox-group v-model="favoriteVisibleColumns" class="jm-column-check-group" @change="handleColumnsChange">
              <el-checkbox v-for="column in favoriteColumnOptions" :key="column.key" :label="column.key">{{column.label}}</el-checkbox>
            </el-checkbox-group>
          </el-dropdown-menu>
        </el-dropdown>
        <el-radio-group v-model="viewMode" class="jm-view-switch" size="mini">
          <el-radio-button label="list"><i class="el-icon-s-unfold"></i> 列表</el-radio-button>
          <el-radio-button label="waterfall"><i class="el-icon-s-grid"></i> 瀑布流</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 列表/瀑布流：与 JM主记录 tab 共用同一个组件，展示的信息完全一致 -->
      <jm-album-view ref="albumView"
                     :albums="albums"
                     :loading="loading"
                     :view-mode="viewMode"
                     :visible-columns="favoriteVisibleColumns"
                     :actions="favoriteRowActions"
                     :action-loading="operationLoading"
                     :action-disabled="operationDisabled"
                     :favorite-name-map="favoriteNameMap"
                     empty-text="该收藏夹下还没有漫画"
                     @action="handleRowAction"
                     @collect-toggle="handleCollectToggle"
                     @selection-change="handleSelectionChange"
                     @chapters="emitChapters"
                     @jump-album="emitJumpAlbum"></jm-album-view>

      <div class="pagination-box">
        <el-pagination background
                       :current-page="page.currentPage"
                       :page-size="page.pageSize"
                       :total="page.total"
                       :layout="paginationLayout"
                       @current-change="currentChange" />
      </div>
    </div>
  </div>
</template>

<script>
import {
  listFavorites,
  renameFavorite,
  deleteFavorite,
  searchAlbums,
  removeAlbumsFromFavorite
} from "@/api/jmcomic";
import JmAlbumView from "./jm-album-view.vue";
import { FAVORITE_COLUMN_OPTIONS, DEFAULT_FAVORITE_COLUMNS } from "./jm-album-columns";
import { getStore, setStore } from "@/util/store";

const FAVORITE_VIEW_MODE_KEY = 'jmFavoriteViewMode';

export default {
  name: 'JmFavoritePanel',
  components: { JmAlbumView },
  props: {
    /**
     * 查询条件由父组件（jmcomic/index.vue）持有：
     * 查询条件已经独立成一块 basic-container 放在父组件里，
     * 和本组件所在的卡片平级，父组件的查询/重置按钮通过 searchFirst() 驱动本组件。
     */
    query: {
      type: Object,
      default: () => ({ name: '', author: '' })
    },
    /**
     * 下载/生成zip/pdf 的按钮 loading/disabled 判定，由父组件传入，
     * 与 JM主记录 tab 用的是同一套（(row, action) => boolean）
     */
    operationLoading: {
      type: Function,
      default: null
    },
    operationDisabled: {
      type: Function,
      default: null
    },
    /**
     * 进行中+排队中的任务数，用于任务队列按钮的角标（与 JM主记录 tab 共用一个面板）
     */
    taskActiveCount: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      favorites: [],
      favoriteLoading: false,
      activeFavoriteId: null,
      sideExpanded: false,
      albums: [],
      loading: false,
      selection: [],
      page: { currentPage: 1, pageSize: 20, total: 0 },
      viewMode: getStore({ name: FAVORITE_VIEW_MODE_KEY }) || 'waterfall',
      // 列定义与 JM主记录 tab 共用（jm-album-columns.js），另外多两列收藏夹专属信息
      favoriteVisibleColumns: [...DEFAULT_FAVORITE_COLUMNS],
      favoriteColumnOptions: FAVORITE_COLUMN_OPTIONS,
      favoriteRowActions: [
        { key: 'preview', icon: 'el-icon-view', type: 'primary', tooltip: '预览漫画' },
        { key: 'download', icon: 'el-icon-download', type: 'primary', tooltip: '下载漫画' },
        { key: 'zip', icon: 'el-icon-folder-add', type: 'success', tooltip: '生成zip' },
        { key: 'pdf', icon: 'el-icon-document-add', type: 'warning', tooltip: '生成pdf' },
        { key: 'change-favorites', icon: 'el-icon-folder-opened', type: 'primary', tooltip: '更改收藏夹' },
        { key: 'remove', icon: 'el-icon-delete', type: 'danger', tooltip: '移出本收藏夹' }
      ]
    }
  },
  computed: {
    // 手机上分页按钮过多会换行，去掉 jumper
    paginationLayout() {
      return this.isMobileView ? 'total, prev, pager, next' : 'total, prev, pager, next, jumper'
    },
    activeFavorite() {
      return this.favorites.find(item => item.id === this.activeFavoriteId) || null
    },
    /**
     * 收藏夹id -> 名称，供列表展示"所属收藏夹"
     */
    favoriteNameMap() {
      const map = {}
      this.favorites.forEach(item => {
        map[`${item.id}`] = item.name
      })
      return map
    }
  },
  watch: {
    viewMode(value) {
      setStore({ name: FAVORITE_VIEW_MODE_KEY, content: value })
    }
  },
  created() {
    this.init()
  },
  methods: {
    async init() {
      await this.loadFavorites()
      // 默认选中第一个收藏夹（后端已保证默认收藏夹排在最前）
      if (!this.activeFavoriteId && this.favorites.length > 0) {
        this.activeFavoriteId = this.favorites[0].id
      }
      if (this.activeFavoriteId) {
        this.searchFirst()
      }
    },
    async loadFavorites() {
      this.favoriteLoading = true
      try {
        const { data: { code, message, data } } = await listFavorites()
        if (code !== 200) {
          this.$message.error(message || '加载收藏夹失败')
          return
        }
        this.favorites = data || []
        // 当前选中的收藏夹被删掉时回落到第一个
        if (this.activeFavoriteId && !this.favorites.some(item => item.id === this.activeFavoriteId)) {
          this.activeFavoriteId = this.favorites.length > 0 ? this.favorites[0].id : null
          if (this.activeFavoriteId) {
            this.searchFirst()
          } else {
            this.albums = []
            this.page.total = 0
          }
        }
      } catch (error) {
        this.$emit('error', error)
      } finally {
        this.favoriteLoading = false
      }
    },
    selectFavorite(item) {
      if (item.id === this.activeFavoriteId) {
        return
      }
      this.activeFavoriteId = item.id
      if (this.isMobileView) {
        this.sideExpanded = false
      }
      this.searchFirst()
    },
    /**
     * 从第一页开始查询，供内部（初始化/切换收藏夹）与父组件的查询按钮调用。
     * 查询条件取值自 query prop，父组件是同一个对象引用，改完立即生效。
     */
    searchFirst() {
      this.page.currentPage = 1
      this.loadAlbums()
    },
    currentChange(page) {
      this.page.currentPage = page
      this.loadAlbums()
    },
    async loadAlbums() {
      if (!this.activeFavoriteId) {
        this.albums = []
        this.page.total = 0
        return
      }
      this.loading = true
      try {
        const { data: { code, message, data } } = await searchAlbums({
          currentPage: this.page.currentPage,
          pageSize: this.page.pageSize,
          name: this.query.name || null,
          author: this.query.author || null,
          favoriteId: this.activeFavoriteId
        })
        if (code !== 200) {
          this.$message.error(message || '查询失败')
          return
        }
        this.albums = data.records || []
        this.page.total = data.total || 0
      } catch (error) {
        this.$emit('error', error)
      } finally {
        this.loading = false
      }
    },
    handleSelectionChange(selection) {
      this.selection = selection
    },
    handleColumnsChange() {
      if (this.$refs.albumView) {
        this.$refs.albumView.doLayout()
      }
    },
    emitChapters(row) {
      this.$emit('chapters', row)
    },
    emitJumpAlbum(id) {
      this.$emit('jump-album', id)
    },
    /**
     * 行操作分发：
     * - remove / change-favorites 是收藏夹 tab 独有的，本组件自己处理
     * - preview / download / zip / pdf 抛给父组件，用与 JM主记录 tab 完全相同的那套逻辑
     */
    handleRowAction(key, row) {
      if (key === 'remove') {
        this.removeOneFromFavorite(row)
        return
      }
      if (key === 'change-favorites') {
        this.$emit('change-favorites', { albumIds: [row.id], favoriteIds: [...(row.favoriteIds || [])] })
        return
      }
      this.$emit('action', key, row)
    },
    /**
     * 收藏夹 tab 里的漫画必然已收藏，星标只提供"取消收藏（从所有收藏夹移出）"
     */
    handleCollectToggle(row) {
      this.doRemove([row.id],
        `确认取消收藏「${row.name || ('JM' + row.id)}」？<br/>会把它从<b>所有收藏夹</b>移出。`, false)
    },
    openCreateFavorite() {
      this.$prompt('请输入收藏夹名称', '新建收藏夹', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValidator: value => !!String(value || '').trim() || '名称不能为空'
      }).then(({ value }) => {
        this.$emit('create-favorite', String(value).trim())
      }).catch(() => {})
    },
    openRenameFavorite(item) {
      this.$prompt('请输入新的收藏夹名称', '重命名收藏夹', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: item.name,
        inputValidator: value => !!String(value || '').trim() || '名称不能为空'
      }).then(async ({ value }) => {
        const name = String(value).trim()
        if (name === item.name) {
          return
        }
        const { data: { code, message } } = await renameFavorite({ id: item.id, name })
        if (code !== 200) {
          return this.$message.error(message || '重命名失败')
        }
        this.$message.success('重命名成功')
        this.loadFavorites()
      }).catch(() => {})
    },
    confirmDeleteFavorite(item) {
      this.$confirm(`确认删除收藏夹「${item.name}」？<br/>其中的漫画若不在其他收藏夹，会自动移入默认收藏夹。`, '删除收藏夹', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true
      }).then(async () => {
        const { data: { code, message } } = await deleteFavorite(item.id)
        if (code !== 200) {
          return this.$message.error(message || '删除失败')
        }
        this.$message.success('删除完成')
        await this.loadFavorites()
        this.$emit('albums-changed')
      }).catch(() => {})
    },
    removeSelectedFromFavorite() {
      if (this.selection.length === 0) {
        return
      }
      this.doRemove(this.selection.map(row => row.id),
        `确认把选中的 ${this.selection.length} 本漫画移出本收藏夹？`)
    },
    removeOneFromFavorite(row) {
      this.doRemove([row.id], `确认把「${row.name || ('JM' + row.id)}」移出本收藏夹？`)
    },
    /**
     * 更改选中漫画的收藏夹。
     * 回显取所有选中漫画"共同所属"的收藏夹；保存后统一改为弹窗里勾选的集合
     */
    changeSelectedFavorites() {
      if (this.selection.length === 0) {
        return
      }
      const commonFavoriteIds = this.selection.reduce((acc, row) => {
        const ids = row.favoriteIds || []
        return acc === null ? [...ids] : acc.filter(id => ids.includes(id))
      }, null) || []
      this.$emit('change-favorites', {
        albumIds: this.selection.map(row => row.id),
        favoriteIds: commonFavoriteIds
      })
    },
    /**
     * 移出收藏夹
     * @param keepInOtherFavorites true=只移出当前收藏夹；false=从所有收藏夹移出（取消收藏）
     */
    doRemove(albumIds, tip, keepInOtherFavorites = true) {
      this.$confirm(tip, keepInOtherFavorites ? '移出收藏夹' : '取消收藏', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true
      }).then(async () => {
        const { data: { code, message } } = await removeAlbumsFromFavorite({
          albumIds,
          favoriteId: keepInOtherFavorites ? this.activeFavoriteId : undefined
        })
        if (code !== 200) {
          return this.$message.error(message || '移出失败')
        }
        this.$message.success(keepInOtherFavorites ? '已移出本收藏夹' : '已取消收藏')
        await this.loadFavorites()
        this.loadAlbums()
        this.$emit('albums-changed')
      }).catch(() => {})
    },
    /* ==================== 供父组件调用 ==================== */
    reload() {
      this.loadFavorites()
      if (this.activeFavoriteId) {
        this.loadAlbums()
      }
    },
    /**
     * 收藏夹名称 -> id，供父组件收藏时定位默认收藏夹
     */
    findFavoriteByName(name) {
      return this.favorites.find(item => item.name === name) || null
    }
  }
}
</script>

<style lang="scss" scoped>
.jm-favorite {
  display: flex;
  // 左右两块顶对齐，避免侧栏被拉伸到与表格同高
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}

/**
 * 左侧收藏夹列表。
 * 放在 basic-container 的卡片内，因此自身不再需要边框和底，改用右侧分隔线。
 * flex-shrink:0 保证窄屏下不会被右侧表格挤扁。
 */
.jm-favorite-side {
  flex: 0 0 170px;
  width: 170px;
  min-width: 0;
  border-right: 1px solid #ebeef5;
  padding-right: 12px;

  &-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;
  }

  &-title {
    font-weight: 600;
    font-size: 14px;
  }

  &-list {
    max-height: 620px;
    overflow-y: auto;
  }
}

.jm-favorite-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;

  &:hover {
    background: #f5f7fa;
  }

  &--active {
    background: #ecf5ff;
    color: #409eff;
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

  &-ops {
    flex: 0 0 auto;
    display: inline-flex;
    align-items: center;
  }
}

.jm-favorite-empty {
  color: #909399;
  font-size: 12px;
  padding: 8px;
  text-align: center;
}

.jm-favorite-main {
  flex: 1 1 auto;
  min-width: 0;
}

/**
 * 操作按钮行：与 index.vue 里 "#JmcomicManage .data-table-option-buts" 保持一致。
 * index.vue 那条规则写在 scoped 样式里（编译后带 [data-v-xxx]），只能命中它自己
 * 模板里的元素，本组件的模板命不中，这一行就会退化成普通块级元素，
 * "列表/瀑布流"的 margin-left:auto 随之失效，切换按钮会紧贴左侧按钮。
 * 所以这里必须再声明一份。
 */
.data-table-option-buts {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .el-button {
    margin-left: 0;
  }
}

/**
 * 列表/瀑布流切换靠最右，与左侧按钮拉开距离。
 * 需要上面 .data-table-option-buts 是 flex 容器才生效。
 */
.jm-view-switch {
  margin-left: auto;
}

.jm-favorite-current-ops {
  display: none;
}

/**
 * 移动端：左侧竖列收成顶部横向子 tab，先把纵向空间还给内容
 */
@media screen and (max-width: 768px) {
  .jm-favorite {
    display: block;
  }

  .jm-favorite-side {
    flex: none;
    width: 100%;
    // 竖列改为横向子 tab 后，右侧分隔线不再有意义
    border-right: none;
    padding-right: 0;
    margin-bottom: 8px;

    &-list {
      display: flex;
      flex-wrap: nowrap;
      gap: 6px;
      max-height: none;
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      padding-bottom: 4px;
    }
  }

  .jm-favorite-item {
    flex: 0 0 auto;
    border: 1px solid #ebeef5;

    // 横向 tab 里放不下重命名/删除，改由下方当前收藏夹操作行提供
    &-ops {
      display: none;
    }
  }

  .jm-favorite-current-ops {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 6px 8px;
    margin-bottom: 6px;
    background: #f5f7fa;
    border-radius: 4px;
    font-size: 13px;
  }

  .jm-favorite-current-name {
    flex: 1 1 auto;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-weight: 600;
  }

  .jm-favorite-current-btns {
    flex: 0 0 auto;
  }
}
</style>

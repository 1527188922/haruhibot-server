<template>
  <basic-container>
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
        <el-form :model="query" label-width="70px" inline size="small">
          <el-form-item label="名称" prop="name">
            <el-input v-model="query.name" class="form-input" clearable @keyup.enter.native="searchFirst"></el-input>
          </el-form-item>
          <el-form-item label="作者" prop="author">
            <el-input v-model="query.author" class="form-input" clearable @keyup.enter.native="searchFirst"></el-input>
          </el-form-item>
        </el-form>
        <el-row class="query-form-option-buts">
          <el-button type="primary" size="small" plain icon="el-icon-search" @click="searchFirst">查询</el-button>
          <el-button type="primary" size="small" plain icon="el-icon-refresh-right" @click="resetQuery">重置</el-button>
        </el-row>

        <div class="data-table-option-buts">
          <el-button type="danger" size="small" plain icon="el-icon-delete" :disabled="selection.length === 0"
                     @click="removeSelectedFromFavorite">移出本收藏夹</el-button>
          <el-radio-group v-model="viewMode" class="jm-view-switch" size="mini">
            <el-radio-button label="list"><i class="el-icon-s-unfold"></i> 列表</el-radio-button>
            <el-radio-button label="waterfall"><i class="el-icon-s-grid"></i> 瀑布流</el-radio-button>
          </el-radio-group>
        </div>

        <el-table v-if="viewMode === 'list'" :data="albums" v-loading="loading" border stripe
                  max-height="800" size="small" :fixed="false" highlight-current-row
                  @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center"></el-table-column>
          <el-table-column label="操作" width="150" align="center">
            <template slot-scope="{row}">
              <div class="jm-action-grid">
                <el-tooltip content="预览漫画" placement="top">
                  <el-button type="primary" size="mini" plain icon="el-icon-view" @click="$emit('preview', row)"></el-button>
                </el-tooltip>
                <el-tooltip content="下载漫画" placement="top">
                  <el-button type="primary" size="mini" plain icon="el-icon-download" @click="$emit('download', row)"></el-button>
                </el-tooltip>
                <el-tooltip content="生成zip" placement="top">
                  <el-button type="success" size="mini" plain icon="el-icon-folder-add" @click="$emit('zip', row)"></el-button>
                </el-tooltip>
                <el-tooltip content="生成pdf" placement="top">
                  <el-button type="warning" size="mini" plain icon="el-icon-document-add" @click="$emit('pdf', row)"></el-button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="序号" width="50" align="center">
            <template slot-scope="scope">{{scope.$index + 1}}</template>
          </el-table-column>
          <el-table-column label="JM ID" prop="id" min-width="110" align="center">
            <template slot-scope="{row}">
              <span class="primary-text" style="cursor:pointer;" @click="$emit('chapters', row)">{{row.id}}</span>
            </template>
          </el-table-column>
          <el-table-column label="封面" width="96" align="center">
            <template slot-scope="{row}">
              <el-image v-if="albumCoverSrc(row)" class="jm-cover-image" :src="albumCoverSrc(row)"
                        :preview-src-list="[albumCoverSrc(row)]" fit="cover" referrerpolicy="no-referrer">
                <div slot="placeholder" class="jm-cover-state"><i class="el-icon-loading"></i></div>
                <div slot="error" class="jm-cover-state"><i class="el-icon-picture-outline"></i></div>
              </el-image>
              <div v-else class="jm-cover-state"><i class="el-icon-picture-outline"></i></div>
            </template>
          </el-table-column>
          <el-table-column label="名称" prop="name" min-width="240" show-overflow-tooltip></el-table-column>
          <el-table-column label="作者" prop="author" min-width="160">
            <template slot-scope="{row}">
              <div class="jm-tag-list">
                <el-tag v-for="(item, index) in row.authorList" :key="`fav-author-${row.id}-${index}`" size="mini" type="info">{{item}}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="标签" prop="tags" min-width="200">
            <template slot-scope="{row}">
              <div v-if="row.tagsList && row.tagsList.length > 0" class="jm-tag-list">
                <el-tag v-for="(item, index) in visibleItems(row.tagsList, 3)" :key="`fav-tag-${row.id}-${index}`" size="mini" type="success">{{item}}</el-tag>
                <span v-if="row.tagsList.length > 3">+{{row.tagsList.length - 3}}</span>
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="加入时间" prop="favoriteAddTime" min-width="150" align="center" show-overflow-tooltip></el-table-column>
        </el-table>

        <el-empty v-else-if="albums.length === 0" description="该收藏夹下还没有漫画" :image-size="80"></el-empty>
        <div v-else v-loading="loading" class="jm-waterfall">
          <div v-for="row in albums" :key="`fav-wf-${row.id}`" class="jm-waterfall-card">
            <div class="jm-waterfall-cover">
              <el-image v-if="albumCoverSrc(row)" :src="albumCoverSrc(row)" :preview-src-list="[albumCoverSrc(row)]"
                        fit="cover" referrerpolicy="no-referrer">
                <div slot="placeholder" class="jm-waterfall-placeholder"><i class="el-icon-loading"></i></div>
                <div slot="error" class="jm-waterfall-placeholder"><i class="el-icon-picture-outline"></i></div>
              </el-image>
              <div v-else class="jm-waterfall-placeholder"><i class="el-icon-picture-outline"></i></div>
              <i v-if="row.zipExists" class="jm-waterfall-badge jm-waterfall-badge-zip" title="已有ZIP">ZIP</i>
              <i v-if="row.pdfExists" class="jm-waterfall-badge jm-waterfall-badge-pdf" title="已有PDF">PDF</i>
            </div>
            <div class="jm-waterfall-body">
              <div class="jm-waterfall-name" :title="row.name">{{row.name}}</div>
              <div class="jm-waterfall-meta">
                <span class="jm-waterfall-id jm-waterfall-id-link" title="查看章节" @click="$emit('chapters', row)">JM{{row.id}}</span>
                <el-tag v-for="(item, index) in visibleItems(row.authorList, 2)" :key="`fav-wf-author-${row.id}-${index}`" size="mini" type="info">{{item}}</el-tag>
                <span v-if="row.authorList && row.authorList.length > 2">+{{row.authorList.length - 2}}</span>
              </div>
              <div v-if="row.tagsList && row.tagsList.length > 0" class="jm-waterfall-meta">
                <el-tag v-for="(item, index) in visibleItems(row.tagsList, 3)" :key="`fav-wf-tag-${row.id}-${index}`" size="mini" type="success">{{item}}</el-tag>
                <span v-if="row.tagsList.length > 3">+{{row.tagsList.length - 3}}</span>
              </div>
              <div class="jm-waterfall-time">加入于 {{row.favoriteAddTime}}</div>
              <div class="jm-waterfall-actions">
                <el-tooltip content="预览漫画" placement="top">
                  <el-button type="primary" size="mini" plain icon="el-icon-view" @click="$emit('preview', row)"></el-button>
                </el-tooltip>
                <el-tooltip content="下载漫画" placement="top">
                  <el-button type="primary" size="mini" plain icon="el-icon-download" @click="$emit('download', row)"></el-button>
                </el-tooltip>
                <el-tooltip content="生成zip" placement="top">
                  <el-button type="success" size="mini" plain icon="el-icon-folder-add" @click="$emit('zip', row)"></el-button>
                </el-tooltip>
                <el-tooltip content="生成pdf" placement="top">
                  <el-button type="warning" size="mini" plain icon="el-icon-document-add" @click="$emit('pdf', row)"></el-button>
                </el-tooltip>
                <el-tooltip content="移出本收藏夹" placement="top">
                  <el-button type="danger" size="mini" plain icon="el-icon-delete" @click="removeOneFromFavorite(row)"></el-button>
                </el-tooltip>
              </div>
            </div>
          </div>
        </div>

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
  </basic-container>
</template>

<script>
import {
  listFavorites,
  renameFavorite,
  deleteFavorite,
  searchAlbums,
  removeAlbumsFromFavorite
} from "@/api/jmcomic";
import { getStore, setStore } from "@/util/store";

const FAVORITE_VIEW_MODE_KEY = 'jmFavoriteViewMode';

export default {
  name: 'JmFavoritePanel',
  data() {
    return {
      favorites: [],
      favoriteLoading: false,
      activeFavoriteId: null,
      sideExpanded: false,
      albums: [],
      loading: false,
      selection: [],
      query: { name: '', author: '' },
      page: { currentPage: 1, pageSize: 20, total: 0 },
      viewMode: getStore({ name: FAVORITE_VIEW_MODE_KEY }) || 'list'
    }
  },
  computed: {
    // 手机上分页按钮过多会换行，去掉 jumper
    paginationLayout() {
      return this.isMobileView ? 'total, prev, pager, next' : 'total, prev, pager, next, jumper'
    },
    activeFavorite() {
      return this.favorites.find(item => item.id === this.activeFavoriteId) || null
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
    searchFirst() {
      this.page.currentPage = 1
      this.loadAlbums()
    },
    resetQuery() {
      this.query = { name: '', author: '' }
      this.searchFirst()
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
    doRemove(albumIds, tip) {
      this.$confirm(tip, '移出收藏夹', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        const { data: { code, message } } = await removeAlbumsFromFavorite({
          albumIds,
          favoriteId: this.activeFavoriteId
        })
        if (code !== 200) {
          return this.$message.error(message || '移出失败')
        }
        this.$message.success('已移出本收藏夹')
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
    },
    albumCoverSrc(row) {
      return row.serverCoverUrl || row.coverUrl || ''
    },
    visibleItems(list, count) {
      return (list || []).slice(0, count)
    }
  }
}
</script>

<style lang="scss" scoped>
.jm-favorite {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.jm-favorite-side {
  flex: 0 0 190px;
  width: 190px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;

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

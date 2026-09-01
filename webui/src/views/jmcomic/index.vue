<template>
  <div id="JmcomicManage">
    <basic-container>
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="JM主记录" name="album">
          <el-form :model="albumQuery" label-width="70px" inline ref="albumQueryForm" size="small">
            <el-form-item label="JM ID" prop="id">
              <number-input v-model.trim="albumQuery.id" class="form-input" clearable @keyup.enter.native="searchAlbumsFirst"></number-input>
            </el-form-item>
            <el-form-item label="名称" prop="name">
              <el-input v-model="albumQuery.name" class="form-input" clearable @keyup.enter.native="searchAlbumsFirst"></el-input>
            </el-form-item>
            <el-form-item label="作者" prop="author">
              <el-input v-model="albumQuery.author" class="form-input" clearable @keyup.enter.native="searchAlbumsFirst"></el-input>
            </el-form-item>
            <el-form-item label="标签" prop="tags">
              <el-input v-model="albumQuery.tags" class="form-input" clearable @keyup.enter.native="searchAlbumsFirst"></el-input>
            </el-form-item>
          </el-form>
          <el-row class="query-form-option-buts">
            <el-button type="primary" size="small" plain icon="el-icon-search" @click="searchAlbumsFirst">查询</el-button>
            <el-button type="primary" size="small" plain icon="el-icon-refresh-right" @click="resetAlbumQuery">重置</el-button>
          </el-row>
        </el-tab-pane>
        <el-tab-pane label="JM章节信息" name="chapter">
          <el-form :model="chapterQuery" label-width="80px" inline ref="chapterQueryForm" size="small">
            <el-form-item label="JM ID" prop="albumId">
              <number-input v-model.trim="chapterQuery.albumId" class="form-input" clearable @keyup.enter.native="searchChaptersFirst"></number-input>
            </el-form-item>
            <el-form-item label="章节ID" prop="chapterId">
              <number-input v-model.trim="chapterQuery.chapterId" class="form-input" clearable @keyup.enter.native="searchChaptersFirst"></number-input>
            </el-form-item>
            <el-form-item label="章节标题" prop="chapterTitle">
              <el-input v-model="chapterQuery.chapterTitle" class="form-input" clearable @keyup.enter.native="searchChaptersFirst"></el-input>
            </el-form-item>
            <el-form-item label="图片文件" prop="imageFile">
              <el-input v-model="chapterQuery.imageFile" class="form-input" clearable @keyup.enter.native="searchChaptersFirst"></el-input>
            </el-form-item>
          </el-form>
          <el-row class="query-form-option-buts">
            <el-button type="primary" size="small" plain icon="el-icon-search" @click="searchChaptersFirst">查询</el-button>
            <el-button type="primary" size="small" plain icon="el-icon-refresh-right" @click="resetChapterQuery">重置</el-button>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </basic-container>

    <basic-container v-if="activeTab === 'album'">
      <div class="data-table-option-buts">
        <el-button type="primary" size="small" plain icon="el-icon-plus" :loading="albumRequestLoading" @click="addAlbum">新增</el-button>
        <el-button type="danger" size="small" plain icon="el-icon-delete" :disabled="albumDeleteDisabled" @click="openAlbumDelete">删除</el-button>
        <el-dropdown trigger="click" :hide-on-click="false">
          <el-button type="primary" size="small" plain icon="el-icon-setting">列设置</el-button>
          <el-dropdown-menu slot="dropdown" class="jm-column-dropdown">
            <el-checkbox-group v-model="albumVisibleColumns" class="jm-column-check-group" @change="handleAlbumColumnsChange">
              <el-checkbox v-for="column in albumColumnOptions" :key="column.key" :label="column.key">{{column.label}}</el-checkbox>
            </el-checkbox-group>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
      <el-table tooltip-effect="light" :data="albumData" v-loading="albumLoading" border stripe max-height="800"
                size="small" ref="albumTable" highlight-current-row @selection-change="albumSelectionChange">
        <el-table-column v-if="isAlbumColumnVisible('selection')" type="selection" width="50" align="center"></el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('action')" fixed label="操作" width="96" align="center">
          <template slot-scope="{row}">
            <div class="jm-action-grid">
              <el-tooltip content="预览漫画" placement="top">
                <el-button type="primary" size="mini" plain icon="el-icon-view" @click="openPreview(row)"></el-button>
              </el-tooltip>
              <el-tooltip content="下载漫画" placement="top">
                <el-button type="primary" size="mini" plain icon="el-icon-download" :loading="isAlbumOperation(row, 'download')" :disabled="isAlbumOtherOperation(row, 'download')" @click="downloadAlbumData(row)"></el-button>
              </el-tooltip>
              <el-tooltip content="生成zip" placement="top">
                <el-button type="success" size="mini" plain icon="el-icon-folder-add" :loading="isAlbumOperation(row, 'zip')" :disabled="isAlbumOtherOperation(row, 'zip')" @click="confirmGenerateZip(row)"></el-button>
              </el-tooltip>
              <el-tooltip content="生成pdf" placement="top">
                <el-button type="warning" size="mini" plain icon="el-icon-document-add" :loading="isAlbumOperation(row, 'pdf')" :disabled="isAlbumOtherOperation(row, 'pdf')" @click="confirmGeneratePdf(row)"></el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('index')" fixed label="序号" width="50" align="center">
          <template slot-scope="scope">{{scope.$index + 1}}</template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('id')" fixed label="JM ID" prop="id" min-width="110" align="center">
          <template slot-scope="{row}">
            <span class="primary-text" style="cursor:pointer;" @click="jumpToChapters(row)">{{row.id}}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('name')" label="名称" prop="name" min-width="240" show-overflow-tooltip></el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('author')" label="作者" prop="author" min-width="180">
          <template slot-scope="{row}">
            <div class="jm-tag-list">
              <el-tag v-for="(item, index) in row.authorList" :key="`author-${row.id}-${index}`" size="mini" type="info">{{item}}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('tags')" label="标签" prop="tags" min-width="240">
          <template slot-scope="{row}">
            <div v-if="row.tagsList.length > 0" class="jm-tag-summary">
              <el-tag v-for="(item, index) in visibleItems(row.tagsList, 3)" :key="`tags-${row.id}-${index}`" size="mini" type="success">{{item}}</el-tag>
              <el-popover v-if="row.tagsList.length > 3" placement="bottom-start" trigger="click" width="360" popper-class="jm-tag-popover">
                <div class="jm-popover-title">全部标签</div>
                <div class="jm-popover-tags">
                  <el-tag v-for="(item, index) in row.tagsList" :key="`all-tags-${row.id}-${index}`" size="mini" type="success">{{item}}</el-tag>
                </div>
                <el-button slot="reference" type="text" size="mini">+{{hiddenCount(row.tagsList, 3)}}</el-button>
              </el-popover>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('interactionStats')" label="互动统计" min-width="130" align="center">
          <template slot-scope="{row}">
            <div class="jm-stat-cell">
              <div>观看：{{formatCount(row.totalViews)}}</div>
              <div>Like：{{formatCount(row.likes)}}</div>
              <div>评论：{{formatCount(row.commentTotal)}}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column v-if="isAlbumColumnVisible('imageStats')" label="图片统计" min-width="130" align="center">
          <template slot-scope="{row}">
            <div class="jm-stat-cell">
              <div>数据库：{{formatCount(row.imageCount)}}</div>
              <div>文件：{{formatCount(row.actualImageCount)}}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('zip')" label="ZIP" prop="zipExists" width="80" align="center">
          <template slot-scope="{row}">
            <el-tooltip v-if="row.zipExists" content="点击下载ZIP文件" placement="top">
              <a :href="row.serverZipUrl" target="_blank" download>
                <el-tag size="mini" type="success">{{formatBool(row.zipExists)}}</el-tag>
              </a>
            </el-tooltip>
            <el-tag v-else size="mini" type="info">{{formatBool(row.zipExists)}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('pdf')" label="PDF" prop="pdfExists" width="80" align="center">
          <template slot-scope="{row}">
            <el-tooltip v-if="row.pdfExists" content="点击下载PDF文件" placement="top">
              <a :href="row.serverPdfUrl" target="_blank" download>
                <el-tag size="mini" type="success">{{formatBool(row.pdfExists)}}</el-tag>
              </a>
            </el-tooltip>
            <el-tag v-else size="mini" type="info">{{formatBool(row.pdfExists)}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('createTime')" label="下载时间" prop="createTime" min-width="150" align="center"></el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('chapterCount')" label="章节数" min-width="80" align="center">
          <template slot-scope="{row}">{{(row.chapterList || []).length}}</template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('chapterList')" label="列表章节" min-width="240" show-overflow-tooltip>
          <template slot-scope="{row}">{{formatChapterList(row.chapterList)}}</template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('works')" label="作品" prop="works" min-width="160">
          <template slot-scope="{row}">
            <div class="jm-tag-list">
              <el-tag v-for="(item, index) in row.worksList" :key="`works-${row.id}-${index}`" size="mini" type="warning">{{item}}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('actors')" label="角色" prop="actors" min-width="160">
          <template slot-scope="{row}">
            <div class="jm-tag-list">
              <el-tag v-for="(item, index) in row.actorsList" :key="`actors-${row.id}-${index}`" size="mini">{{item}}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('description')" label="描述" prop="description" min-width="280" show-overflow-tooltip></el-table-column>
<!--        <el-table-column label="系列ID" prop="seriesId" min-width="100" align="center"></el-table-column>-->
        <el-table-column v-if="isAlbumColumnVisible('albumFolderName')" label="文件夹" prop="albumFolderName" min-width="220" show-overflow-tooltip></el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('relatedList')" label="相关列表" prop="relatedList" min-width="300">
          <template slot-scope="{row}">
            <div v-if="row.relatedItems.length > 0" class="jm-related-cell">
              <div v-for="(item, index) in visibleItems(row.relatedItems, 2)" :key="`related-${row.id}-${index}`" class="jm-related-line">
                <el-button type="text" size="mini" @click="jumpToAlbum(item.id)">JM{{item.id}}</el-button>
                <span class="jm-related-name">{{item.name}}</span>
              </div>
              <el-popover placement="bottom-start" trigger="click" width="520" popper-class="jm-related-popover">
                <div class="jm-popover-title">相关漫画</div>
                <div class="jm-related-popover-list">
                  <div v-for="(item, index) in row.relatedItems" :key="`all-related-${row.id}-${index}`" class="jm-related-popover-item">
                    <div class="jm-related-popover-main">
                      <el-button type="text" size="mini" @click="jumpToAlbum(item.id)">JM{{item.id}}</el-button>
                      <span class="jm-related-popover-name">{{item.name}}</span>
                    </div>
                    <div v-if="item.author" class="jm-related-author">{{item.author}}</div>
                  </div>
                </div>
                <el-button slot="reference" type="text" size="mini">全部 {{row.relatedItems.length}} 条</el-button>
              </el-popover>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
<!--        <el-table-column label="已喜欢" prop="liked" min-width="80" align="center">-->
<!--          <template slot-scope="{row}">{{formatBool(row.liked)}}</template>-->
<!--        </el-table-column>-->
<!--        <el-table-column label="已收藏" prop="isFavorite" min-width="80" align="center">-->
<!--          <template slot-scope="{row}">{{formatBool(row.isFavorite)}}</template>-->
<!--        </el-table-column>-->
<!--        <el-table-column label="isAids" prop="isAids" min-width="80" align="center">-->
<!--          <template slot-scope="{row}">{{formatBool(row.isAids)}}</template>-->
<!--        </el-table-column>-->
<!--        <el-table-column label="价格" prop="price" min-width="80" align="center"></el-table-column>-->
<!--        <el-table-column label="已购买" prop="purchased" min-width="90" align="center"></el-table-column>-->
        <el-table-column v-if="isAlbumColumnVisible('addTime')" label="JM发布时间" prop="addTime" min-width="150" align="center">
          <template slot-scope="{row}">{{row.formattedAddTime}}</template>
        </el-table-column>
<!--        <el-table-column label="修改时间" prop="modifyTime" min-width="150" align="center"></el-table-column>-->
<!--        <el-table-column label="封面列表" prop="images" min-width="180" show-overflow-tooltip></el-table-column>-->
<!--        <el-table-column label="series" prop="series" min-width="220" show-overflow-tooltip></el-table-column>-->
        <el-table-column v-if="isAlbumColumnVisible('raw')" label="raw" prop="raw" min-width="100" show-overflow-tooltip></el-table-column>
      </el-table>
      <div class="pagination-box">
        <el-pagination v-bind="albumPagination" @size-change="albumSizeChange" @current-change="albumCurrentChange" />
      </div>
    </basic-container>

    <basic-container v-if="activeTab === 'chapter'">
      <div class="data-table-option-buts">
        <el-button type="primary" size="small" plain icon="el-icon-plus" :loading="chapterRequestLoading" @click="addChapterImages">新增</el-button>
        <el-button type="danger" size="small" plain icon="el-icon-delete" :disabled="chapterDeleteDisabled" @click="openChapterDelete">删除</el-button>
      </div>
      <el-table tooltip-effect="light" :data="chapterData" v-loading="chapterLoading" border stripe max-height="800"
                size="small" ref="chapterTable" highlight-current-row @selection-change="chapterSelectionChange">
        <el-table-column type="selection" width="50" align="center"></el-table-column>
        <el-table-column fixed label="序号" width="50" align="center">
          <template slot-scope="scope">{{scope.$index + 1}}</template>
        </el-table-column>
        <el-table-column fixed label="JM ID" prop="albumId" min-width="110" align="center"></el-table-column>
        <el-table-column label="章节ID" prop="chapterId" min-width="110" align="center"></el-table-column>
        <el-table-column label="章节序号" prop="chapterSort" min-width="90" align="center"></el-table-column>
        <el-table-column label="章节title" prop="chapterTitle" min-width="130" show-overflow-tooltip></el-table-column>
        <el-table-column label="章节名称" prop="chapterName" min-width="180" show-overflow-tooltip></el-table-column>
        <el-table-column label="图片文件" prop="imageFile" min-width="140" show-overflow-tooltip></el-table-column>
        <el-table-column label="图片序号" prop="imageSort" min-width="90" align="center"></el-table-column>
        <el-table-column label="文件存在" prop="imageFileExists" width="90" align="center">
          <template slot-scope="{row}"><el-tag size="mini" :type="row.imageFileExists ? 'success' : 'info'">{{formatBool(row.imageFileExists)}}</el-tag></template>
        </el-table-column>
        <el-table-column label="图片url" prop="imgUrl" min-width="280" show-overflow-tooltip>
          <template slot-scope="{row}"><a :href="row.imgUrl" target="_blank">{{row.imgUrl}}</a></template>
        </el-table-column>
        <el-table-column label="服务器图片url" prop="serverImgUrl" min-width="300" show-overflow-tooltip>
          <template slot-scope="{row}"><a :href="row.serverImgUrl" target="_blank">{{row.serverImgUrl}}</a></template>
        </el-table-column>
        <el-table-column label="seriesId" prop="seriesId" min-width="100" align="center"></el-table-column>
        <el-table-column label="章节添加时间" prop="formattedChapterAddTime" min-width="150" align="center"></el-table-column>
      </el-table>
      <div class="pagination-box">
        <el-pagination v-bind="chapterPagination" @size-change="chapterSizeChange" @current-change="chapterCurrentChange" />
      </div>
    </basic-container>

    <el-dialog title="删除JM主记录" :visible.sync="albumDeleteDialogVisible" width="420px">
      <div class="delete-tip">确认删除选中的 {{albumSelection.length}} 条JM主记录？</div>
      <el-checkbox v-model="albumDeleteOptions.deletePdf">删除pdf文件</el-checkbox>
      <el-checkbox v-model="albumDeleteOptions.deleteZip">删除zip文件</el-checkbox>
      <el-checkbox v-model="albumDeleteOptions.deleteImages">删除图片</el-checkbox>
      <span slot="footer">
        <el-button size="small" @click="albumDeleteDialogVisible = false">取消</el-button>
        <el-button type="danger" size="small" :loading="albumDeleteLoading" @click="deleteAlbumData">确定</el-button>
      </span>
    </el-dialog>

    <el-dialog title="删除章节图片" :visible.sync="chapterDeleteDialogVisible" width="420px">
      <div class="delete-tip">确认删除选中的 {{chapterSelection.length}} 条章节图片记录？</div>
      <el-checkbox v-model="chapterDeleteOptions.deleteFile">是否删除文件</el-checkbox>
      <span slot="footer">
        <el-button size="small" @click="chapterDeleteDialogVisible = false">取消</el-button>
        <el-button type="danger" size="small" :loading="chapterDeleteLoading" @click="deleteChapterData">确定</el-button>
      </span>
    </el-dialog>

    <jm-preview-drawer :visible.sync="previewDrawerVisible" :album="previewAlbum" />
  </div>
</template>

<script>
import JmPreviewDrawer from "./jm-preview-drawer.vue";
import numberInput from "@/components/input/numberInput.vue";
import {
  deleteAlbums,
  deleteChapterImages,
  downloadAlbum,
  generateAlbumPdf,
  generateAlbumZip,
  requestAlbum,
  requestChapterImages,
  searchAlbums,
  searchChapterImages
} from "@/api/jmcomic";

export default {
  name: 'JmcomicManage',
  components: { JmPreviewDrawer, numberInput },
  data() {
    return {
      activeTab: 'album',
      albumLoading: false,
      chapterLoading: false,
      albumRequestLoading: false,
      chapterRequestLoading: false,
      albumDeleteLoading: false,
      chapterDeleteLoading: false,
      albumDeleteDialogVisible: false,
      chapterDeleteDialogVisible: false,
      previewDrawerVisible: false,
      previewAlbum: null,
      albumQuery: { id: '', name: '', author: '', tags: '' },
      chapterQuery: { albumId: '', chapterId: '', chapterTitle: '', imageFile: '' },
      albumData: [],
      chapterData: [],
      albumSelection: [],
      chapterSelection: [],
      albumOperationLoading: {},
      albumVisibleColumns: ['selection', 'action', 'index', 'id', 'name', 'author', 'tags', 'zip', 'pdf', 'createTime','imageStats','interactionStats'],
      albumColumnOptions: [
        { key: 'selection', label: 'selection' },
        { key: 'action', label: '操作' },
        { key: 'index', label: '序号' },
        { key: 'id', label: 'JM ID' },
        { key: 'name', label: '名称' },
        { key: 'author', label: '作者' },
        { key: 'tags', label: '标签' },
        { key: 'interactionStats', label: '互动统计' },
        { key: 'imageStats', label: '图片统计' },
        { key: 'zip', label: 'ZIP' },
        { key: 'pdf', label: 'PDF' },
        { key: 'createTime', label: '下载时间' },
        { key: 'chapterCount', label: '章节数' },
        { key: 'chapterList', label: '列表章节' },
        { key: 'works', label: '作品' },
        { key: 'actors', label: '角色' },
        { key: 'description', label: '描述' },
        { key: 'albumFolderName', label: '文件夹' },
        { key: 'relatedList', label: '相关列表' },
        { key: 'addTime', label: 'JM发布时间' },
        { key: 'raw', label: 'raw' }
      ],
      albumDeleteOptions: { deletePdf: true, deleteZip: true, deleteImages: true },
      chapterDeleteOptions: { deleteFile: true },
      albumPagination: {
        currentPage: 1,
        pageSizes: [5, 10, 30, 50, 100, 500],
        pageSize: 5,
        layout: 'total, sizes, prev, pager, next, jumper',
        background: true,
        total: 0
      },
      chapterPagination: {
        currentPage: 1,
        pageSizes: [5, 10, 30, 50, 100, 500],
        pageSize: 10,
        layout: 'total, sizes, prev, pager, next, jumper',
        background: true,
        total: 0
      }
    }
  },
  computed: {
    albumDeleteDisabled() {
      return !this.albumSelection || this.albumSelection.length === 0
    },
    chapterDeleteDisabled() {
      return !this.chapterSelection || this.chapterSelection.length === 0
    }
  },
  mounted() {
    this.searchAlbumsFirst()
  },
  methods: {
    formatBool(value) {
      return value ? '是' : '否'
    },
    formatCount(value) {
      return value === null || value === undefined || value === '' ? '-' : value
    },
    isAlbumColumnVisible(key) {
      return this.albumVisibleColumns.includes(key)
    },
    handleAlbumColumnsChange() {
      this.$nextTick(() => {
        if (this.$refs.albumTable) {
          this.$refs.albumTable.doLayout()
        }
      })
    },
    formatChapterList(chapterList) {
      if (!chapterList || chapterList.length === 0) {
        return ''
      }
      return chapterList.map(e => `${e.title || e.name || ''}(${e.chapterId})`).join('，')
    },
    parseJsonList(value) {
      if (!value) {
        return []
      }
      if (Array.isArray(value)) {
        return value.map(e => `${e}`).filter(e => e)
      }
      try {
        const parsed = JSON.parse(value)
        if (Array.isArray(parsed)) {
          return parsed.map(e => `${e}`).filter(e => e)
        }
      } catch (e) {
        return [`${value}`]
      }
      return [`${value}`]
    },
    parseJsonArray(value) {
      if (!value) {
        return []
      }
      if (Array.isArray(value)) {
        return value
      }
      try {
        const parsed = JSON.parse(value)
        return Array.isArray(parsed) ? parsed : []
      } catch (e) {
        return []
      }
    },
    parseRelatedList(value) {
      return this.parseJsonArray(value)
        .filter(e => e && e.id)
        .map(e => ({
          id: `${e.id}`,
          name: e.name || '',
          author: e.author || '',
          image: e.image || ''
        }))
    },
    visibleItems(list, count) {
      return (list || []).slice(0, count)
    },
    hiddenCount(list, count) {
      return Math.max((list || []).length - count, 0)
    },
    handleRequestError(error) {
      const message = error && error.data && error.data.message
        ? error.data.message
        : error && error.message
          ? error.message
          : '请求失败'
      this.$message.error(message)
    },
    isAlbumOperation(row, action) {
      return row && this.albumOperationLoading[row.id] === action
    },
    isAlbumOperating(row) {
      return row && !!this.albumOperationLoading[row.id]
    },
    isAlbumOtherOperation(row, action) {
      return this.isAlbumOperating(row) && !this.isAlbumOperation(row, action)
    },
    formatTimestamp(value) {
      if (!value) {
        return ''
      }
      const text = `${value}`.trim()
      if (!/^\d{10}$|^\d{13}$/.test(text)) {
        return text
      }
      const timestamp = text.length === 10 ? Number(text) * 1000 : Number(text)
      const date = new Date(timestamp)
      if (Number.isNaN(date.getTime())) {
        return text
      }
      const pad = val => `${val}`.padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    },
    normalizeAlbum(row) {
      return {
        ...row,
        authorList: this.parseJsonList(row.author),
        tagsList: this.parseJsonList(row.tags),
        worksList: this.parseJsonList(row.works),
        actorsList: this.parseJsonList(row.actors),
        relatedItems: this.parseRelatedList(row.relatedList),
        formattedAddTime: this.formatTimestamp(row.addTime)
      }
    },
    cleanQuery(query) {
      const res = { ...query }
      Object.keys(res).forEach(key => {
        if (res[key] === '') {
          res[key] = null
        }
      })
      return res
    },
    handleTabClick() {
      if (this.activeTab === 'chapter' && this.chapterData.length === 0) {
        this.searchChaptersFirst()
      }
    },
    albumSelectionChange(val) {
      this.albumSelection = val
    },
    chapterSelectionChange(val) {
      this.chapterSelection = val
    },
    searchAlbumsFirst() {
      this.albumPagination.currentPage = 1
      this.selectAlbums()
    },
    searchChaptersFirst() {
      this.chapterPagination.currentPage = 1
      this.selectChapters()
    },
    resetAlbumQuery() {
      this.$refs.albumQueryForm.resetFields()
    },
    resetChapterQuery() {
      this.$refs.chapterQueryForm.resetFields()
    },
    albumSizeChange(v) {
      this.albumPagination.pageSize = v
      this.selectAlbums()
    },
    albumCurrentChange(v) {
      this.albumPagination.currentPage = v
      this.selectAlbums()
    },
    chapterSizeChange(v) {
      this.chapterPagination.pageSize = v
      this.selectChapters()
    },
    chapterCurrentChange(v) {
      this.chapterPagination.currentPage = v
      this.selectChapters()
    },
    selectAlbums() {
      this.albumLoading = true
      searchAlbums({
        ...this.cleanQuery(this.albumQuery),
        currentPage: this.albumPagination.currentPage,
        pageSize: this.albumPagination.pageSize
      }).then(({data: {data}}) => {
        this.albumData = (data.records || []).map(row => this.normalizeAlbum(row))
        this.albumPagination.total = data.total
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.albumLoading = false
      })
    },
    selectChapters() {
      this.chapterLoading = true
      searchChapterImages({
        ...this.cleanQuery(this.chapterQuery),
        currentPage: this.chapterPagination.currentPage,
        pageSize: this.chapterPagination.pageSize
      }).then(({data: {data}}) => {
        this.chapterData = (data.records || []).map(row => {
          return{
            ...row,
            formattedChapterAddTime: this.formatTimestamp(row.chapterAddTime)
          }
        })
        this.chapterPagination.total = data.total
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.chapterLoading = false
      })
    },
    jumpToChapters(row) {
      this.activeTab = 'chapter'
      this.chapterQuery.albumId = row.id
      this.chapterQuery.chapterId = ''
      this.searchChaptersFirst()
    },
    jumpToAlbum(id) {
      this.activeTab = 'album'
      this.albumQuery.id = id
      this.searchAlbumsFirst()
    },
    addAlbum() {
      this.$prompt('请输入JM ID', '新增JM主记录', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^\d+$/,
        inputErrorMessage: 'JM ID必须是数字'
      }).then(({value}) => {
        this.albumRequestLoading = true
        requestAlbum(value).then(({data: {code, message}}) => {
          if (code !== 200) {
            return this.$message.error(message)
          }
          this.$message.success('拉取完成')
          this.searchAlbumsFirst()
        }).catch(error => {
          this.handleRequestError(error)
        }).finally(() => {
          this.albumRequestLoading = false
        })
      }).catch(() => {})
    },
    executeAlbumOperation(row, action, requestFn) {
      const aid = row.id
      this.$set(this.albumOperationLoading, aid, action)
      requestFn(aid).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$message.success(message || '操作完成')
        this.selectAlbums()
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.$delete(this.albumOperationLoading, aid)
      })
    },
    downloadAlbumData(row) {
      this.executeAlbumOperation(row, 'download', downloadAlbum)
    },
    openPreview(row) {
      this.previewAlbum = row
      this.previewDrawerVisible = true
    },
    confirmGenerateZip(row) {
      this.$confirm('确认生成zip？如果本地已有旧zip文件，后端会先删除旧文件再重新生成。', '生成zip', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.executeAlbumOperation(row, 'zip', generateAlbumZip)
      }).catch(() => {})
    },
    confirmGeneratePdf(row) {
      this.$confirm('确认生成pdf？如果本地已有旧pdf文件，后端会先删除旧文件再重新生成。', '生成pdf', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.executeAlbumOperation(row, 'pdf', generateAlbumPdf)
      }).catch(() => {})
    },
    addChapterImages() {
      if (!this.chapterQuery.albumId || !this.chapterQuery.chapterId) {
        return this.$message.warning('请先填写JM ID和章节ID')
      }
      this.chapterRequestLoading = true
      requestChapterImages({
        albumId: this.cleanQuery(this.chapterQuery).albumId,
        chapterId: this.cleanQuery(this.chapterQuery).chapterId
      }).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$message.success(message)
        this.searchChaptersFirst()
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.chapterRequestLoading = false
      })
    },
    openAlbumDelete() {
      if (this.albumDeleteDisabled) {
        return
      }
      this.albumDeleteOptions = { deletePdf: true, deleteZip: true, deleteImages: true }
      this.albumDeleteDialogVisible = true
    },
    openChapterDelete() {
      if (this.chapterDeleteDisabled) {
        return
      }
      this.chapterDeleteOptions = { deleteFile: true }
      this.chapterDeleteDialogVisible = true
    },
    deleteAlbumData() {
      this.albumDeleteLoading = true
      deleteAlbums({
        ids: this.albumSelection.map(e => e.id),
        ...this.albumDeleteOptions
      }).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.albumDeleteDialogVisible = false
        this.$message.success(message)
        this.searchAlbumsFirst()
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.albumDeleteLoading = false
      })
    },
    deleteChapterData() {
      this.chapterDeleteLoading = true
      deleteChapterImages({
        ids: this.chapterSelection.map(e => e.id),
        ...this.chapterDeleteOptions
      }).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.chapterDeleteDialogVisible = false
        this.$message.success(message)
        this.searchChaptersFirst()
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.chapterDeleteLoading = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
#JmcomicManage {
  .delete-tip {
    margin-bottom: 12px;
    line-height: 22px;
  }

  .el-checkbox {
    display: block;
    margin: 8px 0;
  }

  a {
    color: #409eff;
    text-decoration: none;
  }

  .data-table-option-buts {
    align-items: center;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .el-button {
      margin-left: 0;
    }
  }

  .jm-action-grid {
    display: grid;
    gap: 4px;
    grid-template-columns: repeat(2, 28px);
    justify-content: center;

    .el-button {
      height: 28px;
      margin: 0;
      padding: 0;
      width: 28px;
    }
  }

  .jm-tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    max-height: 58px;
    overflow: hidden;
    padding: 2px 0;

    .el-tag {
      max-width: 150px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .jm-tag-summary {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
    padding: 2px 0;

    .el-tag {
      max-width: 120px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .jm-related-cell {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 2px 0;
  }

  .jm-related-line {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }

  .jm-related-name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .jm-stat-cell {
    color: #606266;
    font-size: 12px;
    line-height: 20px;
    text-align: left;
    white-space: nowrap;
  }

}
</style>

<style lang="scss">
.jm-column-dropdown {
  max-height: 360px;
  overflow: auto;
  padding: 8px 12px;
}

.jm-column-check-group {
  display: grid;
  gap: 6px 12px;
  grid-template-columns: repeat(2, minmax(92px, 1fr));

  .el-checkbox {
    margin: 0;
    white-space: nowrap;
  }
}

.jm-tag-popover,
.jm-related-popover {
  .jm-popover-title {
    color: #303133;
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 10px;
  }
}

.jm-tag-popover {
  .jm-popover-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    max-height: 220px;
    overflow: auto;

    .el-tag {
      max-width: 170px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.jm-related-popover {
  .jm-related-popover-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-height: 320px;
    overflow: auto;
  }

  .jm-related-popover-item {
    border-bottom: 1px solid #ebeef5;
    padding-bottom: 8px;

    &:last-child {
      border-bottom: 0;
      padding-bottom: 0;
    }
  }

  .jm-related-popover-main {
    display: flex;
    align-items: flex-start;
    gap: 8px;
  }

  .jm-related-popover-name {
    color: #303133;
    flex: 1;
    line-height: 20px;
    min-width: 0;
  }

  .jm-related-author {
    color: #909399;
    font-size: 12px;
    line-height: 18px;
    margin-left: 56px;
    margin-top: 2px;
  }
}
</style>

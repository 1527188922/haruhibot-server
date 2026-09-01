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
      </div>
      <el-table tooltip-effect="light" :data="albumData" v-loading="albumLoading" border stripe max-height="800"
                size="small" ref="albumTable" highlight-current-row @selection-change="albumSelectionChange">
        <el-table-column type="selection" width="50" align="center"></el-table-column>
        <el-table-column fixed label="序号" width="50" align="center">
          <template slot-scope="scope">{{scope.$index + 1}}</template>
        </el-table-column>
        <el-table-column fixed label="JM ID" prop="id" min-width="110" align="center">
          <template slot-scope="{row}">
            <el-button type="text" size="small" @click="jumpToChapters(row)">{{row.id}}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="名称" prop="name" min-width="240" show-overflow-tooltip></el-table-column>
        <el-table-column label="作者" prop="author" min-width="180">
          <template slot-scope="{row}">
            <div class="jm-tag-list">
              <el-tag v-for="(item, index) in row.authorList" :key="`author-${row.id}-${index}`" size="mini" type="info">{{item}}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标签" prop="tags" min-width="240">
          <template slot-scope="{row}">
            <div v-if="row.tagsList.length > 0" class="jm-tag-summary">
              <el-tag v-for="(item, index) in visibleItems(row.tagsList, 3)" :key="`tags-${row.id}-${index}`" size="mini" type="success">{{item}}</el-tag>
              <el-popover v-if="row.tagsList.length > 3" placement="bottom-start" trigger="click" width="360" popper-class="jm-tag-popover" :append-to-body="false">
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
        <el-table-column label="文件夹" prop="albumFolderName" min-width="220" show-overflow-tooltip></el-table-column>
        <el-table-column label="ZIP" prop="zipExists" width="80" align="center">
          <template slot-scope="{row}"><el-tag size="mini" :type="row.zipExists ? 'success' : 'info'">{{formatBool(row.zipExists)}}</el-tag></template>
        </el-table-column>
        <el-table-column label="PDF" prop="pdfExists" width="80" align="center">
          <template slot-scope="{row}"><el-tag size="mini" :type="row.pdfExists ? 'success' : 'info'">{{formatBool(row.pdfExists)}}</el-tag></template>
        </el-table-column>
        <el-table-column label="章节数" min-width="80" align="center">
          <template slot-scope="{row}">{{(row.chapterList || []).length}}</template>
        </el-table-column>
        <el-table-column label="图片数" prop="imageCount" min-width="90" align="center"></el-table-column>
        <el-table-column label="实际图片数" prop="actualImageCount" min-width="100" align="center"></el-table-column>
        <el-table-column label="列表章节" min-width="240" show-overflow-tooltip>
          <template slot-scope="{row}">{{formatChapterList(row.chapterList)}}</template>
        </el-table-column>
        <el-table-column label="作品" prop="works" min-width="160">
          <template slot-scope="{row}">
            <div class="jm-tag-list">
              <el-tag v-for="(item, index) in row.worksList" :key="`works-${row.id}-${index}`" size="mini" type="warning">{{item}}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" prop="actors" min-width="160">
          <template slot-scope="{row}">
            <div class="jm-tag-list">
              <el-tag v-for="(item, index) in row.actorsList" :key="`actors-${row.id}-${index}`" size="mini">{{item}}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="描述" prop="description" min-width="280" show-overflow-tooltip></el-table-column>
        <el-table-column label="观看数" prop="totalViews" min-width="90" align="center"></el-table-column>
        <el-table-column label="Like数" prop="likes" min-width="90" align="center"></el-table-column>
<!--        <el-table-column label="系列ID" prop="seriesId" min-width="100" align="center"></el-table-column>-->
        <el-table-column label="评论数" prop="commentTotal" min-width="90" align="center"></el-table-column>
        <el-table-column label="相关列表" prop="relatedList" min-width="300">
          <template slot-scope="{row}">
            <div v-if="row.relatedItems.length > 0" class="jm-related-cell">
              <div v-for="(item, index) in visibleItems(row.relatedItems, 2)" :key="`related-${row.id}-${index}`" class="jm-related-line">
                <el-button type="text" size="mini" @click="jumpToAlbum(item.id)">JM{{item.id}}</el-button>
                <span class="jm-related-name">{{item.name}}</span>
              </div>
              <el-popover placement="bottom-start" trigger="click" width="520" popper-class="jm-related-popover" :append-to-body="false">
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
        <el-table-column label="JM发布时间" prop="addTime" min-width="150" align="center">
          <template slot-scope="{row}">{{row.formattedAddTime}}</template>
        </el-table-column>
        <el-table-column label="下载时间" prop="createTime" min-width="150" align="center"></el-table-column>
<!--        <el-table-column label="修改时间" prop="modifyTime" min-width="150" align="center"></el-table-column>-->
<!--        <el-table-column label="封面列表" prop="images" min-width="180" show-overflow-tooltip></el-table-column>-->
<!--        <el-table-column label="series" prop="series" min-width="220" show-overflow-tooltip></el-table-column>-->
        <el-table-column label="raw" prop="raw" min-width="100" show-overflow-tooltip></el-table-column>
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
  </div>
</template>

<script>
import numberInput from "@/components/input/numberInput.vue";
import {
  deleteAlbums,
  deleteChapterImages,
  requestAlbum,
  requestChapterImages,
  searchAlbums,
  searchChapterImages
} from "@/api/jmcomic";

export default {
  name: 'JmcomicManage',
  components: { numberInput },
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
      albumQuery: { id: '', name: '', author: '', tags: '' },
      chapterQuery: { albumId: '', chapterId: '', chapterTitle: '', imageFile: '' },
      albumData: [],
      chapterData: [],
      albumSelection: [],
      chapterSelection: [],
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
        }).finally(() => {
          this.albumRequestLoading = false
        })
      })
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

  .jm-popover-title {
    color: #303133;
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 10px;
  }

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

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
        <el-table-column label="名称" prop="name" min-width="240" show-tooltip-when-overflow></el-table-column>
        <el-table-column label="文件夹" prop="albumFolderName" min-width="220" show-tooltip-when-overflow></el-table-column>
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
        <el-table-column label="列表章节" min-width="240" show-tooltip-when-overflow>
          <template slot-scope="{row}">{{formatChapterList(row.chapterList)}}</template>
        </el-table-column>
        <el-table-column label="作者" prop="author" min-width="180" show-tooltip-when-overflow></el-table-column>
        <el-table-column label="标签" prop="tags" min-width="240" show-tooltip-when-overflow></el-table-column>
        <el-table-column label="作品" prop="works" min-width="160" show-tooltip-when-overflow></el-table-column>
        <el-table-column label="角色" prop="actors" min-width="160" show-tooltip-when-overflow></el-table-column>
        <el-table-column label="浏览" prop="totalViews" min-width="90" align="center"></el-table-column>
        <el-table-column label="喜欢" prop="likes" min-width="90" align="center"></el-table-column>
        <el-table-column label="系列ID" prop="seriesId" min-width="100" align="center"></el-table-column>
        <el-table-column label="评论数" prop="commentTotal" min-width="90" align="center"></el-table-column>
        <el-table-column label="已喜欢" prop="liked" min-width="80" align="center">
          <template slot-scope="{row}">{{formatBool(row.liked)}}</template>
        </el-table-column>
        <el-table-column label="已收藏" prop="isFavorite" min-width="80" align="center">
          <template slot-scope="{row}">{{formatBool(row.isFavorite)}}</template>
        </el-table-column>
        <el-table-column label="isAids" prop="isAids" min-width="80" align="center">
          <template slot-scope="{row}">{{formatBool(row.isAids)}}</template>
        </el-table-column>
        <el-table-column label="价格" prop="price" min-width="80" align="center"></el-table-column>
        <el-table-column label="已购买" prop="purchased" min-width="90" align="center"></el-table-column>
        <el-table-column label="添加时间" prop="addTime" min-width="150" align="center"></el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="150" align="center"></el-table-column>
        <el-table-column label="修改时间" prop="modifyTime" min-width="150" align="center"></el-table-column>
        <el-table-column label="描述" prop="description" min-width="280" show-tooltip-when-overflow></el-table-column>
        <el-table-column label="封面列表" prop="images" min-width="180" show-tooltip-when-overflow></el-table-column>
        <el-table-column label="series" prop="series" min-width="220" show-tooltip-when-overflow></el-table-column>
        <el-table-column label="相关列表" prop="relatedList" min-width="220" show-tooltip-when-overflow></el-table-column>
        <el-table-column label="raw" prop="raw" min-width="260" show-tooltip-when-overflow></el-table-column>
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
        <el-table-column fixed label="ID" prop="id" min-width="80" align="center"></el-table-column>
        <el-table-column fixed label="JM ID" prop="albumId" min-width="110" align="center"></el-table-column>
        <el-table-column label="章节ID" prop="chapterId" min-width="110" align="center"></el-table-column>
        <el-table-column label="章节序号" prop="chapterSort" min-width="90" align="center"></el-table-column>
        <el-table-column label="章节title" prop="chapterTitle" min-width="130" show-tooltip-when-overflow></el-table-column>
        <el-table-column label="章节名称" prop="chapterName" min-width="180" show-tooltip-when-overflow></el-table-column>
        <el-table-column label="图片文件" prop="imageFile" min-width="140" show-tooltip-when-overflow></el-table-column>
        <el-table-column label="图片序号" prop="imageSort" min-width="90" align="center"></el-table-column>
        <el-table-column label="文件存在" prop="imageFileExists" width="90" align="center">
          <template slot-scope="{row}"><el-tag size="mini" :type="row.imageFileExists ? 'success' : 'info'">{{formatBool(row.imageFileExists)}}</el-tag></template>
        </el-table-column>
        <el-table-column label="图片url" prop="imgUrl" min-width="280" show-tooltip-when-overflow>
          <template slot-scope="{row}"><a :href="row.imgUrl" target="_blank">{{row.imgUrl}}</a></template>
        </el-table-column>
        <el-table-column label="服务器图片url" prop="serverImgUrl" min-width="300" show-tooltip-when-overflow>
          <template slot-scope="{row}"><a :href="row.serverImgUrl" target="_blank">{{row.serverImgUrl}}</a></template>
        </el-table-column>
        <el-table-column label="seriesId" prop="seriesId" min-width="100" align="center"></el-table-column>
        <el-table-column label="章节添加时间" prop="chapterAddTime" min-width="150" align="center"></el-table-column>
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
      albumDeleteOptions: { deletePdf: false, deleteZip: false, deleteImages: false },
      chapterDeleteOptions: { deleteFile: false },
      albumPagination: {
        currentPage: 1,
        pageSizes: [5, 10, 30, 50, 100, 500],
        pageSize: 10,
        layout: 'total, sizes, prev, pager, next, jumper',
        background: true,
        total: 0
      },
      chapterPagination: {
        currentPage: 1,
        pageSizes: [10, 30, 50, 100, 500],
        pageSize: 30,
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
        this.albumData = data.records || []
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
        this.chapterData = data.records || []
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
      this.albumDeleteOptions = { deletePdf: false, deleteZip: false, deleteImages: false }
      this.albumDeleteDialogVisible = true
    },
    openChapterDelete() {
      if (this.chapterDeleteDisabled) {
        return
      }
      this.chapterDeleteOptions = { deleteFile: false }
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
}
</style>

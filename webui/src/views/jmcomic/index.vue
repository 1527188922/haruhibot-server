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
              <jm-author-select ref="authorSelect" v-model="albumQuery.author" class="form-input" @keyup.enter.native="searchAlbumsFirst"></jm-author-select>
            </el-form-item>
            <el-form-item label="标签" prop="tags">
              <jm-tag-select ref="tagSelect" v-model="albumQuery.tags" class="form-input" @keyup.enter.native="searchAlbumsFirst"></jm-tag-select>
            </el-form-item>
            <el-form-item label="收藏" prop="collected">
              <el-select v-model="albumQuery.collected" class="form-input" clearable placeholder="全部">
                <el-option label="已收藏" :value="true"></el-option>
                <el-option label="未收藏" :value="false"></el-option>
              </el-select>
            </el-form-item>
          </el-form>
          <el-row class="query-form-option-buts">
            <el-button type="primary" size="small" plain icon="el-icon-search" @click="searchAlbumsFirst">查询</el-button>
            <el-button type="primary" size="small" plain icon="el-icon-refresh-right" @click="resetAlbumQuery">重置</el-button>
          </el-row>
        </el-tab-pane>
        <!--
          收藏夹 tab 这里只保留 tab 标题：el-tabs 本身就挂在一张 basic-container 卡片里，
          内容若写在这个 tab-pane 内，就会被那张卡片包住，收藏夹内部的两块卡片之间
          露出的全是卡片白底，看起来连成一片。它的查询块与内容块改由下面的
          两块 basic-container 承载（与 JM主记录 tab 同处一级）。
        -->
        <el-tab-pane label="收藏夹" name="favorite">
          <el-form :model="favoriteQuery" label-width="70px" inline size="small">
            <el-form-item label="名称" prop="name">
              <el-input v-model="favoriteQuery.name" class="form-input" clearable @keyup.enter.native="searchFavoriteFirst"></el-input>
            </el-form-item>
            <el-form-item label="作者" prop="author">
              <el-input v-model="favoriteQuery.author" class="form-input" clearable @keyup.enter.native="searchFavoriteFirst"></el-input>
            </el-form-item>
          </el-form>
          <el-row class="query-form-option-buts">
            <el-button type="primary" size="small" plain icon="el-icon-search" @click="searchFavoriteFirst">查询</el-button>
            <el-button type="primary" size="small" plain icon="el-icon-refresh-right" @click="resetFavoriteQuery">重置</el-button>
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
        <el-tab-pane label="JM在线搜索" name="online">
          <el-form :model="onlineQuery" label-width="70px" inline ref="onlineQueryForm" size="small" @submit.native.prevent>
            <el-form-item label="关键字" prop="name">
              <el-input v-model.trim="onlineQuery.name" class="form-input" clearable placeholder="漫画名称，JM只取前8个字符" @keyup.enter.native="searchOnlineFirst"></el-input>
            </el-form-item>
            <el-form-item label="排序" prop="sort">
              <el-select v-model="onlineQuery.sort" class="form-input" @change="handleOnlineSortChange">
                <el-option v-for="item in onlineSortOptions" :key="item.value" :label="item.label" :value="item.value"></el-option>
              </el-select>
            </el-form-item>
          </el-form>
          <el-row class="query-form-option-buts">
            <el-button type="primary" size="small" plain icon="el-icon-search" :loading="onlineLoading" @click="searchOnlineFirst">搜索</el-button>
            <el-button type="primary" size="small" plain icon="el-icon-refresh-right" @click="resetOnlineQuery">重置</el-button>
          </el-row>
          <div class="jm-online-history">
            <div class="jm-online-history-head">
              <span class="jm-online-history-title">
                最近搜索
                <el-tag size="mini" type="info">{{onlineHistory.length}}</el-tag>
              </span>
              <span class="jm-online-history-hint">点击历史记录可回显当时的搜索条件与页码</span>
              <span class="jm-online-history-ops">
                <el-button type="text" size="mini" @click="toggleOnlineHistory">{{onlineHistoryCollapsed ? '展开' : '收起'}}</el-button>
                <el-button type="text" size="mini" :disabled="onlineHistory.length === 0" @click="clearOnlineHistory">清空</el-button>
              </span>
            </div>
            <div v-show="!onlineHistoryCollapsed" v-loading="onlineHistoryLoading" class="jm-online-history-list">
              <span v-if="onlineHistory.length === 0" class="jm-online-history-empty">暂无搜索历史</span>
              <el-tag v-for="item in onlineHistory" :key="item.id"
                      class="jm-online-history-item"
                      :type="isOnlineHistoryActive(item) ? 'primary' : 'info'"
                      :effect="isOnlineHistoryActive(item) ? 'dark' : 'plain'"
                      :title="onlineHistoryTitle(item)"
                      size="small"
                      @click="applyOnlineHistory(item)">
                <span class="jm-online-history-name">{{item.name}}</span>
                <span class="jm-online-history-meta">{{item.sortLabel}} · 第{{item.page}}页 · {{formatOnlineHistoryTime(item.searchTime)}}</span>
                <i class="el-icon-close jm-online-history-remove" title="删除这条记录" @click.stop="removeOnlineHistory(item)"></i>
              </el-tag>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </basic-container>

    <basic-container v-if="activeTab === 'album'">
      <div class="data-table-option-buts">
        <el-button type="primary" size="small" plain icon="el-icon-plus" @click="addDialogVisible = true">新增</el-button>
        <el-badge class="jm-task-badge" :value="taskActiveCount" :hidden="taskActiveCount === 0" type="warning">
          <el-button type="primary" size="small" plain icon="el-icon-s-operation" @click="taskPanelVisible = true">任务队列</el-button>
        </el-badge>
        <el-button type="danger" size="small" plain icon="el-icon-delete" :disabled="albumDeleteDisabled" @click="openAlbumDelete">批量删除</el-button>
        <el-button type="warning" size="small" plain icon="el-icon-star-on" :disabled="albumDeleteDisabled || !!albumCollectLoading" :loading="albumCollectLoading === 'collect'" @click="openFavoritePickerForSelection">批量收藏</el-button>
        <el-button type="info" size="small" plain icon="el-icon-star-off" :disabled="albumDeleteDisabled || !!albumCollectLoading" :loading="albumCollectLoading === 'uncollect'" @click="collectSelectedAlbums(false)">取消收藏</el-button>
        <el-button type="danger" size="small" plain icon="el-icon-delete" @click="openAllFileDelete">删除全部</el-button>
        <el-dropdown v-if="albumViewMode === 'list'" trigger="click" :hide-on-click="false">
          <el-button type="primary" size="small" plain icon="el-icon-setting">列设置</el-button>
          <el-dropdown-menu slot="dropdown" class="jm-column-dropdown">
            <el-checkbox-group v-model="albumVisibleColumns" class="jm-column-check-group" @change="handleAlbumColumnsChange">
              <el-checkbox v-for="column in albumColumnOptions" :key="column.key" :label="column.key">{{column.label}}</el-checkbox>
            </el-checkbox-group>
          </el-dropdown-menu>
        </el-dropdown>
        <el-radio-group v-model="albumViewMode" class="jm-view-switch" size="mini">
          <el-radio-button label="list"><i class="el-icon-s-unfold"></i> 列表</el-radio-button>
          <el-radio-button label="waterfall"><i class="el-icon-s-grid"></i> 瀑布流</el-radio-button>
        </el-radio-group>
      </div>
      <!-- 列表 / 瀑布流：与收藏夹 tab 共用同一个组件，两边展示的信息保持一致 -->
      <jm-album-view ref="albumView"
                     :albums="albumData"
                     :loading="albumLoading"
                     :view-mode="albumViewMode"
                     :visible-columns="albumVisibleColumns"
                     :actions="albumRowActions"
                     :action-loading="isAlbumOperation"
                     :action-disabled="albumActionDisabled"
                     empty-text="没有查询到JM主记录"
                     @action="handleAlbumRowAction"
                     @collect-toggle="toggleAlbumCollected"
                     @selection-change="albumSelectionChange"
                     @chapters="jumpToChapters"
                     @jump-album="jumpToAlbum"></jm-album-view>

      <div class="pagination-box">
        <el-pagination v-bind="albumPagination" @size-change="albumSizeChange" @current-change="albumCurrentChange" />
      </div>
    </basic-container>
    <!-- 收藏夹 tab：收藏夹列表 + 漫画列表/瀑布流同处一块 -->
    <basic-container v-if="activeTab === 'favorite'">
      <jm-favorite-panel ref="favoritePanel"
                         :query="favoriteQuery"
                         :operation-loading="isAlbumOperation"
                         :operation-disabled="albumActionDisabled"
                         :task-active-count="taskActiveCount"
                         @action="handleAlbumRowAction"
                         @open-tasks="taskPanelVisible = true"
                         @chapters="jumpToChapters"
                         @jump-album="jumpToAlbum"
                         @change-favorites="handleFavoriteChangeRequest"
                         @error="handleRequestError"
                         @create-favorite="handleCreateFavorite"
                         @albums-changed="handleFavoriteAlbumsChanged"></jm-favorite-panel>
    </basic-container>

    <basic-container v-if="activeTab === 'chapter'">
      <div class="data-table-option-buts">
        <el-button type="primary" size="small" plain icon="el-icon-plus" :loading="chapterRequestLoading" @click="addChapterImages">新增</el-button>
        <el-button type="danger" size="small" plain icon="el-icon-delete" :disabled="chapterDeleteDisabled" @click="openChapterDelete">删除</el-button>
      </div>
      <el-table tooltip-effect="light" :data="chapterData" v-loading="chapterLoading" border stripe max-height="800"
                size="small" ref="chapterTable" highlight-current-row @selection-change="chapterSelectionChange">
        <el-table-column type="selection" width="50" align="center"></el-table-column>
        <el-table-column :fixed="!isMobileView" label="序号" width="50" align="center">
          <template slot-scope="scope">{{scope.$index + 1}}</template>
        </el-table-column>
        <el-table-column :fixed="!isMobileView" label="JM ID" prop="albumId" min-width="110" align="center"></el-table-column>
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
          <template slot-scope="{row}">
            <a v-if="row.serverImgUrl" :href="serverImgUrlFor(row)" target="_blank">{{serverImgUrlFor(row)}}</a>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="seriesId" prop="seriesId" min-width="100" align="center"></el-table-column>
        <el-table-column label="章节添加时间" prop="formattedChapterAddTime" min-width="150" align="center"></el-table-column>
      </el-table>
      <div class="pagination-box">
        <el-pagination v-bind="chapterPagination" @size-change="chapterSizeChange" @current-change="chapterCurrentChange" />
      </div>
    </basic-container>

    <basic-container v-if="activeTab === 'online'">
      <div class="data-table-option-buts">
        <span v-if="onlineSearched" class="jm-online-summary">
          关键字「{{onlineResult.searchQuery}}」，共 {{formatOnlineTotal}} 条，第 {{onlineResult.page}}/{{Math.max(onlineResult.totalPage, 1)}} 页，每页 {{onlineResult.pageSize}} 条
        </span>
        <span v-if="onlineSnapshotTime" class="jm-online-snapshot">
          <el-tag size="mini" type="warning">历史快照 {{formatOnlineHistoryTime(onlineSnapshotTime)}}</el-tag>
          <el-button type="text" size="mini" @click="refreshOnlineSearch">按此条件重新搜索</el-button>
        </span>
        <span v-if="!onlineSearched" class="jm-online-summary">结果为JM服务器实时数据，搜索到的记录需要点「添加」才会入库</span>
        <el-radio-group v-model="onlineViewMode" class="jm-view-switch" size="mini">
          <el-radio-button label="list"><i class="el-icon-s-unfold"></i> 列表</el-radio-button>
          <el-radio-button label="waterfall"><i class="el-icon-s-grid"></i> 瀑布流</el-radio-button>
        </el-radio-group>
      </div>
      <el-empty v-if="!onlineSearched" description="输入关键字后点击搜索"></el-empty>
      <template v-else>
        <el-table v-if="onlineViewMode === 'list'" tooltip-effect="light" :data="onlineResult.content" v-loading="onlineLoading" border stripe
                  max-height="800" size="small" row-key="id">
          <template slot="empty">
            <el-empty description="没有搜索到结果" :image-size="80"></el-empty>
          </template>
          <el-table-column label="封面" width="96" align="center">
            <template slot-scope="{row}">
              <el-image v-if="row.coverUrl" class="jm-cover-image" :src="row.coverUrl"
                        :preview-src-list="[row.coverUrl]" fit="cover" referrerpolicy="no-referrer">
                <div slot="placeholder" class="jm-cover-state"><i class="el-icon-loading"></i></div>
                <div slot="error" class="jm-cover-state"><i class="el-icon-picture-outline"></i></div>
              </el-image>
              <div v-else class="jm-cover-state"><i class="el-icon-picture-outline"></i></div>
            </template>
          </el-table-column>
          <el-table-column label="JM ID" prop="id" width="110" align="center"></el-table-column>
          <el-table-column label="名称" prop="name" min-width="260" show-overflow-tooltip></el-table-column>
          <el-table-column label="作者" prop="author" min-width="150" show-overflow-tooltip></el-table-column>
          <el-table-column label="分类" prop="category" width="140" show-overflow-tooltip></el-table-column>
          <el-table-column label="更新时间" prop="updateTime" width="150" align="center"></el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template slot-scope="{row}">
              <el-tag size="mini" :type="row.existsLocal ? 'success' : 'info'">{{row.existsLocal ? '已入库' : '未入库'}}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center">
            <template slot-scope="{row}">
              <div class="jm-online-actions">
                <el-tooltip :disabled="!row.existsLocal" content="本地已有该记录，无需重复添加" placement="top">
                  <span>
                    <el-button type="primary" size="mini" plain icon="el-icon-plus"
                               :loading="isOnlineAdding(row)" :disabled="isOnlineAddDisabled(row)"
                               @click="addOnlineAlbum(row)">添加</el-button>
                  </span>
                </el-tooltip>
                <el-tooltip content="先入库、再下载漫画，添加与下载一步完成" placement="top">
                  <span>
                    <el-button type="warning" size="mini" plain icon="el-icon-download"
                               :loading="isOnlineDownloading(row)" :disabled="isOnlineDownloadDisabled(row)"
                               @click="addOnlineAlbumAndDownload(row)">入库并下载</el-button>
                  </span>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <template v-else>
          <el-empty v-if="onlineResult.content.length === 0" description="没有搜索到结果" :image-size="80"></el-empty>
          <div v-else v-loading="onlineLoading" class="jm-waterfall">
            <div v-for="row in onlineResult.content" :key="`wf-${row.id}`" class="jm-waterfall-card">
              <div class="jm-waterfall-cover">
                <el-image v-if="row.coverUrl" :src="row.coverUrl" :preview-src-list="[row.coverUrl]"
                          fit="cover" referrerpolicy="no-referrer">
                  <div slot="placeholder" class="jm-waterfall-placeholder"><i class="el-icon-loading"></i></div>
                  <div slot="error" class="jm-waterfall-placeholder"><i class="el-icon-picture-outline"></i></div>
                </el-image>
                <div v-else class="jm-waterfall-placeholder"><i class="el-icon-picture-outline"></i></div>
                <el-tag class="jm-waterfall-status" size="mini" :type="row.existsLocal ? 'success' : 'info'">
                  {{row.existsLocal ? '已入库' : '未入库'}}
                </el-tag>
              </div>
              <div class="jm-waterfall-body">
                <div class="jm-waterfall-name" :title="row.name">{{row.name}}</div>
                <div class="jm-waterfall-meta">
                  <span class="jm-waterfall-id">JM{{row.id}}</span>
                  <span v-if="row.author" class="jm-waterfall-author" :title="row.author">{{row.author}}</span>
                </div>
                <div v-if="row.category" class="jm-waterfall-meta" :title="row.category">{{row.category}}</div>
                <div class="jm-waterfall-footer">
                  <span class="jm-waterfall-time">{{row.updateTime}}</span>
                  <span class="jm-online-wf-actions">
                    <el-button type="primary" size="mini" plain icon="el-icon-plus"
                               :loading="isOnlineAdding(row)" :disabled="isOnlineAddDisabled(row)"
                               @click="addOnlineAlbum(row)">{{row.existsLocal ? '已入库' : '添加'}}</el-button>
                    <el-button type="warning" size="mini" plain icon="el-icon-download"
                               :loading="isOnlineDownloading(row)" :disabled="isOnlineDownloadDisabled(row)"
                               @click="addOnlineAlbumAndDownload(row)">入库并下载</el-button>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </template>
        <div class="pagination-box">
          <el-pagination background
                         :current-page="onlineResult.page"
                         :page-size="onlineResult.pageSize"
                         :total="onlineResult.total"
                         layout="total, prev, pager, next, jumper"
                         @current-change="onlineCurrentChange" />
        </div>
      </template>
    </basic-container>

    <el-dialog title="删除JM主记录" :visible.sync="albumDeleteDialogVisible" width="420px"
               @closed="deleteAlbumDialogClosed">
      <div class="delete-tip">确认删除选中的 {{albumSelection.length}} 条JM主记录？</div>
      <el-checkbox v-model="albumDeleteOptions.deleteData">删除DB数据</el-checkbox>
      <el-checkbox v-model="albumDeleteOptions.deletePdf">删除pdf文件</el-checkbox>
      <el-checkbox v-model="albumDeleteOptions.deleteZip">删除zip文件</el-checkbox>
      <el-checkbox v-model="albumDeleteOptions.deleteImages">删除图片</el-checkbox>
      <span slot="footer">
        <el-button size="small" @click="albumDeleteDialogVisible = false">取消</el-button>
        <el-button type="danger" size="small" :loading="albumDeleteLoading" @click="deleteAlbumData">确定</el-button>
      </span>
    </el-dialog>

    <el-dialog title="删除章节图片" :visible.sync="chapterDeleteDialogVisible" width="420px"
               @closed="deleteChapterDialogClosed">
      <div class="delete-tip">确认删除选中的 {{chapterSelection.length}} 条章节图片记录？</div>
      <el-checkbox v-model="chapterDeleteOptions.deleteData">删除DB数据</el-checkbox>
      <el-checkbox v-model="chapterDeleteOptions.deleteFile">删除图片文件</el-checkbox>
      <span slot="footer">
        <el-button size="small" @click="chapterDeleteDialogVisible = false">取消</el-button>
        <el-button type="danger" size="small" :loading="chapterDeleteLoading" @click="deleteChapterData">确定</el-button>
      </span>
    </el-dialog>
    <el-dialog title="删除所有文件" :visible.sync="deleteAllFileDialogVisible" width="420px"
               @closed="deleteAllFileDialogClosed">
      <div class="delete-tip">确认删除所有文件？</div>
      <el-checkbox v-model="deleteAllFileOptions.deletePdf">删除PDF文件</el-checkbox>
      <el-checkbox v-model="deleteAllFileOptions.deleteZip">删除ZIP文件</el-checkbox>
      <el-checkbox v-model="deleteAllFileOptions.deleteImages">删除图片文件</el-checkbox>
      <span slot="footer">
        <el-button size="small" @click="deleteAllFileDialogVisible = false">取消</el-button>
        <el-button type="danger" size="small" :loading="allFileDeleteLoading" :disabled="allFileDeleteDisabled" @click="submitDeleteAllFile">确定</el-button>
      </span>
    </el-dialog>


    <jm-preview-drawer :visible.sync="previewDrawerVisible" :album="previewAlbum" />
    <jm-task-panel :visible.sync="taskPanelVisible" @snapshot="handleTaskSnapshot" />
    <jm-add-dialog :visible.sync="addDialogVisible"
                   :favorite-options="favoriteOptions"
                   @added="handleAlbumsAdded"
                   @collect="handleAddDialogCollect"></jm-add-dialog>
    <jm-favorite-picker :visible.sync="favoritePickerVisible"
                        :album-ids="favoritePickerAlbumIds"
                        :mode="favoritePickerMode"
                        :current-favorite-ids="favoritePickerCurrentIds"
                        :favorite-options="favoriteOptions"
                        @confirm="handleFavoritePicked"
                        @error="handleRequestError"></jm-favorite-picker>
  </div>
</template>

<script>
import JmPreviewDrawer from "./jm-preview-drawer.vue";
import JmTaskPanel from "./jm-task-panel.vue";
import JmTagSelect from "./jm-tag-select.vue";
import JmAuthorSelect from "./jm-author-select.vue";
import JmFavoritePanel from "./jm-favorite-panel.vue";
import JmAlbumView from "./jm-album-view.vue";
import JmAddDialog from "./jm-add-dialog.vue";
import JmFavoritePicker from "./jm-favorite-picker.vue";
import { ALBUM_COLUMN_OPTIONS, DEFAULT_ALBUM_COLUMNS } from "./jm-album-columns";
import numberInput from "@/components/input/numberInput.vue";
import {
  deleteAlbums,
  deleteAllFile,
  deleteChapterImages,
  downloadAlbum,
  collectAlbums,
  addAlbumsToFavorite,
  saveAlbumFavorites,
  createFavorite,
  listFavorites,
  generateAlbumPdf,
  generateAlbumZip,
  listJmTasks,
  requestAlbum,
  requestChapterImages,
  searchAlbums,
  searchChapterImages,
  searchOnlineAlbums,
  searchOnlineHistory,
  searchOnlineHistoryDetail,
  deleteSearchOnlineHistory
} from "@/api/jmcomic";
import { getStore, setStore } from "@/util/store";

// 列表/瀑布流的选择记在本地，下次进来保持上次的选择
const ONLINE_VIEW_MODE_KEY = 'jmOnlineViewMode';
const ALBUM_VIEW_MODE_KEY = 'jmAlbumViewMode';
// 任务面板关闭时只做低频轮询，仅用于刷新徽标和行内状态
const TASK_IDLE_POLL_MILLIS = 5000;
/**
 * 互斥的耗时操作：同一个本子同时只允许跑其中一个。
 * 预览、收藏、更改收藏夹等不受它们影响，任何时候都可以点。
 */
const ALBUM_BUSY_ACTIONS = ['download', 'zip', 'pdf'];

export default {
  name: 'JmcomicManage',
  components: { JmPreviewDrawer, JmTaskPanel, JmTagSelect, JmAuthorSelect, JmFavoritePanel, JmAlbumView, JmAddDialog, JmFavoritePicker, numberInput },
  data() {
    return {
      activeTab: 'album',
      albumLoading: false,
      chapterLoading: false,
      chapterRequestLoading: false,
      albumDeleteLoading: false,
      albumCollectLoading: null,
      chapterDeleteLoading: false,
      allFileDeleteLoading: false,
      albumDeleteDialogVisible: false,
      chapterDeleteDialogVisible: false,
      deleteAllFileDialogVisible: false,
      // 新增JM主记录弹窗（支持剪贴板/批量/链接解析）
      addDialogVisible: false,
      // 收藏夹选择弹窗：收藏时先选收藏夹；mode=edit 时是"更改收藏夹"
      favoritePickerVisible: false,
      favoritePickerAlbumIds: [],
      favoritePickerMode: 'collect',
      // 更改收藏夹时回显：这些漫画当前所属的收藏夹id
      favoritePickerCurrentIds: [],
      // 收藏夹列表，供新增弹窗与收藏选择弹窗复用，避免各自请求
      favoriteOptions: [],
      previewDrawerVisible: false,
      previewAlbum: null,
      // 内存中的JM任务面板(不持久化，后端重启即清空)
      taskPanelVisible: false,
      taskSnapshot: this.defTaskSnapshot(),
      taskPollTimer: null,
      // 全局WebSocket是否已连通(连通时任务状态由推送更新，不再轮询)
      wsConnected: false,
      // 是否已订阅任务主题，避免重复订阅造成引用计数泄漏
      taskPushBound: false,
      taskSnapshotOff: null,
      taskStatusOff: null,
      albumQuery: { id: '', name: '', author: '', tags: [], collected: '' },
      // 收藏夹 tab 的查询条件：和 JM主记录一样由页面持有（查询块是父组件里的独立卡片），
      // 收藏夹面板只通过 query prop 读取，查询/重置按钮也在父组件这一侧
      favoriteQuery: { name: '', author: '' },
      // JM主记录展示方式：list=表格，waterfall=瀑布流卡片(默认)
      albumViewMode: getStore({ name: ALBUM_VIEW_MODE_KEY }) || 'waterfall',
      chapterQuery: { albumId: '', chapterId: '', chapterTitle: '', imageFile: '' },
      // JM在线搜索：分页由JM服务器完成，页码从1开始
      onlineQuery: { name: '', sort: 'mr', page: 1 },
      onlineSortOptions: [
        { value: 'mr', label: '最新' },
        { value: 'mv', label: '最多观看' },
        { value: 'mp', label: '最多图片' },
        { value: 'tf', label: '最多喜欢' }
      ],
      onlineLoading: false,
      onlineSearched: false,
      // 搜索结果展示方式：list=表格，waterfall=瀑布流卡片(默认)
      onlineViewMode: getStore({ name: ONLINE_VIEW_MODE_KEY }) || 'waterfall',
      onlineResult: this.defOnlineResult(),
      // 当前展示的是哪条历史快照，为空表示是实时搜索结果
      onlineSnapshotTime: null,
      // 当前激活的历史记录id(只高亮被点的那一条)
      onlineHistoryActiveId: null,
      // 搜索历史
      onlineHistory: [],
      onlineHistoryLoading: false,
      onlineHistoryLoaded: false,
      onlineHistoryCollapsed: false,
      // 正在添加的搜索结果，key=jmId
      onlineAddingMap: {},
      // 正在"入库并下载"的搜索结果，key=jmId
      onlineDownloadingMap: {},
      // 在线搜索里添加过记录后，JM主记录列表需要重新查询
      albumListDirty: false,
      albumData: [],
      chapterData: [],
      albumSelection: [],
      chapterSelection: [],
      albumOperationLoading: {},
      albumVisibleColumns: [...DEFAULT_ALBUM_COLUMNS],
      // 列定义与收藏夹 tab 共用（jm-album-columns.js），避免两边列集合漂移
      albumColumnOptions: ALBUM_COLUMN_OPTIONS,
      // 行操作按钮：由共享组件 jm-album-view 渲染，点击后回到 handleAlbumRowAction
      albumRowActions: [
        { key: 'preview', icon: 'el-icon-view', type: 'primary', tooltip: '预览漫画' },
        { key: 'download', icon: 'el-icon-download', type: 'primary', tooltip: '下载漫画' },
        { key: 'zip', icon: 'el-icon-folder-add', type: 'success', tooltip: '生成zip' },
        { key: 'pdf', icon: 'el-icon-document-add', type: 'warning', tooltip: '生成pdf' }
      ],
      albumDeleteOptions: this.defAlbumDeleteOptions(),
      chapterDeleteOptions: this.defChapterDeleteOptions(),
      deleteAllFileOptions: this.defDeleteAllFileOptions(),
      albumPagination: {
        currentPage: 1,
        pageSizes: [5, 10, 30, 50, 100, 500],
        pageSize: 50,
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
    },
    allFileDeleteDisabled(){
      return !this.deleteAllFileOptions.deletePdf && !this.deleteAllFileOptions.deleteZip && !this.deleteAllFileOptions.deleteImages
    },
    /**
     * JM的total上限就是10000，到顶时按"10000+"展示，避免让人以为是精确总数
     */
    formatOnlineTotal() {
      const total = Number(this.onlineResult.total || 0)
      return total >= 10000 ? `${total}+` : total
    },
    /**
     * 进行中 + 排队中的任务数，用于工具栏徽标
     */
    taskActiveCount() {
      const counters = this.taskSnapshot.counters || {}
      return (counters.running || 0) + (counters.queued || 0)
    },
    /**
     * aid -> 正在执行或排队中的任务，用于让列表里的操作按钮与任务队列保持一致
     */
    taskMap() {
      const map = {}
      const collect = (list, status) => (list || []).forEach(task => {
        if (task && task.aid) {
          map[`${task.aid}`] = { status, action: task.action, taskId: task.taskId }
        }
      })
      collect(this.taskSnapshot.runningList, 'running')
      collect(this.taskSnapshot.queuedList, 'queued')
      return map
    }
  },
  watch: {
    onlineViewMode(val) {
      setStore({ name: ONLINE_VIEW_MODE_KEY, content: val })
    },
    albumViewMode(val) {
      setStore({ name: ALBUM_VIEW_MODE_KEY, content: val })
      // 勾选状态由列表组件持有，列表/瀑布流共用，切换展示方式时不再清空
    }
  },
  mounted() {
    this.searchAlbumsFirst()
    this.startTaskPolling()
    this.bindTaskPush()
    // 收藏夹列表供"新增"与"收藏到收藏夹"复用，进页面就取一次
    this.loadFavoriteOptions()
  },
  beforeDestroy() {
    this.stopTaskPolling()
    this.unbindTaskPush()
  },
  methods: {
    formatBool(value) {
      return value ? '是' : '否'
    },
    formatCount(value) {
      return value === null || value === undefined || value === '' ? '-' : value
    },
    safeNumber(v){
      return !v || !(typeof v === 'number') ? 0 : v
    },
    /**
     * 章节图片的服务器地址，同样按当前访问站点替换，便于直接点开查看
     */
    serverImgUrlFor(row) {
      return this.$localUrl(row && row.serverImgUrl)
    },
    isAlbumCollected(row) {
      return !!(row && row.collected)
    },
    /**
     * 共享列表组件里的行操作：按 action key 分发到具体处理逻辑
     */
    handleAlbumRowAction(key, row) {
      if (key === 'preview') {
        return this.openPreview(row)
      }
      if (key === 'download') {
        return this.downloadAlbumData(row)
      }
      if (key === 'zip') {
        return this.confirmGenerateZip(row)
      }
      if (key === 'pdf') {
        return this.confirmGeneratePdf(row)
      }
    },
    handleAlbumColumnsChange() {
      if (this.$refs.albumView) {
        this.$refs.albumView.doLayout()
      }
    },
    handleRequestError(error) {
      // const message = error && error.data && error.data.message
      //   ? error.data.message
      //   : error && error.message
      //     ? error.message
      //     : '请求失败'
      // this.$message.error(message)
    },
    /**
     * 标签候选由组件在首次展开下拉时拉取，新增/删除JM记录后标签可能变化，通知组件重新拉取
     */
    refreshTagOptions() {
      if (this.$refs.tagSelect) {
        this.$refs.tagSelect.refresh()
      }
    },
    /**
     * 作者候选同样由组件在首次展开下拉时拉取，新增/删除JM记录后作者可能变化，通知组件重新拉取
     */
    refreshAuthorOptions() {
      if (this.$refs.authorSelect) {
        this.$refs.authorSelect.refresh()
      }
    },
    isAlbumOperation(row, action) {
      if (!row) {
        return false
      }
      if (this.albumOperationLoading[row.id] === action) {
        return true
      }
      // 串行模式下HTTP请求会立刻返回"已加入队列"，之后靠任务快照维持按钮的loading态
      return this.isAlbumTaskOperation(row, action)
    },
    isAlbumOperating(row) {
      return !!(row && (this.albumOperationLoading[row.id] || this.isAlbumTaskBusy(row)))
    },
    isAlbumOtherOperation(row, action) {
      return this.isAlbumOperating(row) && !this.isAlbumOperation(row, action)
    },
    /**
     * 行操作按钮/收藏星标的禁用判定。
     * 只有下载/ZIP/PDF 这三个耗时操作之间互斥（同一个本子不能同时跑两个），
     * 预览、收藏、更改收藏夹、移出收藏夹都不受影响——以前用 isAlbumOtherOperation
     * 会把它们一起禁用，导致下载中连预览和收藏都点不了。
     */
    albumActionDisabled(row, action) {
      if (!ALBUM_BUSY_ACTIONS.includes(action)) {
        return false
      }
      return this.isAlbumOtherOperation(row, action)
    },
    isAlbumTaskBusy(row) {
      return !!(row && this.taskMap[`${row.id}`])
    },
    isAlbumTaskOperation(row, action) {
      const task = row ? this.taskMap[`${row.id}`] : null
      return !!task && task.action === action
    },
    defTaskSnapshot() {
      return {
        parallel: false,
        runningList: [],
        queuedList: [],
        finishedList: [],
        counters: { running: 0, queued: 0, success: 0, fail: 0, cancelled: 0 }
      }
    },
    /**
     * 任务状态走全局WebSocket推送；WebSocket未连通时回退到低频HTTP轮询，
     * 保证代理挡掉ws、后端未重启等情况下页面依然有数据
     */
    startTaskPolling() {
      this.stopTaskPolling()
      this.pollJmTasks()
      this.taskPollTimer = setInterval(() => {
        if (this.wsConnected || document.hidden) {
          return
        }
        this.pollJmTasks()
      }, TASK_IDLE_POLL_MILLIS)
    },
    stopTaskPolling() {
      if (this.taskPollTimer) {
        clearInterval(this.taskPollTimer)
        this.taskPollTimer = null
      }
    },
    /**
     * 注册全局WebSocket的JM任务推送，并把连接状态同步给轮询兜底
     */
    bindTaskPush() {
      if (this.taskPushBound) {
        return
      }
      this.taskPushBound = true
      this.taskSnapshotOff = this.$ws.on('jm.task.snapshot', this.handleTaskSnapshot)
      this.taskStatusOff = this.$ws.onStatus(this.handleWsStatus)
      this.$ws.subscribe('jm.task')
    },
    unbindTaskPush() {
      if (!this.taskPushBound) {
        return
      }
      this.taskPushBound = false
      if (this.taskSnapshotOff) {
        this.taskSnapshotOff()
        this.taskSnapshotOff = null
      }
      if (this.taskStatusOff) {
        this.taskStatusOff()
        this.taskStatusOff = null
      }
      this.$ws.unsubscribe('jm.task')
    },
    handleWsStatus(status) {
      this.wsConnected = status === 'open'
    },
    pollJmTasks() {
      return listJmTasks().then(({data: {code, data}}) => {
        if (code !== 200 || !data) {
          return
        }
        this.applyTaskSnapshot(data)
      }).catch(() => {
        // 轮询失败静默处理
      })
    },
    handleTaskSnapshot(data) {
      this.applyTaskSnapshot(data)
    },
    /**
     * HTTP轮询、WebSocket推送、任务面板回传三条来源都汇总到这里
     */
    applyTaskSnapshot(data) {
      if (!data) {
        return
      }
      const previousActive = this.taskActiveCount
      this.taskSnapshot = data
      // 任务从"有"变为"无"时刷新一次列表，让zip/pdf等落盘状态跟上
      if (previousActive > 0 && this.taskActiveCount === 0 && this.activeTab === 'album') {
        this.selectAlbums()
      }
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
    cleanQuery(query) {
      const res = { ...query }
      Object.keys(res).forEach(key => {
        if (res[key] === '') {
          res[key] = null
        } else if (Array.isArray(res[key]) && res[key].length === 0) {
          // 标签等数组参数没有选中项时传null，避免后端把它当成一个有效的过滤条件
          res[key] = null
        }
      })
      return res
    },
    handleTabClick() {
      if (this.activeTab === 'chapter' && this.chapterData.length === 0) {
        this.searchChaptersFirst()
      }
      if (this.activeTab === 'online') {
        this.ensureOnlineHistory()
      }
      // 收藏夹 tab 由 v-if 控制挂载，面板挂载时会自己拉收藏夹列表，
      // 这里不要再调 loadFavoriteOptions()，否则一次切tab会请求两次 /favorite/list
      if (this.activeTab === 'favorite') {
        this.refreshFavoritePanel()
      }
      // 在线搜索里添加过记录，切回主记录时再刷新，避免每次添加都白查一次
      if (this.activeTab === 'album' && this.albumListDirty) {
        this.albumListDirty = false
        this.searchAlbumsFirst()
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
    defOnlineResult() {
      return { searchQuery: '', total: 0, page: 1, pageSize: 80, totalPage: 0, content: [] }
    },
    searchOnlineFirst() {
      this.onlineQuery.page = 1
      this.searchOnline()
    },
    /**
     * JM在线搜索：排序与分页都直接透传给JM服务器，后端每页固定80条
     */
    searchOnline() {
      const name = (this.onlineQuery.name || '').trim()
      if (!name) {
        return this.$message.warning('请输入搜索关键字')
      }
      this.onlineLoading = true
      searchOnlineAlbums({
        name,
        sort: this.onlineQuery.sort,
        page: this.onlineQuery.page
      }).then(({data: {code, message, data}}) => {
        if (code !== 200) {
          return this.$message.error(message || 'JM在线搜索失败')
        }
        this.onlineResult = {
          searchQuery: data.searchQuery || name,
          total: Number(data.total || 0),
          page: Number(data.page || this.onlineQuery.page),
          pageSize: Number(data.pageSize || 80),
          totalPage: Number(data.totalPage || 0),
          content: (data.content || []).map(e => ({...e, existsLocal: !!e.existsLocal}))
        }
        this.onlineQuery.page = this.onlineResult.page
        this.onlineSearched = true
        // 实时搜索：不再是历史快照，历史高亮也取消
        this.onlineSnapshotTime = null
        this.onlineHistoryActiveId = null
        // 后端每次搜索都会记一条历史，这里同步刷新
        this.loadOnlineHistory()
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.onlineLoading = false
      })
    },
    /**
     * 排序变化后重新搜索(还没搜索过就不发请求)
     */
    handleOnlineSortChange() {
      if (this.onlineSearched) {
        this.searchOnlineFirst()
      }
    },
    onlineCurrentChange(page) {
      this.onlineQuery.page = page
      this.searchOnline()
    },
    resetOnlineQuery() {
      this.onlineQuery = { name: '', sort: 'mr', page: 1 }
      this.onlineResult = this.defOnlineResult()
      this.onlineSearched = false
      this.onlineSnapshotTime = null
      this.onlineHistoryActiveId = null
      if (this.$refs.onlineQueryForm) {
        this.$refs.onlineQueryForm.clearValidate()
      }
    },
    /**
     * 搜索历史：列表按时间倒序，条数由 jm.search.history.limit 控制
     */
    loadOnlineHistory() {
      this.onlineHistoryLoading = true
      searchOnlineHistory().then(({data: {code, message, data}}) => {
        if (code !== 200) {
          return this.$message.error(message || '搜索历史查询失败')
        }
        this.onlineHistory = data || []
        this.onlineHistoryLoaded = true
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.onlineHistoryLoading = false
      })
    },
    /**
     * 切到在线搜索页签时才拉历史，拉过一次就不再重复请求
     */
    ensureOnlineHistory() {
      if (!this.onlineHistoryLoaded) {
        this.loadOnlineHistory()
      }
    },
    toggleOnlineHistory() {
      this.onlineHistoryCollapsed = !this.onlineHistoryCollapsed
    },
    /**
     * 历史时间在后端是 yyyyMMddHHmmss，胶囊里只展示 MM-dd HH:mm
     */
    formatOnlineHistoryTime(value) {
      const text = `${value || ''}`.trim()
      if (!/^\d{14}$/.test(text)) {
        return text
      }
      return `${text.slice(4, 6)}-${text.slice(6, 8)} ${text.slice(8, 10)}:${text.slice(10, 12)}`
    },
    onlineHistoryTitle(item) {
      if (!item) {
        return ''
      }
      return `关键字：${item.name || ''}\n排序：${item.sortLabel || ''}\n页码：第${item.page || 1}页`
        + `\n结果：共${item.total || 0}条，本次返回${item.resultCount || 0}条`
        + `\n搜索时间：${item.searchTime || ''}`
    },
    /**
     * 当前激活的历史记录：只认被点击的那一条(条件相同的多条记录不会一起高亮)
     */
    isOnlineHistoryActive(item) {
      return !!item && item.id === this.onlineHistoryActiveId
    },
    /**
     * 点击历史记录：回显当时的搜索条件与页码，并直接渲染当时保存的结果快照，不再请求JM
     * 快照不存在(比如已被清理)时退回实时搜索，保证点了有反应
     */
    applyOnlineHistory(item) {
      if (!item || !item.id) {
        return
      }
      this.onlineQuery.name = item.name || ''
      this.onlineQuery.sort = item.sort || 'mr'
      this.onlineQuery.page = item.page || 1
      this.onlineHistoryActiveId = item.id
      searchOnlineHistoryDetail(item.id).then(({data: {code, message, data}}) => {
        if (code !== 200) {
          this.$message.warning(message || '历史记录不可用，已改为实时搜索')
          this.onlineHistoryActiveId = null
          return this.searchOnline()
        }
        this.onlineResult = this.buildOnlineResultFromSnapshot(data, item)
        this.onlineSnapshotTime = data.searchTime || item.searchTime || ''
        this.onlineSearched = true
      }).catch(error => {
        this.handleRequestError(error)
      })
    },
    /**
     * 用历史快照拼出与实时搜索一致的结果结构
     */
    buildOnlineResultFromSnapshot(data, item) {
      const pageSize = Number(data.pageSize || 80)
      const total = Number(data.total || 0)
      return {
        searchQuery: data.name || item.name || '',
        total,
        page: Number(data.page || item.page || 1),
        pageSize,
        totalPage: pageSize > 0 ? Math.ceil(total / pageSize) : 0,
        content: (data.items || []).map(e => ({...e, existsLocal: !!e.existsLocal}))
      }
    },
    /**
     * 当前展示的是历史快照时，按同样的条件重新实时搜索一次
     */
    refreshOnlineSearch() {
      this.onlineSnapshotTime = null
      this.onlineHistoryActiveId = null
      this.searchOnline()
    },
    removeOnlineHistory(item) {
      if (!item) {
        return
      }
      deleteSearchOnlineHistory({ ids: [item.id] }).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message || '删除失败')
        }
        this.$message.success(message || '删除完成')
        if (this.onlineHistoryActiveId === item.id) {
          this.onlineHistoryActiveId = null
        }
        this.loadOnlineHistory()
      }).catch(error => {
        this.handleRequestError(error)
      })
    },
    clearOnlineHistory() {
      this.$confirm('确认清空全部JM在线搜索历史？', '清空搜索历史', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteSearchOnlineHistory({ clearAll: true }).then(({data: {code, message}}) => {
          if (code !== 200) {
            return this.$message.error(message || '清空失败')
          }
          this.$message.success(message || '清空完成')
          this.loadOnlineHistory()
        }).catch(error => {
          this.handleRequestError(error)
        })
      }).catch(() => {})
    },
    isOnlineAdding(row) {
      return !!(row && this.onlineAddingMap[row.id])
    },
    isOnlineAddDisabled(row) {
      return !row || !!row.existsLocal || this.isOnlineAdding(row)
    },
    isOnlineDownloading(row) {
      return !!(row && this.onlineDownloadingMap[row.id])
    },
    /**
     * "入库并下载"的禁用判定。
     * 已经入库不影响使用（本地有记录但没下载过时正好用它补下载），
     * 只有自己正在请求、或该JM已有任务在跑时才不可点
     */
    isOnlineDownloadDisabled(row) {
      if (!row || this.isOnlineDownloading(row)) {
        return true
      }
      return !!this.taskMap[`${row.id}`]
    },
    /**
     * 添加入库并下载：/manage/album/download/{aid} 内部会先请求本子详情入库再下载，
     * 一步即可完成"添加 + 下载"。下载是异步任务，提交成功后让主记录列表待刷新
     */
    addOnlineAlbumAndDownload(row) {
      if (this.isOnlineDownloadDisabled(row)) {
        return
      }
      this.$set(this.onlineDownloadingMap, row.id, true)
      downloadAlbum(row.id).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message || '入库并下载失败')
        }
        this.$message.success(message || `JM${row.id} 已加入下载队列`)
        this.albumListDirty = true
        // 任务已提交，立即刷新任务快照，让角标与行内按钮状态跟上
        this.pollJmTasks()
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.$delete(this.onlineDownloadingMap, row.id)
      })
    },
    /**
     * 快捷添加：复用已有的 /manage/album/request/{aid}，只入库不下载
     * 同一行添加中不允许重复点，添加成功后标记已入库并让主记录列表待刷新
     */
    addOnlineAlbum(row) {
      if (this.isOnlineAddDisabled(row)) {
        return
      }
      this.$set(this.onlineAddingMap, row.id, true)
      requestAlbum(row.id).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message || '添加失败')
        }
        this.$set(row, 'existsLocal', true)
        this.$message.success(`JM${row.id} 添加完成`)
        // 入库后标签/作者候选可能变化，组件内部没加载过候选时不会发请求
        this.refreshTagOptions()
        this.refreshAuthorOptions()
        this.albumListDirty = true
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.$delete(this.onlineAddingMap, row.id)
      })
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
        // 记录里的JSON字段（作者/标签/作品/角色/相关列表）由共享列表组件统一展开
        this.albumData = data.records || []
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
        // 立即刷新任务快照，让徽标和行内按钮状态马上跟上
        this.pollJmTasks()
      })
    },
    downloadAlbumData(row) {
      this.executeAlbumOperation(row, 'download', downloadAlbum)
    },
    /**
     * 单条收藏/取消收藏，只更新该行，避免整页刷新
     * 收藏：先让用户选收藏夹；取消：直接从所有收藏夹移出
     * 当前带有收藏筛选条件时，该行可能已经不满足条件，重新查询一次
     */
    toggleAlbumCollected(row) {
      // 只挡住"收藏"自身的重复点击；本子正在下载/打包时依然可以收藏与取消收藏
      if (this.isAlbumOperation(row, 'collect')) {
        return
      }
      if (!this.isAlbumCollected(row)) {
        // 收藏前先选收藏夹，默认选中默认收藏夹
        this.openFavoritePicker([row.id])
        return
      }
      this.$set(this.albumOperationLoading, row.id, 'collect')
      collectAlbums({ ids: [row.id], collected: false }).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$set(row, 'collected', false)
        this.$set(row, 'favoriteIds', [])
        this.$message.success(message || '取消收藏完成')
        this.refreshFavoritePanel()
        this.searchAlbumsIfCollectedFiltered()
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.$delete(this.albumOperationLoading, row.id)
      })
    },
    /**
     * 批量收藏：先选收藏夹再提交
     */
    openFavoritePickerForSelection() {
      if (this.albumDeleteDisabled) {
        return
      }
      this.openFavoritePicker(this.albumSelection.map(e => e.id))
    },
    /**
     * 打开收藏夹选择弹窗
     * @param albumIds 待处理的漫画id
     * @param mode collect=收藏到收藏夹（默认）；edit=更改漫画所属的收藏夹
     * @param currentFavoriteIds edit 模式下这些漫画当前所属的收藏夹id，用于回显
     */
    openFavoritePicker(albumIds, mode = 'collect', currentFavoriteIds = []) {
      const ids = (albumIds || []).filter(id => id !== null && id !== undefined)
      if (ids.length === 0) {
        return
      }
      this.favoritePickerMode = mode
      this.favoritePickerAlbumIds = ids
      this.favoritePickerCurrentIds = currentFavoriteIds || []
      // 打开前刷新收藏夹列表，保证刚新建的收藏夹能立刻选到
      this.loadFavoriteOptions()
      this.favoritePickerVisible = true
    },
    /**
     * 收藏夹 tab 里点"更改收藏夹"：面板只负责告诉父组件要改哪些漫画，
     * 弹窗与请求统一由父组件处理（与"收藏到收藏夹"共用同一个弹窗）
     */
    handleFavoriteChangeRequest({ albumIds, favoriteIds }) {
      this.openFavoritePicker(albumIds, 'edit', favoriteIds)
    },
    getFavoritePanel() {
      return this.$refs.favoritePanel || null
    },
    refreshFavoritePanel() {
      const panel = this.getFavoritePanel()
      if (panel) {
        panel.reload()
      }
    },
    /**
     * 收藏夹 tab 查询：查询条件在父组件，取数逻辑在收藏夹面板里
     */
    searchFavoriteFirst() {
      const panel = this.getFavoritePanel()
      if (panel) {
        panel.searchFirst()
      }
    },
    /**
     * 重置收藏夹查询条件
     * 这里逐项改属性、不整体替换对象：面板读的是同一个对象引用，立即生效；
     * 若整个对象替换掉，要等父组件重新渲染才会同步到 prop，紧接着的查询会用旧条件。
     */
    resetFavoriteQuery() {
      this.favoriteQuery.name = ''
      this.favoriteQuery.author = ''
      this.searchFavoriteFirst()
    },
    /**
     * 收藏夹选择确认，两种模式共用一个弹窗：
     * - collect：收藏到勾选的收藏夹（可多选、可按名称新建）
     * - edit：更改漫画所属的收藏夹，以勾选结果为准（未勾选的移出，全不勾=从所有收藏夹移出）
     */
    handleFavoritePicked(payload) {
      const albumIds = this.favoritePickerAlbumIds
      if (!albumIds || albumIds.length === 0) {
        return
      }
      const isEdit = this.favoritePickerMode === 'edit'
      const favoriteIds = payload.favoriteIds || []
      const favoriteNames = payload.favoriteNames || []
      this.favoritePickerVisible = false
      const isBatch = albumIds.length > 1
      if (isBatch) {
        this.albumCollectLoading = isEdit ? 'favorite' : 'collect'
      } else {
        this.$set(this.albumOperationLoading, albumIds[0], isEdit ? 'favorite' : 'collect')
      }
      const request = isEdit
        ? saveAlbumFavorites({ albumIds, favoriteIds, favoriteNames })
        : collectAlbums({ ids: albumIds, collected: true, favoriteIds, favoriteNames })
      request.then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$message.success(message || (isEdit ? '收藏夹已更新' : '收藏完成'))
        this.loadFavoriteOptions()
        this.refreshFavoritePanel()
        if (isEdit) {
          // 收藏夹归属变了，主记录列表的收藏标记要重新查（切回主记录 tab 时刷新）
          this.albumListDirty = true
          return
        }
        if (isBatch) {
          this.selectAlbums()
        } else {
          // 单条就地更新，避免整页刷新
          const row = this.albumData.find(item => item.id === albumIds[0])
          if (row) {
            this.$set(row, 'collected', true)
          }
          this.searchAlbumsIfCollectedFiltered()
        }
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        if (isBatch) {
          this.albumCollectLoading = null
        } else {
          this.$delete(this.albumOperationLoading, albumIds[0])
        }
        this.favoritePickerAlbumIds = []
        this.favoritePickerCurrentIds = []
        this.favoritePickerMode = 'collect'
      })
    },
    /**
     * 取消收藏选中的记录（从所有收藏夹移出）
     */
    collectSelectedAlbums(collected) {
      if (this.albumDeleteDisabled) {
        return
      }
      const ids = this.albumSelection.map(e => e.id)
      this.albumCollectLoading = collected ? 'collect' : 'uncollect'
      collectAlbums({ ids, collected }).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$message.success(message || (collected ? '收藏完成' : '取消收藏完成'))
        this.refreshFavoritePanel()
        this.selectAlbums()
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.albumCollectLoading = null
      })
    },
    /**
     * 加载收藏夹列表，供新增弹窗与收藏选择弹窗复用
     */
    loadFavoriteOptions() {
      listFavorites().then(({data: {code, data}}) => {
        if (code === 200) {
          this.favoriteOptions = data || []
        }
      }).catch(() => {
        // 收藏夹列表失败不阻塞主流程
      })
    },
    /**
     * 新增弹窗里选择了"同时收藏到"时的处理
     */
    handleAddDialogCollect({ albumIds, favoriteName }) {
      if (!albumIds || albumIds.length === 0 || !favoriteName) {
        return
      }
      addAlbumsToFavorite({ albumIds, favoriteName }).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message || '收藏失败')
        }
        this.$message.success(`已收藏到「${favoriteName}」`)
        this.loadFavoriteOptions()
        this.refreshFavoritePanel()
      }).catch(error => {
        this.handleRequestError(error)
      })
    },
    /**
     * 新增弹窗完成拉取后刷新列表与标签/作者候选
     */
    handleAlbumsAdded({ successIds }) {
      if (!successIds || successIds.length === 0) {
        return
      }
      this.refreshTagOptions()
      this.refreshAuthorOptions()
      this.searchAlbumsFirst()
      this.refreshFavoritePanel()
      this.loadFavoriteOptions()
    },
    /**
     * 收藏夹 tab 内新建收藏夹
     */
    handleCreateFavorite(name) {
      createFavorite({ name }).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message || '新建失败')
        }
        this.$message.success('新建成功')
        this.loadFavoriteOptions()
        this.refreshFavoritePanel()
      }).catch(error => {
        this.handleRequestError(error)
      })
    },
    /**
     * 收藏夹内容变化（移出/删除收藏夹）后，主记录列表的收藏标记可能已过期
     */
    handleFavoriteAlbumsChanged() {
      this.loadFavoriteOptions()
      this.searchAlbumsIfCollectedFiltered()
    },
    /**
     * 收藏筛选生效时(已收藏/未收藏)，收藏状态变化会改变筛选结果，需要重新查询
     */
    searchAlbumsIfCollectedFiltered() {
      const collected = this.albumQuery.collected
      if (collected === true || collected === false) {
        this.selectAlbums()
      }
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
      this.albumDeleteDialogVisible = true
    },
    openAllFileDelete(){
      this.deleteAllFileDialogVisible = true
    },
    openChapterDelete() {
      if (this.chapterDeleteDisabled) {
        return
      }
      this.chapterDeleteDialogVisible = true
    },
    defChapterDeleteOptions(){
      return { deleteFile: true,deleteData:false }
    },
    defAlbumDeleteOptions(){
      return { deleteData: false,deletePdf: true, deleteZip: true, deleteImages: false }
    },
    defDeleteAllFileOptions(){
      return { deletePdf: true, deleteZip: false, deleteImages: false }
    },
    deleteAlbumDialogClosed(){
      this.albumDeleteOptions = this.defAlbumDeleteOptions()
    },
    deleteChapterDialogClosed(){
      this.chapterDeleteOptions = this.defChapterDeleteOptions()
    },
    deleteAllFileDialogClosed(){
      this.deleteAllFileOptions = this.defDeleteAllFileOptions()
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
        this.refreshTagOptions()
        this.refreshAuthorOptions()
        this.searchAlbumsFirst()
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.albumDeleteLoading = false
      })
    },
    submitDeleteAllFile(){
      this.allFileDeleteLoading = true
      deleteAllFile(this.deleteAllFileOptions).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.deleteAllFileDialogVisible = false
        this.$message.success(message)
        this.searchChaptersFirst()
      }).catch(error => {

      }).finally(() => {
        this.allFileDeleteLoading = false
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

  /**
   * 任务队列徽标：避免角标盖住右侧按钮。
   * 主记录 tab 与收藏夹 tab 都要用，定义见 styles/jm-shared.scss（全局）
   */

  /**
   * 以下类由 jmcomic/index.vue 与 jm-favorite-panel.vue 共用，定义已移到
   * styles/jm-shared.scss（全局），避免只对某个组件的模板生效：
   * jm-waterfall*、jm-action-grid、jm-tag-list、jm-tag-summary、
   * jm-cover-image、jm-cover-state、jm-stat-cell、jm-album-collected-row、
   * jm-related-cell、jm-related-line、jm-related-name
   * （列表/瀑布流本身也已抽到 jm-album-view.vue，两个 tab 共用）
   */

  .jm-online-summary {
    color: #606266;
    font-size: 13px;
    line-height: 28px;
  }

  /**
   * 在线搜索结果的"添加 / 入库并下载"两个按钮：表格里并排居中，瀑布流里并排在右下角
   */
  .jm-online-actions {
    align-items: center;
    display: flex;
    gap: 6px;
    justify-content: center;

    .el-button {
      margin-left: 0;
    }
  }

  /**
   * 在线搜索瀑布流底部的时间 +「添加 / 入库并下载」：
   * 卡片很窄（column-width 176px），两个按钮放不下就在容器内换行。
   * 注意这里必须是 flex: 0 1 auto（而不是 0 0 auto）：后者容器宽度恒等于按钮的
   * max-content 宽度，比卡片内容区还宽时内部的 flex-wrap 根本不生效，按钮会直接
   * 溢出到 padding 区（"已入库"比"添加"长，更容易触发）
   */
  .jm-online-wf-actions {
    display: inline-flex;
    flex: 0 1 auto;
    flex-wrap: wrap;
    gap: 6px;
    max-width: 100%;
    min-width: 0;

    .el-button {
      margin-left: 0;
      padding: 5px 8px;
    }
  }

  .jm-view-switch {
    margin-left: auto;
  }

  .jm-online-snapshot {
    align-items: center;
    display: inline-flex;
    gap: 6px;

    .el-button {
      padding: 0;
    }
  }

  /**
   * 搜索历史：胶囊列表，点击回显条件并重新搜索
   */
  .jm-online-history {
    border-top: 1px solid #ebeef5;
    margin-top: 12px;
    padding-top: 10px;
  }

  .jm-online-history-head {
    align-items: center;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .jm-online-history-title {
    color: #303133;
    font-size: 13px;
    font-weight: 600;
  }

  .jm-online-history-hint {
    color: #c0c4cc;
    font-size: 12px;
  }

  .jm-online-history-ops {
    margin-left: auto;

    .el-button {
      margin-left: 8px;
      padding: 0;
    }
  }

  .jm-online-history-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 8px;
    max-height: 116px;
    min-height: 32px;
    overflow: auto;
  }

  .jm-online-history-empty {
    color: #c0c4cc;
    font-size: 12px;
    line-height: 32px;
  }

  .jm-online-history-item {
    cursor: pointer;
    max-width: 100%;

    .jm-online-history-name {
      font-weight: 600;
    }

    .jm-online-history-meta {
      margin-left: 6px;
      opacity: .8;
    }

    .jm-online-history-remove {
      cursor: pointer;
      margin-left: 6px;

      &:hover {
        color: #f56c6c;
      }
    }
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

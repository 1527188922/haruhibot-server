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
        <el-button type="primary" size="small" plain icon="el-icon-plus" :loading="albumRequestLoading" @click="addAlbum">新增</el-button>
        <el-badge class="jm-task-badge" :value="taskActiveCount" :hidden="taskActiveCount === 0" type="warning">
          <el-button type="primary" size="small" plain icon="el-icon-s-operation" @click="taskPanelVisible = true">任务队列</el-button>
        </el-badge>
        <el-button type="danger" size="small" plain icon="el-icon-delete" :disabled="albumDeleteDisabled" @click="openAlbumDelete">批量删除</el-button>
        <el-button type="warning" size="small" plain icon="el-icon-star-on" :disabled="albumDeleteDisabled || !!albumCollectLoading" :loading="albumCollectLoading === 'collect'" @click="collectSelectedAlbums(true)">批量收藏</el-button>
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
      <el-table v-if="albumViewMode === 'list'" tooltip-effect="light" :data="albumData" v-loading="albumLoading" border stripe max-height="800"
                size="small" ref="albumTable" highlight-current-row :row-class-name="albumRowClassName" @selection-change="albumSelectionChange">
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
        <el-table-column v-if="isAlbumColumnVisible('cover')" label="封面" width="96" align="center">
          <template slot-scope="{row}">
            <el-tooltip v-if="albumCoverSrc(row)" :content="albumCoverTip(row)" placement="right">
              <el-image
                class="jm-cover-image"
                :src="albumCoverSrc(row)"
                :preview-src-list="[albumCoverSrc(row)]"
                fit="cover"
                referrerpolicy="no-referrer">
                <div slot="placeholder" class="jm-cover-state"><i class="el-icon-loading"></i></div>
                <div slot="error" class="jm-cover-state"><i class="el-icon-picture-outline"></i></div>
              </el-image>
            </el-tooltip>
            <el-tooltip v-else content="本地与JM均无封面" placement="right">
              <div class="jm-cover-state"><i class="el-icon-picture-outline"></i></div>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column v-if="isAlbumColumnVisible('collected')" label="收藏" width="70" align="center">
          <template slot-scope="{row}">
            <el-tooltip :content="isAlbumCollected(row) ? '点击取消收藏' : '点击收藏'" placement="top">
              <el-button
                :type="isAlbumCollected(row) ? 'warning' : 'default'"
                size="mini"
                plain
                :icon="isAlbumCollected(row) ? 'el-icon-star-on' : 'el-icon-star-off'"
                :loading="isAlbumOperation(row, 'collect')"
                :disabled="isAlbumOtherOperation(row, 'collect')"
                @click="toggleAlbumCollected(row)"></el-button>
            </el-tooltip>
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
              <div :class="safeNumber(row.imageCount) === 0 ? 'danger-text' : ''">数据库：{{formatCount(row.imageCount)}}</div>
              <div :class="safeNumber(row.actualImageCount) === 0 ? 'danger-text' : ''">文件：{{formatCount(row.actualImageCount)}}</div>
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

      <el-empty v-else-if="albumData.length === 0" description="没有查询到JM主记录" :image-size="80"></el-empty>
      <div v-else v-loading="albumLoading" class="jm-waterfall">
        <div v-for="row in albumData" :key="`album-wf-${row.id}`" class="jm-waterfall-card"
             :class="{'jm-waterfall-card-collected': isAlbumCollected(row)}">
          <div class="jm-waterfall-cover">
            <el-image v-if="albumCoverSrc(row)" :src="albumCoverSrc(row)" :preview-src-list="[albumCoverSrc(row)]"
                      fit="cover" referrerpolicy="no-referrer">
              <div slot="placeholder" class="jm-waterfall-placeholder"><i class="el-icon-loading"></i></div>
              <div slot="error" class="jm-waterfall-placeholder"><i class="el-icon-picture-outline"></i></div>
            </el-image>
            <div v-else class="jm-waterfall-placeholder"><i class="el-icon-picture-outline"></i></div>
            <el-tag v-if="isAlbumCollected(row)" class="jm-waterfall-status" size="mini" type="warning">已收藏</el-tag>
            <i v-if="row.zipExists" class="jm-waterfall-badge jm-waterfall-badge-zip" title="已有ZIP">ZIP</i>
            <i v-if="row.pdfExists" class="jm-waterfall-badge jm-waterfall-badge-pdf" title="已有PDF">PDF</i>
          </div>
          <div class="jm-waterfall-body">
            <div class="jm-waterfall-name" :title="row.name">{{row.name}}</div>
            <div class="jm-waterfall-meta">
              <span class="jm-waterfall-id jm-waterfall-id-link" title="查看章节" @click="jumpToChapters(row)">JM{{row.id}}</span>
              <el-tag v-for="(item, index) in visibleItems(row.authorList, 2)" :key="`album-wf-author-${row.id}-${index}`" size="mini" type="info">{{item}}</el-tag>
              <span v-if="row.authorList.length > 2">+{{row.authorList.length - 2}}</span>
            </div>
            <div v-if="row.tagsList.length > 0" class="jm-waterfall-meta">
              <el-tag v-for="(item, index) in visibleItems(row.tagsList, 3)" :key="`album-wf-tag-${row.id}-${index}`" size="mini" type="success">{{item}}</el-tag>
              <span v-if="row.tagsList.length > 3">+{{row.tagsList.length - 3}}</span>
            </div>
            <div class="jm-waterfall-meta">
              <span :class="safeNumber(row.imageCount) === 0 ? 'danger-text' : ''">DB图片 {{formatCount(row.imageCount)}}</span>
              <span :class="safeNumber(row.actualImageCount) === 0 ? 'danger-text' : ''">文件 {{formatCount(row.actualImageCount)}}</span>
            </div>
            <div class="jm-waterfall-time">{{row.createTime}}</div>
            <div class="jm-waterfall-actions">
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
              <el-tooltip :content="isAlbumCollected(row) ? '取消收藏' : '收藏'" placement="top">
                <el-button :type="isAlbumCollected(row) ? 'warning' : 'default'" size="mini" plain
                           :icon="isAlbumCollected(row) ? 'el-icon-star-on' : 'el-icon-star-off'"
                           :loading="isAlbumOperation(row, 'collect')" :disabled="isAlbumOtherOperation(row, 'collect')"
                           @click="toggleAlbumCollected(row)"></el-button>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
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
          <el-table-column label="操作" width="100" align="center">
            <template slot-scope="{row}">
              <el-tooltip :disabled="!row.existsLocal" content="本地已有该记录，无需重复添加" placement="top">
                <span>
                  <el-button type="primary" size="mini" plain icon="el-icon-plus"
                             :loading="isOnlineAdding(row)" :disabled="isOnlineAddDisabled(row)"
                             @click="addOnlineAlbum(row)">添加</el-button>
                </span>
              </el-tooltip>
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
                  <el-button type="primary" size="mini" plain icon="el-icon-plus"
                             :loading="isOnlineAdding(row)" :disabled="isOnlineAddDisabled(row)"
                             @click="addOnlineAlbum(row)">{{row.existsLocal ? '已入库' : '添加'}}</el-button>
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
  </div>
</template>

<script>
import JmPreviewDrawer from "./jm-preview-drawer.vue";
import JmTaskPanel from "./jm-task-panel.vue";
import JmTagSelect from "./jm-tag-select.vue";
import JmAuthorSelect from "./jm-author-select.vue";
import numberInput from "@/components/input/numberInput.vue";
import {
  deleteAlbums,
  deleteAllFile,
  deleteChapterImages,
  downloadAlbum,
  collectAlbums,
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

export default {
  name: 'JmcomicManage',
  components: { JmPreviewDrawer, JmTaskPanel, JmTagSelect, JmAuthorSelect, numberInput },
  data() {
    return {
      activeTab: 'album',
      albumLoading: false,
      chapterLoading: false,
      albumRequestLoading: false,
      chapterRequestLoading: false,
      albumDeleteLoading: false,
      albumCollectLoading: null,
      chapterDeleteLoading: false,
      allFileDeleteLoading: false,
      albumDeleteDialogVisible: false,
      chapterDeleteDialogVisible: false,
      deleteAllFileDialogVisible: false,
      previewDrawerVisible: false,
      previewAlbum: null,
      // 内存中的JM任务面板(不持久化，后端重启即清空)
      taskPanelVisible: false,
      taskSnapshot: this.defTaskSnapshot(),
      taskPollTimer: null,
      albumQuery: { id: '', name: '', author: '', tags: [], collected: '' },
      // JM主记录展示方式：list=表格(默认)，waterfall=瀑布流卡片
      albumViewMode: getStore({ name: ALBUM_VIEW_MODE_KEY }) || 'list',
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
      // 搜索结果展示方式：list=表格(默认)，waterfall=瀑布流卡片
      onlineViewMode: getStore({ name: ONLINE_VIEW_MODE_KEY }) || 'list',
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
      // 在线搜索里添加过记录后，JM主记录列表需要重新查询
      albumListDirty: false,
      albumData: [],
      chapterData: [],
      albumSelection: [],
      chapterSelection: [],
      albumOperationLoading: {},
      albumVisibleColumns: ['selection', 'action', 'index', 'id', 'cover', 'collected', 'name', 'author', 'tags', 'zip', 'pdf', 'createTime','imageStats','interactionStats'],
      albumColumnOptions: [
        { key: 'selection', label: 'selection' },
        { key: 'action', label: '操作' },
        { key: 'index', label: '序号' },
        { key: 'id', label: 'JM ID' },
        { key: 'cover', label: '封面' },
        { key: 'collected', label: '收藏' },
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
      albumDeleteOptions: this.defAlbumDeleteOptions(),
      chapterDeleteOptions: this.defChapterDeleteOptions(),
      deleteAllFileOptions: this.defDeleteAllFileOptions(),
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
      // 切到瀑布流后表格会卸载，选中态留着会让批量按钮作用在看不见的行上
      this.albumSelection = []
    }
  },
  mounted() {
    this.searchAlbumsFirst()
    this.startTaskPolling()
  },
  beforeDestroy() {
    this.stopTaskPolling()
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
    isAlbumColumnVisible(key) {
      return this.albumVisibleColumns.includes(key)
    },
    /**
     * 封面展示地址：优先本地服务器封面(下载漫画时会落盘)，本地没有时回退JM远程封面
     */
    albumCoverSrc(row) {
      return row ? (row.serverCoverUrl || row.coverUrl || '') : ''
    },
    albumCoverTip(row) {
      return row && row.serverCoverUrl ? `本地封面：${row.serverCoverUrl}` : `JM封面：${row.coverUrl || ''}`
    },
    isAlbumCollected(row) {
      return !!(row && row.collected)
    },
    /**
     * 已收藏的行加个底色，方便在长列表里一眼区分
     */
    albumRowClassName({row}) {
      return this.isAlbumCollected(row) ? 'jm-album-collected-row' : ''
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
     * 任务面板关闭时低频轮询，只为刷新徽标和行内状态；
     * 面板打开时由面板自己按服务端建议频率轮询，并通过 snapshot 事件回传，避免重复请求
     */
    startTaskPolling() {
      this.stopTaskPolling()
      this.pollJmTasks()
      this.taskPollTimer = setInterval(() => {
        if (this.taskPanelVisible || document.hidden) {
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
     * 任务面板打开时由面板轮询并通过事件回传，关闭时由本页低频轮询，两条路径都要走这里
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
          this.refreshTagOptions()
          this.refreshAuthorOptions()
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
        // 立即刷新任务快照，让徽标和行内按钮状态马上跟上
        this.pollJmTasks()
      })
    },
    downloadAlbumData(row) {
      this.executeAlbumOperation(row, 'download', downloadAlbum)
    },
    /**
     * 单条收藏/取消收藏，只更新该行，避免整页刷新
     * 当前带有收藏筛选条件时，该行可能已经不满足条件，重新查询一次
     */
    toggleAlbumCollected(row) {
      if (this.isAlbumOperating(row)) {
        return
      }
      const collected = !this.isAlbumCollected(row)
      this.$set(this.albumOperationLoading, row.id, 'collect')
      collectAlbums({ ids: [row.id], collected }).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$set(row, 'collected', collected)
        this.$message.success(message || (collected ? '收藏完成' : '取消收藏完成'))
        this.searchAlbumsIfCollectedFiltered()
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.$delete(this.albumOperationLoading, row.id)
      })
    },
    /**
     * 批量收藏/取消收藏选中的记录
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
        this.selectAlbums()
      }).catch(error => {
        this.handleRequestError(error)
      }).finally(() => {
        this.albumCollectLoading = null
      })
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
   * 任务队列徽标：避免角标盖住右侧按钮
   */
  .jm-task-badge {
    line-height: 1;
    margin-right: 8px;
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

  /**
   * 封面缩略图：本地封面优先，本地没有时回退JM远程封面(见 albumCoverSrc)
   */
  .jm-cover-image {
    cursor: pointer;
    display: block;
    height: 76px;
    margin: 0 auto;
    width: 58px;

    ::v-deep .el-image__inner {
      border: 1px solid #ebeef5;
      border-radius: 4px;
      height: 100%;
      width: 100%;
    }
  }

  .jm-online-summary {
    color: #606266;
    font-size: 13px;
    line-height: 28px;
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

  /**
   * 瀑布流卡片：column-width 让列宽自适应，卡片高度由封面原始比例决定
   */
  .jm-waterfall {
    column-gap: 12px;
    column-width: 176px;
    padding: 4px 0;
  }

  .jm-waterfall-card {
    background-color: #fff;
    border: 1px solid #ebeef5;
    border-radius: 6px;
    break-inside: avoid;
    display: inline-block;
    margin: 0 0 12px;
    overflow: hidden;
    transition: box-shadow .2s;
    width: 100%;

    &:hover {
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, .1);
    }
  }

  .jm-waterfall-cover {
    background-color: #f5f7fa;
    position: relative;

    ::v-deep .el-image {
      display: block;
      width: 100%;
    }

    ::v-deep .el-image__inner {
      display: block;
      height: auto;
      width: 100%;
    }
  }

  .jm-waterfall-placeholder {
    align-items: center;
    color: #c0c4cc;
    display: flex;
    font-size: 18px;
    height: 180px;
    justify-content: center;
  }

  .jm-waterfall-status {
    position: absolute;
    right: 6px;
    top: 6px;
  }

  /**
   * 卡片左下角的ZIP/PDF角标
   */
  .jm-waterfall-badge {
    border-radius: 3px;
    bottom: 6px;
    color: #fff;
    font-size: 10px;
    font-style: normal;
    line-height: 16px;
    padding: 0 4px;
    position: absolute;

    &.jm-waterfall-badge-zip {
      background-color: rgba(103, 194, 58, .9);
      left: 6px;
    }

    &.jm-waterfall-badge-pdf {
      background-color: rgba(230, 162, 60, .9);
      left: 46px;
    }
  }

  /**
   * 已收藏的卡片描边提示，与表格里的行底色对应
   */
  .jm-waterfall-card-collected {
    border-color: #f0c78a;
    box-shadow: inset 0 0 0 1px #fdf0cc;
  }

  .jm-waterfall-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    margin-top: 8px;

    .el-button {
      margin: 0;
      padding: 5px 7px;
    }
  }

  .jm-waterfall-body {
    padding: 8px;
  }

  .jm-waterfall-name {
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    color: #303133;
    display: -webkit-box;
    font-size: 13px;
    line-height: 18px;
    overflow: hidden;
    word-break: break-all;
  }

  .jm-waterfall-meta {
    align-items: center;
    color: #909399;
    display: flex;
    font-size: 12px;
    gap: 6px;
    line-height: 18px;
    margin-top: 4px;
    min-width: 0;
  }

  .jm-waterfall-id {
    color: #409eff;
    flex-shrink: 0;
  }

  .jm-waterfall-id-link {
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }

  .jm-waterfall-author {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .jm-waterfall-footer {
    align-items: center;
    display: flex;
    justify-content: space-between;
    margin-top: 8px;
  }

  .jm-waterfall-time {
    color: #c0c4cc;
    font-size: 12px;
  }

  .jm-cover-state {
    align-items: center;
    background-color: #f5f7fa;
    border: 1px dashed #dcdfe6;
    border-radius: 4px;
    color: #c0c4cc;
    display: flex;
    font-size: 16px;
    height: 76px;
    justify-content: center;
    margin: 0 auto;
    width: 58px;
  }

  /**
   * 已收藏的行加底色，选择器带上ID/类名是为了盖过表格的斑马纹与hover样式
   */
  .el-table__body tr.jm-album-collected-row > td {
    background-color: #fff8e1;
  }

  .el-table__body tr.jm-album-collected-row:hover > td {
    background-color: #fdf0cc;
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

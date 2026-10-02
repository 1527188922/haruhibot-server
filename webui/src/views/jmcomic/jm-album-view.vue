<template>
  <div class="jm-album-view">
    <!--
      JM漫画列表 / 瀑布流。
      主记录 tab（jmcomic/index.vue）与收藏夹 tab（jm-favorite-panel.vue）共用这一份实现：
      列集合、单元格内容、瀑布流卡片只在这里维护一份，两边展示的信息不会再出现差异。

      行操作按钮由父组件通过 actions 传入（key/图标/文案/颜色），点击后 emit('action', key, row)，
      因此两个 tab 可以有不同的按钮，但渲染与布局完全一致。

      勾选批量操作在列表与瀑布流两种展示方式下都可用：勾选状态保存在本组件（selectedRows），
      表格里的复选框与瀑布流卡片左上角的复选框共用同一份状态，切换展示方式不会丢选中，
      变化时 emit('selection-change', rows) 给父组件做批量操作。
    -->
    <el-table v-if="viewMode === 'list'" tooltip-effect="light" :data="rows" v-loading="loading" border stripe
              max-height="800" size="small" ref="albumTable" highlight-current-row
              :row-class-name="rowClassName">
      <el-table-column v-if="isVisible('selection')" width="50" align="center">
        <template slot="header">
          <el-checkbox :value="allSelected" :indeterminate="selectionIndeterminate"
                       @change="toggleSelectAll"></el-checkbox>
        </template>
        <template slot-scope="{row}">
          <el-checkbox :value="isRowSelected(row)" @change="toggleRowSelected(row)"></el-checkbox>
        </template>
      </el-table-column>
      <el-table-column v-if="isVisible('action')" :fixed="!isMobileView" label="操作" :width="actionColumnWidth" align="center">
        <template slot-scope="{row}">
          <div class="jm-action-grid">
            <el-tooltip v-for="action in actions" :key="action.key" :content="action.tooltip" placement="top">
              <el-button :type="action.type" size="mini" plain :icon="action.icon"
                         :loading="isActionLoading(row, action.key)"
                         :disabled="isActionDisabled(row, action.key)"
                         @click="$emit('action', action.key, row)"></el-button>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="isVisible('index')" :fixed="!isMobileView" label="序号" width="50" align="center">
        <template slot-scope="scope">{{scope.$index + 1}}</template>
      </el-table-column>
      <el-table-column v-if="isVisible('id')" :fixed="!isMobileView" label="JM ID" prop="id" min-width="110" align="center">
        <template slot-scope="{row}">
          <span class="primary-text" style="cursor:pointer;" @click="$emit('chapters', row)">{{row.id}}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="isVisible('cover')" label="封面" width="96" align="center">
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
      <el-table-column v-if="isVisible('collected')" label="收藏" width="70" align="center">
        <template slot-scope="{row}">
          <el-tooltip :content="isCollected(row) ? '点击取消收藏' : '点击收藏'" placement="top">
            <el-button
              :type="isCollected(row) ? 'warning' : 'default'"
              size="mini"
              plain
              :icon="isCollected(row) ? 'el-icon-star-on' : 'el-icon-star-off'"
              :loading="isActionLoading(row, 'collect')"
              :disabled="isActionDisabled(row, 'collect')"
              @click="$emit('collect-toggle', row)"></el-button>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column v-if="isVisible('favorites')" label="所属收藏夹" min-width="180">
        <template slot-scope="{row}">
          <div v-if="favoriteNamesOf(row).length > 0" class="jm-tag-summary">
            <el-tag v-for="(name, index) in visibleItems(favoriteNamesOf(row), 3)" :key="`fav-names-${row.id}-${index}`" size="mini" type="warning">{{name}}</el-tag>
            <el-popover v-if="favoriteNamesOf(row).length > 3" placement="bottom-start" trigger="click" width="320" popper-class="jm-related-popover">
              <div class="jm-popover-title">所属收藏夹</div>
              <div class="jm-popover-tags">
                <el-tag v-for="(name, index) in favoriteNamesOf(row)" :key="`all-fav-names-${row.id}-${index}`" size="mini" type="warning">{{name}}</el-tag>
              </div>
              <el-button slot="reference" type="text" size="mini">+{{hiddenCount(favoriteNamesOf(row), 3)}}</el-button>
            </el-popover>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column v-if="isVisible('name')" label="名称" prop="name" min-width="240" show-overflow-tooltip></el-table-column>
      <el-table-column v-if="isVisible('author')" label="作者" prop="author" min-width="180">
        <template slot-scope="{row}">
          <div class="jm-tag-list">
            <el-tag v-for="(item, index) in row.authorList" :key="`author-${row.id}-${index}`" size="mini" type="info">{{item}}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="isVisible('tags')" label="标签" prop="tags" min-width="240">
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
      <el-table-column v-if="isVisible('interactionStats')" label="互动统计" min-width="130" align="center">
        <template slot-scope="{row}">
          <div class="jm-stat-cell">
            <div>观看：{{formatCount(row.totalViews)}}</div>
            <div>Like：{{formatCount(row.likes)}}</div>
            <div>评论：{{formatCount(row.commentTotal)}}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="isVisible('imageStats')" label="图片统计" min-width="130" align="center">
        <template slot-scope="{row}">
          <div class="jm-stat-cell">
            <div :class="safeNumber(row.imageCount) === 0 ? 'danger-text' : ''">数据库：{{formatCount(row.imageCount)}}</div>
            <div :class="safeNumber(row.actualImageCount) === 0 ? 'danger-text' : ''">文件：{{formatCount(row.actualImageCount)}}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="isVisible('zip')" label="ZIP" prop="zipExists" width="80" align="center">
        <template slot-scope="{row}">
          <el-tooltip v-if="row.zipExists" content="点击下载ZIP文件" placement="top">
            <a :href="$localUrl(row.serverZipUrl)" target="_blank" download>
              <el-tag size="mini" type="success">{{formatBool(row.zipExists)}}</el-tag>
            </a>
          </el-tooltip>
          <el-tag v-else size="mini" type="info">{{formatBool(row.zipExists)}}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="isVisible('pdf')" label="PDF" prop="pdfExists" width="80" align="center">
        <template slot-scope="{row}">
          <el-tooltip v-if="row.pdfExists" content="点击下载PDF文件" placement="top">
            <a :href="$localUrl(row.serverPdfUrl)" target="_blank" download>
              <el-tag size="mini" type="success">{{formatBool(row.pdfExists)}}</el-tag>
            </a>
          </el-tooltip>
          <el-tag v-else size="mini" type="info">{{formatBool(row.pdfExists)}}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="isVisible('createTime')" label="下载时间" prop="createTime" min-width="150" align="center"></el-table-column>
      <el-table-column v-if="isVisible('favoriteAddTime')" label="加入时间" prop="favoriteAddTime" min-width="150" align="center" show-overflow-tooltip></el-table-column>
      <el-table-column v-if="isVisible('chapterCount')" label="章节数" min-width="80" align="center">
        <template slot-scope="{row}">{{(row.chapterList || []).length}}</template>
      </el-table-column>
      <el-table-column v-if="isVisible('chapterList')" label="列表章节" min-width="240" show-overflow-tooltip>
        <template slot-scope="{row}">{{formatChapterList(row.chapterList)}}</template>
      </el-table-column>
      <el-table-column v-if="isVisible('works')" label="作品" prop="works" min-width="160">
        <template slot-scope="{row}">
          <div class="jm-tag-list">
            <el-tag v-for="(item, index) in row.worksList" :key="`works-${row.id}-${index}`" size="mini" type="warning">{{item}}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="isVisible('actors')" label="角色" prop="actors" min-width="160">
        <template slot-scope="{row}">
          <div class="jm-tag-list">
            <el-tag v-for="(item, index) in row.actorsList" :key="`actors-${row.id}-${index}`" size="mini">{{item}}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="isVisible('description')" label="描述" prop="description" min-width="280" show-overflow-tooltip></el-table-column>
      <el-table-column v-if="isVisible('albumFolderName')" label="文件夹" prop="albumFolderName" min-width="220" show-overflow-tooltip></el-table-column>
      <el-table-column v-if="isVisible('relatedList')" label="相关列表" prop="relatedList" min-width="300">
        <template slot-scope="{row}">
          <div v-if="row.relatedItems.length > 0" class="jm-related-cell">
            <div v-for="(item, index) in visibleItems(row.relatedItems, 2)" :key="`related-${row.id}-${index}`" class="jm-related-line">
              <el-button type="text" size="mini" @click="$emit('jump-album', item.id)">JM{{item.id}}</el-button>
              <span class="jm-related-name">{{item.name}}</span>
            </div>
            <el-popover placement="bottom-start" trigger="click" width="520" popper-class="jm-related-popover">
              <div class="jm-popover-title">相关漫画</div>
              <div class="jm-related-popover-list">
                <div v-for="(item, index) in row.relatedItems" :key="`all-related-${row.id}-${index}`" class="jm-related-popover-item">
                  <div class="jm-related-popover-main">
                    <el-button type="text" size="mini" @click="$emit('jump-album', item.id)">JM{{item.id}}</el-button>
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
      <el-table-column v-if="isVisible('addTime')" label="JM发布时间" prop="addTime" min-width="150" align="center">
        <template slot-scope="{row}">{{row.formattedAddTime}}</template>
      </el-table-column>
      <el-table-column v-if="isVisible('raw')" label="raw" prop="raw" min-width="100" show-overflow-tooltip></el-table-column>
    </el-table>

    <el-empty v-else-if="rows.length === 0" :description="emptyText" :image-size="80"></el-empty>
    <div v-else v-loading="loading" class="jm-waterfall">
      <div v-for="row in rows" :key="`jm-wf-${row.id}`" class="jm-waterfall-card"
           :class="{'jm-waterfall-card-collected': isCollected(row)}">
        <div class="jm-waterfall-cover">
          <!-- 瀑布流下也能勾选做批量操作，与列表视图共用同一份选中态 -->
          <el-checkbox v-if="isVisible('selection')" class="jm-waterfall-select"
                       :value="isRowSelected(row)" @change="toggleRowSelected(row)"></el-checkbox>
          <el-image v-if="albumCoverSrc(row)" :src="albumCoverSrc(row)" :preview-src-list="[albumCoverSrc(row)]"
                    fit="cover" referrerpolicy="no-referrer">
            <div slot="placeholder" class="jm-waterfall-placeholder"><i class="el-icon-loading"></i></div>
            <div slot="error" class="jm-waterfall-placeholder"><i class="el-icon-picture-outline"></i></div>
          </el-image>
          <div v-else class="jm-waterfall-placeholder"><i class="el-icon-picture-outline"></i></div>
          <el-tag v-if="isCollected(row)" class="jm-waterfall-status" size="mini" type="warning">已收藏</el-tag>
          <i v-if="row.zipExists" class="jm-waterfall-badge jm-waterfall-badge-zip" title="已有ZIP">ZIP</i>
          <i v-if="row.pdfExists" class="jm-waterfall-badge jm-waterfall-badge-pdf" title="已有PDF">PDF</i>
        </div>
        <div class="jm-waterfall-body">
          <div class="jm-waterfall-name" :title="row.name">{{row.name}}</div>
          <div class="jm-waterfall-meta">
            <span class="jm-waterfall-id jm-waterfall-id-link" title="查看章节" @click="$emit('chapters', row)">JM{{row.id}}</span>
            <el-tag v-for="(item, index) in visibleItems(row.authorList, 2)" :key="`jm-wf-author-${row.id}-${index}`" size="mini" type="info">{{item}}</el-tag>
            <span v-if="row.authorList.length > 2">+{{row.authorList.length - 2}}</span>
          </div>
          <div v-if="row.tagsList.length > 0" class="jm-waterfall-meta">
            <el-tag v-for="(item, index) in visibleItems(row.tagsList, 3)" :key="`jm-wf-tag-${row.id}-${index}`" size="mini" type="success">{{item}}</el-tag>
            <span v-if="row.tagsList.length > 3">+{{row.tagsList.length - 3}}</span>
          </div>
          <div class="jm-waterfall-meta">
            <span :class="safeNumber(row.imageCount) === 0 ? 'danger-text' : ''">DB图片 {{formatCount(row.imageCount)}}</span>
            <span :class="safeNumber(row.actualImageCount) === 0 ? 'danger-text' : ''">文件 {{formatCount(row.actualImageCount)}}</span>
          </div>
          <div v-if="favoriteNameMap && favoriteNamesOf(row).length > 0" class="jm-waterfall-meta">
            <el-tag v-for="(name, index) in visibleItems(favoriteNamesOf(row), 2)" :key="`jm-wf-fav-${row.id}-${index}`" size="mini" type="warning">{{name}}</el-tag>
            <span v-if="favoriteNamesOf(row).length > 2">+{{favoriteNamesOf(row).length - 2}}</span>
          </div>
          <div class="jm-waterfall-time">{{row.createTime}}</div>
          <div v-if="row.favoriteAddTime" class="jm-waterfall-time">加入于 {{row.favoriteAddTime}}</div>
          <div class="jm-waterfall-actions">
            <el-tooltip v-for="action in actions" :key="action.key" :content="action.tooltip" placement="top">
              <el-button :type="action.type" size="mini" plain :icon="action.icon"
                         :loading="isActionLoading(row, action.key)"
                         :disabled="isActionDisabled(row, action.key)"
                         @click="$emit('action', action.key, row)"></el-button>
            </el-tooltip>
            <el-tooltip v-if="isVisible('collected')" :content="isCollected(row) ? '取消收藏' : '收藏'" placement="top">
              <el-button :type="isCollected(row) ? 'warning' : 'default'" size="mini" plain
                         :icon="isCollected(row) ? 'el-icon-star-on' : 'el-icon-star-off'"
                         :loading="isActionLoading(row, 'collect')"
                         :disabled="isActionDisabled(row, 'collect')"
                         @click="$emit('collect-toggle', row)"></el-button>
            </el-tooltip>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
/**
 * JM漫画列表/瀑布流展示组件。
 *
 * 只负责"展示 + 把行操作抛给父组件"，不持有业务状态：
 * - 数据、loading、展示方式（列表/瀑布流）、显示哪些列由父组件传入
 * - 行操作按钮由 actions 声明，点击后 emit('action', key, row)
 * - 收藏列的星标 emit('collect-toggle', row)
 *
 * 记录在这里统一做一次字段展开（author/tags/works/actors/relatedList 都是JSON字符串），
 * 两个 tab 不再各自解析，展示结果必然一致。
 */
export default {
  name: 'JmAlbumView',
  props: {
    albums: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    // list=表格，waterfall=瀑布流卡片
    viewMode: {
      type: String,
      default: 'list'
    },
    // 显示哪些列，取值见 jm-album-columns.js
    visibleColumns: {
      type: Array,
      default: () => []
    },
    /**
     * 行操作按钮：{key, icon, type, tooltip}
     * 点击后 emit('action', key, row)
     */
    actions: {
      type: Array,
      default: () => []
    },
    /**
     * 按钮 loading/disabled 判定，签名 (row, actionKey) => boolean
     * 不传时都不 loading/disabled
     */
    actionLoading: {
      type: Function,
      default: null
    },
    actionDisabled: {
      type: Function,
      default: null
    },
    /**
     * 收藏夹id -> 名称。传了才展示"所属收藏夹"列与瀑布流里的收藏夹标签
     */
    favoriteNameMap: {
      type: Object,
      default: null
    },
    emptyText: {
      type: String,
      default: '没有查询到数据'
    }
  },
  data() {
    return {
      // 当前勾选的行：列表与瀑布流共用，切换展示方式不会丢
      selectedRows: []
    };
  },
  computed: {
    rows() {
      // 每行展开一次派生字段，模板里直接用 authorList/tagsList/... ，避免在模板里解析JSON
      return (this.albums || []).map(row => this.normalizeAlbum(row));
    },
    /**
     * 操作列宽度：2列固定28px的小按钮，行数变多时同步加宽
     */
    actionColumnWidth() {
      const lines = Math.ceil((this.actions.length || 1) / 2);
      return Math.max(96, lines * 32 + 12);
    },
    allSelected() {
      return this.rows.length > 0 && this.selectedRows.length === this.rows.length;
    },
    selectionIndeterminate() {
      return this.selectedRows.length > 0 && !this.allSelected;
    }
  },
  watch: {
    /**
     * 数据变化（翻页/重新查询）后，把已经不在当前数据里的行从勾选态剔除，
     * 避免批量操作作用在看不见的行上
     */
    rows() {
      const exists = new Set(this.rows.map(row => row.id));
      if (this.selectedRows.every(row => exists.has(row.id))) {
        return;
      }
      this.setSelectedRows(this.selectedRows);
    }
  },
  methods: {
    isVisible(key) {
      return (this.visibleColumns || []).includes(key);
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
      };
    },
    parseJsonList(value) {
      if (!value) {
        return [];
      }
      if (Array.isArray(value)) {
        return value.map(e => `${e}`).filter(e => e);
      }
      try {
        const parsed = JSON.parse(value);
        if (Array.isArray(parsed)) {
          return parsed.map(e => `${e}`).filter(e => e);
        }
      } catch (e) {
        return [`${value}`];
      }
      return [`${value}`];
    },
    parseJsonArray(value) {
      if (!value) {
        return [];
      }
      if (Array.isArray(value)) {
        return value;
      }
      try {
        const parsed = JSON.parse(value);
        return Array.isArray(parsed) ? parsed : [];
      } catch (e) {
        return [];
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
        }));
    },
    formatTimestamp(value) {
      if (!value) {
        return '';
      }
      const text = `${value}`.trim();
      if (!/^\d{10}$|^\d{13}$/.test(text)) {
        return text;
      }
      const timestamp = text.length === 10 ? Number(text) * 1000 : Number(text);
      const date = new Date(timestamp);
      if (Number.isNaN(date.getTime())) {
        return text;
      }
      const pad = val => `${val}`.padStart(2, '0');
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
    },
    formatBool(value) {
      return value ? '是' : '否';
    },
    formatCount(value) {
      return value === null || value === undefined || value === '' ? '-' : value;
    },
    safeNumber(v) {
      return !v || !(typeof v === 'number') ? 0 : v;
    },
    formatChapterList(chapterList) {
      if (!chapterList || chapterList.length === 0) {
        return '';
      }
      return chapterList.map(e => `${e.title || e.name || ''}(${e.chapterId})`).join('，');
    },
    visibleItems(list, count) {
      return (list || []).slice(0, count);
    },
    hiddenCount(list, count) {
      return Math.max((list || []).length - count, 0);
    },
    albumCoverSrc(row) {
      return row ? (this.$localUrl(row.serverCoverUrl) || row.coverUrl || '') : '';
    },
    albumCoverTip(row) {
      return row && row.serverCoverUrl ? `本地封面：${this.$localUrl(row.serverCoverUrl)}` : `JM封面：${row.coverUrl || ''}`;
    },
    isCollected(row) {
      return !!(row && row.collected);
    },
    rowClassName({row}) {
      return this.isCollected(row) ? 'jm-album-collected-row' : '';
    },
    /**
     * 该漫画所属的收藏夹名称；父组件没传映射时返回空数组
     */
    favoriteNamesOf(row) {
      if (!this.favoriteNameMap || !row || !row.favoriteIds) {
        return [];
      }
      return row.favoriteIds
        .map(id => this.favoriteNameMap[`${id}`])
        .filter(name => !!name);
    },
    isActionLoading(row, key) {
      return this.actionLoading ? !!this.actionLoading(row, key) : false;
    },
    isActionDisabled(row, key) {
      return this.actionDisabled ? !!this.actionDisabled(row, key) : false;
    },
    /* ==================== 勾选批量操作 ==================== */
    isRowSelected(row) {
      return !!row && this.selectedRows.some(item => item.id === row.id);
    },
    toggleRowSelected(row) {
      if (!row) {
        return;
      }
      this.setSelectedRows(this.isRowSelected(row)
        ? this.selectedRows.filter(item => item.id !== row.id)
        : [...this.selectedRows, row]);
    },
    toggleSelectAll(checked) {
      this.setSelectedRows(checked ? [...this.rows] : []);
    },
    /**
     * 统一出口：按当前 rows 的顺序与对象重新取一遍，保证父组件拿到的是最新数据，
     * 不会因为翻页/刷新拿到上一批的旧对象
     */
    setSelectedRows(rows) {
      const ids = new Set((rows || []).map(item => item.id));
      this.selectedRows = this.rows.filter(row => ids.has(row.id));
      this.$emit('selection-change', this.selectedRows);
    },
    /* ==================== 供父组件调用 ==================== */
    /**
     * 列显隐变化后重算表格布局
     */
    doLayout() {
      this.$nextTick(() => {
        if (this.$refs.albumTable) {
          this.$refs.albumTable.doLayout();
        }
      });
    },
    clearSelection() {
      this.setSelectedRows([]);
    }
  }
};
</script>

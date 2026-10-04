<template>
  <div class="config-manage">
    <!-- 移动端：文件列表以抽屉形式覆盖在配置项之上 -->
    <div v-if="mobileAsideOpen" class="config-manage__mask" @click="closeMobileAside"></div>

    <!-- 左侧：配置文件分组 -->
    <div class="config-manage__aside" :class="{'config-manage__aside--open': mobileAsideOpen}">
      <div class="aside-title">
        <span>配置文件</span>
        <span class="aside-title__ops">
          <el-tooltip content="重新加载全部配置文件" placement="top">
            <i class="el-icon-refresh" :class="{'is-loading': allLoading}" @click="reloadAll"></i>
          </el-tooltip>
          <i v-if="isMobileView" class="el-icon-close" @click="closeMobileAside"></i>
        </span>
      </div>
      <ul class="file-list">
        <li v-for="file in files"
            :key="file.fileName"
            :class="{active: current && current.fileName === file.fileName, missing: !file.exists}"
            @click="selectFile(file)">
          <div class="file-list__main">
            <div class="file-list__name">{{ file.displayName }}</div>
            <div class="file-list__path">{{ file.fileName }}</div>
          </div>
          <div class="file-list__badges">
            <el-tag size="mini" type="info" effect="plain">{{ file.count }}</el-tag>
            <el-tag v-if="file.hotCount" size="mini" type="success" effect="plain">{{ file.hotCount }}热</el-tag>
          </div>
        </li>
      </ul>
      <div class="aside-tip">
        配置存放于 <b>程序目录/config/</b><br>
        直接编辑文件也会被自动感知
      </div>
    </div>

    <!-- 右侧：配置项 -->
    <div class="config-manage__main" v-if="current">
      <basic-container>
        <div class="main-toolbar">
          <div class="main-toolbar__title">
            <!-- 移动端文件列表收进抽屉，这里给出入口 -->
            <el-button v-if="isMobileView" class="main-toolbar__file-btn" size="small" plain
                       icon="el-icon-folder-opened" @click="toggleMobileAside">配置文件</el-button>
            <span class="name">{{ current.displayName }}</span>
            <span class="file">{{ current.fileName }}</span>
            <el-tag v-if="!current.exists" size="mini" type="warning" effect="plain">文件不存在（保存后自动创建）</el-tag>
          </div>
          <div class="main-toolbar__buttons">
            <el-button size="small" plain icon="el-icon-refresh" :loading="fileRefreshLoading"
                       @click="reloadFile">重新加载此文件</el-button>
            <el-button size="small" plain icon="el-icon-document" @click="viewFile">查看文件</el-button>
            <el-button size="small" type="primary" icon="el-icon-check" :loading="saveLoading"
                       :disabled="!dirtyCount" @click="saveAll">保存修改
              <template v-if="dirtyCount">（{{ dirtyCount }}）</template>
            </el-button>
          </div>
        </div>

        <div class="file-remark">{{ current.remark }}</div>

        <!-- 移动端：配置项用卡片展示，控件铺满卡片宽度，长说明折叠进"更多详情" -->
        <mobile-record-list v-if="isMobileView" :rows="current.items" :loading="tableLoading"
                            row-key="key" title-field="displayName" fallback-field="key"
                            :row-class="dirtyRowClass"
                            :fields="[{label:'说明', prop:'remark'}]"
                            :detail-fields="[{label:'来源', format:itemSourceText},{label:'默认值', format:itemDefaultText}]">
          <template #header="{row}">
            <el-tag v-if="row.dirty" size="mini" type="danger" effect="plain">未保存</el-tag>
            <el-tag size="mini" :type="row.hot ? 'success' : 'warning'" effect="plain">
              {{ row.hot ? '即时生效' : '需重启' }}
            </el-tag>
          </template>
          <template #summary="{row}">
            <div class="config-item-key">{{ row.key }}</div>
            <config-value-editor :row="row" :value="row.editValue"
                                 @change="onValueChange(row, $event)"></config-value-editor>
          </template>
          <template #actions="{row}">
            <el-button size="small" type="primary" plain :disabled="!row.dirty" @click="saveOne(row)">保存</el-button>
            <el-button v-if="row.hot" size="small" plain @click="refreshOne(row)">刷新</el-button>
            <el-button size="small" plain class="danger-text" @click="resetOne(row)">重置</el-button>
          </template>
        </mobile-record-list>

        <el-table v-show="!isMobileView" :data="current.items" v-loading="tableLoading" size="small" border stripe
                  :row-class-name="rowClass" max-height="720">
          <el-table-column label="配置项" min-width="220">
            <template slot-scope="{row}">
              <div class="item-name">{{ row.displayName }}</div>
              <div class="item-key">{{ row.key }}</div>
            </template>
          </el-table-column>

          <el-table-column label="值" min-width="300">
            <template slot-scope="{row}">
              <config-value-editor :row="row" :value="row.editValue"
                                   @change="onValueChange(row, $event)"></config-value-editor>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="180" align="center">
            <template slot-scope="{row}">
              <el-tag v-if="row.hot" size="mini" type="success" effect="plain">即时生效</el-tag>
              <el-tag v-else size="mini" type="warning" effect="plain">需重启</el-tag>
              <div class="item-source">
                {{ row.source === 'FILE' ? '来自' + current.fileName : '默认值' }}
              </div>
            </template>
          </el-table-column>

          <el-table-column label="说明" min-width="260">
            <template slot-scope="{row}">
              <div class="item-remark">{{ row.remark }}</div>
              <div v-if="row.configured && row.type !== 'SECRET'" class="item-default">
                默认值：{{ row.defaultValue === '' ? '（空）' : row.defaultValue }}
              </div>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="180" align="center" :fixed="!isMobileView ? 'right' : false">
            <template slot-scope="{row}">
              <el-button type="text" size="mini" :disabled="!row.dirty" @click="saveOne(row)">保存</el-button>
              <el-button v-if="row.hot" type="text" size="mini" @click="refreshOne(row)">刷新</el-button>
              <el-button type="text" size="mini" class="danger-text" @click="resetOne(row)">重置</el-button>
            </template>
          </el-table-column>
        </el-table>
      </basic-container>
    </div>

    <!-- 查看文件原始内容 -->
    <el-dialog :visible.sync="fileDialog.visible" :title="fileDialog.title" width="760px"
               custom-class="config-file-dialog" :fullscreen="isMobileView"
               :dialog-drag-enabled="!isMobileView">
      <pre class="file-preview">{{ fileDialog.content }}</pre>
      <span slot="footer">
        <el-button @click="fileDialog.visible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  list as listApi,
  fileContent as fileContentApi,
  save as saveApi,
  batchSave as batchSaveApi,
  reset as resetApi,
  refresh as refreshApi,
  refreshFile as refreshFileApi,
  refreshAll as refreshAllApi
} from '@/api/config';
import ConfigValueEditor from './config-value-editor';
import {decorateItem} from '@/util/config-item';

export default {
  name: 'ConfigManage',
  components: {ConfigValueEditor},
  data() {
    return {
      files: [],
      current: null,
      tableLoading: false,
      saveLoading: false,
      fileRefreshLoading: false,
      allLoading: false,
      // 移动端文件列表抽屉；桌面端恒为 false（文件列表常驻左侧）
      mobileAsideOpen: false,
      fileDialog: {
        visible: false,
        title: '',
        content: ''
      }
    }
  },
  computed: {
    dirtyCount() {
      if (!this.current) {
        return 0
      }
      return this.current.items.filter(e => e.dirty).length
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    load(keepFile) {
      this.tableLoading = true
      listApi().then(({data: {code, message, data}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.files = data || []
        const target = keepFile
          ? this.files.find(e => e.fileName === keepFile)
          : (this.current && this.files.find(e => e.fileName === this.current.fileName)) || this.files[0]
        this.selectFile(target, true)
      }).catch(e => {
        this.$message.error(e.message)
      }).finally(() => {
        this.tableLoading = false
      })
    },

    // ==================== 文件切换 ====================
    /** 移动端文件列表抽屉开关（桌面端文件列表常驻，按钮不渲染） */
    toggleMobileAside() {
      this.mobileAsideOpen = !this.mobileAsideOpen
    },
    closeMobileAside() {
      this.mobileAsideOpen = false
    },
    selectFile(file, force) {
      if (!file) {
        return
      }
      if (!force && this.current && this.current.fileName === file.fileName) {
        // 移动端重复点当前文件，收起点开的抽屉即可
        this.closeMobileAside()
        return
      }
      if (this.dirtyCount && !force) {
        this.$confirm('当前文件有未保存的修改，切换后将丢失，是否继续？', '提示', {
          confirmButtonText: '继续',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.applyFile(file)
        }).catch(() => {
        })
        return
      }
      this.applyFile(file)
    },
    applyFile(file) {
      // 拷贝一份，编辑时不污染原始数据，便于"放弃修改"
      const items = (file.items || []).map(decorateItem)
      this.current = Object.assign({}, file, {items})
      // 选完文件自动收起抽屉，移动端才有"选完即见内容"的连贯感
      this.closeMobileAside()
    },
    /** 控件里的值变了：写回编辑值并重新判断是否有未保存修改 */
    onValueChange(row, value) {
      row.editValue = value
      this.markDirty(row)
    },
    markDirty(row) {
      row.dirty = row.editValue !== row.originValue
    },
    rowClass({row}) {
      return row.dirty ? 'config-row--dirty' : ''
    },
    /** 移动端卡片：未保存的配置项整卡高亮，与表格的 config-row--dirty 对齐 */
    dirtyRowClass(row) {
      return row.dirty ? 'config-item--dirty' : ''
    },
    itemSourceText(row) {
      return row.source === 'FILE' ? '来自' + this.current.fileName : '默认值'
    },
    itemDefaultText(row) {
      if (!row.configured || row.type === 'SECRET') {
        return '—'
      }
      return row.defaultValue === '' ? '（空）' : row.defaultValue
    },

    // ==================== 保存 ====================
    saveOne(row) {
      this.saveAllRows([row]).then(() => {
        this.load(this.current.fileName)
      })
    },
    saveAll() {
      const changed = this.current.items.filter(e => e.dirty)
      if (!changed.length) {
        return
      }
      this.saveAllRows(changed).then(() => {
        this.load(this.current.fileName)
      })
    },
    saveAllRows(rows) {
      this.saveLoading = true
      return batchSaveApi({
        // 带上 fileName：后端按"文件 + key"定位配置项
        items: rows.map(e => ({key: e.key, value: e.editValue, fileName: this.current.fileName}))
      }).then(({data: {code, message}}) => {
        if (code !== 200) {
          this.$message.error(message)
          return Promise.reject(new Error(message))
        }
        this.$message.success(message)
      }).catch(e => {
        if (e && e.message) {
          this.$message.error(e.message)
        }
        return Promise.reject(e)
      }).finally(() => {
        this.saveLoading = false
      })
    },

    // ==================== 刷新 ====================
    refreshOne(row) {
      refreshApi({key: row.key, fileName: this.current.fileName}).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$message.success(message)
        this.load(this.current.fileName)
      })
    },
    reloadFile() {
      this.fileRefreshLoading = true
      refreshFileApi({fileName: this.current.fileName}).then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$message.success(message)
        this.load(this.current.fileName)
      }).finally(() => {
        this.fileRefreshLoading = false
      })
    },
    reloadAll() {
      this.allLoading = true
      refreshAllApi().then(({data: {code, message}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.$message.success(message)
        this.load(this.current && this.current.fileName)
      }).finally(() => {
        this.allLoading = false
      })
    },

    // ==================== 重置 ====================
    resetOne(row) {
      this.$confirm('确认将该配置重置为默认值？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        resetApi({key: row.key, fileName: this.current.fileName}).then(({data: {code, message}}) => {
          if (code !== 200) {
            return this.$message.error(message)
          }
          this.$message.success(message)
          this.load(this.current.fileName)
        })
      }).catch(() => {
      })
    },

    // ==================== 文件 ====================
    viewFile() {
      fileContentApi({fileName: this.current.fileName}).then(({data: {code, message, data}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.fileDialog.title = this.current.path
        this.fileDialog.content = data
        this.fileDialog.visible = true
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.config-manage {
  display: flex;
  align-items: flex-start;
  padding: 10px 6px;
  min-height: calc(100vh - 120px);

  &__aside {
    flex: 0 0 232px;
    width: 232px;
    margin-right: 10px;
    background: #fff;
    border-radius: 4px;
    padding: 12px 0 8px;
    box-sizing: border-box;
  }

  &__main {
    flex: 1 1 auto;
    min-width: 0;
  }
}

// 让配置表格填满右侧区域（basic-container 默认带内边距）
.config-manage__main ::v-deep .basic-container {
  padding: 0;
}

.config-manage ::v-deep .config-row--dirty {
  background: #fdf6ec !important;

  td {
    background: #fdf6ec !important;
  }
}

// 移动端卡片：未保存的配置项整卡高亮（卡片由 mobile-record-list 渲染，需穿透 scoped）
.config-manage ::v-deep .mobile-record.config-item--dirty {
  border-color: #f0c78a;
  background: #fdf6ec;
}

.aside-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px 10px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;

  &__ops {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  i {
    cursor: pointer;
    font-size: 15px;
    color: #909399;
    transition: color .2s;

    &:hover {
      color: #409eff;
    }
  }
}

.file-list {
  list-style: none;
  margin: 0;
  padding: 6px 8px;

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 10px;
    border-radius: 4px;
    cursor: pointer;
    transition: background .15s;

    &:hover {
      background: #f5f7fa;
    }

    &.active {
      background: #ecf5ff;
      box-shadow: inset 3px 0 0 #409eff;
    }

    &.missing .file-list__name {
      color: #909399;
    }
  }

  &__main {
    min-width: 0;
  }

  &__name {
    font-size: 13px;
    color: #303133;
    line-height: 18px;
  }

  &__path {
    font-size: 11px;
    color: #a8abb2;
    line-height: 16px;
  }

  &__badges {
    display: flex;
    align-items: center;
    gap: 4px;
    flex: 0 0 auto;
  }
}

.aside-tip {
  padding: 8px 14px 4px;
  font-size: 11px;
  line-height: 18px;
  color: #a8abb2;
  border-top: 1px solid #ebeef5;
  margin-top: 6px;
}

.main-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;

  &__title {
    display: flex;
    align-items: center;
    gap: 8px;

    .name {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
    }

    .file {
      font-size: 12px;
      color: #a8abb2;
    }
  }
}

.file-remark {
  margin: 8px 0 10px;
  font-size: 12px;
  color: #909399;
  line-height: 18px;
}

// 移动端卡片里的配置key（表格里由独立列展示）
.config-item-key {
  margin-bottom: 6px;
  font-size: 11px;
  color: #a8abb2;
  word-break: break-all;
}

.item-name {
  font-size: 13px;
  color: #303133;
}

.item-key {
  font-size: 11px;
  color: #a8abb2;
  word-break: break-all;
}

.item-source {
  margin-top: 4px;
  font-size: 11px;
  color: #a8abb2;
}

.item-remark {
  font-size: 12px;
  color: #606266;
  line-height: 18px;
}

.item-default {
  margin-top: 2px;
  font-size: 11px;
  color: #c0c4cc;
}

.danger-text {
  color: #f56c6c;
}

.file-preview {
  max-height: 520px;
  overflow: auto;
  margin: 0;
  padding: 12px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
  word-break: break-all;
}

/**
 * 手机端：左侧文件列表改为覆盖式抽屉（默认收起，由工具栏"配置文件"按钮唤出），
 * 配置项表格换成卡片列表（见 template 里的 mobile-record-list）。
 * 抽屉用 absolute 定位脱离文档流，收起时不影响主区宽度；再靠 transform 滑出，
 * visibility 一起过渡，避免收起后里面还能被键盘 Tab 到。
 */
@media screen and (max-width: 767.98px) {
  .config-manage {
    position: relative;
    display: block;
    padding: 8px 4px;
    min-height: 0;
  }

  .config-manage__aside {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    z-index: 12;
    width: 84vw;
    max-width: 300px;
    margin-right: 0;
    overflow-y: auto;
    visibility: hidden;
    transform: translateX(-105%);
    transition: transform .2s ease, visibility .2s;
    box-shadow: 2px 0 12px rgba(0, 0, 0, .25);
  }

  .config-manage__aside--open {
    visibility: visible;
    transform: translateX(0);
  }

  .config-manage__mask {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 11;
    background: rgba(0, 0, 0, .35);
  }

  // 文件列表项加大点击区域，方便手指点选
  .file-list li {
    min-height: 48px;
  }

  // 抽屉标题栏的刷新/关闭是纯图标，靠 padding 把热区撑到 ~36px，再用负 margin 抵消布局影响
  .aside-title i {
    font-size: 18px;
    padding: 10px 6px;
    margin: -10px 0;
  }

  .main-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .main-toolbar__title {
    flex-wrap: wrap;
    row-gap: 6px;

    .name,
    .file {
      max-width: 100%;
      overflow-wrap: anywhere;
    }
  }

  .main-toolbar__buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;

    .el-button {
      flex: 1 1 auto;
      min-height: 40px;
      margin-left: 0;
    }
  }

  .file-preview {
    max-height: 50vh;
  }
}
</style>

<style lang="scss">
/**
 * 查看文件弹窗（el-dialog 的 custom-class 挂在非根元素上，scoped 样式选不中，
 * 所以这段不能加 scoped）。手机端用 fullscreen 整屏阅读长配置。
 *
 * 这里 position:fixed 铺满视口，而不是只靠 element 的 .is-fullscreen(height:100%)：
 * el-dialog__wrapper 在移动端带 20px 的 padding-bottom（见 styles/media.scss），
 * 只写 100% 底部会露出一条缝；宽度也要 !important，否则会被 media.scss 的
 * "窄屏弹窗 92vw" 规则压回去。
 */
.config-file-dialog.el-dialog.is-fullscreen {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  width: 100% !important;
  max-width: 100% !important;
  height: 100% !important;
  margin: 0 !important;
  border-radius: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .el-dialog__header {
    flex: 0 0 auto;
  }

  .el-dialog__body {
    display: flex;
    flex-direction: column;
    flex: 1 1 auto;
    min-height: 0;
    max-height: none;
    padding: 8px 10px;
    overflow: hidden;
  }

  // 内容区自己滚动，高度锁在弹窗剩余空间内
  .file-preview {
    flex: 1 1 auto;
    min-height: 0;
    max-height: none;
  }
}
</style>

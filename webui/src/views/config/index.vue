<template>
  <div class="config-manage">
    <!-- 左侧：配置文件分组 -->
    <div class="config-manage__aside">
      <div class="aside-title">
        <span>配置文件</span>
        <el-tooltip content="重新加载全部配置文件" placement="top">
          <i class="el-icon-refresh" :class="{'is-loading': allLoading}" @click="reloadAll"></i>
        </el-tooltip>
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

        <el-table :data="current.items" v-loading="tableLoading" size="small" border stripe
                  :row-class-name="rowClass" max-height="720">
          <el-table-column label="配置项" min-width="220">
            <template slot-scope="{row}">
              <div class="item-name">{{ row.displayName }}</div>
              <div class="item-key">{{ row.key }}</div>
            </template>
          </el-table-column>

          <el-table-column label="值" min-width="300">
            <template slot-scope="{row}">
              <!-- yml 类配置只读：缩进/锚点难以安全改写 -->
              <span v-if="!row.writable" class="value-readonly">
                {{ row.type === 'SECRET' ? (row.hasValue ? row.maskedValue : '未设置')
                    : (row.value === '' || row.value === null ? '（空）' : row.value) }}
                <el-tooltip content="该配置属于 yml 文件，请在服务器上直接编辑该文件后重启" placement="top">
                  <i class="el-icon-lock"></i>
                </el-tooltip>
              </span>

              <!-- 布尔 -->
              <el-switch v-else-if="row.type === 'BOOL'"
                         v-model="row.editValue"
                         active-value="true"
                         inactive-value="false"
                         active-text="开启"
                         inactive-text="关闭"
                         @change="markDirty(row)">
              </el-switch>

              <!-- 整数 -->
              <el-input v-else-if="row.type === 'INT'"
                        v-model.trim="row.editValue"
                        size="small"
                        class="value-input value-input--number"
                        @input="markDirty(row)">
                <template slot="append">整数</template>
              </el-input>

              <!-- 敏感信息 -->
              <el-input v-else-if="row.type === 'SECRET'"
                        v-model="row.editValue"
                        size="small"
                        show-password
                        class="value-input"
                        :placeholder="row.hasValue ? ('已设置 ' + row.maskedValue) : '未设置'"
                        @input="markDirty(row)">
              </el-input>

              <!-- 列表 -->
              <el-input v-else-if="row.type === 'LIST'"
                        v-model="row.editValue"
                        size="small"
                        type="textarea"
                        :autosize="{minRows:1, maxRows:3}"
                        class="value-input"
                        placeholder="多个值用逗号分隔"
                        @input="markDirty(row)">
              </el-input>

              <!-- JSON -->
              <el-input v-else-if="row.type === 'JSON'"
                        v-model="row.editValue"
                        size="small"
                        type="textarea"
                        :autosize="{minRows:1, maxRows:6}"
                        class="value-input"
                        @input="markDirty(row)">
              </el-input>

              <!-- 字符串 -->
              <el-input v-else
                        v-model="row.editValue"
                        size="small"
                        class="value-input"
                        clearable
                        @input="markDirty(row)">
              </el-input>
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

          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template slot-scope="{row}">
              <el-button type="text" size="mini" :disabled="!row.dirty || !row.writable"
                         @click="saveOne(row)">保存</el-button>
              <el-button v-if="row.hot" type="text" size="mini" @click="refreshOne(row)">刷新</el-button>
              <el-button type="text" size="mini" class="danger-text" :disabled="!row.writable"
                         @click="resetOne(row)">重置</el-button>
            </template>
          </el-table-column>
        </el-table>
      </basic-container>
    </div>

    <!-- 查看文件原始内容 -->
    <el-dialog :visible.sync="fileDialog.visible" :title="fileDialog.title" width="760px" v-dialogDrag>
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

export default {
  name: 'ConfigManage',
  data() {
    return {
      files: [],
      current: null,
      tableLoading: false,
      saveLoading: false,
      fileRefreshLoading: false,
      allLoading: false,
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
    selectFile(file, force) {
      if (!file) {
        return
      }
      if (!force && this.current && this.current.fileName === file.fileName) {
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
      const items = (file.items || []).map(e => this.decorate(e))
      this.current = Object.assign({}, file, {items})
    },
    decorate(item) {
      return Object.assign({}, item, {
        // SECRET 类型后端不下发明文，用空串开始编辑
        editValue: item.value === null || item.value === undefined ? '' : item.value,
        dirty: false,
        originValue: item.value === null || item.value === undefined ? '' : item.value
      })
    },
    markDirty(row) {
      row.dirty = row.writable && row.editValue !== row.originValue
    },
    rowClass({row}) {
      return row.dirty ? 'config-row--dirty' : ''
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
        items: rows.map(e => ({key: e.key, value: e.editValue}))
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
      refreshApi({key: row.key}).then(({data: {code, message}}) => {
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
        resetApi({key: row.key}).then(({data: {code, message}}) => {
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

.aside-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px 10px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;

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

.value-input {
  max-width: 460px;
}

.value-readonly {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
  word-break: break-all;

  i {
    color: #c0c4cc;
  }
}

.value-input--number {
  max-width: 180px;
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
</style>

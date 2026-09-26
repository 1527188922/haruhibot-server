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
              <!-- 开关 -->
              <el-switch v-if="controlType(row) === 'SWITCH'"
                         v-model="row.editValue"
                         active-value="true"
                         inactive-value="false"
                         active-text="开启"
                         inactive-text="关闭"
                         @change="markDirty(row)">
              </el-switch>

              <!-- 下拉：单选 -->
              <el-select v-else-if="controlType(row) === 'SELECT' && !isMulti(row)"
                         v-model="row.editValue"
                         size="small"
                         class="value-input"
                         clearable
                         default-first-option
                         :filterable="control(row).allowCustom"
                         :allow-create="control(row).allowCustom"
                         :placeholder="control(row).allowCustom ? '选择或直接输入' : '请选择'"
                         @change="markDirty(row)">
                <el-option v-for="opt in options(row)"
                           :key="opt.value"
                           :label="opt.label"
                           :value="opt.value">
                </el-option>
              </el-select>

              <!-- 下拉：多选（允许自定义值时相当于标签输入框，回车创建） -->
              <el-select v-else-if="controlType(row) === 'SELECT'"
                         v-model="row.editList"
                         size="small"
                         class="value-input"
                         multiple
                         default-first-option
                         :filterable="control(row).allowCustom"
                         :allow-create="control(row).allowCustom"
                         :placeholder="control(row).allowCustom ? '选择或输入后回车' : '请选择'"
                         @change="syncList(row)">
                <el-option v-for="opt in options(row)"
                           :key="opt.value"
                           :label="opt.label"
                           :value="opt.value">
                </el-option>
              </el-select>

              <!-- 复选组（多个值，逗号拼接） -->
              <el-checkbox-group v-else-if="controlType(row) === 'CHECKBOX' && isMulti(row)"
                                 v-model="row.editList"
                                 class="value-checks"
                                 @change="syncList(row)">
                <el-checkbox v-for="opt in options(row)"
                             :key="opt.value"
                             :label="opt.value">{{ opt.label }}</el-checkbox>
              </el-checkbox-group>

              <!-- 单个复选框 -->
              <el-checkbox v-else-if="controlType(row) === 'CHECKBOX'"
                           :value="row.editValue === 'true'"
                           @change="v => setScalarValue(row, v ? 'true' : 'false')">
                {{ row.editValue === 'true' ? '开启' : '关闭' }}
              </el-checkbox>

              <!-- 单选框组：必须用 v-model，:value + @change 拿不到点击后的值 -->
              <el-radio-group v-else-if="controlType(row) === 'RADIO'"
                              v-model="row.editValue"
                              class="value-radios"
                              @change="markDirty(row)">
                <el-radio v-for="opt in options(row)"
                          :key="opt.value"
                          :label="opt.value">{{ opt.label }}</el-radio>
              </el-radio-group>

              <!-- 输入框：按值类型细分 -->
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

              <!-- 列表 / JSON：多行文本 -->
              <el-input v-else-if="row.type === 'LIST' || row.type === 'JSON'"
                        v-model="row.editValue"
                        size="small"
                        type="textarea"
                        :autosize="{minRows:1, maxRows: row.type === 'JSON' ? 6 : 3}"
                        class="value-input"
                        :placeholder="row.type === 'JSON' ? 'JSON 文本' : '多个值用逗号分隔'"
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
              <el-button type="text" size="mini" :disabled="!row.dirty" @click="saveOne(row)">保存</el-button>
              <el-button v-if="row.hot" type="text" size="mini" @click="refreshOne(row)">刷新</el-button>
              <el-button type="text" size="mini" class="danger-text" @click="resetOne(row)">重置</el-button>
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
      const value = item.value === null || item.value === undefined ? '' : item.value
      const row = Object.assign({}, item, {
        // SECRET 类型后端不下发明文，用空串开始编辑
        editValue: value,
        // 多值控件（多选下拉 / 复选组）绑定用的数组模型，保存时再拼成逗号分隔的字符串
        editList: [],
        dirty: false,
        originValue: value
      })
      if (this.isMulti(row)) {
        // 归一化后再比较，避免 "a, b" 和 "a,b" 这种差异被当成改动
        row.editList = this.listValue(row)
        row.editValue = row.editList.join(',')
        row.originValue = row.editValue
      }
      return row
    },
    markDirty(row) {
      row.dirty = row.editValue !== row.originValue
    },
    rowClass({row}) {
      return row.dirty ? 'config-row--dirty' : ''
    },

    // ==================== 控件 ====================
    /**
     * 控件元数据（后端 ConfigItem.control）：
     * 控件类型与值类型解耦，同一个值类型可以配不同控件
     */
    control(row) {
      return row.control || {type: 'INPUT', multiple: false, allowCustom: false, options: []}
    },
    controlType(row) {
      return this.control(row).type
    },
    /** 是否多值控件（多选下拉 / 复选组） */
    isMulti(row) {
      const control = this.control(row)
      return (control.type === 'SELECT' || control.type === 'CHECKBOX') && control.multiple === true
    },
    /**
     * 候选项：把"当前值/原值里有、候选项里却没有"的值也补进去，
     * 否则已配置的值显示不出来，一保存还会被丢掉（例如 druid filters 里多配了一项）；
     * 原值也补是为了取消勾选之后还能再勾回来
     */
    options(row) {
      const declared = this.control(row).options || []
      const known = declared.map(e => e.value)
      const values = this.listValue(row).concat(this.originList(row))
      const extra = values
        .filter((e, i) => !known.includes(e) && values.indexOf(e) === i)
        .map(e => ({value: e, label: e}))
      return declared.concat(extra)
    },
    /** 多值控件的模型：逗号/空白分隔的字符串 <-> 数组 */
    listValue(row) {
      return this.splitList(row.editValue)
    },
    /** 原值的数组模型（用于候选项补全） */
    originList(row) {
      return this.splitList(row.originValue)
    },
    splitList(value) {
      const text = value === null || value === undefined ? '' : String(value)
      return text === '' ? [] : text.split(/[,，\s]+/).filter(e => e !== '')
    },
    /** 多值控件 v-model 写回数组后，同步成保存用的逗号分隔字符串 */
    syncList(row) {
      row.editValue = (row.editList || []).join(',')
      this.markDirty(row)
    },
    /** 单值控件（单个复选框）写回字符串值 */
    setScalarValue(row, value) {
      row.editValue = value
      this.markDirty(row)
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

.value-input--number {
  max-width: 180px;
}

// 复选组 / 单选框组：换行排列，收掉 element 默认的 30px 间距
.value-checks,
.value-radios {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  line-height: 32px;

  ::v-deep .el-checkbox,
  ::v-deep .el-radio {
    margin-right: 12px;
  }

  ::v-deep .el-checkbox + .el-checkbox,
  ::v-deep .el-radio + .el-radio {
    margin-left: 0;
  }
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

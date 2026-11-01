<template>
  <!--
    配置项的值控件：桌面表格与移动端卡片共用同一套分支，
    避免"表格里改一个控件、卡片里忘了改"这种走样。

    row 只用来读控件元数据/值类型，真正的编辑值走 value + change 事件：
    控件写值 -> $emit('change', 新值) -> 父组件写回 row.editValue 并标记 dirty。
    这样这里不直接改 prop，符合 vue/no-mutating-props。
  -->
  <div class="config-value-editor">
    <!-- 开关 -->
    <el-switch v-if="type === 'SWITCH'"
               v-model="valueModel"
               active-value="true"
               inactive-value="false"
               active-text="开启"
               inactive-text="关闭">
    </el-switch>

    <!-- 下拉：单选（线路类配置多一颗「测速」按钮） -->
    <div v-else-if="type === 'SELECT' && !multi" class="value-select-wrap">
      <el-select v-model="valueModel"
                 size="small"
                 class="value-input"
                 clearable
                 default-first-option
                 :filterable="control.allowCustom"
                 :allow-create="control.allowCustom"
                 :placeholder="control.allowCustom ? '选择或直接输入' : '请选择'">
        <el-option v-for="opt in options"
                   :key="opt.value"
                   :label="opt.label"
                   :value="opt.value">
          <span class="value-option">
            <span class="value-option__label">{{ opt.label }}</span>
            <span v-if="probeResults[opt.value]"
                  :class="['value-option__probe', 'value-option__probe--' + probeResults[opt.value].level.toLowerCase()]">
              {{ probeText(probeResults[opt.value]) }}
            </span>
          </span>
        </el-option>
      </el-select>
      <el-button v-if="showProbe"
                 type="text"
                 size="mini"
                 class="value-probe-btn"
                 icon="el-icon-odometer"
                 :loading="probing"
                 :disabled="probing"
                 @click="runProbe">测速</el-button>
    </div>

    <!-- 下拉：多选（允许自定义值时相当于标签输入框，回车创建） -->
    <el-select v-else-if="type === 'SELECT'"
               v-model="listModel"
               size="small"
               class="value-input"
               multiple
               default-first-option
               :filterable="control.allowCustom"
               :allow-create="control.allowCustom"
               :placeholder="control.allowCustom ? '选择或输入后回车' : '请选择'">
      <el-option v-for="opt in options"
                 :key="opt.value"
                 :label="opt.label"
                 :value="opt.value">
      </el-option>
    </el-select>

    <!-- 复选组（多个值，逗号拼接） -->
    <el-checkbox-group v-else-if="type === 'CHECKBOX' && multi"
                       v-model="listModel"
                       class="value-checks">
      <el-checkbox v-for="opt in options"
                   :key="opt.value"
                   :label="opt.value">{{ opt.label }}</el-checkbox>
    </el-checkbox-group>

    <!-- 单个复选框 -->
    <el-checkbox v-else-if="type === 'CHECKBOX'"
                 :value="value === 'true'"
                 @change="v => $emit('change', v ? 'true' : 'false')">
      {{ value === 'true' ? '开启' : '关闭' }}
    </el-checkbox>

    <!-- 单选框组 -->
    <el-radio-group v-else-if="type === 'RADIO'"
                    v-model="valueModel"
                    class="value-radios">
      <el-radio v-for="opt in options"
                :key="opt.value"
                :label="opt.value">{{ opt.label }}</el-radio>
    </el-radio-group>

    <!-- 输入框：按值类型细分 -->
    <el-input v-else-if="row.type === 'INT'"
              v-model.trim="valueModel"
              size="small"
              class="value-input value-input--number">
      <template slot="append">整数</template>
    </el-input>

    <!-- 敏感信息 -->
    <el-input v-else-if="row.type === 'SECRET'"
              v-model="valueModel"
              size="small"
              show-password
              class="value-input"
              :placeholder="row.hasValue ? ('已设置 ' + row.maskedValue) : '未设置'">
    </el-input>

    <!-- 列表 / JSON：多行文本 -->
    <el-input v-else-if="row.type === 'LIST' || row.type === 'JSON'"
              v-model="valueModel"
              size="small"
              type="textarea"
              :autosize="{minRows:1, maxRows: row.type === 'JSON' ? 6 : 3}"
              class="value-input"
              :placeholder="row.type === 'JSON' ? 'JSON 文本' : '多个值用逗号分隔'">
    </el-input>

    <!-- 字符串 -->
    <el-input v-else
              v-model="valueModel"
              size="small"
              class="value-input"
              clearable>
    </el-input>

    <!--
      测速结果（只对带 control.probe 的线路类配置出现）：最快的那条 + 失败条数，可一键采用。
      必须放在 v-if / v-else-if 链之后，链中间插别的 v-if 会让后面的 v-else-if 失配。
    -->
    <div v-if="showProbe && probeDone" class="value-probe-result">
      <template v-if="fastestResult">
        最快 <b>{{ fastestResult.label }}</b> · {{ fastestResult.latencyMs }}ms
        <el-button v-if="value !== fastestResult.host"
                   type="text"
                   size="mini"
                   class="value-probe-apply"
                   @click="applyFastest">用这条</el-button>
      </template>
      <span v-else>所有线路都测速失败</span>
      <span v-if="failedCount" class="value-probe-failed">（{{ failedCount }} 条不可用）</span>
    </div>
  </div>
</template>

<script>
import {controlOf, controlTypeOf, isMultiControl, optionsOf, probeKindOf, splitList, supportsProbe} from '@/util/config-item'
import {probeJmLines} from '@/api/jmcomic'

export default {
  name: 'ConfigValueEditor',
  props: {
    /** 配置项行：只读，用来取控件元数据与值类型 */
    row: {
      type: Object,
      required: true
    },
    /** 当前编辑值（字符串形态，多值控件用逗号分隔） */
    value: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      // 测速进行中
      probing: false,
      // 测过一次之后才显示结果行（没测过不要占位）
      probeDone: false,
      // host -> {host, label, latencyMs, level, message}
      probeResults: {}
    }
  },
  computed: {
    control() {
      return controlOf(this.row)
    },
    type() {
      return controlTypeOf(this.row)
    },
    multi() {
      return isMultiControl(this.row)
    },
    options() {
      return optionsOf(this.row)
    },
    /** 测速类型（空串=该配置项不支持测速） */
    probeKind() {
      return probeKindOf(this.row)
    },
    /** 是否显示「测速」按钮/结果行：只有"下拉单选 + 声明了 probe"的配置项才有 */
    showProbe() {
      return supportsProbe(this.row)
    },
    /** 测速成功的线路里最快的那个（同耗时取清单里靠前的） */
    fastestResult() {
      let fastest = null
      this.options.forEach(opt => {
        const result = this.probeResults[opt.value]
        if (!result || result.level === 'FAILED' || result.latencyMs === null || result.latencyMs === undefined) {
          return
        }
        if (!fastest || result.latencyMs < fastest.latencyMs) {
          fastest = result
        }
      })
      return fastest
    },
    failedCount() {
      return Object.values(this.probeResults).filter(e => e.level === 'FAILED').length
    },
    /** 单值控件的 v-model：写回时把新值交给父组件 */
    valueModel: {
      get() {
        return this.value
      },
      set(value) {
        this.$emit('change', value === null || value === undefined ? '' : String(value))
      }
    },
    /** 多值控件的 v-model：数组 <-> 逗号分隔字符串 */
    listModel: {
      get() {
        return splitList(this.value)
      },
      set(list) {
        this.$emit('change', (list || []).join(','))
      }
    }
  },
  methods: {
    /** 下拉项里的耗时文案：320ms / 超时 / 不可用 */
    probeText(result) {
      if (!result) {
        return ''
      }
      if (result.level === 'FAILED') {
        return result.message || '不可用'
      }
      return `${result.latencyMs}ms`
    },
    /**
     * 逐条测速。候选项由后端按线路清单补齐，所以这里只需要把"用户手填、清单里没有"
     * 的当前值作为额外域名带过去。
     */
    async runProbe() {
      if (!this.probeKind) {
        return
      }
      this.probing = true
      try {
        const current = (this.value || '').trim()
        const hosts = current && !this.options.some(e => e.value === current) ? [current] : []
        const {data: {code, message, data}} = await probeJmLines({kind: this.probeKind, hosts})
        if (code !== 200) {
          this.$message.error(message)
          return
        }
        const results = {}
        ;(data || []).forEach(e => {
          results[e.host] = e
        })
        this.probeResults = results
        this.probeDone = true
        if (!this.fastestResult) {
          this.$message.warning('所有线路都测速失败，请检查本机网络或稍后重试')
        }
      } catch (e) {
        this.$message.error(e.message)
      } finally {
        this.probing = false
      }
    },
    /** 一键采用最快线路（仍走 change 事件写回，父组件照常标记未保存） */
    applyFastest() {
      if (this.fastestResult) {
        this.$emit('change', this.fastestResult.host)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.value-input {
  max-width: 460px;
}

.value-input--number {
  max-width: 180px;
}

/**
 * 线路类下拉：选择框 + 测速按钮一行。
 * el-select 是 inline-block，按钮用 inline-flex 跟着走，窄屏时允许换行。
 */
.value-select-wrap {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  max-width: 100%;
}

.value-probe-btn {
  padding: 0 2px;
  font-size: 12px;
}

/** 下拉选项：左边名字，右边耗时/不可用 */
.value-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.value-option__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.value-option__probe {
  flex: 0 0 auto;
  font-size: 11px;
  line-height: 16px;
  padding: 0 6px;
  border-radius: 8px;
  color: #909399;
  background: #f4f4f5;

  &--smooth {
    color: #529b2e;
    background: #f0f9eb;
  }

  &--normal {
    color: #b88230;
    background: #fdf6ec;
  }

  &--laggy {
    color: #c45656;
    background: #fef0f0;
  }

  &--failed {
    color: #a8abb2;
    background: #f4f4f5;
  }
}

/** 测速结果行：最快线路 + 不可用条数 */
.value-probe-result {
  margin-top: 4px;
  font-size: 12px;
  line-height: 18px;
  color: #909399;

  b {
    color: #303133;
    font-weight: 600;
  }
}

.value-probe-apply {
  padding: 0 4px;
  font-size: 12px;
}

.value-probe-failed {
  color: #c45656;
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

/**
 * 手机端：控件铺满可用宽度。
 * el-select 是 width:auto 的 inline-block，不显式给宽度只会缩成输入框的固有宽度；
 * 表格形态下由单元格撑开，卡片形态下没有这层约束，必须自己铺满。
 */
@media screen and (max-width: 767.98px) {
  .config-value-editor {
    width: 100%;
    min-width: 0;
  }

  .value-input,
  .value-input--number {
    width: 100%;
    max-width: 100%;
  }

  /**
   * 线路下拉 + 测速按钮：窄屏时按钮单独占一行（触控高度拉到 40px），
   * 下拉自己铺满剩余宽度，否则 el-select 的 100% 宽度会把按钮挤出卡片。
   */
  .value-select-wrap {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    gap: 2px;
    width: 100%;
  }

  .value-probe-btn {
    align-self: flex-start;
    min-height: 40px;
    padding: 0 4px;
    font-size: 13px;
  }

  .value-checks,
  .value-radios {
    // 触控高度交给子项的 min-height，组容器自己不放行高：
    // 行高会被子项继承，撑大它们内部的行盒（见下面 el-switch 的说明）
    ::v-deep .el-checkbox,
    ::v-deep .el-radio {
      min-height: 40px;
      margin-right: 16px;
    }
  }

  /**
   * 开关只放大触控区域（min-height），不要再给 .el-switch 设 line-height。
   *
   * .el-switch__label 的高度被 element 固定为 20px，里面那层
   * <span aria-hidden="true">开启/关闭</span> 是它的行内内容：
   * 一旦 label 继承到 40px 行高，行盒就比 20px 的 label 框高，
   * 文字会被顶到 label 框下面（按 CSS 规范计入 strut 的 Gecko/WebKit 上必现，
   * Blink 在 label 里没有文本节点时会漏算 strut，所以只有部分浏览器看得出来）。
   * 这里再把 label 行高钉回它的固定高度，外层以后无论怎么改行高都不会再顶偏。
   */
  .config-value-editor ::v-deep .el-switch {
    min-height: 40px;

    .el-switch__label {
      line-height: 20px;
    }
  }
}
</style>

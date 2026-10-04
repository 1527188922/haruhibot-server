/**
 * 「配置管理」页里单个配置项（后端 ConfigItem）的纯计算逻辑。
 *
 * 桌面表格与移动端卡片由同一个控件组件（views/config/config-value-editor.vue）渲染，
 * 控件类型判断、候选项补全、多值字符串<->数组转换都收在这里，避免两边各写一份后逐渐走样。
 *
 * 全部是纯函数：只读入参，不改动 row；写回编辑值由组件自己负责。
 */

/** 控件元数据，后端未下发时按输入框兜底 */
export function controlOf(row) {
  return (row && row.control) || {type: 'INPUT', multiple: false, allowCustom: false, options: []}
}

/**
 * 控件类型（SWITCH / SELECT / CHECKBOX / RADIO / INPUT）。
 * 与值类型解耦：同一个值类型可以配不同控件，前端只认这份元数据
 */
export function controlTypeOf(row) {
  return controlOf(row).type
}

/** 是否多值控件（多选下拉 / 复选组） */
export function isMultiControl(row) {
  const control = controlOf(row)
  return (control.type === 'SELECT' || control.type === 'CHECKBOX') && control.multiple === true
}

/** 多值控件的模型：逗号/空白分隔的字符串 <-> 数组 */
export function splitList(value) {
  const text = value === null || value === undefined ? '' : String(value)
  return text === '' ? [] : text.split(/[,，\s]+/).filter(e => e !== '')
}

/** 多值控件的当前值（数组模型） */
export function listValue(row) {
  return splitList(row.editValue)
}

/**
 * 候选项：把"当前值/原值里有、候选项里却没有"的值也补进去，
 * 否则已配置的值显示不出来，一保存还会被丢掉（例如 druid filters 里多配了一项）；
 * 原值也补是为了取消勾选之后还能再勾回来
 */
export function optionsOf(row) {
  const declared = controlOf(row).options || []
  const known = declared.map(e => e.value)
  const values = listValue(row).concat(splitList(row.originValue))
  const extra = values
    .filter((e, i) => !known.includes(e) && values.indexOf(e) === i)
    .map(e => ({value: e, label: e}))
  return declared.concat(extra)
}

/** 后端配置项 -> 可编辑行：拷贝一份，编辑时不污染原始数据，便于"放弃修改" */
export function decorateItem(item) {
  const value = item.value === null || item.value === undefined ? '' : item.value
  const row = Object.assign({}, item, {
    // SECRET 类型后端不下发明文，用空串开始编辑
    editValue: value,
    dirty: false,
    originValue: value
  })
  if (isMultiControl(row)) {
    // 归一化后再比较，避免 "a, b" 和 "a,b" 这种差异被当成改动
    row.editValue = splitList(value).join(',')
    row.originValue = row.editValue
  }
  return row
}

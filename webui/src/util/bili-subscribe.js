/**
 * bilibili订阅推送目标(群号/qq号)工具
 * 后端以逗号分割的字符串存储，前端各弹框统一用这里的方法做字符串与数组的互转，避免重复实现解析逻辑
 */

/**
 * 解析逗号分割的群号/qq号，兼容中文逗号与空白符
 * @param ids 逗号分割的字符串，或已经是数组
 * @returns {Number[]}
 */
export const parseIds = ids => {
  if (!ids) {
    return []
  }
  if (Array.isArray(ids)) {
    return ids.map(e => Number(e)).filter(e => !isNaN(e))
  }
  return String(ids).split(/[,，\s]+/)
    .filter(e => e)
    .map(e => Number(e))
    .filter(e => !isNaN(e))
}

/**
 * 拼接为逗号分割的字符串，用于提交给后端
 * @param ids {Number[]}
 * @returns {String}
 */
export const joinIds = ids => {
  if (!ids || !ids.length) {
    return ''
  }
  return ids.map(e => Number(e)).filter(e => !isNaN(e)).join(',')
}

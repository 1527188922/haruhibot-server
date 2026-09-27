/**
 * JM ID 解析工具。
 *
 * 单独抽成模块而不是内联在弹窗组件里，是为了能被直接测试。
 * 支持的输入形态：
 * - 纯数字：422866
 * - 带前缀：JM422866 / jm422866
 * - 链接：https://18comic.vip/album/422866/ 、/photos/422866、?id=422866
 * - 从 JM 页面复制的整段文本，其中夹带若干 ID
 */

/**
 * 匹配 4~10 位数字，允许前面带 jm 前缀（不区分大小写）。
 * 用捕获组只取数字部分，避免把前缀算进结果。
 */
const JM_ID_PATTERN = /(?:jm)?(\d{4,10})/gi;

/**
 * 从任意文本里提取并去重 JM ID，保持出现顺序。
 * @param {string} text
 * @returns {string[]}
 */
export function parseJmIds(text) {
  if (!text) {
    return []
  }
  const ids = []
  const seen = new Set()
  let match
  // 正则带 g 标志，复用前必须重置 lastIndex
  JM_ID_PATTERN.lastIndex = 0
  while ((match = JM_ID_PATTERN.exec(String(text))) !== null) {
    const id = match[1]
    if (!seen.has(id)) {
      seen.add(id)
      ids.push(id)
    }
  }
  return ids
}

/**
 * 判断一段文本是否包含可用的 JM ID
 * @param {string} text
 * @returns {boolean}
 */
export function hasJmId(text) {
  return parseJmIds(text).length > 0
}

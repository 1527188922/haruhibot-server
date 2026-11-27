/**
 * CQ 码解析：把消息 content 拆分成可渲染的片段，供聊天记录气泡统一展示。
 * 片段类型只有三种：
 * - text：普通文本
 * - image：可展示的图片
 * - tag：无法直接展示的内容（@、表情、视频、转发等）统一渲染为主题色占位标签
 */

const CQ_REGEX = /\[CQ:([a-zA-Z0-9_.-]+)((?:,[^\]]*)?)\]/g
const CQ_EXISTS_REGEX = /\[CQ:[^\]]+\]/
// 属性值里未转义的逗号不应打断拆分，只在逗号后紧跟 key= 时才拆分
const ATTR_SPLIT_REGEX = /,(?=[a-zA-Z0-9_.-]+=)/

// 非文本、非图片类型统一使用占位文案
const TYPE_LABELS = {
  reply: '[回复]',
  forward: '[聊天记录]',
  node: '[聊天记录]',
  video: '[视频]',
  record: '[语音]',
  tts: '[语音]',
  file: '[文件]',
  json: '[卡片消息]',
  xml: '[卡片消息]',
  cardimage: '[卡片消息]',
  share: '[链接]',
  music: '[音乐]',
  location: '[位置]',
  contact: '[名片]',
  poke: '[戳一戳]',
  shake: '[戳一戳]',
  dice: '[骰子]',
  rps: '[猜拳]',
  redbag: '[红包]',
  gift: '[礼物]',
  sface: '[表情]',
  bface: '[表情]'
}

/**
 * 还原 CQ 码转义。&amp; 必须最后解码，避免 &amp;#91; 被二次还原成 [
 */
export function decodeCqEntities (text) {
  if (!text) {
    return ''
  }
  return String(text)
    .replace(/&#91;/g, '[')
    .replace(/&#93;/g, ']')
    .replace(/&#44;/g, ',')
    .replace(/&amp;/g, '&')
}

function parseAttrs (attrText) {
  const attrs = {}
  if (!attrText) {
    return attrs
  }
  attrText.split(ATTR_SPLIT_REGEX).forEach(pair => {
    const index = pair.indexOf('=')
    if (index <= 0) {
      return
    }
    attrs[pair.slice(0, index).trim()] = decodeCqEntities(pair.slice(index + 1))
  })
  return attrs
}

// 历史消息里可能残留 http 图片，https 页面下会被浏览器拦截
function normalizeUrl (url) {
  if (!url) {
    return ''
  }
  return url.indexOf('http://') === 0 ? 'https://' + url.slice(7) : url
}

function resolveAtText (attrs) {
  if (attrs.qq === 'all') {
    return '@全体成员'
  }
  const name = attrs.name || attrs.qq
  return name ? '@' + name : '@'
}

// face 的 raw 里带有 faceText（如 /喝彩），解析出来比统一显示 [表情] 更直观
function resolveFaceText (attrs) {
  if (attrs.raw) {
    try {
      const faceText = JSON.parse(attrs.raw).faceText
      if (faceText) {
        return '[' + String(faceText).replace(/^\//, '') + ']'
      }
    } catch (e) {
      // raw 不是合法 JSON 时降级为通用表情占位
    }
  }
  return '[表情]'
}

function toSegment (type, attrs) {
  switch (type) {
    case 'image':
    case 'mface': {
      const url = normalizeUrl(attrs.url)
      if (url) {
        return { type: 'image', url }
      }
      return { type: 'tag', text: type === 'image' ? '[图片]' : '[表情]' }
    }
    case 'at':
      return { type: 'tag', text: resolveAtText(attrs) }
    case 'face':
      return { type: 'tag', text: resolveFaceText(attrs) }
    default:
      return { type: 'tag', text: TYPE_LABELS[type] || '[消息]' }
  }
}

function pushText (segments, text) {
  if (!text) {
    return
  }
  const prev = segments[segments.length - 1]
  if (prev && prev.type === 'text') {
    prev.text += text
    return
  }
  segments.push({ type: 'text', text })
}

/**
 * @param {String} content 原始消息内容（可能包含任意个 CQ 码与文本）
 * @return {Array} 按出现顺序排列的片段
 */
export function parseCqContent (content) {
  if (content === null || content === undefined || content === '') {
    return []
  }
  const raw = String(content)
  if (!CQ_EXISTS_REGEX.test(raw)) {
    const text = decodeCqEntities(raw)
    return text ? [{ type: 'text', text }] : []
  }

  const segments = []
  let lastIndex = 0
  let match
  CQ_REGEX.lastIndex = 0
  while ((match = CQ_REGEX.exec(raw)) !== null) {
    if (match.index > lastIndex) {
      pushText(segments, decodeCqEntities(raw.slice(lastIndex, match.index)))
    }
    segments.push(toSegment(match[1], parseAttrs(match[2])))
    lastIndex = match.index + match[0].length
  }
  if (lastIndex < raw.length) {
    pushText(segments, decodeCqEntities(raw.slice(lastIndex)))
  }
  return segments
}

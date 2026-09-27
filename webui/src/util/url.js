/**
 * 后端返回的"服务器本地资源"地址（JM封面/漫画图片、图片、音频等）是按配置的
 * internet-host（或自动探测到的公网IP/内网IP）拼出来的绝对地址，形如
 * http://203.0.113.10:8090/jmcomic/xxx/1.jpg
 *
 * 当网页是被局域网里其他机器访问时，这个host很可能不可达（例如配成了公网IP或是127.0.0.1），
 * 于是封面/漫画图片加载失败。这里提供把这些地址换成"当前访问站点"的全局方法。
 *
 * 注意：只处理确实指向后端静态资源目录的路径，JM CDN 等第三方地址、相对地址、data/blob
 * 一律原样返回。因此它不是"一刀切替换"，需要由调用方在确实展示本地资源的字段上显式调用
 * （模板里是 $localUrl(...)，见 main.js 的注册）。
 */

/**
 * 与后端 WebServletConfig 的 /** 静态资源映射、WebResourceConfig 中的目录保持一致
 */
const LOCAL_RESOURCE_PATH_PREFIXES = ['/jmcomic', '/image', '/audio', '/excel', '/video', '/logs']

function normalize(url) {
  if (url === null || url === undefined) {
    return ''
  }
  return String(url).trim()
}

function isLocalResourcePath(pathname) {
  return LOCAL_RESOURCE_PATH_PREFIXES.some(prefix => pathname === prefix || pathname.startsWith(prefix + '/'))
}

function currentOrigin() {
  return typeof window !== 'undefined' && window.location ? window.location.origin : ''
}

/**
 * 该地址是否指向后端本地静态资源（只看路径前缀，不改变原值）
 */
export function isLocalResourceUrl(url) {
  const text = normalize(url)
  if (!/^https?:/i.test(text)) {
    return false
  }
  try {
    return isLocalResourcePath(new URL(text).pathname)
  } catch (e) {
    return false
  }
}

/**
 * 把后端本地静态资源地址的协议+域名+端口换成当前访问站点，其余地址原样返回。
 *
 * - 已经是当前站点的地址：原样返回
 * - JM CDN 等第三方地址：原样返回（不动）
 * - 相对地址 / data: / blob: / 非法地址：原样返回
 *
 * @param {string} url 原始地址
 * @param {string} [origin] 目标站点，默认取 window.location.origin（供测试注入）
 */
export function toCurrentSiteUrl(url, origin) {
  const text = normalize(url)
  if (!text || !/^https?:/i.test(text)) {
    return text
  }
  let parsed
  try {
    parsed = new URL(text)
  } catch (e) {
    return text
  }
  if (!isLocalResourcePath(parsed.pathname)) {
    return text
  }
  const targetOrigin = origin || currentOrigin()
  if (!targetOrigin || parsed.origin === targetOrigin) {
    return text
  }
  return targetOrigin + parsed.pathname + parsed.search + parsed.hash
}

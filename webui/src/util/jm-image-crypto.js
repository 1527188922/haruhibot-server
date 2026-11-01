import CryptoJS from 'crypto-js/core'
import AES from 'crypto-js/aes'
import 'crypto-js/mode-ecb'
import 'crypto-js/pad-nopadding'

/**
 * JM图片本地加密文件的浏览器端解密工具。
 *
 * 文件格式与后端 JmImageCipherUtil 保持一致：
 * 0-3 magic "JMEN"；4 version=1；5 flags(bit0=整图加密)；6-7 保留；
 * 8-11 prefixLen(int32 大端，仅部分加密时有效)；12-15 nonce(4字节)。
 *
 * AES-CTR 计数器为完整16字节大端自增，前4字节是 nonce，其余为0。
 * 局域网 http 页面拿不到 crypto.subtle（非安全上下文），因此优先用 WebCrypto，
 * 不可用时用 crypto-js 逐块 ECB 生成密钥流，保证两种路径结果一致。
 */

const MAGIC = [0x4a, 0x4d, 0x45, 0x4e] // "JMEN"
const HEADER_LENGTH = 16
const FLAG_FULL = 1
const KEY_TEXT = 'HaruhiJmImgKey01'
const KEY_WORD_ARRAY = CryptoJS.enc.Utf8.parse(KEY_TEXT)
const KEY_BYTES = new Uint8Array(KEY_TEXT.length)
for (let i = 0; i < KEY_TEXT.length; i++) {
  KEY_BYTES[i] = KEY_TEXT.charCodeAt(i)
}

const MIME_MAP = {
  webp: 'image/webp',
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  gif: 'image/gif',
  bmp: 'image/bmp'
}

// blob url 缓存：同一个地址只请求/解密一次；超出上限时回收最久未使用的
const MAX_CACHE_ENTRIES = 64
const MAX_PARALLEL = 3
const cache = new Map()
const waitQueue = []
let runningCount = 0
let webCryptoKeyPromise = null

function bytesToWordArray(bytes) {
  const words = []
  for (let i = 0; i < bytes.length; i++) {
    words[i >>> 2] |= (bytes[i] & 0xff) << (24 - (i % 4) * 8)
  }
  return CryptoJS.lib.WordArray.create(words, bytes.length)
}

function wordArrayToBytes(wordArray, length) {
  const len = length === undefined ? wordArray.sigBytes : length
  const out = new Uint8Array(len)
  const words = wordArray.words
  for (let i = 0; i < len; i++) {
    out[i] = (words[i >>> 2] >>> (24 - (i % 4) * 8)) & 0xff
  }
  return out
}

function incrementCounter(counter) {
  for (let i = counter.length - 1; i >= 0; i--) {
    counter[i] = (counter[i] + 1) & 0xff
    if (counter[i] !== 0) {
      break
    }
  }
}

function ctrCryptWithCryptoJs(data, nonce) {
  const out = new Uint8Array(data.length)
  const counter = new Uint8Array(16)
  counter.set(nonce, 0)
  for (let offset = 0; offset < data.length; offset += 16) {
    const keystream = wordArrayToBytes(AES.encrypt(bytesToWordArray(counter), KEY_WORD_ARRAY, {
      mode: CryptoJS.mode.ECB,
      padding: CryptoJS.pad.NoPadding
    }).ciphertext)
    const len = Math.min(16, data.length - offset)
    for (let i = 0; i < len; i++) {
      out[offset + i] = data[offset + i] ^ keystream[i]
    }
    incrementCounter(counter)
  }
  return out
}

function canUseWebCrypto() {
  return typeof window !== 'undefined' &&
    window.crypto &&
    window.crypto.subtle &&
    typeof window.crypto.subtle.decrypt === 'function'
}

function getWebCryptoKey() {
  if (!webCryptoKeyPromise) {
    webCryptoKeyPromise = window.crypto.subtle.importKey(
      'raw',
      KEY_BYTES,
      { name: 'AES-CTR' },
      false,
      ['decrypt']
    )
  }
  return webCryptoKeyPromise
}

async function ctrCryptWithWebCrypto(data, nonce) {
  const key = await getWebCryptoKey()
  const counter = new Uint8Array(16)
  counter.set(nonce, 0)
  const plain = await window.crypto.subtle.decrypt(
    { name: 'AES-CTR', counter, length: 128 },
    key,
    data
  )
  return new Uint8Array(plain)
}

async function ctrCrypt(data, nonce) {
  if (canUseWebCrypto()) {
    try {
      return await ctrCryptWithWebCrypto(data, nonce)
    } catch (e) {
      // 某些环境（如旧浏览器）不支持该参数组合，退回纯JS实现
    }
  }
  return ctrCryptWithCryptoJs(data, nonce)
}

function isEncryptedBytes(bytes) {
  if (bytes.length < HEADER_LENGTH) {
    return false
  }
  return bytes[0] === MAGIC[0] && bytes[1] === MAGIC[1] &&
    bytes[2] === MAGIC[2] && bytes[3] === MAGIC[3]
}

function parseHeader(bytes) {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength)
  return {
    full: (bytes[5] & FLAG_FULL) !== 0,
    prefixLength: view.getUint32(8, false),
    nonce: bytes.slice(12, 16)
  }
}

function mimeOf(url) {
  try {
    const path = new URL(url, window.location.origin).pathname
    const dot = path.lastIndexOf('.')
    const ext = dot === -1 ? '' : path.slice(dot + 1).toLowerCase()
    return MIME_MAP[ext] || 'application/octet-stream'
  } catch (e) {
    return 'application/octet-stream'
  }
}

async function fetchAndDecrypt(url) {
  const response = await fetch(url, { referrerPolicy: 'no-referrer' })
  if (!response.ok) {
    throw new Error(`图片请求失败：${response.status}`)
  }
  const bytes = new Uint8Array(await response.arrayBuffer())
  const type = mimeOf(url)
  if (!isEncryptedBytes(bytes)) {
    // 历史明文图片：原样展示
    return URL.createObjectURL(new Blob([bytes], { type }))
  }
  const header = parseHeader(bytes)
  const prefixLength = Math.max(header.prefixLength, 0)
  const cipherEnd = header.full
    ? bytes.length
    : Math.min(bytes.length, HEADER_LENGTH + prefixLength)
  let plainPrefix = new Uint8Array(0)
  if (cipherEnd > HEADER_LENGTH) {
    plainPrefix = await ctrCrypt(bytes.subarray(HEADER_LENGTH, cipherEnd), header.nonce)
  }
  const parts = [plainPrefix]
  if (!header.full && cipherEnd < bytes.length) {
    // 部分加密：前缀之外保持明文
    parts.push(bytes.subarray(cipherEnd))
  }
  return URL.createObjectURL(new Blob(parts, { type }))
}

function schedule(task) {
  return new Promise((resolve, reject) => {
    waitQueue.push({ task, resolve, reject })
    pumpQueue()
  })
}

function pumpQueue() {
  while (runningCount < MAX_PARALLEL && waitQueue.length > 0) {
    const item = waitQueue.shift()
    runningCount++
    item.task().then(
      value => {
        runningCount--
        item.resolve(value)
        pumpQueue()
      },
      error => {
        runningCount--
        item.reject(error)
        pumpQueue()
      }
    )
  }
}

function trimCache() {
  if (cache.size <= MAX_CACHE_ENTRIES) {
    return
  }
  // 只回收已经没有任何组件在用的条目，避免正在展示的图片被 revoke 成空白
  const evictable = [...cache.entries()]
    .filter(([, entry]) => entry.objectUrl && entry.refs <= 0)
    .sort((a, b) => a[1].lastUsed - b[1].lastUsed)
  for (const [url, entry] of evictable) {
    if (cache.size <= MAX_CACHE_ENTRIES) {
      break
    }
    cache.delete(url)
    URL.revokeObjectURL(entry.objectUrl)
  }
}

/**
 * 获取可直接用于 <img> 的 blob url：自动识别明文/加密文件，加密文件解密后再生成。
 * 同一地址会复用缓存与在途请求，避免列表滚动时重复下载。
 * 拿到结果后占用一次引用，组件卸载或切换地址时必须调用 releaseJmImageBlobUrl。
 */
export function acquireJmImageBlobUrl(src) {
  const url = `${src || ''}`.trim()
  if (!url) {
    return Promise.resolve('')
  }
  let entry = cache.get(url)
  if (!entry) {
    entry = { lastUsed: Date.now(), objectUrl: '', promise: null, refs: 0 }
    entry.promise = schedule(() => fetchAndDecrypt(url)).then(objectUrl => {
      entry.objectUrl = objectUrl
      entry.lastUsed = Date.now()
      return objectUrl
    }).catch(error => {
      if (cache.get(url) === entry) {
        cache.delete(url)
      }
      throw error
    })
    cache.set(url, entry)
  }
  entry.refs++
  entry.lastUsed = Date.now()
  trimCache()
  return entry.promise
}

/**
 * 释放一次引用；条目被淘汰或释放到0后，在超出上限时会被回收
 */
export function releaseJmImageBlobUrl(src) {
  const url = `${src || ''}`.trim()
  const entry = cache.get(url)
  if (!entry) {
    return
  }
  entry.refs = Math.max(entry.refs - 1, 0)
  entry.lastUsed = Date.now()
  trimCache()
}

/**
 * 清空blob缓存（退出登录等需要释放内存的场景可调用）
 */
export function clearJmImageCache() {
  cache.forEach(entry => {
    if (entry.objectUrl) {
      URL.revokeObjectURL(entry.objectUrl)
    }
  })
  cache.clear()
}

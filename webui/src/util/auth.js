// import Cookies from 'js-cookie'
import website from '@/config/website'
const Authorization = website.Authorization
const headerUserNameKey = website.headerUserNameKey
// var inFifteenMinutes = new Date(new Date().getTime() + website.tokenTime * 1000);

const storage = localStorage

//读取storage中的有效值，过滤掉 null/undefined 以及被写入的 'null'/'undefined' 字符串
function readValue (key) {
  const value = storage.getItem(key)
  if (value === null || value === undefined) return ''
  const text = String(value).trim()
  if (!text || text === 'null' || text === 'undefined') return ''
  return text
}

export function getToken (needPrefix = true) {
  // return Cookies.get(Authorization)
  const token = readValue(Authorization)
  //没有token时必须返回空字符串：拼接后 'Bearer null' 是真值，会导致权限守卫误判为已登录
  if (!token) return ''
  return needPrefix ? "Bearer " + token : token
}

export function getUsername () {
  // return Cookies.get(Authorization)
  return readValue(headerUserNameKey)
}


export function setToken (token,userName) {
  // return Cookies.set(Authorization, token, { expires: inFifteenMinutes })
  const value = token === null || token === undefined ? '' : String(token).trim()
  //空token表示登出，直接清除，避免写入 'undefined' 字符串
  if (!value) {
    removeToken()
    return
  }
  if (userName !== null && userName !== undefined && userName !== '') {
    storage.setItem(headerUserNameKey, userName)
  }
  return storage.setItem(Authorization, value)
}

export function removeToken () {
  // return Cookies.remove(Authorization)
  storage.removeItem(headerUserNameKey)
  return storage.removeItem(Authorization)
}

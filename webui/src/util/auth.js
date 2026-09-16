// import Cookies from 'js-cookie'
import website from '@/config/website'
const Authorization = website.Authorization
const headerUserNameKey = website.headerUserNameKey
// var inFifteenMinutes = new Date(new Date().getTime() + website.tokenTime * 1000);

const storage = localStorage

export function getToken (needPrefix = true) {
  // return Cookies.get(Authorization)
  return needPrefix ? "Bearer "+storage.getItem(Authorization) : storage.getItem(Authorization)
}

export function getUsername () {
  // return Cookies.get(Authorization)
  return storage.getItem(headerUserNameKey)
}


export function setToken (token,userName) {
  // return Cookies.set(Authorization, token, { expires: inFifteenMinutes })
  storage.setItem(headerUserNameKey,userName)
  return storage.setItem(Authorization,token)
}

export function removeToken () {
  // return Cookies.remove(Authorization)
  storage.removeItem(headerUserNameKey)
  return storage.removeItem(Authorization)
}
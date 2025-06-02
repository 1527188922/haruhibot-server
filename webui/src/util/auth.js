// import Cookies from 'js-cookie'
import website from '@/config/website'
const Authorization = website.Authorization
const headerUserNameKey = website.headerUserNameKey
// var inFifteenMinutes = new Date(new Date().getTime() + website.tokenTime * 1000);
export function getToken () {
  // return Cookies.get(Authorization)
  return sessionStorage.getItem(Authorization)
}

export function getUsername () {
  // return Cookies.get(Authorization)
  return sessionStorage.getItem(headerUserNameKey)
}


export function setToken (token,userName) {
  // return Cookies.set(Authorization, token, { expires: inFifteenMinutes })
  sessionStorage.setItem(headerUserNameKey,userName)
  return sessionStorage.setItem(Authorization,token)
}

export function removeToken () {
  // return Cookies.remove(Authorization)
  sessionStorage.removeItem(headerUserNameKey)
  return sessionStorage.removeItem(Authorization)
}
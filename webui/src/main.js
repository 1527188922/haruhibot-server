import Vue from 'vue';
import axios from './router/axios';
import VueAxios from 'vue-axios';
import App from './App';
import router from './router/router';
import './permission'; // 权限
import './error'; // 日志
import './cache';//页面缓冲
import store from './store';
import Element from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import AVUE from '@smallwei/avue'
import '@smallwei/avue/lib/index.css'
import i18n from './lang' // Internationalization
import './styles/common.scss';
import basicBlock from './components/basic-block/main'
import basicContainer from './components/basic-container/main'
import crudCommon from '@/mixins/crud.js'
import dayjs from 'dayjs'
import website from '@/config/website'
import './util/directives'
import Print from 'vue-print-nb'
import wsClient, { WS_CLOSE_LOGOUT, WS_EVENT_AUTH_EXPIRED } from '@/api/ws-client'
Vue.use(Print)
window.$crudCommon = crudCommon
Vue.prototype.$dayjs = dayjs
Vue.prototype.website = website;
Vue.config.productionTip = false;
Vue.use(VueAxios, axios)
Vue.use(Element, {
  i18n: (key, value) => i18n.t(key, value)
})
Vue.use(AVUE, {
  i18n: (key, value) => i18n.t(key, value)
})
//注册全局容器
Vue.component('basicContainer', basicContainer)
Vue.component('basicBlock', basicBlock)

//全局WebSocket：登录后建立一条连接，任何页面/组件都通过 this.$ws 注册监听或发送消息
Vue.prototype.$ws = wsClient
wsClient.bootstrap()
//token变化时连接/断开：登录后立即连接，登出(或401清理token)时关闭
store.watch((state) => state.user.token, (token) => {
  if (token) {
    wsClient.connect()
  } else {
    wsClient.close(WS_CLOSE_LOGOUT, 'logout')
  }
})
//服务端判定登录态失效时，走与401一致的退出登录流程
wsClient.on(WS_EVENT_AUTH_EXPIRED, () => {
  store.dispatch('FedLogOut').finally(() => {
    if (router.currentRoute && router.currentRoute.path !== '/login') {
      router.replace({ path: '/login' }).catch(() => {})
    }
  })
})

new Vue({
  router,
  store,
  i18n,
  render: h => h(App)
}).$mount('#app')
//基础路径 注意发布之前要先修改这里
// const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin

const devTarget = 'http://127.0.0.1:8090'
//本地静态资源目录：前端会把后端返回的封面/漫画图片地址换成当前访问站点(src/util/url.js)，
//开发环境这些路径同样需要代理到后端，否则dev(8091)下会404
const localResourcePaths = ['/jmcomic', '/image', '/audio', '/excel', '/video', '/logs']

//这些前缀下真正的静态资源都是带扩展名的文件（封面/图片/音频/视频/日志等），
//不带扩展名的路径则是前端自己的路由：/jmcomic 是JM漫画菜单、/system/logs 是日志页。
//所以只代理"带扩展名的静态资源"，其余交给 dev server（命中 historyApiFallback）。
//否则 F5 刷新前端路由会被代理到后端，后端找不到 index.html 就返回 404
const ASSET_PATH_PATTERN = /\.[a-zA-Z0-9]+$/
const bypassSpaRoute = (req) => {
  const pathname = (req.url || '').split('?')[0]
  if (!ASSET_PATH_PATTERN.test(pathname)) {
    //不带扩展名的都不是静态资源（前端路由不会带扩展名），跳过代理交给 dev server
    return req.url
  }
}

const devProxy = {
  '/api': {
    target: devTarget,
    // target: 'http://115.29.215.124:8090',
    changeOrigin: true,
    // webui 全局WebSocket(/api/webui/ws)需要走代理，缺少ws:true时开发环境永远连不上
    ws: true,
  }
}
localResourcePaths.forEach(path => {
  devProxy[path] = {
    target: devTarget,
    changeOrigin: true,
    bypass: bypassSpaRoute,
  }
})

module.exports = {
  publicPath: process.env.VUE_APP_BASE_URL,
  lintOnSave: true,
  parallel: false,
  productionSourceMap: false,
  // configureWebpack: config => {
  //     if (process.env.NODE_ENV === 'production') {
  //         return {
  //             plugins: [
  //                 new BundleAnalyzerPlugin()
  //             ]
  //         }
  //     }
  // },
  chainWebpack: (config) => {
    const entry = config.entry('app')
    entry
      .add('babel-polyfill')
      .end()
    entry
      .add('classlist-polyfill')
      .end()
    if (process.env.NODE_ENV !== 'production') {
      entry.add('@/mock').end()
    }
  },
  css: {
    extract: { ignoreOrder: true },
    loaderOptions: {
      css: {
        url: {
          filter: url => !url.startsWith('/')
        }
      }
    }
  },
  //代理服务器配置
  devServer: {
    open: true,
    hot: true,
    host: '127.0.0.1',
    port: 8091,
    client: {
      overlay: {
        warnings: false,
        errors: true,
        runtimeErrors: error => {
          //已由axios拦截器统一提示并处理过的业务错误（如401登录过期），不再弹overlay
          if (error && error.handled === true) return false
          //vue-router的导航重定向/取消/重复导航属于预期行为，不是程序异常
          if (error && error._isRouter === true) return false
          const message = error && error.message
          return ![
            'ResizeObserver loop completed with undelivered notifications.',
            'ResizeObserver loop limit exceeded'
          ].includes(message)
        }
      }
    },
    proxy: devProxy
  }
}

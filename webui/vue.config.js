//基础路径 注意发布之前要先修改这里
// const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin

const devTarget = 'http://127.0.0.1:8090'
//本地静态资源目录：前端会把后端返回的封面/漫画图片地址换成当前访问站点(src/util/url.js)，
//开发环境这些路径同样需要代理到后端，否则dev(8091)下会404
const localResourcePaths = ['/jmcomic', '/image', '/audio', '/excel', '/video', '/logs']
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

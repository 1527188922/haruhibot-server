// 基础路径 注意发布之前要先修改这里
// const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin
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
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8091',
        // target: 'http://115.29.215.124:8090',
        changeOrigin: true,
      }
    }
  }
}

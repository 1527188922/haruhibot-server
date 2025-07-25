// 基础路径 注意发布之前要先修改这里
// const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin
module.exports = {
  publicPath: process.env.VUE_APP_BASE_URL,
  lintOnSave: true,
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
    entry
      .add('@/mock')
      .end()
  },
  css: {
    extract: { ignoreOrder: true }
  },
  //代理服务器配置
  devServer: {
    open: true,
    hot: true,
    host: '127.0.0.1',
    port: 8091,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8090',
        // target: 'http://115.29.215.124:8090',
        changeOrigin: true,
      }
    }
  }
}
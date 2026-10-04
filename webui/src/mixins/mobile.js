/**
 * 移动端适配公共 mixin
 *
 * 全站混入（见 main.js），为所有组件提供统一的移动端标记与基础响应式数据：
 * - isMobileView：手机布局（<768px）；isCompactView：紧凑导航（<992px）。
 * - screenWidth / screenHeight：视口尺寸，仅在跨越断点或尺寸变化时更新，供需要像素计算
 *   （如漫画预览的缩放基准宽度）的组件使用。
 *
 * 之所以不沿用 util/validate.js 里的 isMobile(s)，那个是校验手机号的函数，语义完全不同。
 */
import { mapGetters } from 'vuex'
import admin from '@/util/admin'
import { viewportMode, paginationForViewport } from '@/util/mobile-layout'

export default {
  /**
   * 尽早提交一次断点，保证首屏渲染时 isMobileView 就是正确值。
   * 父组件的 mounted 晚于子组件，如果只在那里初始化，首屏会以 screen=-1（非移动端）渲染一次，
   * 造成移动端首屏闪一下桌面布局。
   */
  beforeCreate() {
    if (typeof window === 'undefined') {
      return
    }
    const store = this.$store
    if (!store || store.getters.screen >= 0) {
      return
    }
    store.commit('SET_SCREEN', {
      screen: admin.getScreen(),
      width: admin.getWindowWidth(),
      height: admin.getWindowHeight()
    })
  },
  computed: {
    ...mapGetters(['screen', 'screenWidth', 'screenHeight']),
    isCompactView() {
      return viewportMode(this.screenWidth || admin.getWindowWidth()).compact
    },
    isMobileView() {
      return viewportMode(this.screenWidth || admin.getWindowWidth()).phone
    }
  },
  methods: {
    responsivePagination(options) {
      return paginationForViewport(options, this.isMobileView)
    },
    /**
     * 按当前窗口宽度计算视口宽度，与 admin.getScreen 使用同一套取值方式，
     * 避免 document.body.clientWidth 在滚动条/1px 边框下的偏差。
     */
    measureScreenWidth() {
      return admin.getWindowWidth()
    }
  }
}

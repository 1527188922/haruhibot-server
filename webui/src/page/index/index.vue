<template>
  <div class="avue-contail"
       :style="isMobileView && screenHeight ? {height: screenHeight + 'px'} : null"
       :class="{'avue--collapse':isCollapse,}">
    <screenshot v-if="setting.screenshot"></screenshot>
<!--    <setting></setting>-->
    <!-- 移动端侧边栏遮罩：点空白处收起抽屉 -->
    <div class="avue-sidebar-mask"
         v-if="mobileSidebarOpen"
         @click="closeMobileSidebar"></div>
    <div class="avue-layout"
         :class="{'avue-layout--horizontal':isHorizontal}">
      <div class="avue-sidebar"
           v-show="validSidebar">
        <!-- 左侧导航栏 -->
        <logo />
        <sidebar />
      </div>
      <div class="avue-main">
        <!-- 顶部导航栏 -->
        <top ref="top" />
        <!-- 顶部标签卡 -->
        <tags />
        <transition name="fade-scale">
          <search class="avue-view"
                  v-show="isSearch"></search>
        </transition>
        <!-- 主体视图层：保持视口宽度，表格内部独立横向滚动 -->
        <div style="flex:auto;overflow-y:auto;overflow-x:hidden;"
             id="avue-view"
             v-show="!isSearch">
          <div class="page-table-scroll">
            <keep-alive>
              <router-view class="avue-view"
                           :key="key"
                           v-if="isRefresh" />
            </keep-alive>
          </div>
        </div>
        <div class="avue-footer">
<!--          <p class="copyright">© 2018-2021 Avue designed by smallwei</p>-->
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import { mapGetters } from "vuex";
import tags from "./tags";
import screenshot from './screenshot';
// import setting from './setting';
import search from "./search";
import logo from "./logo";
import top from "./top/";
import sidebar from "./sidebar/";
import admin from "@/util/admin";
import { validatenull } from "@/util/validate";
import index from '@/mixins/index'
export default {
  components: {
    top,
    logo,
    tags,
    search,
    sidebar,
    // setting,
    screenshot
  },
  name: "index",
  mixins: [index],
  provide () {
    return {
      index: this
    };
  },
  data () {
    return {
      //搜索控制
      isSearch: false,
      // 是否已完成首屏初始化：避免初始化过程中 isCompactView 由 false 变 true 时误关侧边栏
      screenReady: false
    };
  },
  mounted () {
    this.init();
  },
  beforeDestroy () {
    window.removeEventListener('resize', this.handleWindowResize);
    window.removeEventListener('orientationchange', this.handleWindowResize);
    if (window.visualViewport) window.visualViewport.removeEventListener('resize', this.handleWindowResize);
  },
  computed: {
    ...mapGetters(["isHorizontal", "setting", "isRefresh", "isCollapse", "menu"]),
    key () {
      return this.$route.path
    },
    validSidebar () {
      return !((this.$route.meta || {}).menu == false || (this.$route.query || {}).menu == 'false')
    },
    // 移动端侧边栏以抽屉形式展开时，显示遮罩以便点击空白处收起
    mobileSidebarOpen () {
      return this.isCompactView && this.isCollapse && this.validSidebar
    }
  },
  watch: {
    /**
     * isCollapse 在桌面端表示"折叠成图标栏"，在移动端却表示"抽屉展开"，
     * 两者共用同一份状态。若在桌面折叠过侧边栏再把窗口缩到手机宽度，
     * 进页面就会莫名其妙弹出抽屉和遮罩。进入移动端形态时统一复位为收起。
     */
    isCompactView (value) {
      if (!this.screenReady) {
        return
      }
      if (value && this.isCollapse) {
        this.$store.commit("SET_COLLAPSE");
      }
    }
  },
  props: [],
  methods: {
    // 屏幕检测：断点变化时才提交，避免拖动窗口/移动端地址栏伸缩导致的高频渲染
    init () {
      this.commitScreen(true);
      window.addEventListener('resize', this.handleWindowResize);
      window.addEventListener('orientationchange', this.handleWindowResize);
      if (window.visualViewport) window.visualViewport.addEventListener('resize', this.handleWindowResize);
      // 首屏渲染完成后再开启跨断点复位逻辑
      this.$nextTick(() => {
        this.screenReady = true;
      });
    },
    handleWindowResize () {
      this.commitScreen(false);
    },
    commitScreen (force) {
      const width = admin.getWindowWidth();
      const height = admin.getWindowHeight();
      if (!force
        && width === this.screenWidth
        && height === this.screenHeight
        && admin.getScreen() === this.screen) {
        return;
      }
      this.$store.commit("SET_SCREEN", {
        screen: admin.getScreen(),
        width,
        height
      });
    },
    // 收起移动端侧边栏抽屉
    closeMobileSidebar () {
      if (this.isCollapse) {
        this.$store.commit("SET_COLLAPSE");
      }
    },
    //打开菜单
    openMenu (item = {}) {
      this.$store.dispatch("GetMenu", item.parentId).then(data => {
        if (data.length !== 0) {
          this.$router.$avueRouter.formatRoutes(data, true);
        }
        //当点击顶部菜单做的事件
        this.handleMenu(item)
      });
    },
    handleMenu(item){
      if (!validatenull(item)) {
        let itemActive = {},
            childItemActive = 0;
        //vue-router路由
        if (item.path) {
          itemActive = item;
        } else {
          if (this.menu[childItemActive].length === 0) {
            itemActive = this.menu[childItemActive];
          } else {
            itemActive = this.menu[childItemActive].children[childItemActive];
          }
        }
        this.$store.commit('SET_MENUID', item);
        this.$router.push({
          path: itemActive.path
        });
      }
    }
  }
};
</script>
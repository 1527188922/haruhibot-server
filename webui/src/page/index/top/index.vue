<template>
  <div class="avue-top">
    <div class="top-bar__left">
      <!--
        侧边栏开关。图标改用 element-ui 自带字体：
        原来的 icon-navicon 依赖 index.html 里 //at.alicdn.com 的外部 iconfont，
        该字体在国内手机网络/拦截规则下经常加载失败，<i> 会渲染成空白，
        导致移动端看不到也点不到菜单入口。el-icon 字体随 element-ui 一起打包，不依赖外网。
      -->
      <div class="avue-breadcrumb"
           v-if="setting.collapse&&!isHorizontal">
        <i class="avue-breadcrumb__icon"
           :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"
           title="展开/收起菜单"
           @click="setCollapse"></i>
      </div>
    </div>
    <div class="top-bar__title">
      <div class="top-bar__item top-bar__item--show"
           v-if="setting.menu">
        <top-menu ref="topMenu"></top-menu>
      </div>
      <span class="top-bar__item"
            v-if="setting.search">
        <top-search></top-search>
      </span>
    </div>
    <div class="top-bar__right">
      <el-tooltip v-if="setting.color"
                  effect="dark"
                  :content="$t('navbar.color')"
                  placement="bottom">
        <div class="top-bar__item">
          <top-color></top-color>
        </div>
      </el-tooltip>
      <el-tooltip v-if="setting.debug"
                  effect="dark"
                  :content="logsFlag?$t('navbar.bug'):logsLen+$t('navbar.bugs')"
                  placement="bottom">
        <div class="top-bar__item">
          <top-logs></top-logs>
        </div>
      </el-tooltip>
      <el-tooltip v-if="setting.lock"
                  effect="dark"
                  :content="$t('navbar.lock')"
                  placement="bottom">
        <div class="top-bar__item">
          <top-lock></top-lock>
        </div>
      </el-tooltip>
      <el-tooltip v-if="setting.theme"
                  effect="dark"
                  :content="$t('navbar.theme')"
                  placement="bottom">
        <div class="top-bar__item top-bar__item--show">
          <top-theme></top-theme>
        </div>
      </el-tooltip>
<!--      <el-tooltip effect="dark"-->
<!--                  :content="$t('navbar.notice')"-->
<!--                  placement="bottom">-->
<!--        <div class="top-bar__item top-bar__item&#45;&#45;show">-->
<!--          <top-notice></top-notice>-->
<!--        </div>-->
<!--      </el-tooltip>-->
<!--      <el-tooltip effect="dark"-->
<!--                  :content="$t('navbar.language')"-->
<!--                  placement="bottom">-->
<!--        <div class="top-bar__item top-bar__item&#45;&#45;show">-->
<!--          <top-lang></top-lang>-->
<!--        </div>-->
<!--      </el-tooltip>-->
      <el-tooltip v-if="setting.fullscren"
                  effect="dark"
                  :content="isFullScren?$t('navbar.screenfullF'):$t('navbar.screenfull')"
                  placement="bottom">
        <div class="top-bar__item">
          <i :class="isFullScren?'icon-tuichuquanping':'icon-quanping'"
             @click="handleScreen"></i>
        </div>
      </el-tooltip>
      <el-tooltip effect="dark"
                  :content="$t('navbar.restart')"
                  placement="bottom">
        <div class="top-bar__item">
          <i class="el-icon-refresh"
             style="font-size: 18px"
             @click="handleRestart"></i>
        </div>
      </el-tooltip>
<!--      <img class="top-bar__img"-->
<!--           :src="userInfo.avatar">-->
      <el-dropdown>
        <span class="el-dropdown-link">
          {{userInfo.username}}
          <i class="el-icon-arrow-down el-icon--right"></i>
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item>
            <router-link to="/">{{$t('navbar.dashboard')}}</router-link>
          </el-dropdown-item>
          <el-dropdown-item>
            <router-link to="/info/index">{{$t('navbar.userinfo')}}</router-link>
          </el-dropdown-item>
          <el-dropdown-item>
            <router-link to="/info/setting">{{$t('navbar.setting')}}</router-link>
          </el-dropdown-item>
          <el-dropdown-item @click.native="logout"
                            divided>{{$t('navbar.logOut')}}</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>
<script>
import { mapGetters } from "vuex";
import { fullscreenToggel, listenfullscreen } from "@/util/util";
import topLock from "./top-lock";
import topMenu from "./top-menu";
import topSearch from "./top-search";
import topTheme from "./top-theme";
import topLogs from "./top-logs";
import topColor from "./top-color";
import topNotice from './top-notice'
import topLang from "./top-lang";
import {restartBot} from "@/api/system";
export default {
  components: {
    topLock,
    topMenu,
    topSearch,
    topTheme,
    topLogs,
    topColor,
    topNotice,
    topLang
  },
  name: "top",
  data () {
    return {};
  },
  filters: {},
  created () { },
  mounted () {
    listenfullscreen(this.setScreen);
  },
  computed: {
    ...mapGetters([
      "setting",
      "userInfo",
      "isFullScren",
      "tagWel",
      "tagList",
      "isCollapse",
      "tag",
      "logsLen",
      "logsFlag",
      "isHorizontal"
    ])
  },
  methods: {
    handleRestart(){
      this.$confirm('确认重启Bot?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(()=>{
        restartBot().then(({data:{code,message}})=>{
          if (code !== 200) {
            return this.$message.error(message)
          }
          this.$message.success(message)
        }).catch(e =>{
          alert(e.toString())
        })
      })
    },
    handleScreen () {
      fullscreenToggel();
    },
    setCollapse () {
      this.$store.commit("SET_COLLAPSE");
    },
    setScreen () {
      this.$store.commit("SET_FULLSCREN");
    },
    logout () {
      this.$confirm(this.$t("logoutTip"), this.$t("tip"), {
        confirmButtonText: this.$t("submitText"),
        cancelButtonText: this.$t("cancelText"),
        type: "warning"
      }).then(() => {
        this.$store.dispatch("LogOut").then(() => {
          this.$router.push({ path: "/login" });
        });
      });
    }
  }
};
</script>

<style lang="scss" scoped>
/**
 * 侧边栏开关按钮。
 * 图标用 element-ui 自带字体，不依赖 index.html 里的外部 iconfont CDN；
 * 字体家族显式声明，避免字体未就绪时把内容当普通文本渲染成方框。
 */
.avue-breadcrumb {
  &__icon {
    cursor: pointer;
    display: inline-block;
    font-family: element-icons !important;
    // 保证足够大的点击区域，便于手机上点按
    padding: 0 2px;
  }
}
</style>
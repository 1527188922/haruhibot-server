<template>
  <div class="right-panel">
    <el-container>
      <el-aside>
        <div class="btn-dev" title="执行">
          <el-button type="text" @click="exec">▶</el-button>
        </div>
        <div class="btn-dev" title="执行选中的" @mousedown.prevent="(e)=>{}">
          <template v-if="selectedText && selectedText.trim().length !== 0">
            <el-button type="text" class="success-text-btn" @click="execSelected">▶</el-button>
          </template>
          <template v-else>
            <el-button type="text" class="info-text-btn" disabled>▶</el-button>
          </template>
        </div>
      </el-aside>
      <el-container>
        <el-main ref="main" :style="{ height: mainHeight + 'px' }">
          <sql-textarea :value-obj="valueObj" @selection-change="handleSelection" @contextmenu="openMenu"
                        @ctrl-enter="handleCtrlEnter"
                        @change="handleSqlChange"></sql-textarea>
        </el-main>

        <!-- 拖动条 -->
        <div class="drag-bar" @mousedown="startDrag" :style="{ height: dragBarHeight + 'px'}"></div>

        <el-footer ref="footer" :style="{ height: footerHeight + 'px' }">
          <result-panel ref="resultPanel"></result-panel>

        </el-footer>
      </el-container>

    </el-container>
    <context-menu ref="contextMenu" min-width="200px" :items="menuItems" @menu-click="handleMenuClick"></context-menu>
  </div>
</template>
<script>
import ContextMenu from "@/components/context-menu.vue";
import SqlTextarea from "./sql-textarea.vue";
import ResultPanel from "./result-panel.vue";
import {execAndExport as execAndExportApi, executeSql as executeSqlApi, getSqlCache, saveSqlCache} from "@/api/database";
import { getStore,setStore } from "@/util/store.js";
import {downloadFileUrl} from "@/api/system";
import {downloadLink} from "@/util/util";

export default {
  components:{
    ContextMenu,
    ResultPanel,
    SqlTextarea,
  },
  data(){
    return{
      dragBarHeight: 5,//拖动条高度
      mainHeight: 400,      // main初始高度 这里初始化无意思
      footerHeight: 400,    // footer初始高度
      minMainHeight: 0,   // main最小高度
      minFooterHeight: 40, // footer最小高度
      isDragging: false,
      valueObj:{value:''},
      selectedText:'',
      menuItems: [
        { text: '▶执行', action: 'execAll' },
        { text: '▶执行选中的', action: 'execSelected',rightText:'Ctrl+Enter'},
        { text: '导出查询结果', action: 'export',icon:'el-icon-download' }
      ]
    }
  },
  mounted() {
    this.mainHeight = this.$refs.main.$el.offsetHeight; // main初始高度
    document.addEventListener('mousemove', this.handleDrag);
    document.addEventListener('mouseup', this.stopDrag);
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.handleDrag);
    document.removeEventListener('mouseup', this.stopDrag);
  },
  created() {
    // this.initContentInLocal()
    this.initContent()
  },
  methods:{
    initContentInLocal(){
      let sql = getStore({name:'sql-cache'})
      this.valueObj.value = sql || ''
    },
    initContent(){
      getSqlCache().then(({data:{data}})=>{
        this.valueObj.value = data || ''
      })
    },
    exec(){
      this.executeSql(this.valueObj.value)
    },
    execSelected(){
      this.executeSql(this.selectedText)
    },
    execAndExport(){
      if(!this.selectedText){
        return this.$message.warning('请选择sql')
      }
      const loading = this.$loading({ lock: true,  text: 'Loading', spinner: 'el-icon-loading'});
      execAndExportApi({sql:this.selectedText}).then(({data:{code,data,message},headers})=>{
        if (code !== 200) {
          return this.$message.error(message)
        }
        const contentDisposition = headers['content-disposition']
        let fileName = 'db_export_'+new Date().getTime()+'.xlsx'
        if (contentDisposition) {
          const fileNameMatch = contentDisposition.match(/filename=(.+)/)
          if (fileNameMatch.length > 1) {
            fileName = decodeURIComponent(fileNameMatch[1].replace(/"/g, ''))
          }
        }
        let h = downloadFileUrl(encodeURI(data))
        downloadLink(h, fileName)
      }).catch(e =>{
        if(e.message){
          return this.$message.error(e.message)
        }
        this.$message.error('导出异常')
      }).finally(()=>{
        loading.close()
      })
    },
    executeSql(sql){
      if(!sql || sql.trim().length === 0){
        return
      }

      const loading = this.$loading({ lock: true,  text: 'Loading', spinner: 'el-icon-loading'});
      executeSqlApi({sql}).then(({data:{code,message,data}})=>{
        if(code !== 200){
          return this.$message.error(message)
        }
        this.$refs.resultPanel.updateResult(data)
      }).catch(e =>{
        if (e.message) {
          this.$message.error(e.message)
        }
      }).finally(()=>{
        loading.close()
      })
    },
    saveSqlToLocal(v){
      setStore({
        name:'sql-cache',
        content:v
      })
    },
    saveSql(v){
      saveSqlCache({sql:v}).then((res)=>{

      })
    },
    handleSqlChange(v){
      // this.saveSqlToLocal(v)
      this.saveSql(v)
    },
    // 右键事件
    openMenu(event){
      this.$refs.contextMenu.open({event,data:null})
    },
    // 右键菜单 点击item事件
    handleMenuClick({ action },data){
      if (action === 'execAll') {
        this.exec()
      }else if(action === 'execSelected'){
        this.execSelected()
      }else if(action === 'export'){
        this.execAndExport()
      }
    },
    prependSql(text){
      let t = this.valueObj.value
      this.valueObj.value = text + t
    },
    handleCtrlEnter(e){
      this.execSelected()
    },
    handleSelection(v){
      this.selectedText = v;
    },
    startDrag(e) {
      this.isDragging = true;
      this.startY = e.clientY;
      this.startMainHeight = this.mainHeight;
      document.body.style.cursor = 'ns-resize';
      document.body.style.userSelect = 'none';
    },
    handleDrag(e) {
      if (!this.isDragging) {
        return
      }
      const deltaY = e.clientY - this.startY;
      const newMainHeight = this.startMainHeight + deltaY;

      // 计算可用空间
      const containerHeight = this.$el.clientHeight - this.dragBarHeight;
      const maxMainHeight = containerHeight - this.minFooterHeight;

      // 应用高度限制
      if (newMainHeight > this.minMainHeight && newMainHeight < maxMainHeight) {
        this.mainHeight = newMainHeight;
        this.footerHeight = containerHeight - newMainHeight;
      }
    },
    stopDrag() {
      this.isDragging = false;
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    }
  }
}
</script>
<style scoped lang="scss">
.right-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  ::v-deep .el-container{
    height: calc(100vh - 110px) !important;
  }

  ::v-deep .el-aside{
    // 减去
    //height: calc(100vh - 110px) !important;
    width: 30px !important;
    padding: 5px;
    text-align: center;
    border-top: 1px solid #DCDFE6;
    border-left: 1px solid #DCDFE6;
    border-bottom: 1px solid #DCDFE6;
    .btn-dev{
      .el-button{
        padding-top: 10px;
        padding-bottom: 10px;
        span{
          font-size: 20px;
        }
      }
    }
    .btn-dev:not(:last-child) {
      border-bottom: 1px solid #DCDFE6;
    }

  }

  ::v-deep .el-footer{
    border:  1px solid #DCDFE6;
    padding: 0;
  }


  .drag-bar {
    background-color: #f0f2f5;
    cursor: ns-resize;
    position: relative;
    z-index: 1;
    transition: background 0.3s;
  }
  .drag-bar:hover {
    background: #409EFF;
  }

}
</style>
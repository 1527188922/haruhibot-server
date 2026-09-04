<template>
  <div class="result-panel" ref="resultPanelDiv">
    <template v-if="results && results.length > 0">
      <el-tabs v-model="activeName" type="border-card" closable @tab-remove="handleTabRemove"
               ref="resultTab"
      @tab-click="tabClick">
        <el-tab-pane :key="item.name" v-for="(item, index) in results" :name="item.name">
          <span slot="label" :title="item.type" @contextmenu.prevent="(e)=>openMenu(e,item)">
<!--            hover click 只有query展示popover-->
            <el-popover placement="top-start" trigger="click"
                        :disabled="!isQuery(item)"
                        popper-class="query-result-sql-popover">
              <div>{{`获取行数：${item.count}`}}</div>
              <div v-if="item.cost || item.cost === 0">{{`耗时：${item.cost}ms`}}</div>
              <pre>{{item.sql}}</pre>
              <span slot="reference">
                <i :class="item.type === 'ERROR' ? 'el-icon-error danger-text' : 'el-icon-success success-text'"></i>
                {{ item.title }}
              </span>
          </el-popover>
          </span>
          <template v-if="isQuery(item)">
            <el-table :data="item.rows" border size="small" :fit="false" :key="tableKey"
                      ref="dataTable"
                      :height="`${tableHeight}px`"
                      highlight-current-row>
              <!-- 动态表头 -->
              <el-table-column  v-for="(field, index) in item.tableHeaders" sortable
                  :key="index"
                  show-overflow-tooltip
                  :label="field"
                  :prop="field">
                <template slot-scope="scope">
                  <span v-if="scope.row[field] !== null">
                    {{ scope.row[field] }}
                  </span>
                  <span v-else class="null-value">
                    NULL
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </template>
          <template v-else>
            <div class="update-result">
              <el-form :model="item" label-width="80px">
                <el-form-item label="影响行数：" v-if="item.data || item.data === 0">
                  {{item.data}}
                </el-form-item>
                <el-form-item label="耗时：" v-if="item.cost || item.cost === 0">
                  {{item.cost}}ms
                </el-form-item>
                <el-form-item label="错误：" v-if="item.errorMessage">
                  {{item.errorMessage}}
                </el-form-item>
                <el-form-item label="SQL：" v-if="item.sql">
                  <pre>{{item.sql}}</pre>
                </el-form-item>
              </el-form>
            </div>
          </template>
        </el-tab-pane>
      </el-tabs>
    </template>
    <context-menu ref="contextMenu" :items="menuItems" @menu-click="handleMenuClick"></context-menu>
  </div>
</template>
<script>
import ContextMenu from "@/components/context-menu.vue";
import {execAndExport} from "@/api/database";
import {downloadFileUrl} from "@/api/system";
import {downloadLink} from "@/util/util";

export default {
  components: {
    ContextMenu
  },
  data(){
    return{
      results:[],
      activeName:'',
      currentClickTabName:'',
      observer:null,
      tableHeight:0,
      tableKey:0,
      tableUpdated:false,
      menuItems:[{ text: '导出结果', action: 'export',icon:'el-icon-download' }]
    }
  },
  computed:{
  },
  mounted() {
   this.initObserver()
  },
  beforeDestroy() {
    this.disconnect()
  },
  methods:{
    openMenu(event,item){
      if(this.isQuery(item)){
        this.$refs.contextMenu.open({event,data:item})
      }
    },
    // 右键菜单 点击item事件
    handleMenuClick({ action },data){
      if(action === 'export'){
        this.exportResult(data)
      }
    },
    exportResult(data){
      if(!data || data.length === 0){
        return this.$message.warning('无数据')
      }
      const loading = this.$loading({ lock: true,  text: 'Loading', spinner: 'el-icon-loading'});
      execAndExport({
        data
      }).then(({data:{code,data,message},headers})=>{
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
    getResultPanelHeight(){
      return this.$refs.resultPanelDiv.offsetHeight
    },
    getResultPanelWidth(){
      return this.$refs.resultPanelDiv.offsetWidth
    },
    getCurrentTable(){
      if(!this.results || this.results.length === 0 || !this.$refs.dataTable
      || this.$refs.dataTable.length === 0 || !this.activeName){
        return null
      }
      let queryResult = this.results.filter(e => this.isQuery(e.type))
      let i = queryResult.findIndex(e => e.name === this.activeName)
      return this.$refs.dataTable[i]
    },
    initObserver(){
      this.observer = new ResizeObserver(entries => {
        for (let entry of entries) {
          let newHeight = entry.contentRect.height
          let newWidth = entry.contentRect.width
          this.handlePanelResize(newHeight,newWidth)
        }
      })
      this.observer.observe(this.$refs.resultPanelDiv)
    },
    disconnect(){
      if (this.observer) {
        this.observer.disconnect()
        this.observer = null
      }
    },
    handlePanelResize(height,width){
      let ts = this.$refs.resultTab
      let tableHeaderHeight = 0
      if(ts){
        let headerEl = ts.$el.querySelector('.el-tabs__header')
        tableHeaderHeight = headerEl.offsetHeight
      }
      this.tableHeight = height - tableHeaderHeight
      let t = this.getCurrentTable()
      if(t){
        let tHeaderWidth = t.$el.querySelector('.el-table__header').offsetWidth
        if(width < tHeaderWidth && !this.tableUpdated){
          this.tableKey++
          this.tableUpdated = true
        }
      }
    },
    tabClick(v){
      if(this.currentClickTabName === v.name){
        return
      }
      this.currentClickTabName = v.name
      this.tableKey++
    },
    handleTabRemove(name){
      let index = this.results.findIndex(e => e.name === name)
      this.results.splice(index,1)
    },
    updateResult(v){
      this.results = this.handleResult(v)
      this.$nextTick(()=>{
        this.handlePanelResize(this.getResultPanelHeight(),this.getResultPanelWidth())
        this.tableKey++
        this.tableUpdated = false
      })
    },
    handleResult(v){
      if(!v || v.length === 0){
        return [];
      }
      let list = v.map((e,i) => {
        let {type,data} = e;
        let obj = {
          name:type + 'name'+i,
          title:`结果(${i+1})`,//tab展示的title
          ...e
        };
        if(this.isQuery(type)){
          obj['tableHeaders'] = data && data.length > 0 ? this.dynamicHeaders(data) : [];
          let rows = data && data.length > 1 ? this.formatTableData(data) : [];
          obj['rows'] = rows;
          obj['count'] = rows.length;
        }
        return obj
      })
      this.activeName = list[0].name
      this.currentClickTabName = this.activeName
      return list
    },
    isQuery(item) {
      if(typeof item === 'string'){
        return item === 'QUERY'
      }
      return item.type === 'QUERY'
    },
    formatTableData(data){
      return data.slice(1).map(item => {
        const obj = {};
        this.dynamicHeaders(data).forEach((header, index) => {
          obj[header] = item[index];
        });
        return obj;
      });
    },
    dynamicHeaders(data) {
      return data[0]; // 第一行作为表头
    },
  }
}
</script>
<style lang="scss">
.query-result-sql-popover{
  padding: 10px;
  min-width: 200px;
  max-width: 500px;
  pre{
    margin: 0;
  }
}
</style>
<style scoped lang="scss">
.result-panel{
  width: 100%;
  height: 100%;
  ::v-deep .el-tabs{
    border: none !important;
    box-shadow: none !important;
    .el-tabs__content{
      padding: 0;
    }
  }
  .null-value{
    color: #C0C4CC;
  }

  .update-result{
    padding: 5px 10px 5px 10px;
    ::v-deep .el-form-item{
      margin-bottom: 0;

      .el-form-item__label{
        padding-right: 0;
        line-height: 30px;
        height: 30px;
        font-weight: bold;
      }

      .el-form-item__content{
        line-height: 30px;
        min-height: 30px;
        height: auto;
        pre{
          margin: 0;
        }
      }
    }
  }

}

</style>
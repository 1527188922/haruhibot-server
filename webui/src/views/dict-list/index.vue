<template>
  <div id="DictList">
    <basic-container>
      <el-row>
        <el-form :model="queryFormObj" label-width="60px" inline ref="queryForm" size="small">
          <el-form-item label="key" prop="key">
            <el-input v-model="queryFormObj.key" class="form-input" maxlength="255" clearable></el-input>
          </el-form-item>
          <el-form-item label="value" prop="content">
            <el-input v-model="queryFormObj.content" class="form-input" maxlength="255" clearable></el-input>
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="queryFormObj.remark" class="form-input" maxlength="500" clearable></el-input>
          </el-form-item>
        </el-form>
      </el-row>
      <el-row class="query-form-option-buts">
        <el-button type="primary" size="small" @click="search" plain
                   icon="el-icon-search">查询</el-button>
        <!--        <el-button type="primary" size="small" @click="exportAsExcel" :loading="exportLoading" plain>导出</el-button>-->
        <el-button type="primary" size="small" @click="resetQueryForm" plain
                   icon="el-icon-refresh-right">重置</el-button>
      </el-row>
    </basic-container>


    <basic-container>
      <div class="data-table-option-buts">
        <el-button @click="refreshCache" type="primary" size="small" plain
                   icon="el-icon-refresh" :loading="refreshLoading">刷新缓存</el-button>
      </div>
      <el-table tooltip-effect="light" :data="tableData" v-loading="tableLoading" border
                stripe max-height="800" size="small" ref="dataTable" highlight-current-row >
        <el-table-column fixed label="序号" width="45" align="center">
          <template slot-scope="scope">{{scope.$index+1}}</template>
        </el-table-column>
        <el-table-column label="key" prop="key" min-width="190" show-tooltip-when-overflow sortable></el-table-column>
        <el-table-column label="value" prop="content" min-width="190" show-tooltip-when-overflow ></el-table-column>
        <el-table-column label="备注" prop="remark" min-width="200" ></el-table-column>
        <el-table-column label="修改时间" prop="modifyTime" min-width="140" align="center" show-tooltip-when-overflow/>
        <el-table-column label="创建时间" prop="createTime" min-width="140" align="center" show-tooltip-when-overflow/>
      </el-table>
      <div class="pagination-box">
        <el-pagination v-bind="pagination" @size-change="sizeChange" @current-change="currentChange" />
      </div>
    </basic-container>
  </div>
</template>
<script>
import {search as searchApi,refresh as refreshApi} from "@/api/dictionary";

export default {
  name:'DictList',
  components:{
  },
  data(){
    return{
      tableLoading:false,
      exportLoading:false,
      refreshLoading:false,
      queryFormObj:{
        key:'',
        content:'',
        remark:''
      },
      tableData:[],
      pagination:{
        currentPage: 1,
        pageSizes: [5, 10, 30, 50, 100, 500],
        pageSize: 10,
        layout: 'total, sizes, prev, pager, next, jumper',
        background: true,
        total: 0
      },
    }
  },
  created() {

  },
  mounted() {
    this.search()
  },
  methods:{
    refreshCache(){
      this.refreshLoading = true
      refreshApi().then(({data:{code,message}})=>{
        if(code !== 200){
          return this.$message.error(message)
        }
        this.$message.success(message)
      }).finally(()=>{
        this.refreshLoading = false
      })
    },
    search(){
      this.pagination.currentPage = 1
      this.selectTableData()
    },
    exportAsExcel(){

    },
    resetQueryForm(){
      this.$refs.queryForm.resetFields()
    },
    sizeChange(v){
      this.pagination.pageSize = v
      this.selectTableData()
    },
    currentChange(v){
      this.pagination.currentPage = v
      this.selectTableData()
    },
    selectTableData(){
      this.tableLoading = true
      searchApi({
        ...this.queryFormObj,
        currentPage:this.pagination.currentPage,
        pageSize:this.pagination.pageSize
      }).then(({data:{data}})=>{
        this.tableData = data.records || []
        this.pagination.total = data.total
      }).finally(()=>{
        this.tableLoading = false
      })
    }
  }
}
</script>
<style lang="scss" scoped>
#DictList{

}
</style>
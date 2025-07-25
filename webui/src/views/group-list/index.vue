<template>
  <div id="GroupList">
    <basic-container>
      <el-row>
        <el-form :model="queryFormObj" label-width="60px" inline ref="queryForm" size="small">
          <el-form-item label="机器人" prop="selfId">
            <number-input v-model.trim="queryFormObj.selfId" class="form-input" maxlength="20" clearable
                          placeholder="机器人QQ"></number-input>
          </el-form-item>
          <el-form-item label="群号" prop="groupId">
            <number-input v-model.trim="queryFormObj.groupId" class="form-input" maxlength="20" clearable></number-input>
          </el-form-item>
          <el-form-item label="群名" prop="groupName">
            <el-input v-model="queryFormObj.groupName" class="form-input" maxlength="60" clearable></el-input>
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
                   icon="el-icon-refresh" :loading="refreshLoading">刷新群聊</el-button>
      </div>
      <el-table tooltip-effect="light" :data="tableData" v-loading="tableLoading" border
                stripe max-height="800" size="small" ref="dataTable" highlight-current-row >
        <el-table-column fixed label="序号" width="45" align="center">
          <template slot-scope="scope">{{scope.$index+1}}</template>
        </el-table-column>
        <el-table-column label="群号" prop="groupId" min-width="190" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <div class="group-cell">
              <el-row :title="`群号：${row.groupId}`">
                {{row.groupId}}
              </el-row>
              <el-row :title="`群名：${row.groupName}`">
                {{row.groupName}}
              </el-row>
            </div>

          </template>
        </el-table-column>
        <el-table-column label="机器人QQ" prop="selfId" min-width="130" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <div class="face-and-id">
              <img :src="row.selfAvatarUrl">
              {{row.selfId}}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="群员数量" prop="memberCount" min-width="100" align="center" show-tooltip-when-overflow/>
        <el-table-column label="最大群员数量" prop="maxMemberCount" min-width="100" align="center" show-tooltip-when-overflow/>
        <el-table-column label="群等级" prop="groupLevel" min-width="80" align="center" show-tooltip-when-overflow/>
        <el-table-column label="建群时间" prop="groupCreateTime" min-width="140" align="center" show-tooltip-when-overflow/>
        <el-table-column label="GroupAllShut" prop="groupAllShut" min-width="110" align="center" show-tooltip-when-overflow/>
        <el-table-column label="GroupRemark" prop="groupRemark" min-width="110" align="center" show-tooltip-when-overflow/>
        <el-table-column label="GroupMemo" prop="groupMemo" min-width="100" align="center" show-tooltip-when-overflow/>
      </el-table>
      <div class="pagination-box">
        <el-pagination v-bind="pagination" @size-change="sizeChange" @current-change="currentChange" />
      </div>
    </basic-container>
    <refresh-result-dialog ref="refreshResultDialog"/>
  </div>
</template>
<script>
import numberInput from "@/components/input/numberInput.vue"
import RefreshResultDialog  from "./refresh-result-dialog";
import {search as searchApi,refresh as refreshApi} from "@/api/group";

export default {
  name:'GroupList',
  components:{
    numberInput,
    RefreshResultDialog
  },
  data(){
    return{
      tableLoading:false,
      exportLoading:false,
      refreshLoading:false,
      queryFormObj:{
        selfId:'',
        groupId:'',
        groupName:''
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
      refreshApi().then(({data:{data,code,message}})=>{
        if(code !== 200){
          return this.$message.error(message)
        }
        this.$refs.refreshResultDialog.open(data)
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
#GroupList{

}
</style>
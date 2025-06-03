<template>
  <div id="ChatRecord">
    <basic-container>
      <el-row>
        <el-form :model="queryFormObj" label-width="70px" inline ref="queryForm" size="small">
          <el-form-item label="消息类型" prop="messageType">
            <el-select v-model="queryFormObj.messageType" class="form-input" clearable>
              <el-option v-for="(value,key) in typeMap" :key="key" :value="key" :label="value"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="QQ号" prop="userId">
            <number-input v-model.trim="queryFormObj.userId" class="form-input" clearable></number-input>
          </el-form-item>
          <el-form-item label="群号" prop="groupId">
            <number-input v-model.trim="queryFormObj.groupId" class="form-input" clearable></number-input>
          </el-form-item>
          <el-form-item label="消息内容" prop="content">
            <el-input v-model="queryFormObj.content" class="form-input" maxlength="1000" clearable></el-input>
          </el-form-item>
          <el-form-item label="QQ昵称" prop="nickName">
            <el-input v-model="queryFormObj.nickName" class="form-input" maxlength="30" clearable></el-input>
          </el-form-item>
          <el-form-item label="群内昵称" prop="card">
            <el-input v-model="queryFormObj.card" class="form-input" maxlength="30" clearable></el-input>
          </el-form-item>
        </el-form>
      </el-row>
      <el-row class="query-form-option-buts">
        <el-button type="primary" size="small" @click="search" plain>查询</el-button>
<!--        <el-button type="primary" size="small" @click="exportAsExcel" :loading="exportLoading" plain>导出</el-button>-->
        <el-button type="primary" size="small" @click="resetQueryForm" plain>重置</el-button>
      </el-row>
    </basic-container>
    <basic-container>
      <el-table tooltip-effect="light" :data="tableData" v-loading="tableLoading" border
                stripe max-height="800" size="small" ref="dataTable" highlight-current-row >
        <el-table-column fixed label="序号" width="45" align="center">
          <template slot-scope="scope">{{scope.$index+1}}</template>
        </el-table-column>
        <el-table-column label="消息" prop="content" min-width="90" align="center" show-tooltip-when-overflow />
        <el-table-column label="QQ号" prop="userId" min-width="90" align="center" show-tooltip-when-overflow />
        <el-table-column label="群号" prop="groupId" min-width="90" align="center" show-tooltip-when-overflow />
        <el-table-column label="QQ昵称" prop="nickname" min-width="90" align="center" show-tooltip-when-overflow />
        <el-table-column label="群内昵称" prop="card" min-width="90" align="center" show-tooltip-when-overflow />
        <el-table-column label="消息类型" prop="messageType" min-width="90" align="center" show-tooltip-when-overflow :formatter="(row)=>{return typeMap[row.messageType]}"/>
      </el-table>
      <div class="pagination-box">
        <el-pagination v-bind="pagination" @size-change="sizeChange" @current-change="currentChange" />
      </div>
    </basic-container>
  </div>
</template>
<script>
import numberInput from "@/components/input/numberInput.vue"
import {search as searchApi} from "@/api/chat-record";

export default {
  name:'ChatRecord',
  components:{
    numberInput
  },
  data(){
    return{
      tableLoading:false,
      exportLoading:false,
      queryFormObj:{
        content:'',
        messageType:'',
        userId:'',
        groupId:'',
        nickName:'',
        card:''
      },
      typeList:[{
        value:'private',
        label:'私聊'
      },{
        value:'group',
        label:'群聊'
      }],
      typeMap:{
        private:'私聊',
        group:'群聊'
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
      }).then(({data:{data,code,message}})=>{
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
#ChatRecord{

}
</style>
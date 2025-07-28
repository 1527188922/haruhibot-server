<template>
  <div id="WordStrip">
    <basic-container>
      <el-row>
        <el-form :model="queryFormObj" label-width="70px" inline ref="queryForm" size="small">
          <el-form-item label="关键字" prop="keyWord">
            <el-input v-model="queryFormObj.keyWord" class="form-input" maxlength="500" clearable></el-input>
          </el-form-item>
          <el-form-item label="回复内容" prop="answer">
            <el-input v-model="queryFormObj.answer" class="form-input" maxlength="500" clearable></el-input>
          </el-form-item>
          <el-form-item label="QQ号" prop="userId">
            <number-input v-model.trim="queryFormObj.userId" class="form-input" clearable
                          placeholder="创建人QQ号"></number-input>
          </el-form-item>
          <el-form-item label="群号" prop="groupId">
            <number-input v-model.trim="queryFormObj.groupId" class="form-input" clearable></number-input>
          </el-form-item>
          <el-form-item label="机器人" prop="selfId">
            <number-input v-model.trim="queryFormObj.selfId" class="form-input" clearable
                          placeholder="机器人QQ号"></number-input>
          </el-form-item>
        </el-form>
      </el-row>
      <el-row class="query-form-option-buts">
        <el-button type="primary" size="small" @click="search" plain
                   icon="el-icon-search">查询</el-button>
        <el-button type="primary" size="small" @click="resetQueryForm" plain
                   icon="el-icon-refresh-right">重置</el-button>
      </el-row>
    </basic-container>
    <basic-container>
      <div class="data-table-option-buts">
        <el-button @click="refreshCache" type="primary" size="small" plain
                   icon="el-icon-refresh">刷新缓存</el-button>
        <el-button @click="deleteData" type="danger" size="small" plain
                   :disabled="deleteBatchDisabled"
                   icon="el-icon-delete">删除</el-button>
      </div>
      <el-table tooltip-effect="light" :data="tableData" v-loading="tableLoading" border
                stripe max-height="800" size="small" ref="dataTable" highlight-current-row
                @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column fixed label="序号" width="45" align="center">
          <template slot-scope="scope">{{scope.$index+1}}</template>
        </el-table-column>
        <el-table-column label="关键字" prop="keyWord" min-width="100"></el-table-column>
        <el-table-column label="回复内容" prop="answer" min-width="200" show-tooltip-when-overflow>
          <template slot-scope="{row}">
            <span class="chat-content" @click="view(row)">
              {{row.answer}}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建人QQ" prop="userId" min-width="130" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <div class="face-and-id">
              <img :src="row.userAvatarUrl">
              {{row.userId}}
            </div>

          </template>
        </el-table-column>
        <el-table-column label="群号" prop="groupId" min-width="90" align="center" show-tooltip-when-overflow />
        <el-table-column label="机器人QQ" prop="selfId" min-width="130" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <div class="face-and-id">
              <img :src="row.selfAvatarUrl">
              {{row.selfId}}
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-box">
        <el-pagination v-bind="pagination" @size-change="sizeChange" @current-change="currentChange" />
      </div>
    </basic-container>
    <chat-view ref="chatView"></chat-view>
  </div>
</template>

<script>
import numberInput from "@/components/input/numberInput.vue";
import ChatView from "@/components/dialog/chat-view";
import {search as searchApi,refresh,deleteBatch} from "@/api/word-strip";

export default {
  name:'WordStrip',
  components: {
    numberInput,
    ChatView
  },
  data(){
    return{
      tableLoading:false,
      refreshLoading:false,
      queryFormObj:{
        keyWord:'',
        userId:'',
        groupId:'',
        answer:'',
        selfId:''
      },
      tableData:[],
      multipleSelection: [],
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
  computed:{
    deleteBatchDisabled(){
      return !this.multipleSelection || this.multipleSelection.length === 0
    }
  },
  mounted() {
    this.search()
  },
  methods:{
    handleSelectionChange(val){
      this.multipleSelection = val
    },
    deleteData(){
      if(this.deleteBatchDisabled){
        return
      }
      this.$confirm('确认删除？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(()=>{
        deleteBatch(this.multipleSelection).then(({data:{code,message}})=>{
          if(code !== 200){
            return this.$message.error(message)
          }
          this.search()
          this.$message.success(message)
        }).finally(()=>{
        })
      })
    },
    refreshCache(){
      this.$confirm('确认刷新缓存吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(()=>{
        this.refreshLoading = true
        refresh().then(({data:{code,message}})=>{
          if(code !== 200){
            return this.$message.error(message)
          }
          this.$message.success(message)
        }).finally(()=>{
          this.refreshLoading = false
        })
      })

    },
    view(row){
      this.$refs.chatView.open(row.answer,`${row.userId}`,row.userAvatarUrl)
    },
    search(){
      this.pagination.currentPage = 1
      this.selectTableData()
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
<template>
  <div id="ChatRecord">
    <basic-container>
      <el-row>
        <el-form :model="queryFormObj" label-width="70px" inline ref="queryForm" size="small">
          <el-form-item label="消息内容" prop="content">
            <el-input v-model="queryFormObj.content" class="form-input" maxlength="1000" clearable></el-input>
          </el-form-item>
          <el-form-item label="发送人" prop="userId">
            <number-input v-model.trim="queryFormObj.userId" class="form-input" maxlength="20" clearable
                          placeholder="消息发送人QQ"></number-input>
          </el-form-item>
          <el-form-item label="群号" prop="groupId">
            <number-input v-model.trim="queryFormObj.groupId" class="form-input" maxlength="20" clearable></number-input>
          </el-form-item>
          <el-form-item label="消息类型" prop="messageType">
            <el-select v-model="queryFormObj.messageType" class="form-input" clearable>
              <el-option v-for="(value,key) in typeMap" :key="key" :value="key" :label="value">
                <span :class="key === 'private' ? 'danger-text' : ''">{{ value }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="QQ昵称" prop="nickName">
            <el-input v-model="queryFormObj.nickName" class="form-input" maxlength="30" clearable
                      placeholder="消息发送人QQ昵称"></el-input>
          </el-form-item>
          <el-form-item label="群内昵称" prop="card">
            <el-input v-model="queryFormObj.card" class="form-input" maxlength="30" clearable
                      placeholder="消息发送人群内昵称"></el-input>
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
      <el-table tooltip-effect="light" :data="tableData" v-loading="tableLoading" border
                stripe max-height="800" size="small" ref="dataTable" highlight-current-row >
        <el-table-column fixed label="序号" width="45" align="center">
          <template slot-scope="scope">{{scope.$index+1}}</template>
        </el-table-column>
        <el-table-column label="消息" prop="content" min-width="300">
          <template slot-scope="{row}">
            <span class="chat-content" @click="view(row)">
              {{row.content}}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="发送人" prop="userId" min-width="130" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <div class="face-and-id">
              <img :src="row.userAvatarUrl">
              <div>
                <el-row :title="`QQ：${row.userId}`">
                  {{row.userId}}
                </el-row>
                <el-row :title="`QQ昵称：${row.nickname}`">
                  {{row.nickname}}
                </el-row>
              </div>
            </div>

          </template>
        </el-table-column>
        <el-table-column label="发送人群昵称" prop="card" min-width="100" align="center" />
        <el-table-column label="消息类型" prop="messageType" min-width="70" align="center" show-tooltip-when-overflow>
          <template slot-scope="{row}">
            <span :class="row.messageType === 'private' ? 'danger-text' : ''">
              {{typeMap[row.messageType]}}
            </span>
          </template>
        </el-table-column>
<!--        <el-table-column label="群号" prop="groupId" min-width="90" align="center" show-tooltip-when-overflow />-->
        <el-table-column label="群号" prop="groupId" min-width="90" align="center" show-tooltip-when-overflow >
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
        <el-table-column label="发送时间" prop="time" min-width="140" align="center" show-tooltip-when-overflow/>
        <el-table-column label="操作" width="100" align="center" >
          <template slot-scope="{row}">
            <el-button type="text" size="small" @click="showRaw(row)">原始报文</el-button>
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
import ChatView from "@/components/dialog/chat-view";
import numberInput from "@/components/input/numberInput.vue"
import {search as searchApi,selectExtend} from "@/api/chat-record";

export default {
  name:'ChatRecord',
  components:{
    numberInput,
    ChatView
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
        pageSize: 5,
        layout: 'total, sizes, prev, pager, next, jumper',
        background: true,
        total: 0
      },
    }
  },
  created() {

  },
  mounted() {
    // this.search()
  },
  methods:{
    search(){
      this.pagination.currentPage = 1
      this.selectTableData()
    },
    showRaw(row){
      selectExtend(row).then(({data:{data}})=>{
        this.$alert(data.rawWsMessage, {
          customClass:"raw-message-alert",
          confirmButtonText:'关闭'
        })
        console.log(JSON.parse(data.rawWsMessage))
      }).catch(e =>{
        alert(e.toString())
      })
    },
    view(row){
      this.$refs.chatView.open(row.content,`${row.userId}`,row.userAvatarUrl)
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
#ChatRecord{
}
</style>
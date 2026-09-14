<template>
  <div id="GroupList">
    <basic-container>
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="群列表" name="group">
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
          <el-row class="query-form-option-buts">
            <el-button type="primary" size="small" @click="search" plain
                       icon="el-icon-search">查询</el-button>
            <el-button type="primary" size="small" @click="resetQueryForm" plain
            icon="el-icon-refresh-right">重置</el-button>
          </el-row>
        </el-tab-pane>
        <el-tab-pane label="群成员" name="member">
          <el-form :model="memberQueryFormObj" label-width="60px" inline ref="memberQueryForm" size="small">
            <el-form-item label="群号" prop="groupId">
              <number-input v-model.trim="memberQueryFormObj.groupId" class="form-input" maxlength="20" clearable></number-input>
            </el-form-item>
            <el-form-item label="QQ" prop="userId">
              <number-input v-model.trim="memberQueryFormObj.userId" class="form-input" maxlength="20" clearable></number-input>
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="memberQueryFormObj.nickname" class="form-input" maxlength="60" clearable></el-input>
            </el-form-item>
            <el-form-item label="群昵称" prop="card">
              <el-input v-model="memberQueryFormObj.card" class="form-input" maxlength="60" clearable></el-input>
            </el-form-item>
            <el-form-item label="状态" prop="leftFlag">
              <el-select v-model="memberQueryFormObj.leftFlag" class="form-input" clearable placeholder="全部">
                <el-option label="在群" :value="0"></el-option>
                <el-option label="已离群" :value="1"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="机器人" prop="selfId">
              <number-input v-model.trim="memberQueryFormObj.selfId" class="form-input" maxlength="20" clearable
                            placeholder="机器人QQ"></number-input>
            </el-form-item>
          </el-form>
          <el-row class="query-form-option-buts">
            <el-button type="primary" size="small" @click="searchMember" plain
                       icon="el-icon-search">查询</el-button>
            <el-button type="primary" size="small" @click="resetMemberQueryForm" plain
            icon="el-icon-refresh-right">重置</el-button>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </basic-container>


    <basic-container v-if="activeTab === 'group'">
      <div class="data-table-option-buts">
        <el-button @click="refreshCache" type="primary" size="small" plain
                   icon="el-icon-refresh" :loading="refreshLoading">刷新群聊</el-button>
      </div>
      <el-table tooltip-effect="light" :data="tableData" v-loading="tableLoading" border
                stripe max-height="800" size="small" ref="dataTable" highlight-current-row >
        <el-table-column fixed label="序号" width="45" align="center">
          <template slot-scope="scope">{{scope.$index+1}}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center" fixed>
          <template slot-scope="{row}">
            <el-button type="text" size="small" @click="showMemberList(row)">查看群员</el-button>
            <el-button type="text" size="small" @click="showUserList(row)">发言人列表</el-button>
            <el-button type="text" class="danger-text" size="small" :loading="isMemberRefreshing(row)"
                       @click="refreshGroupMember(row)">刷新成员</el-button>
          </template>
        </el-table-column>
        <el-table-column label="群号" prop="groupId" min-width="190" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <multi-cell :text-list="[row.groupId,row.groupName]"
                        :title-list="[`群号：${row.groupId}`,`群名称：${row.groupName}`]"
                        :image-url="row.groupAvatarUrl"></multi-cell>
          </template>
        </el-table-column>
        <el-table-column label="机器人QQ" prop="selfId" min-width="130" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <multi-cell :image-url="row.selfAvatarUrl" :text-list="[row.selfId]"
                        :title-list="[`QQ：${row.selfId}`]"></multi-cell>
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

    <basic-container v-if="activeTab === 'member'">
      <div class="data-table-option-buts">
      </div>
      <el-table tooltip-effect="light" :data="memberTableData" v-loading="memberTableLoading" border
                stripe max-height="800" size="small" ref="memberDataTable" highlight-current-row >
        <el-table-column fixed label="序号" width="45" align="center">
          <template slot-scope="scope">{{scope.$index+1}}</template>
        </el-table-column>
        <el-table-column label="状态" prop="leftFlag" width="80" align="center" fixed>
          <template slot-scope="{row}">
            <el-tag size="mini" :type="row.leftFlag === 1 ? 'danger' : 'success'">{{row.leftFlag === 1 ? '已离群' : '在群'}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="群成员" prop="userId" min-width="170" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <multi-cell :image-url="row.userAvatarUrl" :text-list="[row.userId,row.card || row.nickname]"></multi-cell>
          </template>
        </el-table-column>
<!--        <el-table-column label="群昵称" prop="card" min-width="110" align="center" show-tooltip-when-overflow/>-->
        <el-table-column label="身份" prop="role" min-width="80" align="center" show-tooltip-when-overflow>
          <template slot-scope="{row}">
            <el-tag v-if="row.role === 'owner'" type="warning" size="small" effect="dark">{{formatRole(row.role)}}</el-tag>
            <el-tag v-if="row.role === 'admin'" type="success" size="small" effect="dark">{{formatRole(row.role)}}</el-tag>
            <el-tag v-if="row.role === 'member'" type="info" size="small" effect="dark">{{formatRole(row.role)}}</el-tag>
            </template>
        </el-table-column>
<!--        <el-table-column label="性别" prop="sex" min-width="60" align="center" show-tooltip-when-overflow>-->
<!--          <template slot-scope="{row}">{{formatSex(row.sex)}}</template>-->
<!--        </el-table-column>-->
        <el-table-column label="等级" prop="level" min-width="60" align="center" show-tooltip-when-overflow/>
        <el-table-column label="入群时间" prop="joinTime" min-width="150" align="center" show-tooltip-when-overflow>
          <template slot-scope="{row}">{{row.formattedJoinTime}}</template>
        </el-table-column>
        <el-table-column label="最近发言时间" prop="lastSentTime" min-width="150" align="center" show-tooltip-when-overflow>
          <template slot-scope="{row}">{{row.formattedLastSentTime}}</template>
        </el-table-column>
<!--        <el-table-column label="名片可修改" prop="cardChangeable" min-width="100" align="center" show-tooltip-when-overflow>-->
<!--          <template slot-scope="{row}">{{formatBool(row.cardChangeable)}}</template>-->
<!--        </el-table-column>-->
        <el-table-column label="所属群" prop="groupId" min-width="190" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <multi-cell :image-url="row.groupAvatarUrl"
                        :text-list="[row.groupId,row.groupName]"></multi-cell>
          </template>
        </el-table-column>
        <el-table-column label="机器人QQ" prop="selfId" min-width="120" align="center" show-tooltip-when-overflow>
          <template slot-scope="{row}">
            <multi-cell :image-url="row.selfAvatarUrl"
                        :text-list="[row.selfId]"></multi-cell>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-box">
        <el-pagination v-bind="memberPagination" @size-change="memberSizeChange" @current-change="memberCurrentChange" />
      </div>
    </basic-container>
    <refresh-result-dialog ref="refreshResultDialog"/>
    <member-refresh-result-dialog ref="memberRefreshResultDialog"/>
    <user-list-dialog ref="userListDialog"></user-list-dialog>
  </div>
</template>
<script>
import numberInput from "@/components/input/numberInput.vue"
import RefreshResultDialog  from "./refresh-result-dialog";
import MemberRefreshResultDialog from "./member-refresh-result-dialog.vue";
import UserListDialog from "./user-list-dialog.vue";
import {search as searchApi,refresh as refreshApi,searchMember as searchMemberApi,refreshMember as refreshMemberApi} from "@/api/group";
import MultiCell from "@/components/multi-cell.vue";

export default {
  name:'GroupList',
  components:{
    MultiCell,
    numberInput,
    RefreshResultDialog,
    MemberRefreshResultDialog,
    UserListDialog
  },
  data(){
    return{
      activeTab:'group',
      tableLoading:false,
      exportLoading:false,
      refreshLoading:false,
      memberTableLoading:false,
      memberLoaded:false,
      memberRefreshLoading:{},
      queryFormObj:{
        selfId:'',
        groupId:'',
        groupName:''
      },
      memberQueryFormObj:{
        selfId:'',
        groupId:'',
        userId:'',
        nickname:'',
        card:'',
        leftFlag:null
      },
      tableData:[],
      memberTableData:[],
      pagination:{
        currentPage: 1,
        pageSizes: [5, 10, 30, 50, 100, 500],
        pageSize: 10,
        layout: 'total, sizes, prev, pager, next, jumper',
        background: true,
        total: 0
      },
      memberPagination:{
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
    formatBool(value){
      if (value === null || value === undefined || value === '') {
        return ''
      }
      return value ? '是' : '否'
    },
    formatRole(role){
      const map = {owner:'群主',admin:'管理员',member:'群员'}
      return map[role] || role || ''
    },
    formatSex(sex){
      const map = {male:'男',female:'女',unknown:'未知'}
      return map[sex] || sex || ''
    },
    formatTimestamp(value){
      if (value === null || value === undefined || value === '') {
        return ''
      }
      const text = `${value}`.trim()
      if (!/^\d{10}$|^\d{13}$/.test(text)) {
        return text
      }
      const timestamp = text.length === 10 ? Number(text) * 1000 : Number(text)
      const date = new Date(timestamp)
      if (Number.isNaN(date.getTime())) {
        return text
      }
      const pad = val => `${val}`.padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    },
    cleanQuery(query){
      const res = { ...query }
      Object.keys(res).forEach(key => {
        if (res[key] === '') {
          res[key] = null
        }
      })
      return res
    },
    handleTabClick(){
      if (this.activeTab === 'member' && !this.memberLoaded) {
        this.searchMember()
      }
    },
    isMemberRefreshing(row){
      return !!(row && this.memberRefreshLoading[row.groupId])
    },
    refreshGroupMember(row){
      if (this.isMemberRefreshing(row)) {
        return
      }
      if (!row.selfId) {
        return this.$message.error('该群缺少机器人QQ，无法刷新群成员')
      }
      this.$set(this.memberRefreshLoading, row.groupId, true)
      refreshMemberApi({
        botId: row.selfId,
        groupId: row.groupId
      }).then(({data:{data,code,message}})=>{
        if(code !== 200){
          return this.$message.error(message)
        }
        this.$refs.memberRefreshResultDialog.open(row, data)
        if (this.memberLoaded) {
          this.selectMemberTableData()
        }
      }).catch(()=>{
      }).finally(()=>{
        this.$delete(this.memberRefreshLoading, row.groupId)
      })
    },
    showUserList(row){
      this.$refs.userListDialog.open(row)
    },
    /**
     * 跳转到群成员tab，并带上当前行的机器人与群号查询条件
     */
    showMemberList(row){
      this.activeTab = 'member'
      this.memberQueryFormObj.groupId = row.groupId
      this.memberQueryFormObj.selfId = row.selfId
      this.memberQueryFormObj.userId = ''
      this.memberQueryFormObj.nickname = ''
      this.memberQueryFormObj.card = ''
      this.memberQueryFormObj.leftFlag = null
      this.searchMember()
    },
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
    searchMember(){
      this.memberPagination.currentPage = 1
      this.selectMemberTableData()
    },
    exportAsExcel(){

    },
    resetQueryForm(){
      this.$refs.queryForm.resetFields()
    },
    resetMemberQueryForm(){
      this.$refs.memberQueryForm.resetFields()
    },
    sizeChange(v){
      this.pagination.pageSize = v
      this.selectTableData()
    },
    currentChange(v){
      this.pagination.currentPage = v
      this.selectTableData()
    },
    memberSizeChange(v){
      this.memberPagination.pageSize = v
      this.selectMemberTableData()
    },
    memberCurrentChange(v){
      this.memberPagination.currentPage = v
      this.selectMemberTableData()
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
    },
    selectMemberTableData(){
      this.memberTableLoading = true
      searchMemberApi({
        ...this.cleanQuery(this.memberQueryFormObj),
        currentPage:this.memberPagination.currentPage,
        pageSize:this.memberPagination.pageSize
      }).then(({data:{data,code,message}})=>{
        if (code !== 200) {
          this.memberTableData = []
          this.memberPagination.total = 0
          return this.$message.error(message)
        }
        this.memberTableData = (data.records || []).map(row => {
          return {
            ...row,
            formattedJoinTime: this.formatTimestamp(row.joinTime),
            formattedLastSentTime: this.formatTimestamp(row.lastSentTime)
          }
        })
        this.memberPagination.total = data.total
        this.memberLoaded = true
      }).finally(()=>{
        this.memberTableLoading = false
      })
    }
  }
}
</script>
<style lang="scss" scoped>
#GroupList{
  .member-tip{
    color: #909399;
    font-size: 12px;
    line-height: 22px;
  }
}
</style>

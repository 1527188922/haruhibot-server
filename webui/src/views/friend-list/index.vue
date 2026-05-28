<template>
  <div id="FriendList">
    <basic-container>
      <el-row>
        <el-form :model="queryFormObj" label-width="70px" inline ref="queryForm" size="small">

          <el-form-item label="好友QQ" prop="userId">
            <number-input v-model.trim="queryFormObj.userId" placeholder="好友QQ" class="form-input" maxlength="20" clearable></number-input>
          </el-form-item>
          <el-form-item label="好友昵称" prop="nickname">
            <el-input v-model.trim="queryFormObj.nickname" placeholder="好友昵称" class="form-input" maxlength="20" clearable></el-input>
          </el-form-item>
          <el-form-item label="性别" prop="sex">
            <el-select v-model="queryFormObj.sex" class="form-input" clearable>
              <el-option v-for="(v,k) in sexMap" :key="k" :value="k" :label="v"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="机器人" prop="selfId">
            <number-input v-model.trim="queryFormObj.selfId" class="form-input" maxlength="20" clearable
                          placeholder="机器人QQ"></number-input>
          </el-form-item>
        </el-form>
      </el-row>
      <el-row class="query-form-option-buts">
        <el-button type="primary" size="small" @click="search" plain
                   icon="el-icon-search">查询</el-button>
        <!--        <el-button type="primary" size="small" @click="exportAsExcel" :loading="exportLoadin" plain>导出</el-button>-->
        <el-button type="primary" size="small" @click="resetQueryForm" plain
                   icon="el-icon-refresh-right">重置</el-button>
      </el-row>
    </basic-container>


    <basic-container>
      <div class="data-table-option-buts">
<!--        <el-button @click="refreshCache" type="primary" size="small" plain-->
<!--                   icon="el-icon-refresh" :loading="refreshLoading">刷新</el-button>-->
      </div>
      <el-table tooltip-effect="light" :data="tableData" v-loading="tableLoading" border
                stripe max-height="800" size="small" ref="dataTable" highlight-current-row >
        <el-table-column fixed label="序号" width="45" align="center">
          <template slot-scope="scope">{{scope.$index+1}}</template>
        </el-table-column>
        <el-table-column label="好友" prop="userId" min-width="130" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <multi-cell :image-url="row.userAvatarUrl" :text-list="[row.userId,row.nickname]"
                        :title-list="[`QQ：${row.userId}`, `QQ昵称：${row.nickname}`]"></multi-cell>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="100" align="center" show-tooltip-when-overflow/>
        <el-table-column label="性别" prop="sex" min-width="60" align="center" show-tooltip-when-overflow :formatter="(row)=>{
          return sexMap[row.sex]
        }"/>
        <el-table-column label="生日" prop="sex" min-width="100" align="center" show-tooltip-when-overflow :formatter="(row)=>{
          return `${row.birthdayYear}-${row.birthdayMonth}-${row.birthdayDay}`
        }"/>
        <el-table-column label="年龄" prop="age" min-width="60" align="center" show-tooltip-when-overflow/>
        <el-table-column label="等级" prop="level" min-width="130" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <multi-cell :text-list="[formatNumberToEmoji(row.level),row.level]"></multi-cell>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" prop="email" min-width="140" align="center" show-tooltip-when-overflow/>
        <el-table-column label="电话" prop="phoneNum" min-width="140" align="center" show-tooltip-when-overflow/>
        <el-table-column label="机器人QQ" prop="selfId" min-width="130" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <multi-cell :image-url="row.selfAvatarUrl" :text-list="[row.selfId]"
                        :title-list="[`QQ：${row.selfId}`]"></multi-cell>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-box">
        <el-pagination v-bind="pagination" @size-change="sizeChange" @current-change="currentChange" />
      </div>
    </basic-container>
  </div>
</template>
<script>
import numberInput from "@/components/input/numberInput.vue"
import {search as searchApi} from "@/api/friend";
import {formatNumberToEmoji} from "@/util/util";
import MultiCell from "@/components/multi-cell.vue";

export default {
  name:'GroupList',
  components:{
    MultiCell,
    numberInput,
  },
  data(){
    return{
      tableLoading:false,
      exportLoading:false,
      refreshLoading:false,
      queryFormObj:{
        selfId:'',
        userId:'',
        nickname:'',
        sex:''
      },
      sexMap:{
        male:'男',
        female:'女',
        unknown:'未知',
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
    formatNumberToEmoji,
    // refreshCache(){
    //   this.refreshLoading = true
    //   refreshApi().then(({data:{data,code,message}})=>{
    //     if(code !== 200){
    //       return this.$message.error(message)
    //     }
    //     this.$refs.refreshResultDialog.open(data)
    //   }).finally(()=>{
    //     this.refreshLoading = false
    //   })
    // },
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
#FriendList{

}
</style>
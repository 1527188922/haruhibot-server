<template>
  <div id="UserListDialog">
    <el-dialog :visible.sync="visible" :title="title"
               width="700px" @closed="dialogClosed" v-dialogDrag :close-on-click-modal="false">
      <multi-cell slot="title" :text-list="[title]" :image-url="avatarUrl"></multi-cell>
      <el-table tooltip-effect="light" :data="tableData" v-loading="tableLoading" border
                stripe max-height="800" size="small" ref="dataTable" highlight-current-row
                @sort-change="sortChange" class="sortable-table">
        <el-table-column fixed label="序号" width="45" align="center">
          <template slot-scope="scope">{{scope.$index+1}}</template>
        </el-table-column>
        <el-table-column label="群成员" prop="userId" min-width="130" align="center" show-tooltip-when-overflow >
          <template slot-scope="{row}">
            <multi-cell :image-url="row.userAvatarUrl" :text-list="[row.userId,row.nickname]"
                        :title-list="[`QQ：${row.userId}`, `QQ昵称：${row.nickname}`]"></multi-cell>
          </template>
        </el-table-column>
        <el-table-column label="群内昵称" prop="card" min-width="100" align="center" show-tooltip-when-overflow/>
        <el-table-column label="发言数" prop="count" min-width="70" align="center" show-tooltip-when-overflow
                         sortable="custom" :sort-orders="['ascending', 'descending']"/>
        <el-table-column label="最近发言时间" prop="time" min-width="120" align="center" show-tooltip-when-overflow
                         sortable="custom"/>
      </el-table>
      <div class="pagination-box">
        <el-pagination small v-bind="pagination" @size-change="sizeChange" @current-change="currentChange" />
      </div>
    </el-dialog>
  </div>
</template>
<script>
import {queryUser as searchApi} from "@/api/chat-record";
import {deepClone} from "@/util/util";
import MultiCell from "@/components/multi-cell.vue";
export default {
  name:'UserListDialog',
  components:{
    MultiCell,
  },
  data(){
    return{
      visible:false,
      tableLoading:false,
      exportLoading:false,
      queryFormObj:{
        prop:'',
        order:''
      },
      tableData:[],
      row:null,
      pagination:{
        currentPage: 1,
        pageSizes: [5, 10, 30, 50, 100, 500],
        pageSize: 10,
        layout: 'total, sizes, prev, pager, next, jumper',
        background: false,
        total: 0
      },
      defOrderProp:'count'
    }
  },
  created() {
  },
  computed:{
    title(){
      return this.row ? `发言人列表：${this.row.groupName}（${this.row.groupId}）` : '发言人列表'
    },
    avatarUrl(){
      return this.row ? this.row.groupAvatarUrl : null
    }
  },
  methods:{
    sortChange({ column, prop, order }){
      this.queryFormObj.prop = order ? prop : this.defOrderProp
      this.queryFormObj.order = order
      this.selectTableData()
    },
    open(v){
      this.visible = true
      this.$nextTick(()=>{
        this.row = deepClone(v)
        this.search()
      })
    },
    search(){
      this.pagination.currentPage = 1
      this.selectTableData()
    },
    exportAsExcel(){

    },
    resetQueryForm(){
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
        groupId:this.row.groupId,
        currentPage:this.pagination.currentPage,
        pageSize:this.pagination.pageSize
      }).then(({data:{code,data}})=>{
        if (code !== 200) {
          this.tableData = []
          this.pagination.total = 0
          return
        }
        this.tableData = data.list || []
        this.pagination.total = data.total
      }).finally(()=>{
        this.tableLoading = false
      })
    },
    dialogClosed(){
      this.queryFormObj.prop = ''
      this.queryFormObj.order = ''
      this.$refs.dataTable.clearSort()
      this.row = null
      this.pagination.total = 0
      this.tableData = []
    }
  }
}
</script>
<style scoped lang="scss">
#UserListDialog{

}
</style>
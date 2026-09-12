<template>
  <div id="BilibiliSubscribe">
    <basic-container>
      <el-alert v-if="jobInfo" class="job-alert" :type="jobAlert.type" :title="jobAlert.title"
                :description="jobAlert.description" :closable="false" show-icon></el-alert>
      <el-row>
        <el-form :model="queryFormObj" label-width="80px" inline ref="queryForm" size="small">
          <el-form-item label="主播UID" prop="uid">
            <number-input v-model.trim="queryFormObj.uid" class="form-input" maxlength="20" clearable
                          placeholder="b站主播uid"></number-input>
          </el-form-item>
          <el-form-item label="主播昵称" prop="uname">
            <el-input v-model="queryFormObj.uname" class="form-input" maxlength="50" clearable
                      @keyup.enter.native="search"></el-input>
          </el-form-item>
          <el-form-item label="是否启用" prop="enableStatus">
            <el-select v-model="queryFormObj.enableStatus" class="form-input" clearable placeholder="全部">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label"
                         :value="item.value"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="下播推送" prop="offNotify">
            <el-select v-model="queryFormObj.offNotify" class="form-input" clearable placeholder="全部">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label"
                         :value="item.value"></el-option>
            </el-select>
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
        <el-button @click="add" type="primary" size="small" plain
                   icon="el-icon-plus">新增订阅</el-button>
        <el-button @click="deleteData" type="danger" size="small" plain
                   :disabled="deleteBatchDisabled"
                   icon="el-icon-delete">删除</el-button>
        <el-button @click="selectTableData" type="primary" size="small" plain
                   icon="el-icon-refresh">刷新</el-button>
      </div>
      <el-table tooltip-effect="light" :data="tableData" v-loading="tableLoading" border
                stripe max-height="800" size="small" ref="dataTable" highlight-current-row
                @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center"></el-table-column>
        <el-table-column fixed label="序号" width="45" align="center">
          <template slot-scope="scope">{{scope.$index+1}}</template>
        </el-table-column>
        <el-table-column fixed label="操作" width="90" align="center">
          <template slot-scope="{row}">
            <el-button type="text" size="small" @click="edit(row)">修改</el-button>
          </template>
        </el-table-column>
        <el-table-column label="主播" prop="uid" min-width="230" show-tooltip-when-overflow>
          <template slot-scope="{row}">
            <multi-cell :image-url="row.face"
                        :text-list="[row.uname, `UID：${row.uid}`, row.roomId ? `直播间：${row.roomId}` : '']"
                        :link-list="[spaceUrl(row.uid), spaceUrl(row.uid), row.roomId ? liveUrl(row.roomId) : null]"
                        :title-list="[`主播昵称：${row.uname || ''}，点击进入b站个人主页`, `UID：${row.uid}，点击进入b站个人主页`, `直播间id：${row.roomId}，点击进入直播间`]"></multi-cell>
          </template>
        </el-table-column>
        <el-table-column label="直播状态" prop="living" min-width="150" align="center">
          <template slot-scope="{row}">
            <template v-if="row.living">
              <el-tag type="danger" size="mini" effect="dark">直播中</el-tag>
              <div class="live-duration" :title="row.liveStartTimeText ? `开播时间：${row.liveStartTimeText}` : ''">
                {{ liveDurationText(row) }}
              </div>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="推送群" min-width="330">
          <template slot-scope="{row}">
            <div class="target-line">
              <template v-if="row.groupInfos && row.groupInfos.length">
                <el-tag v-for="t in row.groupInfos" :key="'g'+t.id" class="target-chip"
                        size="small" :type="t.found ? 'success' : 'danger'"
                        :title="targetTitle(t,'群')" closable
                        @close="removeTarget(row,'group',t)">
                  <img class="target-avatar" :src="t.avatarUrl" referrerpolicy="no-referrer">{{ t.name || t.id }}
                </el-tag>
              </template>
              <span v-else class="target-empty">未配置</span>
              <el-button type="text" size="mini" icon="el-icon-plus"
                         @click="manageTarget(row,'group')">添加</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="推送好友" min-width="330">
          <template slot-scope="{row}">
            <div class="target-line">
              <template v-if="row.friendInfos && row.friendInfos.length">
                <el-tag v-for="t in row.friendInfos" :key="'f'+t.id" class="target-chip"
                        size="small" :type="t.found ? 'success' : 'danger'"
                        :title="targetTitle(t,'好友')" closable
                        @close="removeTarget(row,'friend',t)">
                  <img class="target-avatar" :src="t.avatarUrl" referrerpolicy="no-referrer">{{ t.name || t.id }}
                </el-tag>
              </template>
              <span v-else class="target-empty">未配置</span>
              <el-button type="text" size="mini" icon="el-icon-plus"
                         @click="manageTarget(row,'friend')">添加</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" prop="enableStatus" min-width="100" align="center">
          <template slot-scope="{row}">
            <div class="switch-line">
              <el-switch :value="row.enableStatus" :active-value="1" :inactive-value="0"
                         :disabled="row.switchLoading"
                         @change="v => changeSwitch(row, 'enableStatus', v)"></el-switch>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="下播推送" prop="enableStatus" min-width="100" align="center">
          <template slot-scope="{row}">
            <div class="switch-line">
              <el-switch :value="row.offNotify" :active-value="1" :inactive-value="0"
                         :disabled="row.switchLoading"
                         @change="v => changeSwitch(row, 'offNotify', v)"></el-switch>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="机器人" prop="selfId" min-width="130" align="center" show-tooltip-when-overflow>
          <template slot-scope="{row}">
            <multi-cell :image-url="row.selfAvatarUrl" :text-list="[row.selfId]"
                        :title-list="[`QQ：${row.selfId}`]"></multi-cell>
          </template>
        </el-table-column>
        <el-table-column label="时间" min-width="180" show-tooltip-when-overflow>
          <template slot-scope="{row}">
            <multi-cell :text-list="[`${row.updateTime}（更新）`, `${row.createTime}（创建）`]"
                        :title-list="['更新时间','创建时间']"></multi-cell>
          </template>
        </el-table-column>
      </el-table>
    </basic-container>
    <edit-dialog ref="editDialog"></edit-dialog>
    <target-dialog ref="targetDialog"></target-dialog>
  </div>
</template>
<script>
import numberInput from "@/components/input/numberInput.vue";
import MultiCell from "@/components/multi-cell.vue";
import EditDialog from "./edit-dialog.vue";
import TargetDialog from "./target-dialog.vue";
import {search as searchApi, update as updateApi, updateTargets, deleteBatch, jobInfo as jobInfoApi}
  from "@/api/bilibili-subscribe";

export default {
  name: 'BilibiliSubscribe',
  components: {
    MultiCell,
    numberInput,
    EditDialog,
    TargetDialog
  },
  data(){
    return{
      tableLoading:false,
      firstActivated:true,
      nowTs: Math.floor(Date.now() / 1000),
      timer:null,
      jobInfo:null,
      statusOptions:[
        {label:'启用',value:1},
        {label:'禁用',value:0}
      ],
      queryFormObj:{
        uid:'',
        uname:'',
        enableStatus:'',
        offNotify:''
      },
      tableData:[],
      multipleSelection: []
    }
  },
  computed:{
    deleteBatchDisabled(){
      return !this.multipleSelection || this.multipleSelection.length === 0
    },
    jobAlert(){
      const info = this.jobInfo
      if(!info){
        return {type:'info', title:'', description:''}
      }
      if(!info.enable){
        return {
          type:'warning',
          title:'BILIBILI直播推送定时任务未开启',
          description:'配置 job.bilibiliLive.enable 未设置为 1，订阅不会推送开播/下播消息'
        }
      }
      if(!info.registered){
        return {
          type:'warning',
          title:'BILIBILI直播推送定时任务已开启但未注册成功',
          description:'配置 job.bilibiliLive.enable = 1，但定时任务未注册到调度器，请检查启动日志'
        }
      }
      return {
        type:'success',
        title:'BILIBILI直播推送定时任务已开启',
        description: info.cron ? `cron表达式：${info.cron}` : '未配置cron表达式(job.bilibiliLive.cron)'
      }
    }
  },
  mounted() {
    this.search()
    this.loadJobInfo()
  },
  activated(){
    // 页面被keep-alive缓存，重新进入时刷新一次，保证直播状态是最新的
    if(this.firstActivated){
      this.firstActivated = false
      return
    }
    this.nowTs = Math.floor(Date.now() / 1000)
    this.selectTableData()
    this.loadJobInfo()
  },
  deactivated() {
    this.clearTimer()
  },
  beforeDestroy() {
    this.clearTimer()
  },
  methods:{
    handleSelectionChange(val){
      this.multipleSelection = val
    },
    add(){
      this.$refs.editDialog.open(null)
    },
    edit(row){
      this.$refs.editDialog.open(row)
    },
    manageTarget(row, type){
      this.$refs.targetDialog.open(row, type, ()=>this.selectTableData())
    },
    spaceUrl(uid){
      return uid ? `https://space.bilibili.com/${uid}` : null
    },
    liveUrl(roomId){
      return roomId ? `https://live.bilibili.com/${roomId}` : null
    },
    loadJobInfo(){
      jobInfoApi().then(({data:{code,data}})=>{
        if(code !== 200){
          return
        }
        this.jobInfo = data
      })
    },
    targetTitle(t, type){
      if(t.found){
        return type === '群' ? `群名称：${t.name || ''}  群号：${t.id}` : `昵称：${t.name || ''}  QQ：${t.id}`
      }
      return type === '群' ? `群号：${t.id}（未查询到群信息，机器人可能未加入该群）`
        : `QQ：${t.id}（未查询到好友信息，机器人可能未添加该好友）`
    },
    liveDurationText(row){
      if(!row.living || !row.liveStartTime){
        return '开播中'
      }
      return '已开播 ' + this.formatDuration(this.nowTs - row.liveStartTime)
    },
    formatDuration(seconds){
      seconds = Math.max(0, Math.floor(seconds || 0))
      const day = Math.floor(seconds / 86400)
      const hour = Math.floor((seconds % 86400) / 3600)
      const minute = Math.floor((seconds % 3600) / 60)
      const second = seconds % 60
      let text = ''
      if(day > 0){ text += day + '天' }
      if(hour > 0){ text += hour + '小时' }
      if(minute > 0){ text += minute + '分' }
      text += second + '秒'
      return text
    },
    startTimer(){
      if(this.timer){
        return
      }
      // 有主播正在开播时，本地每秒刷新一次开播时长(不请求接口)
      this.timer = setInterval(()=>{
        this.nowTs = Math.floor(Date.now() / 1000)
        if(!this.tableData.some(e=>e.living)){
          this.clearTimer()
        }
      }, 1000)
    },
    clearTimer(){
      if(this.timer){
        clearInterval(this.timer)
        this.timer = null
      }
    },
    buildPayload(row){
      return {
        id:row.id,
        uid:row.uid,
        subType:row.subType,
        selfId:row.selfId,
        groupIds:row.groupIds,
        friendIds:row.friendIds,
        enableStatus:row.enableStatus,
        offNotify:row.offNotify
      }
    },
    changeSwitch(row, field, value){
      const oldValue = row[field]
      row[field] = value
      this.$set(row, 'switchLoading', true)
      updateApi(this.buildPayload(row)).then(({data:{code,message}})=>{
        if(code !== 200){
          row[field] = oldValue
          return this.$message.error(message)
        }
        this.$message.success('修改成功')
      }).catch(e=>{
        row[field] = oldValue
        this.$message.error(e.message)
      }).finally(()=>{
        this.$set(row, 'switchLoading', false)
      })
    },
    removeTarget(row, type, target){
      const field = type === 'group' ? 'groupIds' : 'friendIds'
      const rest = row[field].split(/[,，\s]+/).filter(e=>e && e !== String(target.id)).join(',')
      this.$confirm(`确认移除${type === 'group' ? '群' : '好友'}【${target.name || target.id}】？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(()=>{
        const payload = {
          id: row.id,
          groupIds: type === 'group' ? this.parseIds(rest) : this.parseIds(row.groupIds),
          friendIds: type === 'friend' ? this.parseIds(rest) : this.parseIds(row.friendIds)
        }
        updateTargets(payload).then(({data:{code,message}})=>{
          if(code !== 200){
            return this.$message.error(message)
          }
          this.$message.success('修改成功')
          this.selectTableData()
        }).catch(e=>{
          this.$message.error(e.message)
        })
      })
    },
    parseIds(ids){
      if(!ids){
        return []
      }
      return String(ids).split(/[,，\s]+/)
        .filter(e=>e)
        .map(e=>Number(e))
        .filter(e=>!isNaN(e))
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
        }).catch(e=>{
          this.$message.error(e.message)
        })
      })
    },
    search(){
      this.selectTableData()
    },
    resetQueryForm(){
      this.$refs.queryForm.resetFields()
      this.queryFormObj.enableStatus = ''
      this.queryFormObj.offNotify = ''
    },
    selectTableData(){
      this.tableLoading = true
      searchApi({
        uid:this.queryFormObj.uid || null,
        uname:this.queryFormObj.uname,
        enableStatus:this.queryFormObj.enableStatus === '' ? null : this.queryFormObj.enableStatus,
        offNotify:this.queryFormObj.offNotify === '' ? null : this.queryFormObj.offNotify
      }).then(({data:{data}})=>{
        this.tableData = data || []
        if(this.tableData.some(e=>e.living)){
          this.startTimer()
        }else{
          this.clearTimer()
        }
      }).finally(()=>{
        this.tableLoading = false
      })
    }
  }
}
</script>
<style lang="scss" scoped>
#BilibiliSubscribe{
  .job-alert{
    margin-bottom: 10px;
  }
  .switch-line{
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 22px;
    .switch-label{
      margin-right: 6px;
      font-size: 12px;
      color: #606266;
    }
  }
  .live-duration{
    font-size: 12px;
    color: #f56c6c;
    margin-top: 2px;
  }
  .target-line{
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    line-height: 24px;
    text-align: left;
    &:not(:first-child){
      border-top: 1px solid #d5dce8;
    }
    .target-label{
      width: 32px;
      flex: none;
      font-size: 12px;
      color: #909399;
    }
    .target-chip{
      display: inline-flex;
      align-items: center;
      margin: 1px 4px 1px 0;
      img.target-avatar{
        display: block;
        flex: none;
        width: 16px;
        height: 16px;
        border-radius: 50%;
        margin-right: 4px;
      }
      // element给close图标加了top:-1px(为inline-block布局做的补偿)，flex布局下会偏上，这里还原
      ::v-deep .el-tag__close{
        top: 0;
        align-self: center;
        flex: none;
      }
    }
    .target-empty{
      font-size: 12px;
      color: #c0c4cc;
    }
  }
}
</style>

<template>
  <div id="BilibiliSubscribeTargetDialog">
    <el-dialog :visible.sync="visible" :title="title" width="600px" @closed="dialogClosed" v-dialogDrag
               :close-on-click-modal="false">
      <div class="box">
        <div class="box-title">已选{{typeLabel}}（{{selectedIds.length}}）</div>
        <div class="chip-box">
          <el-tag v-for="t in selectedInfos" :key="t.id" class="target-chip" size="small"
                  :type="t.found ? 'success' : 'danger'" :title="targetTitle(t)" closable
                  @close="remove(t.id)">
            <img v-if="t.avatarUrl" class="target-avatar" :src="t.avatarUrl"
                 referrerpolicy="no-referrer">{{ t.name || t.id }}<span
              v-if="t.name" class="target-code">（{{t.id}}）</span>
          </el-tag>
          <span v-if="!selectedInfos.length" class="empty-tip">暂未选择</span>
        </div>
        <div class="form-tip">
          红色标签表示未在{{type === 'group' ? '群列表' : '好友列表'}}中查询到，可能是机器人未加群/未添加好友，消息将不会推送给该目标
        </div>
      </div>
      <div class="box">
        <div class="box-title">添加{{typeLabel}}</div>
        <el-select ref="picker" v-model="pendingIds" multiple filterable remote reserve-keyword
                   :remote-method="remoteSearch" :loading="searchLoading" size="small"
                   popper-class="bili-target-select-popper"
                   placeholder="输入群号/群名或QQ号/昵称搜索" @change="pickerChange">
          <el-option v-for="o in options" :key="o.id" :label="optionLabel(o)" :value="o.id">
            <div class="option-item">
              <img v-if="o.avatarUrl" class="target-avatar" :src="o.avatarUrl" referrerpolicy="no-referrer">
              <span class="option-name">{{ `${o.name || o.id}（${o.id}）` }}</span>
            </div>
          </el-option>
        </el-select>
        <div class="manual-box">
          <el-input v-model.trim="manualId" size="small" class="manual-input"
                    :placeholder="type === 'group' ? '手动输入群号' : '手动输入QQ号'"
                    @keyup.enter.native="addManual"></el-input>
          <el-button type="primary" size="small" plain @click="addManual">添加</el-button>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit" :loading="submitLoading">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>
<script>
import {targetList, updateTargets} from "@/api/bilibili-subscribe";

export default {
  name:'BilibiliSubscribeTargetDialog',
  data(){
    return{
      visible:false,
      submitLoading:false,
      searchLoading:false,
      title:'',
      row:null,
      type:'group',
      selfId:null,
      callback:null,
      options:[],
      pendingIds:[],
      manualId:'',
      selectedIds:[],
      infoMap:{}
    }
  },
  computed:{
    typeLabel(){
      return this.type === 'group' ? '群' : '好友'
    },
    selectedInfos(){
      return this.selectedIds.map(id=>this.infoMap[id] || {id, name:null, avatarUrl:null, found:false})
    }
  },
  methods:{
    open(row, type, callback){
      this.visible = true
      this.row = row
      this.type = type
      this.selfId = row.selfId
      this.callback = callback
      this.title = `管理推送${this.type === 'group' ? '群' : '好友'} - ${row.uname || row.uid}`
      this.selectedIds = this.parseIds(type === 'group' ? row.groupIds : row.friendIds)
      this.infoMap = {}
      const infos = (type === 'group' ? row.groupInfos : row.friendInfos) || []
      infos.forEach(info=>this.$set(this.infoMap, info.id, info))
      this.pendingIds = []
      this.manualId = ''
      this.options = []
      this.$nextTick(()=>{
        this.remoteSearch('')
        this.resolveInfos()
      })
    },
    optionLabel(o){
      return o.name ? `${o.name}（${o.id}）` : String(o.id)
    },
    targetTitle(t){
      if(t.found){
        return this.type === 'group' ? `群名称：${t.name || ''}  群号：${t.id}` : `昵称：${t.name || ''}  QQ：${t.id}`
      }
      return this.type === 'group' ? `群号：${t.id}（未查询到群信息，机器人可能未加入该群）`
        : `QQ：${t.id}（未查询到好友信息，机器人可能未添加该好友）`
    },
    remoteSearch(keyword){
      this.searchLoading = true
      targetList({
        selfId:this.selfId,
        type:this.type,
        keyword:keyword || '',
        pageSize:50
      }).then(({data:{data}})=>{
        this.options = data || []
      }).catch(e=>{
        this.$message.error(e.message)
      }).finally(()=>{
        this.searchLoading = false
      })
    },
    /**
     * 查询已选目标中还没有名称/头像的项
     */
    resolveInfos(){
      const unknown = this.selectedIds.filter(id=>!this.infoMap[id])
      if(!unknown.length){
        return
      }
      targetList({
        selfId:this.selfId,
        type:this.type,
        ids:unknown,
        pageSize:unknown.length
      }).then(({data:{data}})=>{
        const list = data || []
        list.forEach(info=>this.$set(this.infoMap, info.id, info))
        unknown.forEach(id=>{
          if(!this.infoMap[id]){
            this.$set(this.infoMap, id, {id, name:null, avatarUrl:null, found:false})
          }
        })
      }).catch(e=>{
        this.$message.error(e.message)
      })
    },
    pickerChange(ids){
      const optionMap = new Map(this.options.map(o=>[o.id, o]))
      ids.forEach(id=>{
        const targetId = Number(id)
        if(!targetId || isNaN(targetId)){
          return
        }
        if(this.selectedIds.includes(targetId)){
          return
        }
        this.selectedIds.push(targetId)
        const option = optionMap.get(id) || optionMap.get(targetId)
        if(option){
          this.$set(this.infoMap, targetId, option)
        }
      })
      this.$nextTick(()=>{
        this.pendingIds = []
        this.resolveInfos()
      })
    },
    addManual(){
      const targetId = Number(this.manualId)
      if(!targetId || isNaN(targetId)){
        return this.$message.warning(this.type === 'group' ? '请输入正确的群号' : '请输入正确的QQ号')
      }
      if(this.selectedIds.includes(targetId)){
        this.manualId = ''
        return this.$message.warning('该目标已存在')
      }
      this.selectedIds.push(targetId)
      this.manualId = ''
      this.resolveInfos()
    },
    remove(id){
      this.selectedIds = this.selectedIds.filter(e=>e !== id)
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
    submit(){
      const payload = {
        id: this.row.id,
        groupIds: this.type === 'group' ? this.selectedIds.slice() : this.parseIds(this.row.groupIds),
        friendIds: this.type === 'friend' ? this.selectedIds.slice() : this.parseIds(this.row.friendIds)
      }
      this.submitLoading = true
      updateTargets(payload).then(({data:{code,message}})=>{
        if(code !== 200){
          return this.$message.error(message)
        }
        this.$message.success('修改成功')
        this.visible = false
        if(this.callback){
          this.callback()
        }
      }).catch(e=>{
        this.$message.error(e.message)
      }).finally(()=>{
        this.submitLoading = false
      })
    },
    dialogClosed(){
      this.row = null
      this.callback = null
      this.options = []
      this.pendingIds = []
      this.manualId = ''
      this.selectedIds = []
      this.infoMap = {}
      this.title = ''
    }
  }
}
</script>
<style lang="scss" scoped>
#BilibiliSubscribeTargetDialog{
  .box{
    margin-bottom: 12px;
  }
  .box-title{
    font-size: 13px;
    color: #606266;
    margin-bottom: 6px;
  }
  .chip-box{
    min-height: 34px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 4px 6px;
  }
  .target-chip{
    display: inline-flex;
    align-items: center;
    margin: 2px 4px 2px 0;
    // element给close图标加了top:-1px(为inline-block布局做的补偿)，flex布局下会偏上，这里还原
    ::v-deep .el-tag__close{
      top: 0;
      align-self: center;
      flex: none;
    }
  }
  .target-avatar{
    display: block;
    flex: none;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    margin-right: 4px;
  }
  .target-code{
    color: #909399;
  }
  .empty-tip{
    font-size: 12px;
    color: #c0c4cc;
  }
  .form-tip{
    font-size: 12px;
    color: #909399;
    line-height: 18px;
    margin-top: 4px;
  }
  .manual-box{
    display: flex;
    margin-top: 8px;
    .manual-input{
      flex: 1;
      margin-right: 8px;
    }
  }
  .el-select{
    width: 100%;
  }
}
</style>
<style lang="scss">
/**
 * el-select的下拉框默认append到body下(不在本组件dom树内)，scoped样式匹配不到，
 * 所以这里不写scoped，用popper-class做命名空间
 */
.bili-target-select-popper{
  .el-select-dropdown__item{
    padding-right: 40px !important; // 给选中时的对勾留位置
    padding-left: 10px !important;
  }
  .option-item{
    display: flex;
    align-items: center;
  }
  .target-avatar{
    display: block;
    flex: none;
    width: 30px;
    height: 30px;
    border-radius: 50%;
    margin-right: 6px;
  }
  .option-name{
    padding: 0 !important;
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    float: left;
  }
}
</style>

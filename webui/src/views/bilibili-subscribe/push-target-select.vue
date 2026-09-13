<template>
  <div class="push-target-select">
    <div class="box-title">已选{{typeLabel}}（{{ids.length}}）</div>
    <!-- 群需要单独控制是否@全体成员，所以用列表展示，每个群一个开关 -->
    <div v-if="isGroup" class="selected-list">
      <div v-for="t in selectedInfos" :key="t.id" class="selected-row">
        <img v-if="t.avatarUrl" class="target-avatar" :src="t.avatarUrl" referrerpolicy="no-referrer">
        <span class="selected-name" :class="{'not-found': !t.found}" :title="targetTitle(t)">{{ t.name || t.id }}</span>
        <span v-if="t.name" class="target-code">（{{t.id}}）</span>
        <span class="row-space"></span>
        <span class="at-all-label">@全体成员</span>
        <el-switch :value="atAllIds.includes(t.id)" :disabled="disabled"
                   @change="v => toggleAtAll(t.id, v)"></el-switch>
        <el-button class="row-remove" type="text" size="mini" icon="el-icon-close"
                   title="移除" @click="remove(t.id)"></el-button>
      </div>
      <span v-if="!selectedInfos.length" class="empty-tip">暂未选择</span>
    </div>
    <div v-else class="chip-box">
      <el-tag v-for="t in selectedInfos" :key="t.id" class="target-chip" size="small"
              :type="t.found ? 'success' : 'danger'" :title="targetTitle(t)" closable
              @close="remove(t.id)">
        <img v-if="t.avatarUrl" class="target-avatar" :src="t.avatarUrl"
             referrerpolicy="no-referrer">{{ t.name || t.id }}
        <span v-if="t.name" class="target-code">（{{t.id}}）</span>
      </el-tag>
      <span v-if="!selectedInfos.length" class="empty-tip">暂未选择</span>
    </div>
    <div class="form-tip">
      红色{{isGroup ? '群名' : '标签'}}表示未在{{isGroup ? '群列表' : '好友列表'}}中查询到，可能是机器人未加群/未添加好友，消息将不会推送给该目标
    </div>
    <div v-if="isGroup" class="form-tip">
      开启@全体成员后，开播消息会在该群@全体成员。需要机器人在群内是群主或管理员、群允许@全体成员且还有剩余次数，否则只推送消息不@全体成员
    </div>
    <div class="add-box">
      <div class="box-title">添加{{typeLabel}}</div>
      <el-select ref="picker" v-model="pendingIds" multiple filterable remote reserve-keyword
                 :remote-method="remoteSearch" :loading="searchLoading" size="small"
                 :disabled="disabled"
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
        <el-input v-model.trim="manualId" size="small" class="manual-input" :disabled="disabled"
                  :placeholder="isGroup ? '手动输入群号' : '手动输入QQ号'"
                  @keyup.enter.native="addManual"></el-input>
        <el-button type="primary" size="small" plain :disabled="disabled" @click="addManual">添加</el-button>
      </div>
    </div>
  </div>
</template>
<script>
import {targetList} from "@/api/bilibili-subscribe";

/**
 * 推送目标(群/好友)选择器
 * 用于bilibili订阅的推送群/推送好友编辑，新增修改订阅弹框与管理推送目标弹框共用，避免样式与逻辑重复维护
 *
 * 用法：
 * <push-target-select ref="xx" type="group" :ids.sync="groupIds" :at-all-ids.sync="atAllIds"/>
 * 弹框打开、或切换机器人后调用 ref.load(selfId) 加载候选与名称；关闭弹框时调用 ref.reset() 清掉缓存
 * selfId通过load方法显式传入，避免依赖prop更新时机(父组件改完selfId后prop要等下一次渲染才更新到子组件)
 * atAllIds(开播消息@全体成员)只对 type='group' 生效
 */
export default {
  name:'PushTargetSelect',
  props:{
    /**
     * group:推送群 friend:推送好友
     */
    type:{
      type:String,
      default:'group'
    },
    /**
     * 已选中的群号/qq号
     */
    ids:{
      type:Array,
      default:()=>[]
    },
    /**
     * 已开启@全体成员的群号，只对type='group'生效
     */
    atAllIds:{
      type:Array,
      default:()=>[]
    },
    /**
     * 提交中时禁用编辑
     */
    disabled:{
      type:Boolean,
      default:false
    }
  },
  data(){
    return{
      searchLoading:false,
      options:[],
      pendingIds:[],
      manualId:'',
      infoMap:{},
      resolvingIds:[],
      // 当前查询候选用的机器人qq号，由load方法传入，不依赖prop
      querySelfId:null
    }
  },
  computed:{
    isGroup(){
      return this.type === 'group'
    },
    typeLabel(){
      return this.isGroup ? '群' : '好友'
    },
    selectedInfos(){
      return this.ids.map(id=>this.infoMap[id] || {id, name:null, avatarUrl:null, found:false})
    }
  },
  watch:{
    // 弹框打开/切换订阅时父组件会替换ids，这里补一次名称解析(已在缓存中的不会重复请求)
    ids:{
      handler(){
        this.resolveInfos()
      },
      deep:true
    }
  },
  methods:{
    /**
     * 加载候选列表，并解析已选目标的名称/头像
     * 弹框打开后、切换机器人后由父组件调用
     * @param selfId 机器人qq号，用于查询该机器人已加入的群/已添加的好友
     */
    load(selfId){
      this.querySelfId = selfId === undefined ? null : selfId
      this.pendingIds = []
      this.manualId = ''
      this.remoteSearch('')
      this.resolveInfos()
    },
    /**
     * 关闭弹框时清掉缓存，避免残留上一个订阅的数据
     */
    reset(){
      this.options = []
      this.pendingIds = []
      this.manualId = ''
      this.infoMap = {}
      this.resolvingIds = []
      this.querySelfId = null
      this.searchLoading = false
    },
    optionLabel(o){
      return o.name ? `${o.name}（${o.id}）` : String(o.id)
    },
    targetTitle(t){
      if(t.found){
        return this.isGroup ? `群名称：${t.name || ''}  群号：${t.id}` : `昵称：${t.name || ''}  QQ：${t.id}`
      }
      return this.isGroup ? `群号：${t.id}（未查询到群信息，机器人可能未加入该群）`
        : `QQ：${t.id}（未查询到好友信息，机器人可能未添加该好友）`
    },
    remoteSearch(keyword){
      this.searchLoading = true
      targetList({
        selfId:this.querySelfId,
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
     * 查询已选目标中还没有名称/头像的项，正在查询中的不重复请求
     */
    resolveInfos(){
      const unknown = this.ids.filter(id=>!this.infoMap[id] && !this.resolvingIds.includes(id))
      if(!unknown.length){
        return
      }
      this.resolvingIds = this.resolvingIds.concat(unknown)
      targetList({
        selfId:this.querySelfId,
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
      }).finally(()=>{
        this.resolvingIds = this.resolvingIds.filter(id=>!unknown.includes(id))
      })
    },
    pickerChange(ids){
      const optionMap = new Map(this.options.map(o=>[o.id, o]))
      const selected = this.ids.slice()
      ids.forEach(id=>{
        const targetId = Number(id)
        if(!targetId || isNaN(targetId) || selected.includes(targetId)){
          return
        }
        selected.push(targetId)
        const option = optionMap.get(id) || optionMap.get(targetId)
        if(option){
          this.$set(this.infoMap, targetId, option)
        }
      })
      this.$nextTick(()=>{
        this.pendingIds = []
        this.updateIds(selected)
        this.resolveInfos()
      })
    },
    addManual(){
      const targetId = Number(this.manualId)
      if(!targetId || isNaN(targetId)){
        return this.$message.warning(this.isGroup ? '请输入正确的群号' : '请输入正确的QQ号')
      }
      if(this.ids.includes(targetId)){
        this.manualId = ''
        return this.$message.warning('该目标已存在')
      }
      this.manualId = ''
      this.updateIds(this.ids.concat(targetId))
      this.resolveInfos()
    },
    remove(id){
      this.updateIds(this.ids.filter(e=>e !== id))
    },
    toggleAtAll(id, value){
      if(value){
        if(!this.atAllIds.includes(id)){
          this.$emit('update:atAllIds', this.atAllIds.concat(id))
        }
      }else{
        this.$emit('update:atAllIds', this.atAllIds.filter(e=>e !== id))
      }
    },
    /**
     * 群被移除后，该群的@全体成员开关也要一并移除
     */
    updateIds(ids){
      this.$emit('update:ids', ids)
      if(this.isGroup){
        const keep = ids.filter(id=>this.atAllIds.includes(id))
        if(keep.length !== this.atAllIds.length){
          this.$emit('update:atAllIds', keep)
        }
      }
    }
  }
}
</script>
<style lang="scss" scoped>
.push-target-select{
  .box-title{
    font-size: 13px;
    color: #606266;
    margin-bottom: 6px;
  }
  .add-box{
    margin-top: 10px;
  }
  .chip-box{
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    min-height: 34px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 4px 6px;
  }
  .selected-list{
    min-height: 34px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 4px 6px;
  }
  .selected-row{
    display: flex;
    align-items: center;
    line-height: 26px;
    & + .selected-row{
      border-top: 1px dashed #ebeef5;
    }
    .selected-name{
      font-size: 13px;
      color: #303133;
      max-width: 220px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      &.not-found{
        color: #f56c6c;
      }
    }
    .target-code{
      flex: none;
      color: #909399;
      font-size: 12px;
    }
    .row-space{
      flex: 1;
    }
    .at-all-label{
      flex: none;
      font-size: 12px;
      color: #909399;
      margin-right: 6px;
    }
    .row-remove{
      flex: none;
      margin-left: 6px;
      color: #909399;
      &:hover{
        color: #f56c6c;
      }
    }
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

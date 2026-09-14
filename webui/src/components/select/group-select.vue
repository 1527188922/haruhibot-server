<template>
  <el-select class="group-select" :style="{width: width}" :value="innerValue" filterable remote
             :remote-method="remoteSearch"
             :allow-create="allowCreateOption"
             :loading="loading"
             :disabled="disabled"
             :clearable="clearable"
             :size="size"
             :placeholder="placeholder"
             :popper-class="popperClass"
             @change="handleChange"
             @visible-change="handleVisibleChange">
    <!-- 同一个群号可能对应多条群信息(不同机器人/群改名)，所以key不能只用群号 -->
    <el-option v-for="o in options" :key="`${o.code}|${o.name}`" :label="optionLabel(o)" :value="o.code">
      <div class="option-item">
        <img v-if="o.avatarUrl" class="target-avatar" :src="o.avatarUrl" referrerpolicy="no-referrer">
        <span class="option-name">{{ `${o.name || o.code}（${o.code}）` }}</span>
      </div>
    </el-option>
  </el-select>
</template>
<script>
import {codeNameList} from "@/api/group";

/**
 * 群号选择器
 * 下拉候选(群号/群名/群头像)由组件内部调用群列表接口获取，使用方只需v-model绑定群号
 * 与推送目标选择器(push-target-select)的下拉option样式保持一致
 *
 * limit>0：按关键字远程搜索(输入防抖)；输入框没有过滤值时每次展开下拉都重新请求一次，避免只展示上一次搜索的结果
 * limit<=0：候选为全量群列表，只在首次展开时请求一次接口，之后(关键字搜索、清空关键字)都只在内存中过滤
 * 输入框里直接输入群号(纯数字)可以选中群列表中不存在的群号，方便机器人未加群时按群号查询
 *
 * 用法：
 * <group-select v-model="queryFormObj.groupId" width="180px" placeholder="输入群号或群名"/>
 * <group-select v-model="queryFormObj.groupId" :limit="0"/>   全量候选，只在首次展开时请求一次
 * 事件：change(value, group)  group为选中项({code,name,avatarUrl})，手动输入且群列表查不到时为null
 */
export default {
  name:'GroupSelect',
  props:{
    /**
     * 群号，空值为''或null
     */
    value:{
      type:[Number,String],
      default:''
    },
    /**
     * 组件宽度，父组件需要不同宽度时传入
     */
    width:{
      type:String,
      default:'100%'
    },
    placeholder:{
      type:String,
      default:'输入群号或群名'
    },
    disabled:{
      type:Boolean,
      default:false
    },
    clearable:{
      type:Boolean,
      default:true
    },
    /**
     * 是否允许输入群列表中不存在的群号
     */
    allowCreate:{
      type:Boolean,
      default:true
    },
    size:{
      type:String,
      default:'small'
    },
    /**
     * 候选数量上限
     * 小于等于0表示不限制数量(全量候选)，此时只在首次展开时请求一次接口，之后都在内存中过滤
     */
    limit:{
      type:Number,
      default:50
    },
    /**
     * 下拉框样式命名空间，需要单独覆盖下拉样式时传入
     */
    popperClass:{
      type:String,
      default:'group-select-popper'
    }
  },
  data(){
    return{
      // 下拉框当前展示的候选
      options:[],
      // 全量模式(limit<=0)下缓存的全量候选
      allOptions:[],
      // 全量候选是否已请求过
      fullLoaded:false,
      loading:false,
      // 当前输入的关键字，el-select的输入框内容不对外暴露，这里在remote-method里自己记录
      keyword:'',
      // 输入防抖定时器
      searchTimer:null,
      // 请求序号，只采用最后一次请求的结果，避免先发出的请求后返回覆盖结果
      searchSeq:0,
      // 正在解析名称的群号，避免重复请求
      resolvingValue:null
    }
  },
  watch:{
    // 外部直接赋值的群号(如回显、重置)也需要拿到群名/头像
    value:{
      immediate:true,
      handler(v){
        this.resolveValue(v)
      }
    }
  },
  computed:{
    /**
     * 统一成数字后再交给el-select，否则外部传入字符串群号时无法与候选(数字群号)匹配上，会直接展示群号
     */
    innerValue(){
      return this.normalizeValue(this.value)
    },
    /**
     * 全量模式：候选就是整个群列表，不需要每次搜索都请求接口
     */
    fullMode(){
      return !(this.limit > 0)
    },
    /**
     * 只有输入的是群号(纯数字)才允许创建候选，避免把群名当成群号提交
     */
    allowCreateOption(){
      return this.allowCreate && /^\d+$/.test(this.keyword)
    }
  },
  beforeDestroy(){
    this.clearSearchTimer()
  },
  methods:{
    optionLabel(o){
      return o.name ? `${o.name}（${o.code}）` : String(o.code)
    },
    /**
     * 输入关键字搜索候选群，防抖处理，避免每输入一个字符都请求接口
     */
    remoteSearch(keyword){
      this.keyword = keyword || ''
      this.clearSearchTimer()
      // 全量模式：只在内存中过滤，只有还没拿到全量候选时才请求一次
      if(this.fullMode){
        this.ensureFullOptions()
        return
      }
      // 没有过滤值：立即拉取(避免展示上一次搜索的结果)，有关键字时防抖
      if(!this.keyword){
        this.loadOptions('')
        return
      }
      this.searchTimer = setTimeout(()=>{
        this.searchTimer = null
        this.loadOptions(this.keyword)
      },300)
    },
    clearSearchTimer(){
      if(this.searchTimer){
        clearTimeout(this.searchTimer)
        this.searchTimer = null
      }
    },
    /**
     * 下拉框展开时加载候选
     * el-select首次展开不会触发remote-method，所以这里主动请求一次
     * 非全量模式每次展开都没有过滤值，重新请求一次，避免只展示上一次搜索的结果
     */
    handleVisibleChange(visible){
      this.keyword = ''
      if(!visible){
        return
      }
      this.clearSearchTimer()
      if(this.fullMode){
        this.ensureFullOptions()
        return
      }
      this.loadOptions('')
    },
    /**
     * 全量模式：全量候选只请求一次，之后(含关键字搜索)都只在内存中过滤
     */
    ensureFullOptions(){
      if(this.fullLoaded){
        this.options = this.filterFromAll(this.keyword)
        return
      }
      if(!this.loading){
        this.loadOptions('')
      }
    },
    loadOptions(keyword){
      const seq = ++this.searchSeq
      this.loading = true
      codeNameList({
        // 全量模式不带过滤条件(limit<=0时后端不会加LIMIT)，拿到全量后在内存中过滤
        codeOrName:this.fullMode ? '' : (keyword || ''),
        limit:this.fullMode ? 0 : this.limit
      }).then(({data:{code,message,data}})=>{
        if(seq !== this.searchSeq){
          return
        }
        if(code !== 200){
          return this.$message.error(message || '群列表查询失败')
        }
        const list = data || []
        if(this.fullMode){
          this.fullLoaded = true
          this.allOptions = list
          // 请求期间用户可能已经输入了关键字，这里按当前关键字过滤
          this.options = this.filterFromAll(this.keyword)
          return
        }
        // 已选中的群如果不在本次候选里(如手动输入的群号)，保留其名称/头像
        const selected = this.findOption(this.value)
        if(selected && !this.contains(list,selected.code)){
          list.push(selected)
        }
        this.options = list
      }).catch(e=>{
        if(seq === this.searchSeq){
          this.$message.error(e.message)
        }
      }).finally(()=>{
        if(seq === this.searchSeq){
          this.loading = false
        }
      })
    },
    /**
     * 全量候选的内存过滤，关键字为空时返回全量
     */
    filterFromAll(keyword){
      const k = (keyword || '').toLowerCase()
      if(!k){
        return this.allOptions.slice()
      }
      return this.allOptions.filter(o=>this.matchKeyword(o,k))
    },
    /**
     * keyword需要是已经转成小写的
     */
    matchKeyword(o,keyword){
      return String(o.code).toLowerCase().indexOf(keyword) !== -1
        || (o.name || '').toLowerCase().indexOf(keyword) !== -1
    },
    /**
     * 查询群号对应的群名/头像，用于外部赋值的群号回显
     * 查不到(机器人未加群或被删除)时不处理，el-select会直接展示群号
     */
    resolveValue(value){
      if(value === '' || value === null || value === undefined){
        return
      }
      // 全量候选已在内存中，直接取
      if(this.fullLoaded){
        this.pushOption(this.findIn(this.allOptions,value))
        return
      }
      if(this.findOption(value)){
        return
      }
      const code = String(value)
      if(this.resolvingValue === code){
        return
      }
      this.resolvingValue = code
      codeNameList({
        codeOrName:code,
        eqCode:true,
        limit:1
      }).then(({data:{code:respCode,data}})=>{
        if(respCode !== 200){
          return
        }
        this.pushOption((data || []).find(e=>String(e.code) === code))
      }).catch(()=>{
        // 回显失败静默处理，不影响组件使用
      }).finally(()=>{
        if(this.resolvingValue === code){
          this.resolvingValue = null
        }
      })
    },
    pushOption(info){
      if(info && !this.contains(this.options,info.code)){
        this.options.push(info)
      }
    },
    handleChange(v){
      const value = this.normalizeValue(v)
      this.$emit('input',value)
      this.$emit('change',value,this.findOption(value))
      // 选中的群不在候选中时补一次名称解析
      this.resolveValue(value)
    },
    /**
     * 选中候选时群号是数字，手动输入时是字符串，这里统一成数字，保证查询参数类型一致
     */
    normalizeValue(v){
      if(v === '' || v === null || v === undefined){
        return ''
      }
      const s = String(v)
      return /^\d+$/.test(s) ? Number(s) : s
    },
    contains(list,code){
      return list.some(e=>String(e.code) === String(code))
    },
    findIn(list,code){
      if(code === '' || code === null || code === undefined){
        return null
      }
      return list.find(e=>String(e.code) === String(code)) || null
    },
    findOption(code){
      return this.findIn(this.options,code)
    }
  }
}
</script>
<style lang="scss" scoped>
.group-select{
  ::v-deep .el-input{
    width: 100%;
  }
}
</style>
<style lang="scss">
/**
 * el-select的下拉框默认append到body下(不在本组件dom树内)，scoped样式匹配不到，
 * 所以这里不写scoped，用popper-class做命名空间
 * 样式与bilibili推送目标选择器(bili-target-select-popper)保持一致
 */
.group-select-popper{
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

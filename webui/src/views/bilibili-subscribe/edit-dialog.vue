<template>
  <div id="BilibiliSubscribeEditDialog">
    <el-dialog :visible.sync="visible" :title="title" width="520px" @closed="dialogClosed" v-dialogDrag
               :close-on-click-modal="false">
      <el-form :model="formData" label-position="right" ref="editForm" label-width="100px" size="small">
        <el-form-item label="主播UID" prop="uid" :rules="[{required: true, message:'请输入b站主播uid',trigger: 'blur'}]">
          <number-input v-model.trim="formData.uid" maxlength="20" placeholder="b站主播uid"></number-input>
        </el-form-item>
        <el-form-item label="机器人QQ" prop="selfId"
                      :rules="[{required: true, message:'请选择或输入机器人QQ号',trigger: 'change'}]">
          <el-select v-model="formData.selfId" class="full-width" filterable allow-create default-first-option
                     placeholder="选择当前已连接的机器人，或直接输入QQ号" @focus="loadBots">
            <el-option v-for="b in botList" :key="b.id" :label="botLabel(b)" :value="b.id">
              <img v-if="b.avatarUrl" class="bot-avatar" :src="b.avatarUrl">
              <span>{{b.name || b.id}}</span>
              <span class="bot-id">{{b.id}}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="订阅类型" prop="subType">
          <el-select v-model="formData.subType" placeholder="请选择">
            <el-option label="b站主播开播推送" value="live"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="推送群号" prop="groupIds">
          <el-input v-model="formData.groupIds" type="textarea" :autosize="{ minRows: 2}"
                    placeholder="多个群号用逗号分割，可不填"></el-input>
        </el-form-item>
        <el-form-item label="推送好友QQ" prop="friendIds">
          <el-input v-model="formData.friendIds" type="textarea" :autosize="{ minRows: 2}"
                    placeholder="多个QQ号用逗号分割，可不填"></el-input>
        </el-form-item>
        <el-form-item label="是否启用" prop="enableStatus">
          <el-switch v-model="formData.enableStatus" :active-value="1" :inactive-value="0"></el-switch>
        </el-form-item>
        <el-form-item label="下播推送" prop="offNotify">
          <el-switch v-model="formData.offNotify" :active-value="1" :inactive-value="0"></el-switch>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit" :loading="submitLoading">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>
<script>
import numberInput from "@/components/input/numberInput.vue";
import {deepClone} from "@/util/util";
import {add, update} from "@/api/bilibili-subscribe";
import {botList as botListApi} from "@/api/system";

export default {
  name:'BilibiliSubscribeEditDialog',
  components:{
    numberInput
  },
  data(){
    return{
      visible:false,
      submitLoading:false,
      isAdd:false,
      title:'',
      botList:[],
      formData:this.emptyData()
    }
  },
  methods:{
    emptyData(){
      return{
        uid:'',
        selfId:'',
        subType:'live',
        groupIds:'',
        friendIds:'',
        enableStatus:1,
        offNotify:0
      }
    },
    botLabel(bot){
      return bot.name ? `${bot.name}（${bot.id}）` : String(bot.id)
    },
    parseNumber(value){
      if(value === '' || value === null || value === undefined){
        return null
      }
      const num = Number(value)
      return isNaN(num) ? null : num
    },
    /**
     * 加载当前ws连接中的机器人，供选择；也允许自定义输入QQ号
     */
    loadBots(){
      botListApi().then(({data:{code,data}})=>{
        if(code !== 200){
          return
        }
        let list = data || []
        const current = this.parseNumber(this.formData.selfId)
        if(current && !list.some(e=>e.id === current)){
          list = [{id:current, name:'未连接', avatarUrl:null}].concat(list)
        }
        this.botList = list
      })
    },
    open(row){
      this.visible = true;
      this.isAdd = row ? false : true;
      this.title = this.isAdd ? '新增BILIBILI订阅' : '修改BILIBILI订阅';
      this.$nextTick(()=>{
        this.formData = row ? this.toFormData(row) : this.emptyData()
        this.loadBots()
      })
    },
    toFormData(row){
      const data = deepClone(row)
      return{
        id:data.id,
        uid:data.uid || '',
        selfId:data.selfId || '',
        subType:data.subType || 'live',
        groupIds:data.groupIds || '',
        friendIds:data.friendIds || '',
        enableStatus:data.enableStatus === 0 ? 0 : 1,
        offNotify:data.offNotify === 1 ? 1 : 0
      }
    },
    submit(){
      const selfId = this.parseNumber(this.formData.selfId)
      if(!selfId){
        return this.$message.warning('请选择或输入正确的机器人QQ号')
      }
      this.$refs.editForm.validate((valid)=>{
        if(!valid){
          return
        }
        this.$confirm('确认提交？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(()=>{
          const fun = this.isAdd ? add : update
          const payload = Object.assign({}, this.formData, {selfId})
          this.submitLoading = true
          fun(payload).then(({data:{code,message}})=>{
            if(code !== 200){
              return this.$message.error(message)
            }
            this.$message.success(message)
            this.visible = false
            if(this.$parent && this.$parent.search){
              this.$parent.search()
            }
          }).catch(e=>{
            this.$message.error(e.message)
          }).finally(()=>{
            this.submitLoading = false
          })
        })
      })
    },
    dialogClosed(){
      this.$refs.editForm.resetFields();
      this.title = ''
      this.isAdd = false
      this.formData = this.emptyData()
    }
  }
}
</script>
<style lang="scss" scoped>
#BilibiliSubscribeEditDialog{
  .full-width{
    width: 100%;
  }
  .bot-avatar{
    width: 16px;
    height: 16px;
    border-radius: 50%;
    margin-right: 5px;
    vertical-align: middle;
  }
  .bot-id{
    color: #909399;
    font-size: 12px;
    margin-left: 8px;
    float: right;
  }
  .form-tip{
    font-size: 12px;
    color: #909399;
    line-height: 18px;
  }
}
</style>

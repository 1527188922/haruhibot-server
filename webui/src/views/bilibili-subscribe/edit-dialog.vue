<template>
  <div id="BilibiliSubscribeEditDialog">
    <el-dialog :visible.sync="visible" :title="title" width="520px" @closed="dialogClosed" v-dialogDrag
               :close-on-click-modal="false">
      <el-form :model="formData" label-position="right" ref="editForm" label-width="100px" size="small">
        <el-form-item label="主播UID" prop="uid" :rules="[{required: true, message:'请输入b站主播uid',trigger: 'blur'}]">
          <number-input v-model.trim="formData.uid" maxlength="20" placeholder="b站主播uid"></number-input>
        </el-form-item>
        <el-form-item label="机器人QQ" prop="selfId" :rules="[{required: true, message:'请输入机器人QQ号',trigger: 'blur'}]">
          <number-input v-model.trim="formData.selfId" maxlength="20" placeholder="推送消息的机器人QQ号"></number-input>
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
    open(row){
      this.visible = true;
      this.isAdd = row ? false : true;
      this.title = this.isAdd ? '新增BILIBILI订阅' : '修改BILIBILI订阅';
      this.$nextTick(()=>{
        this.formData = row ? this.toFormData(row) : this.emptyData()
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
          this.submitLoading = true
          fun(this.formData).then(({data:{code,message}})=>{
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
  .form-tip{
    font-size: 12px;
    color: #909399;
    line-height: 18px;
  }
}
</style>

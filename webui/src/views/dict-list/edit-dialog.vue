<template>
  <div id="DictEditDialog">
    <el-dialog :visible.sync="visible" :title="title" width="450px" @closed="dialogClosed" v-dialogDrag
               :close-on-click-modal="false">
      <el-form :model="formData" label-position="right" ref="editForm" label-width="50px">
        <el-form-item label="key" prop="key" :rules="[{required: true, message:'请输入key',trigger: 'blur'}]">
          <el-input v-model.trim="formData.key" maxlength="255"></el-input>
        </el-form-item>

        <el-form-item label="value" prop="content" >
          <el-input v-model="formData.content" maxlength="500" type="textarea" :autosize="{ minRows: 2}" ></el-input>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" maxlength="500" type="textarea" :autosize="{ minRows: 3}" ></el-input>
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
import {deepClone} from "@/util/util";
import  {add,update} from "@/api/dictionary";

export default {
  name:'DictEditDialog',
  data(){
    return{
      visible:false,
      submitLoading:false,
      isAdd:false,
      title:'',
      formData:this.emptyData()
    }
  },
  created() {

  },
  methods:{
    emptyData(){
      return{
        key:'',
        content:'',
        remark:''
      }
    },
    open(v){
      this.visible = true;
      this.isAdd = v ? false : true;
      this.title = this.isAdd ? '新增' : '修改';
      this.$nextTick(()=>{
        this.formData = v ? deepClone(v) : this.emptyData()
      })
    },
    submit(){
      this.$refs.editForm.validate((valida)=>{
        if(valida){
          this.$confirm('确认提交？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(()=>{
            let fun = this.isAdd ? add : update
            this.submitLoading = true
            fun(this.formData).then(({data:{code,message}})=>{
              if(code !== 200){
                return this.$message.error(message)
              }
              this.$message.success(message)
              this.visible = false
              this.$parent.search()
            }).catch(e=>{
              this.$message.error(e.message)
            }).finally(()=>{
              this.submitLoading = false
            })
          })

        }
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
<style>
#DictEditDialog{
}
</style>
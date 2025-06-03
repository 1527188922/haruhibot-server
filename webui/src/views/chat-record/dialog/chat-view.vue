<template>
  <div id="ChatView">
    <el-dialog :visible.sync="visible" :title="title" width="600px" @closed="dialogClosed" v-dialogDrag>
      <el-form>
        <el-form label=" ">
          <el-input type="textarea" v-model="row.content" :rows="6" readonly></el-input>
        </el-form>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="visible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import {deepClone} from "@/util/util";

export default {
  computed: {
    ro() {
      return ro
    }
  },
  data(){
    return{
      title:'',
      visible:false,
      row:{}
    }
  },
  methods:{
    open(row){
      this.visible = true
      this.$nextTick(()=>{
        this.title = `${row.userId}`
        this.row = deepClone(row)
      })
    },
    dialogClosed(){
      this.title = ''
      this.row = {}
    }
  }
}
</script>
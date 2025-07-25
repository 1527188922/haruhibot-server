<template>
  <div id="RefreshResultDialog">
    <el-dialog :visible.sync="visible" title="刷新群聊完成" width="450px" @closed="dialogClosed" v-dialogDrag
    :close-on-click-modal="false">
<!--      <PopoverResultPlan :result="result"/>-->
      <CollapseResultPlan :result="result"/>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="visible = false"
                   icon="el-icon-circle-close">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
// import PopoverResultPlan from "./popover-result-plan";
import CollapseResultPlan from "./collapse-result-plan";
export default {
  name:'RefreshResultDialog',
  components:{
    // PopoverResultPlan,
    CollapseResultPlan
  },
  data(){
    return{
      visible:false,
      result:[]
    }
  },
  created() {
  },
  methods:{
    open(v){
      this.visible = true
      this.$nextTick(()=>{
        this.result = v
      })
    },
    dialogClosed(){
      this.result = []
    }
  }
}
</script>
<style scoped lang="scss">
#RefreshResultDialog{

}
</style>
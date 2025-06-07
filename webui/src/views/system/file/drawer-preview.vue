<template>
  <div id="DrawerPreview">
    <el-drawer
        :title="nodeData.fileName"
        :visible.sync="drawer"
        :direction="direction"
        :destroy-on-close="true"
        size="70%"
        @closed="closed">
      <div style="padding: 0 20px;min-height: 100px" v-loading="readLoading" >
        <pre style="margin-top: 0">{{content}}</pre>
      </div>
    </el-drawer>
  </div>
</template>
<script>
import {readFileContent} from "@/api/system";

export default {
  name:'DrawerPreview',
  props:{
    direction:{
      type:String,
      default:'rtl'
    },
  },
  data(){
    return{
      readLoading:false,
      drawer: false,
      content:'',
      nodeData: {
        fileName:''
      }
    }
  },
  created() {
  },
  methods:{
    open(nodeData){
      this.drawer = true
      this.$nextTick(()=>{
        this.nodeData = nodeData
        this.load(nodeData)
      })
    },
    closed(){
      this.content = ''
      this.nodeData = {
        fileName:''
      }
    },
    load(nodeData){
      this.readLoading = true
      readFileContent(nodeData).then(({data:{code,data}})=>{
        this.content = data
      }).catch(e=>{
      }).finally(()=>{
        this.readLoading = false
      })
    }
  }
}
</script>
<style lang="scss" scoped>
#DrawerPreview{

}
</style>
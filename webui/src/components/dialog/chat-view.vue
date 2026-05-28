<template>
  <div id="ChatView">
    <el-dialog :visible.sync="visible" :title="title" width="600px" @closed="dialogClosed" v-dialogDrag>
      <multi-cell slot="title" :text-list="[title]" :image-url="avatarUrl"></multi-cell>
      <el-form>
        <el-form label=" ">
          <el-input type="textarea" autosize v-model="text" readonly></el-input>
        </el-form>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="visible = false"
        icon="el-icon-circle-close">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>

import MultiCell from "@/components/multi-cell.vue";
export default {
  name:'ChatViewDialog',
  components: {
    MultiCell
  },
  data(){
    return{
      title:'',
      visible:false,
      text:'',
      avatarUrl:''
    }
  },
  methods:{
    open(text,title,avatarUrl = ''){
      this.visible = true
      this.$nextTick(()=>{
        this.title = title
        this.text = text
        this.avatarUrl = avatarUrl
      })
    },
    dialogClosed(){
      this.title = ''
      this.text = ''
    }
  }
}
</script>
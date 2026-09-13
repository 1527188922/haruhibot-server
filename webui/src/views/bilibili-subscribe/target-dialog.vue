<template>
  <div id="BilibiliSubscribeTargetDialog">
    <el-dialog :visible.sync="visible" :title="title" width="660px" @closed="dialogClosed" v-dialogDrag
               :close-on-click-modal="false">
      <push-target-select ref="selector" :type="type"
                          :ids.sync="selectedIds" :at-all-ids.sync="atAllIds"
                          :disabled="submitLoading"></push-target-select>
      <span slot="footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit" :loading="submitLoading">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>
<script>
import PushTargetSelect from "./push-target-select.vue";
import {updateTargets} from "@/api/bilibili-subscribe";
import {parseIds} from "@/util/bili-subscribe";

export default {
  name:'BilibiliSubscribeTargetDialog',
  components:{
    PushTargetSelect
  },
  data(){
    return{
      visible:false,
      submitLoading:false,
      title:'',
      row:null,
      type:'group',
      selfId:null,
      callback:null,
      selectedIds:[],
      atAllIds:[]
    }
  },
  methods:{
    open(row, type, callback){
      this.visible = true
      this.row = row
      this.type = type
      this.selfId = row.selfId
      this.callback = callback
      this.title = `管理推送${type === 'group' ? '群' : '好友'} - ${row.uname || row.uid}`
      this.selectedIds = parseIds(type === 'group' ? row.groupIds : row.friendIds)
      this.atAllIds = parseIds(row.atAllGroupIds)
      this.$nextTick(()=>{
        this.$refs.selector.load(this.selfId)
      })
    },
    submit(){
      const payload = {
        id: this.row.id,
        groupIds: this.type === 'group' ? this.selectedIds.slice() : parseIds(this.row.groupIds),
        atAllGroupIds: this.type === 'group' ? this.atAllIds.slice() : parseIds(this.row.atAllGroupIds),
        friendIds: this.type === 'friend' ? this.selectedIds.slice() : parseIds(this.row.friendIds)
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
      this.selectedIds = []
      this.atAllIds = []
      this.title = ''
      this.$refs.selector.reset()
    }
  }
}
</script>

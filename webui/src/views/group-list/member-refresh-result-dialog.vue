<template>
  <div id="MemberRefreshResultDialog">
    <el-dialog :visible.sync="visible" :title="title" width="520px" @closed="dialogClosed" v-dialogDrag
               :close-on-click-modal="false">
      <template v-if="result">
        <el-collapse v-model="activeNames">
          <el-collapse-item name="current" :disabled="!memberCount">
            <template slot="title">
              <span>本次获取成员数量：{{memberCount}}</span>
            </template>
            <div>在群成员数量以qq客户端最新数据为准，离群成员只做标记不会从数据库中删除。</div>
          </el-collapse-item>
          <el-collapse-item name="added" :disabled="!hasData(addedList)">
            <template slot="title">
              <span class="success-text">新增成员数量：{{size(addedList)}}</span>
            </template>
            <div v-for="member in addedList" :key="`added-${member.userId}`" class="member-line">
              <multi-cell :image-url="member.userAvatarUrl"
                          :text-list="[member.userId, member.card || member.nickname]"></multi-cell>
            </div>
          </el-collapse-item>
          <el-collapse-item name="left" :disabled="!hasData(leftList)">
            <template slot="title">
              <span class="danger-text">离群成员数量：{{size(leftList)}}</span>
            </template>
            <div v-for="member in leftList" :key="`left-${member.userId}`" class="member-line">
              <multi-cell :image-url="member.userAvatarUrl"
                          :text-list="[member.userId, member.card || member.nickname]"></multi-cell>
            </div>
          </el-collapse-item>
          <el-collapse-item name="rejoin" :disabled="!hasData(rejoinList)">
            <template slot="title">
              <span class="warning-text">重新入群成员数量：{{size(rejoinList)}}</span>
            </template>
            <div v-for="member in rejoinList" :key="`rejoin-${member.userId}`" class="member-line">
              <multi-cell :image-url="member.userAvatarUrl"
                          :text-list="[member.userId, member.card || member.nickname]"></multi-cell>
            </div>
          </el-collapse-item>
        </el-collapse>
      </template>
      <template v-else>
        无数据
      </template>
    </el-dialog>
  </div>
</template>
<script>
import MultiCell from "@/components/multi-cell.vue";

export default {
  name:'MemberRefreshResultDialog',
  components:{MultiCell},
  data(){
    return{
      visible:false,
      row:null,
      result:null,
      activeNames:['current','left']
    }
  },
  computed:{
    title(){
      return this.row ? `刷新群成员完成：${this.row.groupName}（${this.row.groupId}）` : '刷新群成员完成'
    },
    memberCount(){
      return this.result && this.result.memberCount ? this.result.memberCount : 0
    },
    addedList(){
      return (this.result && this.result.addedList) || []
    },
    leftList(){
      return (this.result && this.result.leftList) || []
    },
    rejoinList(){
      return (this.result && this.result.rejoinList) || []
    }
  },
  methods:{
    size(list){
      return (list || []).length
    },
    hasData(list){
      return !!(list && list.length > 0)
    },
    open(row,result){
      this.visible = true
      this.$nextTick(()=>{
        this.row = row
        this.result = result
      })
    },
    dialogClosed(){
      this.row = null
      this.result = null
    }
  }
}
</script>
<style scoped lang="scss">
#MemberRefreshResultDialog{
  .member-line{
    padding: 2px 0;
  }
}
</style>

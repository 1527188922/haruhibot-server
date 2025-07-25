<template>
  <div id="RefreshResultDialog">
    <el-dialog :visible.sync="visible" title="刷新群聊完成" width="450px" @closed="dialogClosed" v-dialogDrag
    :close-on-click-modal="false">
      <el-row v-for="item in result" :key="item.selfId" class="result-list-row">
        <el-col :span="24">
          <label>机器人：</label><span>{{item.selfId}}</span>
        </el-col>
        <el-col :span="24" >
          <el-popover
              placement="right"
              :disabled="!item.groupList || item.groupList.length === 0"
              trigger="click">
            <div v-for="group in item.groupList">
              {{`${group.groupName}(${group.groupId})`}}
            </div>
            <div class="reference" slot="reference">
              <label class="label">当前群聊数量：</label><span>{{item.groupList.length}}</span>
            </div>
          </el-popover>
        </el-col>

        <el-col :span="24" class="success-text">
          <el-popover
              placement="right"
              :disabled="!item.addedGroupList || item.addedGroupList.length === 0"
              trigger="click">
            <div v-for="group in item.addedGroupList">
              {{`${group.groupName}(${group.groupId})`}}
            </div>
            <div class="reference" slot="reference">
              <label class="label">新增群聊数量：</label><span>{{item.addedGroupList.length}}</span>
            </div>
          </el-popover>
        </el-col>

        <el-col :span="24" class="danger-text">
          <el-popover
              placement="right"
              :disabled="!item.removedGroupList || item.removedGroupList.length === 0"
              trigger="click">
            <div v-for="group in item.removedGroupList">
              {{`${group.groupName}(${group.groupId})`}}
            </div>
            <div class="reference" slot="reference">
              <label class="label">离群聊数量：</label><span>{{item.removedGroupList.length}}</span>
            </div>
          </el-popover>
        </el-col>
      </el-row>
      <div slot="footer" class="dialog-footer">
        <el-button size="small" @click="visible = false"
                   icon="el-icon-circle-close">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>
<script>
export default {
  name:'RefreshResultDialog',
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
  .result-list-row{
    border-bottom: 1px solid #d5dce8;
    .el-col{
      .reference{
        width: fit-content;
        cursor: pointer;
        .label{
          cursor: pointer;
        }
      }
    }
  }
}
</style>
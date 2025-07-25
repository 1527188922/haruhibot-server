<template>
  <div class="popover-result-plan">
    <el-row v-for="item in result" :key="item.selfId">
      <el-col :span="24">
        <label>机器人：</label><span>{{item.selfId}}</span>
      </el-col>
      <el-col :span="24" >
        <el-popover
            placement="right"
            :disabled="!item.groupList || item.groupList.length === 0"
            trigger="click">
          <div v-for="group in item.groupList" :key="`current-${group.groupId}`">
            {{`${group.groupName}(${group.groupId})`}}
          </div>
          <div class="reference" slot="reference">
            <label class="label">当前群数量：</label><span>{{item.groupList.length}}</span>
          </div>
        </el-popover>
      </el-col>

      <el-col :span="24" class="success-text">
        <el-popover
            placement="right"
            :disabled="!item.addedGroupList || item.addedGroupList.length === 0"
            trigger="click">
          <div v-for="group in item.addedGroupList" :key="`added-${group.groupId}`">
            {{`${group.groupName}(${group.groupId})`}}
          </div>
          <div class="reference" slot="reference">
            <label class="label">新增群数量：</label><span>{{item.addedGroupList.length}}</span>
          </div>
        </el-popover>
      </el-col>

      <el-col :span="24" class="danger-text">
        <el-popover
            placement="right"
            :disabled="!item.removedGroupList || item.removedGroupList.length === 0"
            trigger="click">
          <div v-for="group in item.removedGroupList" :key="`removed-${group.groupId}`">
            {{`${group.groupName}(${group.groupId})`}}
          </div>
          <div class="reference" slot="reference">
            <label class="label">离群数量：</label><span>{{item.removedGroupList.length}}</span>
          </div>
        </el-popover>
      </el-col>
    </el-row>
  </div>
</template>
<script>
export default {
  name:'popover-result-plan',
  props:{
    result:{
      type:Array,
      default:() => {
        return []
      }
    }
  }
}
</script>
<style lang="scss" scoped>
.popover-result-plan{
  .el-row{
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
<template>
  <div class="collapse-result-plan">
    <el-row v-for="item in result" :key="item.selfId">
      <el-divider content-position="left">
        <div class="face-and-id">
          <img :src="item.selfAvatarUrl"/>
          {{item.selfId}}
        </div>
      </el-divider>

      <el-collapse>
        <el-collapse-item :disabled="!item.groupList || item.groupList.length === 0">
          <template slot="title">
            <span>当前群数量：{{item.groupList.length}}</span>
          </template>
          <div v-for="group in item.groupList" :key="`current-${group.groupId}`">
            {{`${group.groupName}（${group.groupId}）`}}
          </div>
        </el-collapse-item>
        <el-collapse-item :disabled="!item.addedGroupList || item.addedGroupList.length === 0">
          <template slot="title">
            <span class="success-text">新增群数量：{{item.addedGroupList.length}}</span>
          </template>
          <div v-for="group in item.addedGroupList" class="success-text" :key="`added-${group.groupId}`">
            {{`${group.groupName}（${group.groupId}）`}}
          </div>
        </el-collapse-item>
        <el-collapse-item :disabled="!item.removedGroupList || item.removedGroupList.length === 0">
          <template slot="title">
            <span class="danger-text">离群数量：{{item.removedGroupList.length}}</span>
          </template>
          <div v-for="group in item.removedGroupList" class="danger-text" :key="`removed-${group.groupId}`">
            {{`${group.groupName}（${group.groupId}）`}}
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-row>
  </div>
</template>

<script>
export default {
  name:'collapse-result-plan',
  props:{
    result:{
      type:Array,
      default:() => {
        return []
      }
    }
  },
  data(){
    return{
      activeNames:[]
    }
  }
}
</script>
<style lang="scss" scoped>
.collapse-result-plan{

  .el-row:not(:last-child){
    margin-bottom: 30px;
  }
  .el-row{
    border-left: 1px solid #DCDFE6;
    border-right: 1px solid #DCDFE6;
    border-bottom: 1px solid #DCDFE6;
    .el-divider{
      margin-top: 0 !important;
    }
    .el-collapse{
      border-top: none !important;
      ::v-deep .el-collapse-item__header{
        border-bottom: none;
        span{
          padding-left: 10px;
        }
      }
      ::v-deep .el-collapse-item__wrap{
        border-bottom: none;
        .el-collapse-item__content{
          padding-left: 10px;
          padding-bottom: 0;
        }
      }
    }
  }
}
</style>
<template>
  <div class="result-panel">
    <template v-if="results && results.length > 0">
      <el-tabs v-model="activeName" type="border-card" closable @tab-remove="handleTabRemove">
        <el-tab-pane :key="item.name" v-for="(item, index) in results" :name="item.name">
          <span slot="label"><i class="el-icon-date"></i> {{ item.title }}</span>
          <template v-if="item.type === 'QUERY'">
            <el-table :data="formatTableData(item.data)" border size="small">
              <!-- 动态表头 -->
              <el-table-column  v-for="(field, index) in dynamicHeaders(item.data)"
                  :key="index"
                  show-overflow-tooltip
                  :label="field"
                  :prop="field">
                <template slot-scope="scope">
                  <span v-if="scope.row[field] !== null">
                    {{ scope.row[field] }}
                  </span>
                  <span v-else class="null-value">
                    NULL
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </template>

        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>
<script>
export default {
  data(){
    return{
      results:[],
      activeName:''
    }
  },
  computed:{
  },
  methods:{
    handleTabRemove(name){
      let index = this.results.findIndex(e => e.name === name)
      this.results.splice(index,1)
    },
    updateResult(v){
      this.$nextTick(()=>{
        this.results = this.handleResult(v)
      })
    },
    handleResult(v){
      if(!v || v.length === 0){
        return []
      }
      let list = v.map((e,i) => {
        return {
          name:e.type + 'name'+i,
          title:e.type + 'title'+i,//tab展示的title
          type:e.type,
          data:e.data
        }
      })
      this.activeName = list[0].name
      return list
    },
    formatTableData(data){
      return data.slice(1).map(item => {
        const obj = {};
        this.dynamicHeaders(data).forEach((header, index) => {
          obj[header] = item[index];
        });
        return obj;
      });
    },
    dynamicHeaders(data) {
      return data[0]; // 第一行作为表头
    },
  }
}
</script>
<style scoped lang="scss">
.result-panel{
  width: 100%;
  height: 100%;
  ::v-deep .el-tabs{
    border: none !important;
    .el-tabs__content{
      padding: 0;
      .el-table{
        width: 100% !important;
        min-width: 0 !important; /* 覆盖表格默认最小宽度 */
        .el-table__body-wrapper {
          overflow-x: auto; /* 允许横向滚动 */
        }
      }
    }
  }
  .null-value{
    color: #C0C4CC;
  }
}

</style>
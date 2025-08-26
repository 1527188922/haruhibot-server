<template>
  <div class="result-panel">
    <template v-if="results && results.length > 0">
      <el-tabs v-model="activeName" type="border-card" closable @tab-remove="handleTabRemove">
        <el-tab-pane :key="item.name" v-for="(item, index) in results" :name="item.name">
          <span slot="label" :title="item.type">
<!--            hover click 只有query展示popover-->
            <el-popover placement="top" width="200" trigger="click"
                        :disabled="item.type !== 'QUERY'">
               <div>
                 <div v-if="item.cost || item.cost === 0">{{`耗时：${item.cost}ms`}}</div>
                 <div>{{item.sql}}</div>
               </div>
              <span slot="reference">
                <i :class="item.type === 'ERROR' ? 'el-icon-error danger-text' : 'el-icon-success success-text'"></i>
                {{ item.title }}
              </span>
          </el-popover>
          </span>
          <template v-if="item.type === 'QUERY'">
            <el-table :data="formatTableData(item.data)" border size="small" :fit="false"
                      highlight-current-row>
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
          <template v-if="item.type === 'UPDATE' || item.type === 'DDL' || item.type === 'ERROR'">
            <div v-if="item.data || item.data === 0">
              <label>影响行数：</label>{{item.data}}
            </div>
            <div v-if="item.cost || item.cost === 0">
              <label>耗时：</label>{{item.cost}}
            </div>
            <div v-if="item.errorMessage">
              <label>错误：</label>{{item.errorMessage}}
            </div>
            <div v-if="item.sql">
              <label>SQL：</label>{{item.sql}}
            </div>
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
        let {type,sql,cost,data,errorMessage} = e
        return {
          name:type + 'name'+i,
          title:`结果(${i+1})`,//tab展示的title
          type,
          sql,
          cost,
          data,
          errorMessage
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
    box-shadow: none !important;
    .el-tabs__content{
      padding: 0;
      //.el-table{
      //}
    }
  }
  .null-value{
    color: #C0C4CC;
  }
}

</style>
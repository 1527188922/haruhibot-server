<template>
  <div class="result-panel">
    <template v-if="results && results.length > 0">
      <el-tabs v-model="activeName" type="border-card" closable @tab-remove="handleTabRemove">
        <el-tab-pane :key="item.name" v-for="(item, index) in results" :name="item.name">
          <span slot="label" :title="item.type">
<!--            hover click 只有query展示popover-->
            <el-popover placement="top-start" trigger="click"
                        :disabled="!isQuery(item)"
                        popper-class="query-result-sql-popover">
              <div>{{`获取行数：${item.count}`}}</div>
              <div v-if="item.cost || item.cost === 0">{{`耗时：${item.cost}ms`}}</div>
              <pre>{{item.sql}}</pre>
              <span slot="reference">
                <i :class="item.type === 'ERROR' ? 'el-icon-error danger-text' : 'el-icon-success success-text'"></i>
                {{ item.title }}
              </span>
          </el-popover>
          </span>
          <template v-if="isQuery(item)">
            <el-table :data="item.rows" border size="small" :fit="false"
                      highlight-current-row>
              <!-- 动态表头 -->
              <el-table-column  v-for="(field, index) in item.tableHeaders"
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
          <template v-else>
            <div class="update-result">
              <el-form :model="item" label-width="80px">
                <el-form-item label="影响行数：" v-if="item.data || item.data === 0">
                  {{item.data}}
                </el-form-item>
                <el-form-item label="耗时：" v-if="item.cost || item.cost === 0">
                  {{item.cost}}ms
                </el-form-item>
                <el-form-item label="错误：" v-if="item.errorMessage">
                  {{item.errorMessage}}
                </el-form-item>
                <el-form-item label="SQL：" v-if="item.sql">
                  <pre>{{item.sql}}</pre>
                </el-form-item>
              </el-form>
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
        return [];
      }
      let list = v.map((e,i) => {
        let {type,sql,cost,data,errorMessage} = e;
        let obj = {
          name:type + 'name'+i,
          title:`结果(${i+1})`,//tab展示的title
          type,
          sql,
          cost,
          data,
          errorMessage
        };
        if(this.isQuery(type)){
          obj['tableHeaders'] = data && data.length > 0 ? this.dynamicHeaders(data) : [];
          let rows = data && data.length > 1 ? this.formatTableData(data) : [];
          obj['rows'] = rows;
          obj['count'] = rows.length;
        }
        return obj
      })
      this.activeName = list[0].name
      return list
    },
    isQuery(item) {
      if(typeof item === 'string'){
        return item === 'QUERY'
      }
      return item.type === 'QUERY'
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
<style lang="scss">
.query-result-sql-popover{
  padding: 10px;
  min-width: 200px;
  max-width: 500px;
  pre{
    margin: 0;
  }
}
</style>
<style scoped lang="scss">
.result-panel{
  width: 100%;
  height: 100%;
  ::v-deep .el-tabs{
    border: none !important;
    box-shadow: none !important;
    .el-tabs__content{
      padding: 0;
    }
  }
  .null-value{
    color: #C0C4CC;
  }

  .update-result{
    padding: 5px 10px 5px 10px;
    ::v-deep .el-form-item{
      margin-bottom: 0;

      .el-form-item__label{
        padding-right: 0;
        line-height: 30px;
        height: 30px;
        font-weight: bold;
      }

      .el-form-item__content{
        line-height: 30px;
        min-height: 30px;
        height: auto;
        pre{
          margin: 0;
        }
      }
    }
  }

}

</style>
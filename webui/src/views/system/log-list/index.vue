<template>
  <div id="SystemLogList">
    <basic-container>
      <el-row>
        <el-form :model="queryFormObj" label-width="80px" inline ref="queryForm" size="small">
          <el-form-item label="日志级别" prop="level">
            <el-select v-model="queryFormObj.level" class="form-input" clearable placeholder="全部">
              <el-option v-for="item in levelList" :key="item" :value="item" :label="item"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="业务模块" prop="businessModule">
            <el-select v-model="queryFormObj.businessModule" class="form-input" clearable filterable placeholder="全部">
              <el-option v-for="item in businessModuleList" :key="item.code" :value="item.name" :label="item.name">
                <span>{{ item.name }}（{{ item.code }}）</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="TraceId" prop="traceId">
            <el-input v-model="queryFormObj.traceId" class="form-input" maxlength="100" clearable></el-input>
          </el-form-item>
          <el-form-item label="日志内容" prop="message">
            <el-input v-model="queryFormObj.message" class="form-input" maxlength="500" clearable></el-input>
          </el-form-item>
          <el-form-item label="异常内容" prop="throwable">
            <el-input v-model="queryFormObj.throwable" class="form-input" maxlength="500" clearable></el-input>
          </el-form-item>
          <el-form-item label="记录器" prop="loggerName">
            <el-input v-model="queryFormObj.loggerName" class="form-input" maxlength="200" clearable></el-input>
          </el-form-item>
          <el-form-item label="请求地址" prop="requestUri">
            <el-input v-model="queryFormObj.requestUri" class="form-input" maxlength="500" clearable></el-input>
          </el-form-item>
          <el-form-item label="用户名" prop="userName">
            <el-input v-model="queryFormObj.userName" class="form-input" maxlength="100" clearable></el-input>
          </el-form-item>
          <el-form-item label="记录时间" prop="datetimerange">
            <el-date-picker
                class="form-date-picker"
                v-model="queryFormObj.datetimerange"
                type="datetimerange"
                value-format="yyyy-MM-dd HH:mm:ss"
                :default-time="['00:00:00', '23:59:59']"
                :picker-options="pickerOptions"
                range-separator="-"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                align="right">
            </el-date-picker>
          </el-form-item>
        </el-form>
      </el-row>
      <el-row class="query-form-option-buts">
        <el-button type="primary" size="small" @click="search" plain icon="el-icon-search">查询</el-button>
        <el-button type="primary" size="small" @click="resetQueryForm" plain icon="el-icon-refresh-right">重置</el-button>
      </el-row>
    </basic-container>

    <basic-container>
      <el-table tooltip-effect="light" :data="tableData" v-loading="tableLoading" border
                stripe max-height="800" size="small" ref="dataTable" highlight-current-row>
        <el-table-column fixed label="序号" width="45" align="center">
          <template slot-scope="scope">{{ scope.$index + 1 }}</template>
        </el-table-column>
        <el-table-column label="级别" prop="level" width="80" align="center" fixed>
          <template slot-scope="{row}">
            <el-tag size="mini" :type="levelTagType(row.level)" effect="plain">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="记录时间" prop="createTime" min-width="165" align="center" show-tooltip-when-overflow/>
        <el-table-column label="业务模块" prop="businessModule" min-width="110" align="center" show-tooltip-when-overflow/>
        <el-table-column label="日志内容" prop="message" min-width="420">
          <template slot-scope="{row}">
            <div class="log-message-cell" @click="showDetail('日志内容', row.message)">
              {{ row.message }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="异常" prop="throwable" min-width="120" align="center">
          <template slot-scope="{row}">
            <el-button v-if="row.throwable" type="text" size="small" @click="showDetail('异常堆栈', row.throwable)">查看</el-button>
          </template>
        </el-table-column>
        <el-table-column label="TraceId" prop="traceId" min-width="180" show-tooltip-when-overflow/>
        <el-table-column label="请求" min-width="220" show-tooltip-when-overflow>
          <template slot-scope="{row}">
            <span>{{ formatRequest(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" prop="userName" min-width="100" align="center" show-tooltip-when-overflow/>
        <el-table-column label="客户端IP" prop="clientIp" min-width="120" align="center" show-tooltip-when-overflow/>
        <el-table-column label="线程" prop="threadName" min-width="140" show-tooltip-when-overflow/>
        <el-table-column label="记录器" prop="loggerName" min-width="260" show-tooltip-when-overflow/>
        <el-table-column label="类方法" min-width="260" show-tooltip-when-overflow>
          <template slot-scope="{row}">
            <span>{{ formatClassMethod(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="处理器" min-width="260" show-tooltip-when-overflow>
          <template slot-scope="{row}">
            <span>{{ formatHandler(row) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-box">
        <el-pagination v-bind="pagination" @size-change="sizeChange" @current-change="currentChange" />
      </div>
    </basic-container>
  </div>
</template>

<script>
import {enumList, searchSystemLog} from "@/api/log";

function buildRecentRange(offset) {
  const end = new Date();
  const start = new Date(end.getTime() - offset);
  return [start, end];
}

export default {
  name: 'SystemLogList',
  data() {
    return {
      tableLoading: false,
      levelList: ['ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'],
      businessModuleList: [],
      queryFormObj: {
        traceId: '',
        businessModule: '',
        level: '',
        loggerName: '',
        message: '',
        throwable: '',
        requestUri: '',
        userName: '',
        datetimerange: []
      },
      pickerOptions: {
        shortcuts: [{
          text: '近5分钟',
          onClick(picker) {
            picker.$emit('pick', buildRecentRange(5 * 60 * 1000));
          }
        }, {
          text: '近10分钟',
          onClick(picker) {
            picker.$emit('pick', buildRecentRange(10 * 60 * 1000));
          }
        }, {
          text: '近30分钟',
          onClick(picker) {
            picker.$emit('pick', buildRecentRange(30 * 60 * 1000));
          }
        }, {
          text: '近1小时',
          onClick(picker) {
            picker.$emit('pick', buildRecentRange(60 * 60 * 1000));
          }
        }, {
          text: '近2小时',
          onClick(picker) {
            picker.$emit('pick', buildRecentRange(2 * 60 * 60 * 1000));
          }
        }, {
          text: '今天',
          onClick(picker) {
            const end = new Date();
            end.setHours(23, 59, 59);
            const start = new Date();
            start.setHours(0, 0, 0);
            picker.$emit('pick', [start, end]);
          }
        }, {
          text: '近一周',
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
            picker.$emit('pick', [start, end]);
          }
        }, {
          text: '近一个月',
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 30);
            picker.$emit('pick', [start, end]);
          }
        }]
      },
      tableData: [],
      pagination: {
        currentPage: 1,
        pageSizes: [10, 30, 50, 100, 500],
        pageSize: 30,
        layout: 'total, sizes, prev, pager, next, jumper',
        background: true,
        total: 0
      }
    }
  },
  mounted() {
    this.selectBusinessModuleList()
    this.search()
  },
  methods: {
    selectBusinessModuleList() {
      enumList('BusinessModuleEnum').then(({data: {code, message, data}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.businessModuleList = data || []
      })
    },
    search() {
      this.pagination.currentPage = 1
      this.selectTableData()
    },
    resetQueryForm() {
      this.$refs.queryForm.resetFields()
    },
    sizeChange(v) {
      this.pagination.pageSize = v
      this.selectTableData()
    },
    currentChange(v) {
      this.pagination.currentPage = v
      this.selectTableData()
    },
    selectTableData() {
      this.tableLoading = true
      searchSystemLog({
        ...this.queryFormObj,
        currentPage: this.pagination.currentPage,
        pageSize: this.pagination.pageSize
      }).then(({data: {code, message, data}}) => {
        if (code !== 200) {
          return this.$message.error(message)
        }
        this.tableData = data.records || []
        this.pagination.total = data.total
      }).finally(() => {
        this.tableLoading = false
      })
    },
    levelTagType(level) {
      const typeMap = {
        ERROR: 'danger',
        WARN: 'warning',
        INFO: 'success',
        DEBUG: 'info',
        TRACE: ''
      }
      return typeMap[level] || ''
    },
    showDetail(title, content) {
      this.$alert(`<pre class="system-log-detail">${this.escapeHtml(content || '')}</pre>`, title, {
        dangerouslyUseHTMLString: true,
        customClass: 'system-log-detail-alert',
        confirmButtonText: '关闭'
      })
    },
    escapeHtml(text) {
      return text
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
          .replace(/"/g, '&quot;')
          .replace(/'/g, '&#39;')
    },
    formatRequest(row) {
      const method = row.requestMethod ? `[${row.requestMethod}] ` : ''
      const uri = row.requestUri || ''
      const query = row.queryString ? `?${row.queryString}` : ''
      return `${method}${uri}${query}`
    },
    formatClassMethod(row) {
      if (!row.className && !row.methodName) {
        return ''
      }
      return `${row.className || ''}${row.methodName ? '#' + row.methodName : ''}`
    },
    formatHandler(row) {
      if (!row.handlerClass && !row.handlerMethod) {
        return ''
      }
      return `${row.handlerClass || ''}${row.handlerMethod ? '#' + row.handlerMethod : ''}`
    }
  }
}
</script>

<style lang="scss" scoped>
#SystemLogList {
  .form-date-picker {
    width: calc(180px * 2 + 80px + 12px);
  }

  .enum-code {
    float: right;
    color: #8492a6;
    font-size: 12px;
  }

  .log-message-cell {
    display: -webkit-box;
    max-height: 66px;
    overflow: hidden;
    line-height: 22px;
    word-break: break-all;
    white-space: normal;
    cursor: pointer;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
  }
}
</style>

<style lang="scss">
.system-log-detail-alert {
  width: 70vw;
  max-width: 1000px;

  .system-log-detail {
    max-height: 60vh;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-all;
    line-height: 1.5;
    font-family: Consolas, Monaco, 'Courier New', monospace;
  }
}
</style>

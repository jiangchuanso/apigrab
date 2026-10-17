<template>
  <div class="task-list">
    <el-card>
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">采集任务</span>
        <div class="rc-card-header__actions">
          <el-button type="primary" size="small" @click="$router.push('/task/edit')">新建任务</el-button>
        </div>
      </div>

      <!-- 过滤查询 -->
      <el-form :inline="true" size="small" class="rc-filters">
        <el-form-item label="任务名称">
          <el-input v-model="searchForm.name" placeholder="名称模糊搜索" clearable style="width: 180px;" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="数据源">
          <el-select v-model="searchForm.datasourceId" placeholder="全部" clearable style="width: 180px;">
            <el-option v-for="ds in datasourceOptions" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="写入模式">
          <el-select v-model="searchForm.writeMode" placeholder="全部" clearable style="width: 140px;">
            <el-option v-for="opt in writeModeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="调度类型">
          <el-select v-model="searchForm.scheduleType" placeholder="全部" clearable style="width: 140px;">
            <el-option v-for="opt in scheduleTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <div class="rc-filters__actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
      </el-form>

      <el-table :data="list" border size="small" v-loading="loading" style="width: 100%;">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="name" label="任务名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="关联接口" min-width="150" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.apiConfigName || row.apiConfigId }}</template>
        </el-table-column>
        <el-table-column label="目标数据源" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <span v-if="row.datasourceName">【{{ row.datasourceDbType }}】{{ row.datasourceName }}</span>
            <span v-else>{{ row.datasourceId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="targetTable" label="目标表" width="120" show-overflow-tooltip />
        <el-table-column label="写入模式" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini">{{ writeModeText(row.writeMode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="调度类型" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.scheduleType === 'CRON' ? 'warning' : 'info'" size="mini">{{ scheduleTypeText(row.scheduleType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="handleExecute(row)" :loading="executingId === row.id">立即执行</el-button>
            <el-button type="text" size="mini" @click="$router.push(`/task/edit/${row.id}`)">编辑</el-button>
            <el-button
              v-if="row.scheduleType === 'CRON' && row.status === 1"
              type="text" size="mini"
              @click="handlePause(row)">暂停</el-button>
            <el-button
              v-if="row.scheduleType === 'CRON' && row.status === 2"
              type="text" size="mini"
              @click="handleResume(row)">恢复</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C;" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 15px; text-align: right;"
        :current-page="page"
        :page-size="limit"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="onPageChange"
      />
    </el-card>
  </div>
</template>

<script>
import { getTaskList, deleteTask, executeTask, pauseTask, resumeTask } from '@/api/task'
import { getDatasourceList } from '@/api/datasource'

export default {
  name: 'TaskList',
  data() {
    return {
      list: [],
      total: 0,
      page: 1,
      limit: 15,
      loading: false,
      executingId: null,
      datasourceOptions: [],
      searchForm: {
        name: '',
        datasourceId: null,
        writeMode: null,
        scheduleType: null
      },
      writeModeOptions: [
        { value: 'INSERT', label: '追加' },
        { value: 'MERGE', label: '合并' },
        { value: 'TRUNCATE_INSERT', label: '清空追加' }
      ],
      scheduleTypeOptions: [
        { value: 'MANUAL', label: '手动执行' },
        { value: 'ONCE', label: '单次执行' },
        { value: 'CRON', label: '定时执行' }
      ]
    }
  },
  created() {
    this.loadDatasources()
    this.loadList()
  },
  methods: {
    async loadDatasources() {
      try {
        const res = await getDatasourceList({ pageNum: 1, pageSize: 1000 })
        this.datasourceOptions = res.data?.list || res.data || []
      } catch {
        this.datasourceOptions = []
      }
    },
    async loadList() {
      this.loading = true
      try {
        const params = { pageNum: this.page, pageSize: this.limit }
        if (this.searchForm.name) params.name = this.searchForm.name
        if (this.searchForm.datasourceId !== null && this.searchForm.datasourceId !== '') {
          params.datasourceId = this.searchForm.datasourceId
        }
        if (this.searchForm.writeMode) params.writeMode = this.searchForm.writeMode
        if (this.searchForm.scheduleType) params.scheduleType = this.searchForm.scheduleType
        const res = await getTaskList(params)
        this.list = res.data?.list || res.data || []
        this.total = res.data?.total || this.list.length
      } catch {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    search() {
      this.page = 1
      this.loadList()
    },
    resetSearch() {
      this.searchForm = {
        name: '',
        datasourceId: null,
        writeMode: null,
        scheduleType: null
      }
      this.page = 1
      this.loadList()
    },
    onPageChange(p) {
      this.page = p
      this.loadList()
    },
    async handleExecute(row) {
      this.executingId = row.id
      try {
        const res = await executeTask(row.id)
        const data = res.data || res
        const executionId = data && data.executionId
        this.$message.success('任务已触发执行')
        if (executionId) {
          this.$router.push(`/monitor/log/${executionId}`)
        } else {
          this.$message.warning('未获取到执行记录，请前往执行监控查看')
        }
      } catch {
        // handled
      } finally {
        this.executingId = null
      }
    },
    async handlePause(row) {
      await pauseTask(row.id)
      this.$message.success('任务已暂停')
      this.loadList()
    },
    async handleResume(row) {
      await resumeTask(row.id)
      this.$message.success('任务已恢复')
      this.loadList()
    },
    async handleDelete(row) {
      await this.$confirm(`确认删除任务 "${row.name}"？`, '提示', { type: 'warning' })
      await deleteTask(row.id)
      this.$message.success('删除成功')
      this.loadList()
    },
    writeModeText(m) {
      const map = { INSERT: '追加', MERGE: '合并', TRUNCATE_INSERT: '清空追加' }
      return map[m] || m
    },
    scheduleTypeText(t) {
      const map = { MANUAL: '手动执行', ONCE: '单次执行', CRON: '定时执行' }
      return map[t] || t
    }
  }
}
</script>

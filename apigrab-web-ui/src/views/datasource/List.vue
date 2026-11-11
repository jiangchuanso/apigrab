<template>
  <div class="datasource-list">
    <el-card>
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">数据源管理</span>
        <div class="rc-card-header__actions">
          <el-button type="primary" size="small" @click="$router.push('/datasource/edit')">新增数据源</el-button>
        </div>
      </div>

      <el-table :data="list" border size="small" v-loading="loading" style="width: 100%;">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="name" label="数据源名称" min-width="150" />
        <el-table-column label="数据库类型" width="120" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="dbTypeTag(row.dbType)" size="mini">{{ row.dbType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="jdbcUrl" label="JDBC URL" min-width="250" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" width="100" />
        <el-table-column prop="poolMaxSize" label="最大连接" width="80" align="center" />
        <el-table-column label="引用任务数" width="100" align="center">
          <template slot-scope="{ row }">{{ row.refCount == null ? 0 : row.refCount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="handleTest(row)">测试连接</el-button>
            <el-button type="text" size="mini" @click="$router.push(`/datasource/edit/${row.id}`)">编辑</el-button>
            <el-tooltip :disabled="!(row.refCount > 0)" content="该数据源已被任务引用，无法删除" placement="top">
              <span>
                <el-button type="text" size="mini" style="color: #F56C6C;" :disabled="row.refCount > 0" @click="handleDelete(row)">删除</el-button>
              </span>
            </el-tooltip>
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
import { getDatasourceList, deleteDatasource, testDatasource } from '@/api/datasource'

export default {
  name: 'DatasourceList',
  data() {
    return {
      list: [],
      total: 0,
      page: 1,
      limit: 15,
      loading: false
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const res = await getDatasourceList({ pageNum: this.page, pageSize: this.limit })
        this.list = res.data?.list || res.data || []
        this.total = res.data?.total || this.list.length
      } catch {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    onPageChange(p) {
      this.page = p
      this.loadList()
    },
    async handleTest(row) {
      try {
        await testDatasource(row.id)
        this.$message.success('连接成功')
      } catch {
        this.$message.error('连接失败')
      }
    },
    async handleDelete(row) {
      await this.$confirm(`确认删除数据源 "${row.name}"？`, '提示', { type: 'warning' })
      await deleteDatasource(row.id)
      this.$message.success('删除成功')
      this.loadList()
    },
    dbTypeTag(type) {
      const map = {
        MYSQL: 'success', ORACLE: 'danger', SQLSERVER: 'warning', POSTGRESQL: 'primary',
        DM: 'danger', KINGBASE: 'primary', OSCAR: 'primary', GBASE8A: 'success',
        HIGHGO: 'primary', OPENGAUSS: 'primary', OCEANBASE: 'success', UXDB: 'primary'
      }
      return map[type] || 'info'
    }
  }
}
</script>

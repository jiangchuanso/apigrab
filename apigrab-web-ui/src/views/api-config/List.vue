<template>
  <div class="api-config-list">
    <el-card>
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">接口配置</span>
        <div class="rc-card-header__actions">
          <el-button type="primary" size="small" @click="$router.push('/api-config/edit')">新增接口</el-button>
        </div>
      </div>

      <!-- 搜索 -->
      <el-form :inline="true" size="small" class="rc-filters">
        <el-form-item label="接口名称">
          <el-input v-model="searchForm.name" placeholder="名称模糊搜索" clearable @keyup.enter="search" />
        </el-form-item>
        <div class="rc-filters__actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
      </el-form>

      <el-table :data="list" border size="small" v-loading="loading" style="width: 100%;">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="name" label="接口名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="url" label="URL" min-width="200" show-overflow-tooltip />
        <el-table-column label="方法" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="methodType(row.method)" size="mini">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="认证" min-width="140" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.authConfigName || '无认证' }}</template>
        </el-table-column>
        <el-table-column label="分页" width="60" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.pageEnabled ? 'success' : 'info'" size="mini">{{ row.pageEnabled ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="引用任务数" width="100" align="center">
          <template slot-scope="{ row }">{{ row.refCount == null ? 0 : row.refCount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="$router.push(`/api-config/edit/${row.id}`)">编辑</el-button>
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
import { getConfigList, deleteConfig } from '@/api/config'

export default {
  name: 'ApiConfigList',
  data() {
    return {
      list: [],
      total: 0,
      page: 1,
      limit: 15,
      loading: false,
      searchForm: { name: '' }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const res = await getConfigList({ pageNum: this.page, pageSize: this.limit, name: this.searchForm.name })
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
      this.searchForm = { name: '' }
      this.page = 1
      this.loadList()
    },
    onPageChange(p) {
      this.page = p
      this.loadList()
    },
    async handleDelete(row) {
      await this.$confirm(`确认删除接口 "${row.name}"？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
      await deleteConfig(row.id)
      this.$message.success('删除成功')
      this.loadList()
    },
    methodType(m) {
      const map = { GET: 'success', POST: 'primary', PUT: 'warning', DELETE: 'danger' }
      return map[m] || 'info'
    }
  }
}
</script>

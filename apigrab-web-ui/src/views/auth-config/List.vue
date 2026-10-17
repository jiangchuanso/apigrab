<template>
  <div class="auth-config-list">
    <el-card>
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">认证配置</span>
        <div class="rc-card-header__actions">
          <el-button type="primary" size="small" @click="$router.push('/auth-config/edit')">新增认证</el-button>
        </div>
      </div>

      <!-- 搜索 -->
      <el-form :inline="true" size="small" class="rc-filters">
        <el-form-item label="名称">
          <el-input v-model="searchForm.name" placeholder="名称模糊搜索" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.authType" clearable placeholder="全部" style="width: 200px;">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <div class="rc-filters__actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
      </el-form>

      <el-table :data="list" border size="small" v-loading="loading" style="width: 100%;">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="160" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini">{{ typeLabel(row.authType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="引用数" width="90" align="center">
          <template slot-scope="{ row }">{{ row.refCount == null ? 0 : row.refCount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="$router.push(`/auth-config/edit/${row.id}`)">编辑</el-button>
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
import { getAuthConfigList, deleteAuthConfig } from '@/api/authConfig'

const TYPE_OPTIONS = [
  { label: 'Basic Auth', value: 'BASIC' },
  { label: 'Bearer Token', value: 'BEARER' },
  { label: 'OAuth2', value: 'OAUTH2' },
  { label: '自定义 Header', value: 'CUSTOM' },
  { label: '账号密码换 Token', value: 'PASSWORD_TOKEN' }
]

export default {
  name: 'AuthConfigList',
  data() {
    return {
      list: [],
      total: 0,
      page: 1,
      limit: 15,
      loading: false,
      typeOptions: TYPE_OPTIONS,
      searchForm: { name: '', authType: '' }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const res = await getAuthConfigList({
          pageNum: this.page,
          pageSize: this.limit,
          name: this.searchForm.name,
          authType: this.searchForm.authType || undefined
        })
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
      this.searchForm = { name: '', authType: '' }
      this.page = 1
      this.loadList()
    },
    onPageChange(p) {
      this.page = p
      this.loadList()
    },
    async handleDelete(row) {
      try {
        await this.$confirm(`确认删除认证配置 "${row.name}"？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
      } catch {
        return
      }
      try {
        await deleteAuthConfig(row.id)
        this.$message.success('删除成功')
        this.loadList()
      } catch {
        // 失败信息（如“该认证配置已被接口引用，无法删除”）由请求拦截器统一提示
      }
    },
    typeLabel(type) {
      const found = TYPE_OPTIONS.find(o => o.value === type)
      return found ? found.label : (type || '-')
    }
  }
}
</script>

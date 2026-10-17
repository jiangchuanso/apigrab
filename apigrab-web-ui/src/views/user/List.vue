<template>
  <div class="user-list">
    <el-card>
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">用户管理</span>
        <div class="rc-card-header__actions">
          <el-button type="primary" size="small" @click="openCreate">新增用户</el-button>
        </div>
      </div>

      <!-- 过滤查询 -->
      <el-form :inline="true" size="small" class="rc-filters">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="用户名模糊搜索" clearable style="width: 180px;" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="全部" clearable style="width: 140px;">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <div class="rc-filters__actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
      </el-form>

      <el-table :data="list" border size="small" v-loading="loading" style="width: 100%;">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="realName" label="真实姓名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column label="角色" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="mini">{{ roleText(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="300" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="openEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" @click="openResetPwd(row)">重置密码</el-button>
            <el-button v-if="row.status === 1" type="text" size="mini" @click="toggleStatus(row, 0)">禁用</el-button>
            <el-button v-else type="text" size="mini" @click="toggleStatus(row, 1)">启用</el-button>
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

    <!-- 新增/编辑 -->
    <el-dialog :title="dialogMode === 'create' ? '新增用户' : '编辑用户'" :visible.sync="dialogVisible" width="480px">
      <el-form ref="userForm" :model="userForm" :rules="userRules" label-width="90px" size="small">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="dialogMode === 'edit'" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'create'" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" show-password placeholder="请输入密码" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="userForm.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" style="width: 100%;">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="userForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog title="重置密码" :visible.sync="pwdVisible" width="420px">
      <el-form ref="resetForm" :model="resetForm" :rules="resetRules" label-width="90px" size="small">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="请输入新密码" autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPwd" @click="handleResetPwd">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getUserList, createUser, updateUser, deleteUser, resetUserPassword, updateUserStatus } from '@/api/user'

export default {
  name: 'UserList',
  data() {
    return {
      list: [],
      total: 0,
      page: 1,
      limit: 15,
      loading: false,
      searchForm: { username: '', role: null, status: null },
      dialogVisible: false,
      dialogMode: 'create',
      saving: false,
      editingId: null,
      userForm: {
        username: '',
        password: '',
        realName: '',
        email: '',
        address: '',
        role: 'USER',
        status: 1
      },
      userRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
        role: [{ required: true, message: '请选择角色', trigger: 'change' }]
      },
      pwdVisible: false,
      savingPwd: false,
      resetTargetId: null,
      resetForm: { newPassword: '' },
      resetRules: {
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '密码至少 6 位', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const params = { pageNum: this.page, pageSize: this.limit }
        if (this.searchForm.username) params.username = this.searchForm.username
        if (this.searchForm.role) params.role = this.searchForm.role
        if (this.searchForm.status !== null && this.searchForm.status !== '') params.status = this.searchForm.status
        const res = await getUserList(params)
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
      this.searchForm = { username: '', role: null, status: null }
      this.page = 1
      this.loadList()
    },
    onPageChange(p) {
      this.page = p
      this.loadList()
    },
    openCreate() {
      this.dialogMode = 'create'
      this.editingId = null
      this.userForm = { username: '', password: '', realName: '', email: '', address: '', role: 'USER', status: 1 }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.userForm && this.$refs.userForm.clearValidate())
    },
    openEdit(row) {
      this.dialogMode = 'edit'
      this.editingId = row.id
      this.userForm = {
        username: row.username,
        password: '',
        realName: row.realName || '',
        email: row.email || '',
        address: row.address || '',
        role: row.role || 'USER',
        status: row.status
      }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.userForm && this.$refs.userForm.clearValidate())
    },
    handleSubmit() {
      this.$refs.userForm.validate(async (valid) => {
        if (!valid) return
        this.saving = true
        try {
          if (this.dialogMode === 'create') {
            await createUser({
              username: this.userForm.username,
              password: this.userForm.password,
              realName: this.userForm.realName,
              email: this.userForm.email,
              address: this.userForm.address,
              role: this.userForm.role,
              status: this.userForm.status
            })
            this.$message.success('新增成功')
          } else {
            await updateUser(this.editingId, {
              realName: this.userForm.realName,
              email: this.userForm.email,
              address: this.userForm.address,
              role: this.userForm.role,
              status: this.userForm.status
            })
            this.$message.success('保存成功')
          }
          this.dialogVisible = false
          this.loadList()
        } finally {
          this.saving = false
        }
      })
    },
    openResetPwd(row) {
      this.resetTargetId = row.id
      this.resetForm = { newPassword: '' }
      this.pwdVisible = true
      this.$nextTick(() => this.$refs.resetForm && this.$refs.resetForm.clearValidate())
    },
    handleResetPwd() {
      this.$refs.resetForm.validate(async (valid) => {
        if (!valid) return
        this.savingPwd = true
        try {
          await resetUserPassword(this.resetTargetId, { newPassword: this.resetForm.newPassword })
          this.$message.success('密码已重置')
          this.pwdVisible = false
        } finally {
          this.savingPwd = false
        }
      })
    },
    async toggleStatus(row, status) {
      await updateUserStatus(row.id, { status })
      this.$message.success(status === 1 ? '已启用' : '已禁用')
      this.loadList()
    },
    async handleDelete(row) {
      await this.$confirm(`确认删除用户 "${row.username}"？`, '提示', { type: 'warning' })
      await deleteUser(row.id)
      this.$message.success('删除成功')
      this.loadList()
    },
    roleText(role) {
      const map = { ADMIN: '管理员', USER: '普通用户' }
      return map[role] || role
    },
    statusText(status) {
      return status === 1 ? '启用' : '禁用'
    }
  }
}
</script>

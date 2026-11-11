<template>
  <div class="datasource-edit">
    <el-card>
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">{{ isEdit ? '编辑数据源' : '新增数据源' }}</span>
        <div class="rc-card-header__actions">
          <el-button size="small" @click="$router.push('/datasource/list')">返回列表</el-button>
        </div>
      </div>

      <el-form ref="form" :model="form" :rules="rules" label-width="120px" size="small">
        <data-source-form v-model="form" />

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          <el-button type="success" @click="handleTest" :loading="testing">测试连接</el-button>
          <el-button @click="$router.push('/datasource/list')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getDatasourceDetail, createDatasource, updateDatasource, testDatasource, testDatasourceTransient } from '@/api/datasource'
import DataSourceForm from '@/components/DataSourceForm'

export default {
  name: 'DatasourceEdit',
  components: { DataSourceForm },
  data() {
    return {
      isEdit: false,
      saving: false,
      testing: false,
      form: {
        name: '',
        dbType: '',
        jdbcUrl: '',
        driverClass: '',
        dbVersion: '',
        username: '',
        password: '',
        poolMaxSize: 10,
        poolMinIdle: 5,
        driverPath: '',
        connProps: '',
        status: 1,
        remark: ''
      },
      rules: {
        name: [
          { required: true, message: '请输入数据源名称', trigger: 'blur' },
          { max: 100, message: '数据源名称不能超过 100 个字符', trigger: 'blur' }
        ],
        dbType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
        jdbcUrl: [{ required: true, message: '请输入 JDBC URL', trigger: 'blur' }],
        driverClass: [{ required: true, message: '请输入 JDBC 驱动类', trigger: 'blur' }],
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    }
  },
  created() {
    const id = this.$route.params.id
    if (id) {
      this.isEdit = true
      this.loadDetail(id)
    }
  },
  methods: {
    toObject(value) {
      if (value === null || value === undefined || value === '') return null
      if (typeof value === 'object') return value
      try { return JSON.parse(value) } catch { return null }
    },
    async loadDetail(id) {
      try {
        const res = await getDatasourceDetail(id)
        this.form = res.data || res
        this.form.connProps = this.form.connProps || null
      } catch {
        this.$message.error('加载数据源失败')
      }
    },
    async handleSave() {
      const valid = await this.$refs.form.validate()
      if (!valid) return
      this.saving = true
      try {
        this.form.connProps = this.toObject(this.form.connProps)
        if (this.isEdit) {
          await updateDatasource(this.$route.params.id, this.form)
          this.$message.success('更新成功')
        } else {
          await createDatasource(this.form)
          this.$message.success('创建成功')
        }
        this.$router.push('/datasource/list')
      } catch {
        // handled by interceptor
      } finally {
        this.saving = false
      }
    },
    buildTestPayload() {
      const payload = { ...this.form }
      payload.connProps = this.toObject(this.form.connProps)
      return payload
    },
    async handleTest() {
      const valid = await this.$refs.form.validate().catch(() => false)
      if (!valid) {
        this.$message.warning('请先填写必填参数再测试连接')
        return
      }
      this.testing = true
      try {
        if (this.isEdit) {
          await testDatasource(this.$route.params.id)
        } else {
          await testDatasourceTransient(this.buildTestPayload())
        }
        this.$message.success('连接成功')
      } catch {
        // 失败信息由请求拦截器统一提示，不清空表单
      } finally {
        this.testing = false
      }
    }
  }
}
</script>

<template>
  <div class="auth-config-edit">
    <el-card>
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">{{ isEdit ? '编辑认证配置' : '新增认证配置' }}</span>
        <div class="rc-card-header__actions">
          <el-button size="small" @click="$router.push('/auth-config/list')">返回列表</el-button>
        </div>
      </div>

      <el-form ref="form" :model="form" :rules="rules" label-width="140px" size="small">
        <el-form-item label="认证名称" prop="name">
          <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="例如：生产环境 Bearer Token" />
        </el-form-item>

        <auth-config-form v-model="authForm" />

        <el-form-item v-if="tokenTestable" label=" ">
          <el-button type="success" :loading="testingToken" @click="handleTestToken">测试获取 Token</el-button>
          <span class="rc-form-tip">使用当前配置实时换取 Token，成功后在弹窗展示</span>
        </el-form-item>

        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          <el-button @click="$router.push('/auth-config/list')">取消</el-button>
        </el-form-item>
      </el-form>

      <el-dialog title="Token 测试结果" :visible.sync="tokenDialogVisible" width="720px">
        <template v-if="tokenTestResult">
          <el-alert
            v-if="tokenTestResult.message"
            :title="tokenTestResult.message"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 10px;"
          />
          <div v-if="tokenTestResult.token" style="margin-bottom: 10px;">
            <div style="font-size: 12px; color: #909399; margin-bottom: 4px;">提取到的 Token：</div>
            <el-input type="textarea" :rows="2" :value="tokenTestResult.token" readonly />
          </div>
          <api-response-panel
            :result="{ success: true, rawResponse: tokenTestResult.rawResponse }"
            :resp-type="'JSON'"
            :selected-path="currentTokenPath"
            :testing="testingToken"
            :show-parsed-data="false"
            @select-path="onSelectTokenPath"
            @retest="handleTestToken"
          />
          <div style="font-size: 12px; color: #909399; margin-top: 8px;">
            点击上方结构节点即把路径填入「Token 提取路径」；填入后可用下方按钮重新测试确认。
          </div>
        </template>
        <div v-else style="color: #909399;">未获取到 Token 响应</div>
        <span slot="footer">
          <el-button size="small" :loading="testingToken" @click="handleTestToken">用当前路径重新测试</el-button>
          <el-button size="small" type="primary" @click="tokenDialogVisible = false">关闭</el-button>
        </span>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import { getAuthConfigDetail, createAuthConfig, updateAuthConfig, testAuthConfig } from '@/api/authConfig'
import AuthConfigForm from '@/components/AuthConfigForm'
import ApiResponsePanel from '@/components/ApiResponsePanel'

export default {
  name: 'AuthConfigEdit',
  components: { AuthConfigForm, ApiResponsePanel },
  data() {
    return {
      isEdit: false,
      saving: false,
      testingToken: false,
      tokenDialogVisible: false,
      /** 获取 Token 测试的结构化结果：{ rawResponse, token, message } */
      tokenTestResult: null,
      form: {
        name: '',
        authType: 'BASIC',
        authConfig: { authType: 'BASIC' },
        status: 1,
        remark: ''
      },
      rules: {
        name: [
          { required: true, message: '请输入认证名称', trigger: 'blur' },
          { max: 100, message: '认证名称不能超过 100 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    authForm: {
      get() {
        return {
          authType: this.form.authType || 'BASIC',
          authConfig: this.form.authConfig || { authType: this.form.authType || 'BASIC' }
        }
      },
      set(val) {
        this.form.authType = val.authType
        this.form.authConfig = val.authConfig
      }
    },
    tokenTestable() {
      return this.form.authType === 'OAUTH2' || this.form.authType === 'PASSWORD_TOKEN'
    },
    /** 当前已填的 Token 提取路径，用于在面板内回显点选结果 */
    currentTokenPath() {
      const cfg = this.form.authConfig || {}
      return cfg.tokenPath || ''
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
    async loadDetail(id) {
      try {
        const res = await getAuthConfigDetail(id)
        const data = res.data || res
        this.form.name = data.name || ''
        this.form.authType = data.authType || 'BASIC'
        this.form.authConfig = data.authConfig || { authType: this.form.authType }
        this.form.status = data.status != null ? data.status : 1
        this.form.remark = data.remark || ''
      } catch {
        this.$message.error('加载认证配置失败')
      }
    },
    async handleTestToken() {
      this.testingToken = true
      try {
        const res = await testAuthConfig({
          name: this.form.name,
          authType: this.form.authType,
          authConfig: this.authForm.authConfig
        })
        this.tokenTestResult = res.data || res
        this.tokenDialogVisible = true
      } catch {
        // 失败信息由请求拦截器统一提示
      } finally {
        this.testingToken = false
      }
    },
    /**
     * 响应结构点选回填 Token 提取路径。
     * 必须给出即时反馈：对话框内输入框可能不在视野内，静默赋值会让用户以为「点了没反应」。
     */
    onSelectTokenPath(payload) {
      if (!payload) return
      this.form.authConfig = Object.assign({}, this.form.authConfig, {
        authType: this.form.authType,
        tokenPath: payload.path
      })
      if (payload.path) {
        this.$message.success(`已填入 Token 提取路径：${payload.path}`)
      } else {
        this.$message.info('已清除 Token 提取路径')
      }
    },
    async handleSave() {
      const valid = await this.$refs.form.validate()
      if (!valid) return
      this.saving = true
      try {
        const payload = {
          name: this.form.name,
          authType: this.form.authType,
          authConfig: this.authForm.authConfig,
          status: this.form.status,
          remark: this.form.remark
        }
        if (this.isEdit) {
          await updateAuthConfig(this.$route.params.id, payload)
          this.$message.success('更新成功')
        } else {
          await createAuthConfig(payload)
          this.$message.success('创建成功')
        }
        this.$router.push('/auth-config/list')
      } catch {
        // 错误已由 request 拦截器处理
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.rc-form-tip {
  color: #909399;
  font-size: 12px;
  margin-left: 8px;
}
</style>

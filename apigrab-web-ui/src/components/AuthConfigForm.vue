<template>
  <div class="auth-config-form">
    <el-form-item label="认证类型">
      <el-select :value="value.authType" @change="onTypeChange" style="width: 240px;">
        <el-option label="Basic Auth" value="BASIC" />
        <el-option label="Bearer Token" value="BEARER" />
        <el-option label="OAuth2 (Client Credentials)" value="OAUTH2" />
        <el-option label="自定义 Header" value="CUSTOM" />
        <el-option label="账号密码换 Token" value="PASSWORD_TOKEN" />
      </el-select>
    </el-form-item>

    <!-- Basic Auth -->
    <template v-if="value.authType === 'BASIC'">
      <el-form-item label="用户名" required>
        <el-input :value="field('username')" placeholder="请输入用户名" @input="onField('username', $event)" />
      </el-form-item>
      <el-form-item label="密码" required>
        <el-input :value="field('password')" type="password" placeholder="请输入密码" show-password @input="onField('password', $event)" />
      </el-form-item>
    </template>

    <!-- Bearer Token -->
    <template v-if="value.authType === 'BEARER'">
      <el-form-item label="Token" required>
        <el-input :value="field('token')" type="password" placeholder="请输入 Bearer Token" show-password @input="onField('token', $event)" />
      </el-form-item>
    </template>

    <!-- OAuth2 -->
    <template v-if="value.authType === 'OAUTH2'">
      <el-form-item label="Token URL" required>
        <el-input :value="field('tokenUrl')" placeholder="https://example.com/oauth/token" @input="onField('tokenUrl', $event)" />
      </el-form-item>
      <el-form-item label="Client ID" required>
        <el-input :value="field('clientId')" placeholder="Client ID" @input="onField('clientId', $event)" />
      </el-form-item>
      <el-form-item label="Client Secret" required>
        <el-input :value="field('clientSecret')" type="password" placeholder="Client Secret" show-password @input="onField('clientSecret', $event)" />
      </el-form-item>
      <el-form-item label="Scope">
        <el-input :value="field('scope')" placeholder="可选 scope 参数" @input="onField('scope', $event)" />
      </el-form-item>
      <el-form-item label="Token 提取路径">
        <el-input :value="field('tokenPath')" placeholder="$.access_token" @input="onField('tokenPath', $event)" />
        <span class="rc-form-tip">从 Token 响应中按 JSONPath 提取 Token；留空默认 $.access_token</span>
      </el-form-item>
      <el-form-item label="过期时间(秒)">
        <el-input-number :value="numberField('expireSeconds')" :min="1" controls-position="right" style="width: 180px;" @change="onField('expireSeconds', $event)" />
        <span class="rc-form-tip">留空则按响应 expires_in，再回退默认 3600 秒</span>
      </el-form-item>
    </template>

    <!-- Custom Header（多个键值） -->
    <template v-if="value.authType === 'CUSTOM'">
      <el-form-item label="请求头" required>
        <key-value-editor
          :value="field('headers') || {}"
          key-placeholder="Header 名"
          value-placeholder="Header 值"
          @input="onField('headers', $event)"
        />
        <span class="rc-form-tip">可添加多个自定义请求头；键为空的行不会被发送</span>
      </el-form-item>
    </template>

    <!-- 账号密码换 Token -->
    <template v-if="value.authType === 'PASSWORD_TOKEN'">
      <el-form-item label="Token URL" required>
        <el-input :value="field('tokenUrl')" placeholder="https://example.com/login 或 /oauth/token" @input="onField('tokenUrl', $event)" />
      </el-form-item>
      <el-form-item label="请求体格式">
        <el-select :value="field('requestBodyType') || 'FORM'" style="width: 160px;" @change="onField('requestBodyType', $event)">
          <el-option label="表单 (form)" value="FORM" />
          <el-option label="JSON" value="JSON" />
        </el-select>
      </el-form-item>
      <el-form-item label="请求体字段" required>
        <key-value-editor
          :value="field('bodyFields') || {}"
          key-placeholder="字段名（如 username）"
          value-placeholder="字段值"
          @input="onField('bodyFields', $event)"
        />
        <span class="rc-form-tip">字段名与取值均由你填写（如 user=admin、pwd=secret），按目标接口要求填写</span>
      </el-form-item>
      <el-form-item label="Token 提取路径" required>
        <el-input :value="field('tokenPath')" placeholder="$.data.token" @input="onField('tokenPath', $event)" />
        <span class="rc-form-tip">必填：从 Token 响应中按 JSONPath 提取 Token</span>
      </el-form-item>
      <el-form-item label="过期时间(秒)">
        <el-input-number :value="numberField('expireSeconds')" :min="1" controls-position="right" style="width: 180px;" @change="onField('expireSeconds', $event)" />
        <span class="rc-form-tip">留空则按响应 expires_in，再回退默认 3600 秒</span>
      </el-form-item>
      <el-form-item label="Token 放置位置">
        <el-select :value="field('tokenLocation') || 'HEADER'" style="width: 160px;" @change="onField('tokenLocation', $event)">
          <el-option label="请求头 (Header)" value="HEADER" />
          <el-option label="查询参数 (Query)" value="QUERY" />
          <el-option label="请求体 (Body)" value="BODY" />
        </el-select>
      </el-form-item>
      <el-form-item label="Token 键名">
        <el-input :value="field('tokenKey')" placeholder="Authorization" @input="onField('tokenKey', $event)" />
      </el-form-item>
      <el-form-item label="Token 值格式">
        <el-input :value="field('tokenValueFormat')" placeholder="Bearer {token}" @input="onField('tokenValueFormat', $event)" />
        <span class="rc-form-tip">必须包含 {token} 占位符，例如 Bearer {token} 或 {token}</span>
      </el-form-item>
    </template>
  </div>
</template>

<script>
import KeyValueEditor from '@/components/KeyValueEditor'

/**
 * 认证内容编辑器（v-model = { authType, authConfig }）。
 * 后端 AuthConfig 为 Jackson 多态（@JsonTypeInfo），因此 authConfig 内需带 authType 判别字段。
 */
export default {
  name: 'AuthConfigForm',
  components: { KeyValueEditor },
  props: {
    value: {
      type: Object,
      default: () => ({
        authType: 'BASIC',
        authConfig: { authType: 'BASIC' }
      })
    }
  },
  methods: {
    field(key) {
      const cfg = this.value.authConfig || {}
      return cfg[key] == null ? '' : cfg[key]
    },
    numberField(key) {
      const cfg = this.value.authConfig || {}
      const val = cfg[key]
      return val == null || val === '' ? undefined : Number(val)
    },
    onTypeChange(type) {
      // 切换类型时清理不再适用的字段，并带上多态判别字段；对特定类型预填默认值
      const defaults = { authType: type }
      if (type === 'OAUTH2') {
        // 与后端默认一致（OAuth2Handler.DEFAULT_TOKEN_PATH / TokenSupport 兜底 3600），
        // 使用户多数情况下无需再配置；两字段仍可编辑、可清空
        defaults.tokenPath = '$.access_token'
        defaults.expireSeconds = 3600
      }
      if (type === 'PASSWORD_TOKEN') {
        defaults.tokenLocation = 'HEADER'
        defaults.tokenKey = 'Authorization'
        defaults.tokenValueFormat = 'Bearer {token}'
        defaults.requestBodyType = 'FORM'
      }
      this.$emit('input', { authType: type, authConfig: defaults })
      this.$emit('change', type)
    },
    onField(key, val) {
      const authConfig = Object.assign({}, this.value.authConfig, {
        authType: this.value.authType,
        [key]: val
      })
      this.$emit('input', { authType: this.value.authType, authConfig })
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

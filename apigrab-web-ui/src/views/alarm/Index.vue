<template>
  <div class="alarm-config-page">
    <el-card>
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">告警配置</span>
      </div>

      <el-form ref="form" :model="form" label-width="120px" size="small">
        <alarm-config-form v-model="form" />

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          <template v-if="form.status === 1">
            <el-button type="success" @click="handleTest" :loading="testing">测试推送</el-button>
            <el-button @click="loadExample">查看示例数据模型</el-button>
          </template>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getAlarmConfig, saveAlarmConfig, testAlarm, getAlarmExample } from '@/api/alarm'
import AlarmConfigForm from '@/components/AlarmConfigForm'

export default {
  name: 'AlarmConfigView',
  components: { AlarmConfigForm },
  data() {
    return {
      saving: false,
      testing: false,
      form: {
        status: 0,
        endpoint: '',
        contentType: 'application/json',
        inputTemplate: ''
      }
    }
  },
  created() {
    this.loadConfig()
  },
  methods: {
    async loadConfig() {
      try {
        const res = await getAlarmConfig()
        if (res.data) {
          this.form = res.data
        }
      } catch {
        // 可能没有配置，使用默认值
      }
    },
    async handleSave() {
      this.saving = true
      try {
        await saveAlarmConfig(this.form)
        this.$message.success('告警配置已保存')
      } catch {
        // handled
      } finally {
        this.saving = false
      }
    },
    async handleTest() {
      this.testing = true
      try {
        await testAlarm(this.form)
        this.$message.success('告警推送测试成功')
      } catch {
        this.$message.error('告警推送失败')
      } finally {
        this.testing = false
      }
    },
    async loadExample() {
      try {
        const res = await getAlarmExample()
        if (res.data) {
          this.$alert(JSON.stringify(res.data, null, 2), '示例数据模型', { type: 'info' })
        }
      } catch {
        this.$message.info('示例模型：{\n  "taskName": "订单数据采集",\n  "exception": "Connection timeout",\n  "accessTime": "2025-01-01 12:00:00"\n}')
      }
    }
  }
}
</script>

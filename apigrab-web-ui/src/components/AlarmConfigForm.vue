<template>
  <div class="alarm-config-form">
    <el-form-item label="开启告警" prop="status">
      <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="开启" inactive-text="关闭" />
    </el-form-item>

    <template v-if="form.status">
      <el-form-item label="推送地址" prop="endpoint">
        <el-input v-model="form.endpoint" placeholder="https://hooks.example.com/webhook" />
      </el-form-item>

      <el-form-item label="Content-Type" prop="contentType">
        <el-input v-model="form.contentType" placeholder="application/json" />
      </el-form-item>

      <el-form-item label="请求模板" prop="inputTemplate">
        <el-input
          v-model="form.inputTemplate"
          type="textarea"
          :rows="8"
          placeholder='{"msgtype":"text","text":{"content":"数据采集异常\n任务：${taskName}\n异常：${exception}\n时间：${accessTime}"}}'
        />
      </el-form-item>
    </template>

    <!-- 模板变量说明 -->
    <el-card v-if="form.status" style="margin-top: 10px;">
      <div slot="header">
        <span>模板变量说明</span>
        <el-button size="mini" style="float: right;" @click="insertVariable('${taskName}')">插入变量</el-button>
      </div>
      <el-table :data="variables" size="mini" border style="width: 100%;">
        <el-table-column prop="name" label="变量名" width="160" />
        <el-table-column prop="desc" label="说明" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'AlarmConfigForm',
  props: {
    value: {
      type: Object,
      default: () => ({
        status: 0,
        endpoint: '',
        contentType: 'application/json',
        inputTemplate: ''
      })
    }
  },
  data() {
    return {
      variables: [
        { name: '${taskName}', desc: '采集任务名称' },
        { name: '${taskId}', desc: '任务 ID' },
        { name: '${apiConfigId}', desc: '接口配置 ID' },
        { name: '${datasourceId}', desc: '数据源 ID' },
        { name: '${exception}', desc: '异常信息' },
        { name: '${exceptionType}', desc: '异常类型' },
        { name: '${accessTime}', desc: '触发时间' }
      ]
    }
  },
  computed: {
    form: {
      get() { return this.value },
      set(val) { this.$emit('input', val) }
    }
  },
  methods: {
    insertVariable(variable) {
      this.form.inputTemplate = (this.form.inputTemplate || '') + variable
    }
  }
}
</script>

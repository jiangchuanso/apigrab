<template>
  <div class="cron-editor">
    <el-form-item label="调度类型" prop="scheduleType">
      <el-select v-model="localScheduleType" @change="onScheduleTypeChange" style="width: 200px;">
        <el-option label="手动执行" value="MANUAL" />
        <el-option label="定时执行" value="CRON" />
      </el-select>
    </el-form-item>

    <template v-if="localScheduleType === 'CRON'">
      <el-form-item label="Cron 表达式" prop="cronExpression">
        <el-input v-model="localCronExpression" placeholder="0 0/5 * * * ?" @change="onCronChange">
          <el-button slot="append" @click="showHelp = !showHelp">?</el-button>
        </el-input>
      </el-form-item>

      <el-form-item v-if="localCronExpression" label="释义">
        <el-tag type="info">{{ cronDescription }}</el-tag>
      </el-form-item>

      <!-- Cron 帮助 -->
      <el-collapse-transition>
        <el-card v-if="showHelp" style="margin-bottom: 20px;">
          <div slot="header">Cron 表达式参考</div>
          <el-table :data="cronHelpData" size="mini" border>
            <el-table-column prop="field" label="字段" width="100" />
            <el-table-column prop="required" label="必填" width="60" />
            <el-table-column prop="range" label="取值范围" />
            <el-table-column prop="special" label="特殊字符" />
          </el-table>
          <p style="margin-top: 10px; color: #909399; font-size: 12px;">
            示例：<br/>
            0 0/5 * * * ? — 每 5 分钟<br/>
            0 0 2 * * ? — 每天凌晨 2 点<br/>
            0 0 9-18 * * ? — 每天 9 点到 18 点每小时
          </p>
        </el-card>
      </el-collapse-transition>
    </template>
  </div>
</template>

<script>
export default {
  name: 'CronExpressionEditor',
  props: {
    scheduleType: {
      type: String,
      default: 'MANUAL'
    },
    cronExpression: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      showHelp: false,
      cronHelpData: [
        { field: '秒', required: '是', range: '0-59', special: ', - * /' },
        { field: '分', required: '是', range: '0-59', special: ', - * /' },
        { field: '时', required: '是', range: '0-23', special: ', - * /' },
        { field: '日', required: '是', range: '1-31', special: ', - * ? / L W' },
        { field: '月', required: '是', range: '1-12', special: ', - * /' },
        { field: '周', required: '是', range: '1-7 或 SUN-SAT', special: ', - * ? / L #' },
        { field: '年', required: '否', range: '留空或 1970-2099', special: ', - * /' }
      ]
    }
  },
  computed: {
    localScheduleType: {
      get() { return this.scheduleType },
      set(val) { this.$emit('update:scheduleType', val) }
    },
    localCronExpression: {
      get() { return this.cronExpression },
      set(val) { this.$emit('update:cronExpression', val) }
    },
    cronDescription() {
      if (!this.localCronExpression) return ''
      try {
        // 简单释义
        const parts = this.localCronExpression.trim().split(/\s+/)
        if (parts.length < 6) return '表达式格式无效'
        return `${parts[1]}分${parts[2]}时${parts[3]}日 ${parts[4]}月 周${parts[5]}`
      } catch {
        return '未知'
      }
    }
  },
  methods: {
    onScheduleTypeChange() {
      this.$emit('schedule-type-change', this.localScheduleType)
    },
    onCronChange() {
      this.$emit('cron-change', this.localCronExpression)
    }
  }
}
</script>

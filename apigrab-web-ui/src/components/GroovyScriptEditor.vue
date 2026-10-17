<template>
  <div class="groovy-editor">
    <el-select v-model="transformMode" @change="onModeChange" style="margin-bottom: 15px; width: 200px;">
      <el-option label="字段映射模式" value="FIELD_MAPPING" />
      <el-option label="Groovy 脚本模式" value="GROOVY" />
    </el-select>

    <template v-if="transformMode === 'GROOVY'">
      <!-- 模板选择 -->
      <div style="margin-bottom: 10px;">
        <span style="font-size: 13px; color: #606266; margin-right: 10px;">脚本模板：</span>
        <el-select v-model="selectedTemplate" size="mini" @change="onSelectTemplate" style="width: 200px;">
          <el-option label="自定义" value="" />
          <el-option label="过滤空值" value="filterNull" />
          <el-option label="日期格式化" value="dateFormat" />
          <el-option label="字段重命名" value="renameField" />
          <el-option label="聚合统计" value="aggregate" />
        </el-select>
      </div>

      <!-- CodeMirror 编辑器文本框替代 -->
      <el-input
        v-model="localScript"
        type="textarea"
        :rows="15"
        placeholder="// Groovy 转换脚本
// 可用变量: records (List<Map>), log (Logger), json (JSON工具)
// 脚本必须返回 List<Map<String, Object>>

records.each { r ->
  // 在此编写转换逻辑
  if (r.name != null) {
    r.name = r.name.trim()
  }
}
return records"
        style="font-family: 'Courier New', monospace;"
      />

      <!-- 脚本调试 -->
      <div style="margin-top: 15px;">
        <el-button type="primary" size="small" @click="showDebug = !showDebug">
          {{ showDebug ? '收起调试' : '脚本调试' }}
        </el-button>
      </div>

      <el-card v-if="showDebug" style="margin-top: 10px;">
        <div slot="header">脚本调试</div>
        <el-form>
          <el-form-item label="样例数据 (JSON)">
            <el-input
              v-model="sampleData"
              type="textarea"
              :rows="5"
              placeholder='[{"name":"Alice","date":"2025-01-01"}]'
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" @click="runDebug" :loading="debugLoading">执行</el-button>
          </el-form-item>
          <el-form-item label="输出结果">
            <el-input
              v-model="debugResult"
              type="textarea"
              :rows="8"
              readonly
              style="font-family: 'Courier New', monospace;"
            />
          </el-form-item>
        </el-form>
      </el-card>
    </template>
  </div>
</template>

<script>
import { debugScript } from '@/api/config'

export default {
  name: 'GroovyScriptEditor',
  props: {
    value: {
      type: String,
      default: ''
    },
    apiConfigId: {
      type: [Number, String],
      default: null
    },
    configTransformType: {
      type: String,
      default: 'FIELD_MAPPING'
    },
    configConvertScript: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      transformMode: 'FIELD_MAPPING',
      localScript: '',
      selectedTemplate: '',
      showDebug: false,
      sampleData: '',
      debugResult: '',
      debugLoading: false,
      templates: {
        filterNull: `// 过滤空值记录
records = records.findAll { r -> r.name != null && r.name.toString().trim().length() > 0 }
return records`,
        dateFormat: `// 日期格式统一转换
import java.time.LocalDate
import java.time.format.DateTimeFormatter

records.each { r ->
  if (r.createDate) {
    try {
      def date = LocalDate.parse(r.createDate.toString())
      r.createDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    } catch (e) {
      log.warn("Date parse failed: " + r.createDate)
    }
  }
}
return records`,
        renameField: `// 字段重命名
records = records.collect { r ->
  def m = [:]
  m.userId = r.id
  m.userName = r.name
  m.email = r.email
  m.created = r.createTime
  return m
}
return records`,
        aggregate: `// 按分类聚合统计
def grouped = records.groupBy { r -> r.category }
def result = grouped.collect { k, v ->
  [
    category: k,
    count: v.size(),
    totalAmount: v.sum { it.amount instanceof Number ? it.amount : 0 }
  ]
}
return result`
      }
    }
  },
  watch: {
    configTransformType: {
      immediate: true,
      handler(val) {
        this.transformMode = val || 'FIELD_MAPPING'
      }
    },
    configConvertScript: {
      immediate: true,
      handler(val) {
        this.localScript = val || ''
      }
    }
  },
  methods: {
    onModeChange() {
      this.$emit('transform-type-change', this.transformMode)
    },
    onSelectTemplate(tplName) {
      if (tplName && this.templates[tplName]) {
        this.localScript = this.templates[tplName]
        this.$emit('input', this.localScript)
      }
    },
    async runDebug() {
      if (!this.apiConfigId) {
        this.$message.warning('请先保存接口配置再进行脚本调试')
        return
      }
      if (!this.localScript) {
        this.$message.warning('请先编写脚本')
        return
      }
      this.debugLoading = true
      try {
        const res = await debugScript(this.apiConfigId, {
          script: this.localScript,
          sampleData: this.sampleData
        })
        this.debugResult = JSON.stringify(res.data || res, null, 2)
      } catch (e) {
        this.debugResult = '脚本执行出错：' + (e.message || '未知错误')
      } finally {
        this.debugLoading = false
      }
    }
  }
}
</script>

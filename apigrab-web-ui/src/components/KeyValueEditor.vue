<template>
  <div class="kv-editor">
    <div v-if="presets.length" class="kv-presets">
      <span class="kv-presets-label">常用项：</span>
      <el-tag
        v-for="preset in presets"
        :key="preset.key"
        size="mini"
        class="kv-preset-tag"
        @click="addPreset(preset)"
      >{{ preset.label || preset.key }}</el-tag>
      <span class="kv-presets-hint">也可直接手动输入任意{{ keyPlaceholder }}</span>
    </div>

    <div v-for="(row, index) in rows" :key="index" class="kv-row">
      <el-input v-model="row.key" size="small" :placeholder="keyPlaceholder" class="kv-input" @input="emit" />
      <span class="kv-sep">:</span>
      <el-input v-model="row.value" size="small" :placeholder="valuePlaceholder" class="kv-input" @input="emit" />
      <el-button type="danger" icon="el-icon-delete" size="mini" circle @click="removeRow(index)" />
    </div>
    <el-button size="small" @click="addRow">+ 添加一行</el-button>
  </div>
</template>

<script>
export default {
  name: 'KeyValueEditor',
  props: {
    value: {
      type: Object,
      default: () => ({})
    },
    keyPlaceholder: {
      type: String,
      default: '键'
    },
    valuePlaceholder: {
      type: String,
      default: '值'
    },
    /**
     * 常用项快捷加入清单，形如 [{ label, key, value }]。
     * 默认空数组即未启用——本组件同时服务于请求头与查询参数，只有请求头需要该能力
     * （design C1）。清单由调用方提供，本组件不硬编码任何具体预设（design P1）。
     */
    presets: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      rows: [],
      lastEmitted: null
    }
  },
  watch: {
    value: {
      immediate: true,
      handler(val) {
        if (JSON.stringify(val || {}) === this.lastEmitted) return
        this.rows = this.toRows(val)
      }
    }
  },
  methods: {
    toRows(val) {
      if (!val) return []
      if (Array.isArray(val)) {
        return val.map(item => ({ key: item.key || '', value: item.value == null ? '' : String(item.value) }))
      }
      return Object.keys(val).map(key => ({
        key,
        value: val[key] == null ? '' : String(val[key])
      }))
    },
    emit() {
      const obj = {}
      this.rows.forEach(row => {
        const key = (row.key || '').trim()
        if (key) obj[key] = row.value
      })
      this.lastEmitted = JSON.stringify(obj)
      this.$emit('input', obj)
    },
    addRow() {
      this.rows.push({ key: '', value: '' })
    },
    /**
     * 一键加入常用项。已存在同名键（大小写不敏感）时不重复添加，改为抛出事件由调用方提示
     * （design C3；HTTP 头名本就不区分大小写）。
     */
    addPreset(preset) {
      const key = (preset.key || '').trim()
      if (!key) return
      const exists = this.rows.some(row => (row.key || '').trim().toLowerCase() === key.toLowerCase())
      if (exists) {
        this.$emit('preset-exists', { key, label: preset.label || key })
        return
      }
      this.rows.push({ key, value: preset.value || '' })
      this.emit()
    },
    removeRow(index) {
      this.rows.splice(index, 1)
      this.emit()
    }
  }
}
</script>

<style scoped>
.kv-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
.kv-input {
  flex: 1;
}
.kv-sep {
  padding: 0 8px;
  color: #909399;
}
.kv-presets {
  margin-bottom: 8px;
}
.kv-presets-label {
  font-size: 12px;
  color: #909399;
}
.kv-preset-tag {
  margin-right: 6px;
  cursor: pointer;
}
.kv-presets-hint {
  font-size: 12px;
  color: #c0c4cc;
}
</style>

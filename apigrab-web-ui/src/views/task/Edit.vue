<template>
  <div class="task-edit">
    <!-- 任务信息 -->
    <el-card>
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">{{ isEdit ? '编辑采集任务' : '新建采集任务' }}</span>
        <div class="rc-card-header__actions">
          <el-button size="small" @click="$router.push('/task/list')">返回列表</el-button>
        </div>
      </div>

      <el-form ref="form" :model="form" :rules="rules" label-width="140px" size="small">
        <el-form-item label="任务名称" prop="name">
          <!-- 与列 rc_collect_task.name VARCHAR(100) 对齐，避免超长导致存库报错（design C10） -->
          <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="例如：订单数据每日采集" />
        </el-form-item>

        <el-form-item label="接口配置" prop="apiConfigId">
          <el-select v-model="form.apiConfigId" filterable placeholder="请选择已创建的接口配置" style="width: 100%;" @change="onApiConfigChange">
            <el-option v-for="item in apiConfigOptions" :key="item.id" :label="item.name" :value="item.id">
              <div class="rc-option-row">
                <span class="rc-option-row__main">{{ item.name }}</span>
                <span class="rc-option-row__meta">{{ item.method }} {{ item.url }}</span>
              </div>
            </el-option>
          </el-select>
          <el-button type="text" size="mini" :disabled="!form.apiConfigId" @click="goEditApiConfig">查看/编辑该接口配置</el-button>
          <el-button type="text" size="mini" @click="$router.push('/api-config/list')">接口配置列表</el-button>
        </el-form-item>

        <el-form-item label="目标数据源" prop="datasourceId">
          <el-select v-model="form.datasourceId" filterable placeholder="请选择数据源" style="width: 100%;" @change="onDatasourceChange">
            <el-option v-for="item in datasourceOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="目标库/模式" prop="targetSchema">
          <el-select v-model="form.targetSchema" filterable clearable placeholder="请选择库/模式" style="width: 100%;" @change="onSchemaChange">
            <el-option v-for="item in schemaOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="自动建表">
              <el-switch v-model="form.autoCreateTable" :active-value="1" :inactive-value="0" @change="onAutoCreateChange" />
              <span class="form-tip">开启后目标表名可手动输入新表；关闭则从已有表选择</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="批量条数">
              <el-input-number v-model="form.batchSize" :min="1" :max="10000" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="目标表名" prop="targetTable">
          <el-input
            v-if="form.autoCreateTable === 1"
            v-model="form.targetTable"
            placeholder="请输入新表名（自动建表）"
            @change="loadTargetColumns"
          />
          <el-select
            v-else
            v-model="form.targetTable"
            filterable
            clearable
            placeholder="请选择已有表"
            style="width: 100%;"
            @change="loadTargetColumns"
          >
            <el-option v-for="item in tableOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="写入模式" prop="writeMode">
              <el-select v-model="form.writeMode" style="width: 100%;" @change="warnIfMergeKeyUnavailable">
                <el-option label="INSERT 追加" value="INSERT" />
                <el-option label="MERGE 合并" value="MERGE" />
                <el-option label="TRUNCATE+INSERT 清空追加" value="TRUNCATE_INSERT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <!-- 主键统一在下方「目标字段映射」的字段行上勾选，不再在此处手输列名 -->
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="失败重试次数">
              <el-input-number v-model="form.retryTimes" :min="0" :max="10" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="执行超时(ms)">
              <el-input-number v-model="form.timeoutMs" :min="0" :step="1000" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="失败告警">
              <el-switch v-model="form.alarmEnabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">调度策略</el-divider>
        <cron-expression-editor
          :schedule-type.sync="form.scheduleType"
          :cron-expression.sync="form.cronExpression"
          @schedule-type-change="() => {}"
        />

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 字段映射（归属本任务） -->
    <el-card style="margin-top: 16px;">
        <div slot="header" class="rc-card-header"><span class="rc-card-header__title">字段映射（结果集字段 → 目标表字段）</span></div>
      <field-mapping-panel
        v-model="mappings"
        :sample-data="sampleData"
        :target-columns="targetColumns"
        :column-select-mode="form.autoCreateTable === 0"
        @refresh-sample="refreshSample"
      />
    </el-card>

    <div style="margin-top: 16px;">
      <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      <el-button @click="$router.push('/task/list')">取消</el-button>
    </div>
  </div>
</template>

<script>
import { getTaskDetail, createTask, updateTask } from '@/api/task'
import { getDatasourceList, getDatasourceSchemas, getDatasourceTables, getDatasourceColumns } from '@/api/datasource'
import { getConfigList, getConfigDetail, testConfig } from '@/api/config'
import CronExpressionEditor from '@/components/CronExpressionEditor'
import FieldMappingPanel from '@/components/FieldMappingPanel'
import { parseMergeKeys, serializeMergeKeys, findUnmatchedKeys, enforcePrimaryKeyNotNull } from '@/utils/mergeKeys'
import { isTargetFieldValid, primaryKeyColumnsOf, unmappedPrimaryKeyColumns } from '@/utils/targetColumns'

export default {
  name: 'TaskEdit',
  components: { CronExpressionEditor, FieldMappingPanel },
  data() {
    return {
      isEdit: false,
      saving: false,
      apiConfigOptions: [],
      datasourceOptions: [],
      schemaOptions: [],
      tableOptions: [],
      sampleData: null,
      targetColumns: [],
      mappings: [],
      /** 已展示的 MERGE 前置提示标识，避免同一情况重复弹提示（映射变更时重评估） */
      mergeKeyWarnKey: '',
      form: {
        name: '',
        apiConfigId: '',
        datasourceId: '',
        targetSchema: '',
        targetTable: '',
        writeMode: 'INSERT',
        mergeKeys: '',
        autoCreateTable: 1,
        batchSize: 500,
        retryTimes: 0,
        timeoutMs: 0,
        alarmEnabled: 0,
        scheduleType: 'MANUAL',
        cronExpression: '',
        status: 0,
        remark: ''
      },
      rules: {
        name: [
          { required: true, message: '请输入任务名称', trigger: 'blur' },
          { max: 100, message: '任务名称不能超过 100 个字符', trigger: 'blur' }
        ],
        apiConfigId: [{ required: true, message: '请选择接口配置', trigger: 'change' }],
        datasourceId: [{ required: true, message: '请选择目标数据源', trigger: 'change' }],
        targetTable: [{ required: true, message: '请选择目标表名', trigger: 'change' }]
      }
    }
  },
  created() {
    this.loadApiConfigs()
    this.loadDatasources()
    const id = this.$route.params.id
    if (id) {
      this.isEdit = true
      this.loadDetail(id)
    }
  },
  watch: {
    /**
     * 映射变更后重新评估 MERGE 前置提示：保证「主键列已被映射」时不再提示，
     * 未被映射时能及时提示。提示本身做去重，避免每次编辑重复弹出。
     */
    mappings: {
      deep: true,
      handler() {
        this.warnIfMergeKeyUnavailable()
      }
    }
  },
  methods: {
    /**
     * 由既有 mergeKeys 反推逐字段主键勾选（design C9）。
     * 加载旧任务时把原先手输的列名还原成勾选状态，不丢配置。
     *
     * 若有键匹配不到任何映射行（列名拼错、列已被删），**必须提示**——
     * 静默丢弃会让用户以为主键还配着，实际已经没了。
     */
    applyMergeKeysToMappings(mappings, mergeKeysText) {
      const keys = parseMergeKeys(mergeKeysText)
      const rows = (mappings || []).map(row => ({
        ...row,
        isPrimaryKey: keys.length > 0 && keys.indexOf(String(row.targetField || '').trim()) >= 0
      }))
      if (keys.length > 0) {
        const unmatched = findUnmatchedKeys(keys, rows)
        if (unmatched.length > 0) {
          this.$message.warning(`以下合并主键在字段映射中找不到对应列，已忽略：${unmatched.join('、')}`)
        }
      }
      // 旧任务可能存在「主键 + 可空」的历史配置，加载时一并归一（主键隐含非空）
      return enforcePrimaryKeyNotNull(rows)
    },
    async loadApiConfigs() {
      try {
        const res = await getConfigList({ pageNum: 1, pageSize: 9999 })
        this.apiConfigOptions = res.data?.list || res.data || []
      } catch {
        this.apiConfigOptions = []
      }
    },
    async loadDatasources() {
      try {
        const res = await getDatasourceList({ pageNum: 1, pageSize: 9999 })
        this.datasourceOptions = res.data?.list || res.data || []
      } catch { /* ignore */ }
    },
    async loadDetail(id) {      try {
        const res = await getTaskDetail(id)
        const data = res.data || res
        this.form = Object.assign({
          retryTimes: 0, timeoutMs: 0, alarmEnabled: 0, targetSchema: ''
        }, data.task || {})
        this.mappings = this.applyMergeKeysToMappings(data.mappings || [], this.form.mergeKeys)
        if (data.apiConfig && data.apiConfig.sampleData) {
          this.sampleData = this.parseSample(data.apiConfig.sampleData)
        }
        if (this.form.datasourceId) {
          this.loadSchemas()
          this.loadTables()
          this.loadTargetColumns()
        }
      } catch {
        this.$message.error('加载任务失败')
      }
    },
    parseSample(raw) {
      if (raw == null) return null
      if (typeof raw === 'object') return raw
      try { return JSON.parse(raw) } catch { return null }
    },
    async onApiConfigChange() {
      await this.loadSampleFromConfig()
    },
    async loadSampleFromConfig() {
      if (!this.form.apiConfigId) return
      try {
        const res = await getConfigDetail(this.form.apiConfigId)
        const config = res.data || res
        if (config && config.sampleData) {
          this.sampleData = this.parseSample(config.sampleData)
        }
      } catch { /* ignore: keep current sample */ }
    },
    goEditApiConfig() {
      if (this.form.apiConfigId) {
        this.$router.push(`/api-config/edit/${this.form.apiConfigId}`)
      }
    },
    onDatasourceChange() {
      this.form.targetSchema = ''
      this.form.targetTable = ''
      this.schemaOptions = []
      this.tableOptions = []
      this.targetColumns = []
      this.loadSchemas()
      this.loadTables()
    },
    onSchemaChange() {
      this.form.targetTable = ''
      this.targetColumns = []
      this.loadTables()
    },
    onAutoCreateChange() {
      // 切换自动建表时保留已填目标表名（S4），并按需刷新目标表列
      this.loadTables()
      this.loadTargetColumns()
    },
    async loadSchemas() {
      if (!this.form.datasourceId) return
      try {
        const res = await getDatasourceSchemas(this.form.datasourceId)
        this.schemaOptions = res.data || []
      } catch { this.schemaOptions = [] }
    },
    async loadTables() {
      // 目标表列表依赖 schema（后端 /tables 需 schema 参数）；未选 schema 时不请求，避免 400
      if (!this.form.datasourceId || !this.form.targetSchema) {
        this.tableOptions = []
        return
      }
      try {
        const res = await getDatasourceTables(this.form.datasourceId, this.form.targetSchema)
        this.tableOptions = res.data || []
      } catch { this.tableOptions = [] }
    },
    async loadTargetColumns() {
      this.targetColumns = []
      if (!this.form.datasourceId || !this.form.targetSchema || !this.form.targetTable) return
      try {
        const tableRes = await getDatasourceTables(this.form.datasourceId, this.form.targetSchema)
        const tables = tableRes.data || []
        if (!tables.includes(this.form.targetTable)) return
        const colRes = await getDatasourceColumns(this.form.datasourceId, this.form.targetSchema, this.form.targetTable)
        this.targetColumns = colRes.data || []
      } catch {
        this.targetColumns = []
      }
      this.warnIfMergeKeyUnavailable()
    },
    /**
     * MERGE 前置的**非阻断**提示（design D6/D7）：关闭自动建表时，合并主键由目标表主键决定。
     * - 表无主键 → MERGE 无法执行（执行时后端会报既有错误）；
     * - 有主键但未被任何映射行命中 → 合并可能不生效。
     * 仅提示、不拦截保存。
     *
     * 注意：表刚选定/切换时映射往往尚未配置，此时**不得**因「没有映射行命中主键」而误报，
     * 故在没有任何已映射列时直接跳过「主键未映射」评估（`unmappedPrimaryKeyColumns` 已内建该保护）。
     * 通过 `mergeKeyWarnKey` 去重，`mappings` 变更时重评估避免残留误报。
     */
    warnIfMergeKeyUnavailable() {
      if (this.form.autoCreateTable !== 0 || this.form.writeMode !== 'MERGE') {
        this.mergeKeyWarnKey = ''
        return
      }
      if (!this.targetColumns || this.targetColumns.length === 0) {
        this.mergeKeyWarnKey = ''
        return
      }
      if (primaryKeyColumnsOf(this.targetColumns).length === 0) {
        this.showMergeKeyWarning('no-pk', '该表无主键，MERGE 模式将无法执行')
        return
      }
      const targetFields = this.mappings.map(m => (m && m.targetField) || '')
      if (unmappedPrimaryKeyColumns(this.targetColumns, targetFields).length > 0) {
        this.showMergeKeyWarning('unmapped-pk', '目标表主键列未被映射，MERGE 可能无法生效')
      } else {
        this.mergeKeyWarnKey = ''
      }
    },
    /** 展示 MERGE 前置提示并去重：同一 `key` 只弹一次，条件消失后复位以便再次提示。 */
    showMergeKeyWarning(key, message) {
      if (this.mergeKeyWarnKey === key) return
      this.mergeKeyWarnKey = key
      this.$message.warning(message)
    },
    async refreshSample() {
      if (!this.form.apiConfigId) {
        this.$message.warning('请先选择接口配置')
        return
      }
      try {
        const res = await testConfig(this.form.apiConfigId)
        const data = res.data || res
        if (data && data.success === false) {
          this.$message.error('试跑失败：' + (data.error || '未知错误'))
          return
        }
        // 优先使用后端返回的"合并详情后"的采样记录（启用详情补全时），
        // 否则无法在映射面板看到详情字段；未启用时后端返回的即列表首条。
        if (data && data.sampleData && typeof data.sampleData === 'object') {
          this.sampleData = data.sampleData
          this.$message.success('样例数据已更新')
          return
        }
        const parsed = data && data.parsedData
        if (Array.isArray(parsed) && parsed.length > 0) {
          this.sampleData = parsed[0]
          this.$message.success('样例数据已更新')
        } else if (parsed && typeof parsed === 'object') {
          this.sampleData = parsed
          this.$message.success('样例数据已更新')
        } else {
          this.$message.warning('试跑未返回可用样例数据')
        }
      } catch {
        this.$message.error('刷新样例失败，请检查接口配置')
      }
    },
    async handleSave() {
      const valid = await this.$refs.form.validate()
      if (!valid) return
      // 字段映射是采集任务的必需配置：空列表无法生成任何入库字段，保存后执行必然失败
      if (!this.mappings || this.mappings.length === 0) {
        this.$message.error('请至少添加一条目标字段映射后再保存')
        return
      }
      // 关闭自动建表：目标字段必须取自所选表的列（design D5/D6）
      if (this.form.autoCreateTable === 0) {
        if (!this.targetColumns || this.targetColumns.length === 0) {
          this.$message.error('请选择能够读取列的目标表后再保存')
          return
        }
        const invalid = this.mappings.filter(m => !isTargetFieldValid(this.targetColumns, m.targetField))
        if (invalid.length > 0) {
          const names = invalid.map(m => String(m.targetField || '').trim() || '(空)').join('、')
          this.$message.error(`目标字段不在所选表的列中：${names}`)
          return
        }
      }
      this.saving = true
      try {
        // nullable 后端为整型，统一将布尔值归一为 1/0，避免提交 400
        // 主键列不允许为空：保存前再归一一次，防止历史配置或接口写入绕过界面限制
        const mappings = enforcePrimaryKeyNotNull(this.mappings).map(m => ({
          ...m,
          nullable: typeof m.nullable === 'boolean' ? (m.nullable ? 1 : 0) : m.nullable
        }))
        // 合并主键由字段行的勾选唯一决定（design C9）；写回既有 mergeKeys 字段，
        // 后端写入路径与 Upsert 语义完全不变
        const mergeKeys = serializeMergeKeys(mappings)
        const payload = { task: { ...this.form, mergeKeys }, mappings }
        if (this.isEdit) {
          await updateTask(this.$route.params.id, payload)
          this.$message.success('更新成功')
        } else {
          await createTask(payload)
          this.$message.success('创建成功')
        }
        this.$router.push('/task/list')
      } catch {
        // handled
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.form-tip {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

/* 接口配置下拉项：名称靠左、方法+URL 靠右并截断，
   避免长 URL 溢出下拉（原先用 float: right 会遮挡/溢出） */
.rc-option-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.rc-option-row__main {
  flex: 0 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rc-option-row__meta {
  margin-left: auto;
  flex: 0 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--rc-text-muted);
  font-size: var(--rc-text-xs);
}
</style>

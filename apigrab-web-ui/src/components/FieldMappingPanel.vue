<template>
  <div class="field-mapping-panel">
    <el-row :gutter="20">
      <!-- 左侧：源字段 JSON 树 -->
      <el-col :span="10">
        <el-card>
          <div slot="header">
            <span>样例数据</span>
            <el-button size="mini" style="float: right;" @click="refreshSampleData">刷新样例数据</el-button>
          </div>
          <p class="tip">点击字段到右侧目标行即可建立映射</p>
          <template v-if="sampleData">
            <el-tree
              :data="jsonTreeData"
              :props="treeProps"
              node-key="path"
              default-expand-all
              highlight-current
              @node-click="onSampleNodeClick"
            >
              <span
                slot-scope="{ data }"
                class="tree-node"
                :class="{ 'node-used': isNodeUsed(data) }"
                draggable="true"
                @dragstart="onDragStart(data, $event)"
              >
                <span style="font-size: 12px;">{{ data.label }}</span>
                <el-tag v-if="data.isLeaf" size="mini" type="info" style="margin-left: 5px;">{{ data.dataType }}</el-tag>
              </span>
            </el-tree>
          </template>
          <el-empty v-else description="暂无样例数据" />
        </el-card>
      </el-col>

      <!-- 中间：映射操作 -->
      <el-col :span="14">
        <el-card>
          <div slot="header">
            <span>目标字段映射</span>
            <el-button size="mini" type="success" style="float: right;" @click="onAutoGenerate">自动生成</el-button>
          </div>

          <el-table :data="mappings" size="small" border style="width: 100%;" :row-class-name="rowClassName">
            <el-table-column label="源字段" width="180">
              <template slot-scope="{ row }">
                <div class="drop-cell" @dragover.prevent @drop="onDrop(row)">
                  <el-popover
                    placement="left"
                    width="300"
                    trigger="click"
                    @show="onShowSourceSelect(row)">
                    <el-tree
                      :data="jsonTreeData"
                      :props="treeProps"
                      node-key="path"
                      default-expand-all
                      highlight-current
                      style="max-height: 360px; overflow-y: auto;"
                      @node-click="(data) => onSelectSource(row, data)"
                    />
                    <el-input slot="reference" :value="row.sourceField" readonly size="mini" placeholder="点击或拖拽选择" />
                  </el-popover>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="目标字段" width="170">
              <template slot-scope="{ row }">
                <el-select
                  v-if="columnSelectMode"
                  :value="row.targetField"
                  size="mini"
                  filterable
                  clearable
                  placeholder="选择列"
                  style="width: 100%;"
                  @change="(val) => onTargetColumnChange(row, val)"
                >
                  <el-option
                    v-for="opt in targetColumnOptions"
                    :key="opt.name"
                    :label="opt.label"
                    :value="opt.name"
                  />
                </el-select>
                <el-input v-else v-model="row.targetField" size="mini" placeholder="字段名" />
              </template>
            </el-table-column>
            <el-table-column label="类型" width="90">
              <template slot-scope="{ row }">
                <el-select v-model="row.targetType" size="mini" :disabled="columnSelectMode" @change="onTypeChange(row)">
                  <el-option
                    v-for="opt in targetTypeOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <!-- 长度只对字符串类型有意义：其它类型下**不显示**输入框（后端 mapColumnType 亦不使用长度）。
                 用受控 el-input + 归一函数而非 el-input-number：后者渲染的是普通文本输入，
                 非法字符要等到失焦才回退，输入过程中仍会显示；这里在每次输入即收敛，只可能留下数字。 -->
            <el-table-column label="长度" width="110">
              <template slot-scope="{ row }">
                <el-input
                  v-if="lengthApplicable(row)"
                  :value="row.targetLength"
                  size="mini"
                  placeholder="255"
                  :disabled="columnSelectMode"
                  @input="onLengthInput(row, $event)"
                />
                <span v-else class="cell-tip">—</span>
              </template>
            </el-table-column>
            <el-table-column label="主键" width="60" align="center">
              <template slot-scope="{ row }">
                <!-- 关闭自动建表：主键由目标表决定，勾选框只读（design D4）。
                     用 <span> 包裹以便禁用态下仍能触发 tooltip（禁用控件自身不派发鼠标事件）。 -->
                <el-tooltip
                  :disabled="columnSelectMode ? false : isPrimaryKeyCapable(row.targetType)"
                  :content="columnSelectMode
                    ? '主键由目标表决定，不可修改'
                    : '长文本类型（STRING）不能作为主键，请将「类型」调整为 VARCHAR'"
                  placement="top"
                >
                  <span style="display: inline-block;">
                    <el-checkbox
                      v-model="row.isPrimaryKey"
                      :disabled="columnSelectMode"
                      @change="onPrimaryKeyChange(row)"
                    />
                  </span>
                </el-tooltip>
              </template>
            </el-table-column>
            <!-- 主键列不允许为空：SQL 中 PRIMARY KEY 隐含 NOT NULL，勾选主键后此开关强制关闭并锁定 -->
            <el-table-column label="可空" width="60" align="center">
              <template slot-scope="{ row }">
                <el-tooltip :disabled="!row.isPrimaryKey" content="主键列不允许为空" placement="top">
                  <el-switch
                    v-model="row.nullable"
                    :active-value="1"
                    :inactive-value="0"
                    :disabled="!!row.isPrimaryKey"
                  />
                </el-tooltip>
              </template>
            </el-table-column>
            <!-- 内置函数可选、也支持手动输入：值必须是后端函数名本身（不要写成 upper()） -->
            <el-table-column label="转换表达式" width="170">
              <template slot-scope="{ row }">
                <el-select
                  v-model="row.convertExpr"
                  size="mini"
                  filterable
                  allow-create
                  default-first-option
                  clearable
                  placeholder="留空则原样映射"
                  style="width: 100%;"
                >
                  <el-option
                    v-for="opt in convertExprOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="关联" width="60" fixed="right">
              <template slot-scope="{ row }">
                <el-tag v-if="row.sourceField" size="mini" type="success">已关联</el-tag>
                <el-tag v-else size="mini" type="info">未关联</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="排序" width="80" fixed="right">
              <template slot-scope="{ $index }">
                <el-button
                  icon="el-icon-top"
                  size="mini"
                  circle
                  :disabled="$index === 0"
                  @click="moveMapping($index, -1)"
                />
                <el-button
                  icon="el-icon-bottom"
                  size="mini"
                  circle
                  :disabled="$index === mappings.length - 1"
                  @click="moveMapping($index, 1)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" fixed="right">
              <template slot-scope="{ $index }">
                <el-button type="danger" icon="el-icon-delete" size="mini" circle @click="removeMapping($index)" />
              </template>
            </el-table-column>
          </el-table>

          <div style="margin-top: 10px;">
            <el-button size="small" @click="addMapping">+ 添加映射</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { buildMappingsFromColumns, buildMappingsFromSample, inferTypeFromValue, lengthForType, CONVERT_EXPR_OPTIONS, TARGET_TYPE_OPTIONS, sanitizeLengthInput, isPrimaryKeyCapable as isPrimaryKeyCapableType } from '@/utils/fieldMatch'
import { appendSegment, leafName } from '@/utils/jsonPath'
import { moveRow, withSequentialSortOrder } from '@/utils/mappingOrder'
import { columnNameOf, findColumnByName, typeAndLengthForColumn, isPrimaryKeyColumn } from '@/utils/targetColumns'

export default {
  name: 'FieldMappingPanel',
  props: {
    value: {
      type: Array,
      default: () => []
    },
    sampleData: {
      type: [Object, Array, String],
      default: null
    },
    targetColumns: {
      type: Array,
      default: () => []
    },
    /** 关闭自动建表时进入列选择模式：目标字段从列中选择，类型/长度锁定（design D1/D3/D4） */
    columnSelectMode: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    mappings: {
      get() { return this.value },
      set(val) { this.$emit('input', val) }
    },
    usedSourcePaths() {
      const set = new Set()
      this.value.forEach(m => { if (m.sourceField) set.add(m.sourceField) })
      return set
    },
    jsonTreeData() {
      if (!this.sampleData) return []
      let data = this.sampleData
      if (typeof data === 'string') {
        try { data = JSON.parse(data) } catch { return [] }
      }
      if (Array.isArray(data) && data.length > 0) {
        data = data[0]
      }
      return this.buildTree(data, '$')
    },
    /** 目标列下拉选项：列名（附类型）。 */
    targetColumnOptions() {
      return (this.targetColumns || [])
        .map(c => {
          const name = columnNameOf(c)
          const type = c && typeof c === 'object' ? c.type : ''
          return { name, label: type ? `${name}（${type}）` : name }
        })
        .filter(opt => opt.name !== '')
    }
  },
  data() {
    return {
      dragPath: '',
      /** 内置转换函数选项；输入框仍支持手动填写任意值 */
      convertExprOptions: CONVERT_EXPR_OPTIONS,
      /** 目标字段类型选项；与后端 TargetType 取值集合一致 */
      targetTypeOptions: TARGET_TYPE_OPTIONS,
      treeProps: {
        children: 'children',
        label: 'label'
      }
    }
  },
  watch: {
    columnSelectMode(val) {
      if (val) this.syncAllColumnRows()
    },
    targetColumns() {
      if (this.columnSelectMode) this.syncAllColumnRows()
    }
  },
  methods: {
    /**
     * 长度是否适用于该行类型：**仅 VARCHAR**。
     * STRING 是长文本（MySQL LONGTEXT / PostgreSQL TEXT / Oracle CLOB / SQL Server NVARCHAR(MAX)），
     * 不接受长度；其余类型后端 `mapColumnType` 亦不使用长度。
     */
    lengthApplicable(row) {
      return !!row && row.targetType === 'VARCHAR'
    },
    /** 该类型是否可作为主键（模板调用）：长文本 STRING 不可 */
    isPrimaryKeyCapable(targetType) {
      return isPrimaryKeyCapableType(targetType)
    },
    /**
     * 切换类型时同步长度与主键标记：
     * - 切到 VARCHAR 且长度为空 → 补默认 255；切到其它类型 → 清空长度（后端不使用）。
     * - 切到不可作主键的类型（STRING 长文本）→ 取消主键勾选，避免留下建不出表的配置。
     */
    onTypeChange(row) {
      if (!row) return
      if (this.lengthApplicable(row)) {
        if (row.targetLength === null || row.targetLength === undefined || row.targetLength === '') {
          row.targetLength = lengthForType(row.targetType)
        }
      } else {
        row.targetLength = null
      }
      if (!isPrimaryKeyCapableType(row.targetType)) {
        row.isPrimaryKey = false
      }
    },
    /**
     * 主键变更归一：勾选主键即强制非空（开关同时被锁定，见图）。
     * SQL 中 `PRIMARY KEY` 隐含 `NOT NULL`，允许「主键 + 可空」并存会让用户得到一个
     * 自相矛盾的配置——以为可空，实际建表后必然非空。
     * 取消勾选时**不自动恢复**可空：开关会重新启用，由用户自行决定。
     */
    onPrimaryKeyChange(row) {
      if (!row) return
      // 不该作为主键的类型（长文本 STRING）不得被勾上：提示用户先改为 VARCHAR 再设置
      if (row.isPrimaryKey && !isPrimaryKeyCapableType(row.targetType)) {
        row.isPrimaryKey = false
        this.$message.warning('长文本类型（STRING）不能作为主键，请将「类型」调整为 VARCHAR 后再设置主键')
        return
      }
      if (row.isPrimaryKey) {
        row.nullable = 0
      }
    },
    /** 长度输入即时归一：只保留数字并收敛到 [1, 65535]，空表示未设置（后端按 255 处理） */
    onLengthInput(row, value) {
      if (!row) return
      row.targetLength = sanitizeLengthInput(value)
    },
    /** 上移/下移一行，并按界面顺序写回连续 sortOrder（design C8） */
    moveMapping(index, offset) {
      const next = moveRow(this.mappings, index, index + offset)
      this.mappings = withSequentialSortOrder(next)
    },
    /**
     * 路径拼接的唯一实现来自 utils/jsonPath（design C9）。
     * 此前此处以 `${path}.${key}` 直接拼接，XML 属性键 `@id` 得到 `$.@id`；
     * 求值侧对无效路径抛出的异常被静默吞成 null——用户在界面上正常点选、映射也建立成功，
     * 实际每行取到空值且无任何报错。现统一走 appendSegment，属性键产出 `$['@id']`。
     */
    buildTree(obj, path) {
      if (obj === null || obj === undefined) return []
      if (typeof obj !== 'object') {
        return [{ label: `${path} (${obj})`, path, isLeaf: true, dataType: typeof obj, value: obj }]
      }
      if (Array.isArray(obj)) {
        if (obj.length === 0) return [{ label: `${path} []`, path, isLeaf: true, dataType: 'array', value: obj }]
        const item = obj[0]
        if (typeof item === 'object') {
          return [{ label: `${path} [0]`, path, children: this.buildTree(item, `${path}[0]`) }]
        }
        return [{ label: `${path} [${typeof item}]`, path, isLeaf: true, dataType: `array<${typeof item}>`, value: obj }]
      }
      return Object.keys(obj).map(key => {
        const childPath = appendSegment(path, key)
        const val = obj[key]
        if (val !== null && typeof val === 'object') {
          return { label: key, path: childPath, children: this.buildTree(val, childPath) }
        }
        return { label: key, path: childPath, isLeaf: true, dataType: val === null ? 'null' : typeof val, value: val }
      })
    },
    isNodeUsed(node) {
      return node.isLeaf && this.usedSourcePaths.has(node.path)
    },
    rowClassName({ row }) {
      return row && row.sourceField ? 'mapped-row' : ''
    },
    onDragStart(data, event) {
      if (!data.isLeaf) return
      this.dragPath = data.path
      if (event && event.dataTransfer) {
        event.dataTransfer.setData('text/plain', data.path)
        event.dataTransfer.effectAllowed = 'copy'
      }
    },
    onDrop(row) {
      const path = this.dragPath
      this.dragPath = ''
      if (!path) return
      if (row.sourceField && row.sourceField !== path) {
        this.$confirm('该目标字段已有源字段，是否覆盖？', '提示', { type: 'warning' })
          .then(() => this.applySource(row, path))
          .catch(() => {})
      } else {
        this.applySource(row, path)
      }
    },
    applySource(row, path) {
      row.sourceField = path
      const leaf = leafName(path)
      if (this.columnSelectMode) {
        // 列选择模式：仅当叶子名匹配某列时自动选中该列，否则留空待选（design D7）
        const col = findColumnByName(this.targetColumns, leaf)
        this.onTargetColumnChange(row, col ? columnNameOf(col) : '')
      } else if (!row.targetField) {
        row.targetField = leaf
      }
    },
    /** 列选择模式：选定列后同步类型/长度与主键标记（design D3/D4）。 */
    onTargetColumnChange(row, name) {
      if (!row) return
      const targetName = name == null ? '' : String(name)
      row.targetField = targetName
      const col = targetName ? findColumnByName(this.targetColumns, targetName) : null
      if (!col) {
        // 未选列/列不存在：不得留下会误当合并主键的残留勾选
        row.isPrimaryKey = false
        return
      }
      const { targetType, targetLength } = typeAndLengthForColumn(col)
      row.targetType = targetType
      row.targetLength = targetLength
      // 主键由目标表决定：命中主键列才勾选（design D3/D4）
      row.isPrimaryKey = isPrimaryKeyColumn(col)
      if (row.isPrimaryKey) {
        row.nullable = 0
      }
    },
    /**
     * 关闭自动建表（列选择模式）下，按目标表列元数据对齐每行：
     * - 类型/长度取自所选列（design D4）；
     * - 主键由目标表决定：命中主键列的行勾选并置非空，其余不勾选（design D3/D4）。
     *
     * 传入行数组而非读取 `this.value`，便于「自动生成」在写回前对生成结果就地应用。
     */
    applyColumnMetadata(rows) {
      ;(rows || []).forEach(row => {
        if (!row) return
        const col = row.targetField ? findColumnByName(this.targetColumns, row.targetField) : null
        if (col) {
          const { targetType, targetLength } = typeAndLengthForColumn(col)
          row.targetType = targetType
          row.targetLength = targetLength
        }
        // 主键唯一来源是目标表主键：未映射/非主键列一律取消勾选，避免残留错误合并主键
        row.isPrimaryKey = !!col && isPrimaryKeyColumn(col)
        if (row.isPrimaryKey) {
          row.nullable = 0
        }
      })
    },
    /** 列选择模式下，把已选目标字段按列重新同步类型/长度与主键（切换模式或列刷新后调用）。 */
    syncAllColumnRows() {
      this.applyColumnMetadata(this.value)
    },
    onShowSourceSelect(row) {
      this._selectingRow = row
    },
    onSelectSource(row, data) {
      this.applySource(row, data.path)
    },
    addMapping() {
      this.mappings.push({
        sourceField: '',
        targetField: '',
        targetType: 'VARCHAR',
        targetLength: 255,
        nullable: 1,
        defaultValue: '',
        convertExpr: '',
        // 手动新增同样默认不勾主键：主键须由用户显式指定
        isPrimaryKey: false,
        sortOrder: this.mappings.length
      })
    },
    /**
     * 单击样例数据中的**叶子字段**：若该字段尚未建立映射，则在目标字段映射末尾新增一条
     * （源=该字段；目标按列选择模式匹配目标列，否则取叶子名）。非叶子节点或已映射字段不处理，
     * 避免产生重复行。拖拽建映射的既有能力保留（design C6/C7）。
     */
    onSampleNodeClick(data) {
      if (!data || !data.isLeaf) return
      if (this.usedSourcePaths.has(data.path)) return
      // 目标类型按样例值推断，与「自动生成」同源：字符串→STRING，整数→BIGINT，小数→DECIMAL；
      // 非 VARCHAR 类型无默认长度（STRING 为长文本，不接受长度）。
      const targetType = inferTypeFromValue(data.value)
      const row = {
        sourceField: '',
        targetField: '',
        targetType,
        targetLength: lengthForType(targetType),
        nullable: 1,
        defaultValue: '',
        convertExpr: '',
        isPrimaryKey: false,
        sortOrder: this.mappings.length
      }
      this.mappings.push(row)
      this.applySource(row, data.path)
      this.mappings = withSequentialSortOrder(this.mappings)
    },
    removeMapping(index) {
      this.mappings.splice(index, 1)
      this.mappings = withSequentialSortOrder(this.mappings)
    },
    onAutoGenerate() {
      if (!this.sampleData) {
        this.$message.warning('暂无样例数据，请先"刷新样例数据"')
        return
      }
      let data = this.sampleData
      if (typeof data === 'string') {
        try { data = JSON.parse(data) } catch { data = null }
      }
      const record = Array.isArray(data) ? data[0] : data
      if (!record || typeof record !== 'object') {
        this.$message.info('无法从样例数据自动生成映射')
        return
      }
      const columns = this.targetColumns || []
      let autoMappings
      if (columns.length > 0) {
        autoMappings = buildMappingsFromColumns(record, columns)
        if (autoMappings.length === 0) {
          this.$message.info('未能按目标表列匹配生成映射')
          return
        }
      } else {
        autoMappings = buildMappingsFromSample(record)
      }
      if (autoMappings.length > 0) {
        const ordered = withSequentialSortOrder(autoMappings)
        // 关闭自动建表：生成结果的主键标记须与目标表主键一致（design D3）
        if (this.columnSelectMode) this.applyColumnMetadata(ordered)
        this.mappings = ordered
        this.$message.success(`已生成 ${autoMappings.length} 条映射`)
      } else {
        this.$message.info('无法从样例数据自动生成映射')
      }
    },
    refreshSampleData() {
      this.$emit('refresh-sample')
    }
  }
}
</script>

<style scoped>
.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  padding-right: 8px;
  cursor: grab;
}
.tree-node.node-used {
  background-color: #e7f7ec;
  border-radius: 3px;
}
.drop-cell {
  min-height: 28px;
}
.tip {
  font-size: 12px;
  color: #909399;
  margin: 0 0 8px 0;
}
.cell-tip {
  display: block;
  font-size: 11px;
  color: #c0c4cc;
  line-height: 1.2;
}
</style>

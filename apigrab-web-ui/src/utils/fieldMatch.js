/**
 * Field mapping helpers: match sample-data fields to target table columns by name
 * similarity, or fall back to generating mappings directly from sample fields.
 */

export function normalize(name) {
  return String(name == null ? '' : name).toLowerCase().replace(/_/g, '')
}

export function levenshtein(a, b) {
  const m = a.length
  const n = b.length
  if (m === 0) return n
  if (n === 0) return m
  const dp = []
  for (let i = 0; i <= m; i++) {
    dp.push(new Array(n + 1).fill(0))
  }
  for (let i = 0; i <= m; i++) dp[i][0] = i
  for (let j = 0; j <= n; j++) dp[0][j] = j
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1
      dp[i][j] = Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
    }
  }
  return dp[m][n]
}

const DISTANCE_THRESHOLD = 2

/** Find the closest unused column for a sample field name (exact -> contains -> edit distance). */
export function matchColumn(sampleField, columns, used) {
  const target = normalize(sampleField)
  if (!target) return null
  let match = columns.find(c => !used.has(c.name) && normalize(c.name) === target)
  if (match) return match
  match = columns.find(c => {
    if (used.has(c.name)) return false
    const cn = normalize(c.name)
    return cn && (cn.includes(target) || target.includes(cn))
  })
  if (match) return match
  let best = null
  let bestDist = Infinity
  columns.forEach(c => {
    if (used.has(c.name)) return
    const d = levenshtein(target, normalize(c.name))
    if (d < bestDist) {
      bestDist = d
      best = c
    }
  })
  return best && bestDist <= DISTANCE_THRESHOLD ? best : null
}

/** Collect leaf fields of a record as { path: '$.a.b', value }. */
export function collectLeaves(obj, path, out) {
  if (obj === null || obj === undefined || typeof obj !== 'object') {
    out.push({ path, value: obj })
    return
  }
  if (Array.isArray(obj)) {
    if (obj.length > 0 && obj[0] !== null && typeof obj[0] === 'object') {
      collectLeaves(obj[0], `${path}[0]`, out)
    } else {
      out.push({ path, value: obj })
    }
    return
  }
  const keys = Object.keys(obj)
  if (keys.length === 0) {
    out.push({ path, value: obj })
    return
  }
  keys.forEach(key => collectLeaves(obj[key], `${path}.${key}`, out))
}

/** Last key of a JsonPath leaf, e.g. '$.user.name[0]' -> 'name'. */
export function leafKey(path) {
  return String(path).replace(/\[.*?\]/g, '').split('.').pop()
}

export function inferTypeFromValue(value) {
  if (typeof value === 'number') {
    return Number.isInteger(value) ? 'BIGINT' : 'DECIMAL'
  }
  return 'STRING'
}

/**
 * 目标库列类型 → 目标字段类型。必须与后端 `TargetType` 的取值集合保持一致。
 *
 * 注意判断顺序：`'DATETIME'.includes('DATE')` 为真，故必须先判 TIMESTAMP/DATETIME 再判 DATE，
 * 否则带时间戳的列会被误判成纯日期。
 */
export function toTargetType(columnType) {
  const t = String(columnType || '').toUpperCase()
  if (t.includes('TIMESTAMP') || t.includes('DATETIME')) return 'TIMESTAMP'
  if (t.includes('DATE')) return 'DATE'
  if (t.includes('BOOL')) return 'BOOLEAN'
  if (t.includes('BIGINT')) return 'BIGINT'
  if (t.includes('INT')) return 'BIGINT'
  if (t.includes('DECIMAL') || t.includes('NUMERIC') || t.includes('NUMBER')) return 'DECIMAL'
  return 'VARCHAR'
}

/**
 * 内置转换函数选项。
 *
 * 值与后端 `BuiltinConvertFunctions` 的**键名逐字一致**，且必须是**裸函数名**：
 * `FieldMapper` 用 `hasFunction(convertExpr)` 精确匹配键名，并固定传入 `expr = null`。
 * 写成 `upper()` 会匹配失败并被静默跳过——表达式看起来配了、实际没生效。
 *
 * 刻意**不含 `default_value`**：它依赖 `expr` 参数提供默认值，而映射引擎固定传 null；
 * 且其调用被 `value != null` 保护，空值路径根本到不了。列出它只会让用户配出静默无效的表达式。
 */
export const CONVERT_EXPR_OPTIONS = [
  { value: 'trim', label: 'trim — 去首尾空白' },
  { value: 'upper', label: 'upper — 转大写' },
  { value: 'lower', label: 'lower — 转小写' },
  { value: 'to_string', label: 'to_string — 转字符串' },
  { value: 'to_int', label: 'to_int — 转整数' },
  { value: 'to_decimal', label: 'to_decimal — 转小数' },
  { value: 'to_date', label: 'to_date — 转日期（默认格式 yyyy-MM-dd HH:mm:ss）' }
]

/**
 * 目标字段类型选项。
 *
 * 取值必须与后端 `TargetType` **完全一致**——不一致会直接导致保存时反序列化失败
 * （`target_type` 无数值/名称转换器，按枚举名解析）。
 *
 * 注意 **`STRING` 是长文本类型，不是 `VARCHAR` 的别名**：
 * `VARCHAR` → `VARCHAR(length)`（需长度）；`STRING` → 各方言的长文本
 * （MySQL `LONGTEXT`、PostgreSQL `TEXT`、Oracle `CLOB`、SQL Server `NVARCHAR(MAX)`），
 * **不接受长度，也不能作为主键**。
 */
export const TARGET_TYPE_OPTIONS = [
  { value: 'VARCHAR', label: 'VARCHAR（需指定长度）' },
  { value: 'STRING', label: 'STRING（长文本，不可为主键）' },
  { value: 'BIGINT', label: 'BIGINT' },
  { value: 'DECIMAL', label: 'DECIMAL' },
  { value: 'DATE', label: 'DATE' },
  { value: 'TIMESTAMP', label: 'TIMESTAMP' },
  { value: 'BOOLEAN', label: 'BOOLEAN' }
]

/** 可作为主键的类型。长文本（STRING）不可：MySQL 拒绝无前缀长度的 TEXT/BLOB 键。 */
export const PRIMARY_KEY_CAPABLE_TYPES = ['VARCHAR', 'BIGINT', 'DECIMAL', 'DATE', 'TIMESTAMP', 'BOOLEAN']

/** 该类型能否作为主键。 */
export function isPrimaryKeyCapable(targetType) {
  return PRIMARY_KEY_CAPABLE_TYPES.indexOf(targetType) >= 0
}

/**
 * 该目标类型的默认长度：**仅 VARCHAR 有**。
 * 依据：`AbstractSqlDialectStrategy.mapColumnType` 只在 VARCHAR 分支使用长度；
 * `STRING` 是长文本（LONGTEXT / TEXT / CLOB / NVARCHAR(MAX)），不接受长度。
 */
export function lengthForType(targetType) {
  return targetType === 'VARCHAR' ? 255 : null
}
/**
 * 长度默认值：仅在字符串类型下有意义，且目标列已声明长度时沿用列上的值。
 */
function defaultLength(targetType, columnLength) {
  const len = columnLength ? parseInt(columnLength, 10) : NaN
  if (!isNaN(len) && len > 0) return len
  return lengthForType(targetType)
}

function buildMapping(sourceField, targetField, targetType, targetLength, sortOrder) {
  return {
    sourceField,
    targetField,
    targetType,
    targetLength: defaultLength(targetType, targetLength),
    nullable: 1,
    defaultValue: '',
    convertExpr: '',
    // 主键必须默认不勾选：它同时驱动建表主键与 Upsert 冲突键，替用户猜测会产生
    // 意料之外的唯一约束与更新行为（design C7）
    isPrimaryKey: false,
    sortOrder
  }
}

/** Generate mappings by matching sample fields to existing table columns (unmatched fields skipped). */
export function buildMappingsFromColumns(sampleRecord, rawColumns) {
  const columns = (rawColumns || []).map(c => (typeof c === 'string' ? { name: c, type: '' } : c))
  const leaves = []
  collectLeaves(sampleRecord, '$', leaves)
  const used = new Set()
  const mappings = []
  leaves.forEach(leaf => {
    const col = matchColumn(leafKey(leaf.path), columns, used)
    if (!col) return
    used.add(col.name)
    // 直接把列声明长度交给 buildMapping：未声明时由类型决定默认（字符串 255、其余留空）
    mappings.push(buildMapping(leaf.path, col.name, toTargetType(col.type), col.size, mappings.length))
  })
  return mappings
}

/** Fallback: generate mappings directly from sample fields (target name = sample field name). */
export function buildMappingsFromSample(sampleRecord) {
  const leaves = []
  collectLeaves(sampleRecord, '$', leaves)
  const mappings = []
  leaves.forEach(leaf => {
    mappings.push(buildMapping(leaf.path, leafKey(leaf.path), inferTypeFromValue(leaf.value), null, mappings.length))
  })
  return mappings
}

/**
 * 归一长度输入：只保留数字，并收敛到 [min, max]。空（或全非数字）返回 null，表示「未设置」。
 *
 * 后端会把长度直接拼进 DDL（`VARCHAR(length)`），非法值会生成坏 SQL 或让建表失败，
 * 因此必须在**输入阶段**就收敛，而不是等提交后报错。
 *
 * 规则刻意简单、可预测：非数字字符一律丢弃（"12a"→12），而不是在整个输入非法时清空字段——
 * 后者会在用户输入过程中突然抹掉已有内容。
 *
 * @param {string|number|null|undefined} raw
 * @param {{min?: number, max?: number}} [bounds]
 * @returns {number|null}
 */
export function sanitizeLengthInput(raw, bounds = {}) {
  const min = bounds.min === undefined ? 1 : bounds.min
  const max = bounds.max === undefined ? 65535 : bounds.max
  if (raw === null || raw === undefined) return null
  const digits = String(raw).replace(/\D/g, '')
  if (digits === '') return null
  const value = parseInt(digits, 10)
  if (!Number.isFinite(value)) return null
  return Math.min(Math.max(value, min), max)
}

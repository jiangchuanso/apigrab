import { PRIMARY_KEY_CAPABLE_TYPES } from './fieldMatch.js'

/**
 * 逐字段主键 ↔ 任务级 mergeKeys 字符串互转（design C9 / P1）。
 *
 * 主键标记**复用既有 `rc_collect_task.merge_keys`** 存储（逗号分隔列名），因此不新增数据库列。
 * 本模块是「界面勾选」与「既有持久化格式」之间的唯一桥梁：
 * - 加载任务时把 mergeKeys 反推为勾选（`parseMergeKeys`），已有配置不丢；
 * - 保存时把勾选序列化回 mergeKeys（`serializeMergeKeys`），后端写入路径完全不变。
 */

/**
 * 解析逗号分隔的列名列表：去空白、丢弃空项、去重（保持首次出现顺序）。
 * @param {string|null|undefined} text
 * @returns {string[]}
 */
export function parseMergeKeys(text) {
  if (text === null || text === undefined) return []
  const seen = new Set()
  const keys = []
  String(text)
    .split(',')
    .forEach(raw => {
      const key = raw.trim()
      if (!key || seen.has(key)) return
      seen.add(key)
      keys.push(key)
    })
  return keys
}

/**
 * 由勾选行序列化出 mergeKeys。仅取 `isPrimaryKey` 为真且目标字段非空白的行。
 * @param {Array<{targetField?: string, isPrimaryKey?: boolean}>|null|undefined} rows
 * @returns {string}
 */
export function serializeMergeKeys(rows) {
  if (!rows) return ''
  return rows
    .filter(row => row && row.isPrimaryKey)
    .map(row => (row.targetField === null || row.targetField === undefined ? '' : String(row.targetField).trim()))
    .filter(field => field !== '')
    .join(',')
}

/**
 * 找出无法对应到任何映射行的键——用于旧任务里拼错或不存在的列名。
 *
 * **必须由调用方提示这些键**：静默丢弃等于用户以为主键还配着，实际已经没了。
 *
 * 比较时仅去除首尾空白，**不做大小写归一**：列名大小写是否敏感取决于数据库方言，
 * 擅自归一可能匹配到错误的列。
 *
 * @param {string[]|null|undefined} keys
 * @param {Array<{targetField?: string}>|null|undefined} rows
 * @returns {string[]}
 */
export function findUnmatchedKeys(keys, rows) {  if (!keys || !keys.length) return []
  const available = new Set(
    (rows || [])
      .map(row => (row && row.targetField !== null && row.targetField !== undefined ? String(row.targetField).trim() : ''))
      .filter(field => field !== '')
  )
  return keys
    .map(key => (key === null || key === undefined ? '' : String(key).trim()))
    .filter(key => key !== '' && !available.has(key))
}

/**
 * 归一主键相关的不变量：
 * 1. **长文本类型（`STRING`）不能作为主键** —— MySQL 拒绝无前缀长度的 TEXT/BLOB 键，
 *    其它方言亦无等价可用语义。界面对该类型禁用勾选，此处兜住接口写入与历史数据。
 * 2. **主键列不允许为空** —— SQL 中 `PRIMARY KEY` 隐含 `NOT NULL`。若允许「主键 + 可空」并存，
 *    用户会得到一个自相矛盾的配置：以为该列可空，实际建表后必然非空。
 *
 * 返回新数组与新对象，**不修改入参**（行是 Vue 的响应式对象，就地改写会造成界面与表单不一致）。
 * 幂等：重复应用结果不变。
 *
 * 类型缺失时**保守保留**原勾选：不在前端擅自更改用户配置，交由后端 DDL 兜底。
 *
 * @param {Array<{targetType?: string, isPrimaryKey?: boolean, nullable?: number|boolean}>|null|undefined} rows
 * @returns {Array<Object>}
 */
export function enforcePrimaryKeyNotNull(rows) {
  if (!rows) return []
  return rows.map(row => {
    if (!row) return row
    const notKeyCapable = typeof row.targetType === 'string'
      && row.targetType !== ''
      && PRIMARY_KEY_CAPABLE_TYPES.indexOf(row.targetType) < 0
    const isPrimaryKey = row.isPrimaryKey && !notKeyCapable
    if (!isPrimaryKey) {
      return { ...row, isPrimaryKey: false }
    }
    return { ...row, nullable: 0 }
  })
}

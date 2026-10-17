/**
 * 目标表列相关的纯函数：列名归一/匹配、按列推导目标字段类型与长度、目标字段合法性校验。
 * 用于「关闭自动建表 + 选择已有表」时的列选择模式（design D3/D4/D6）。
 */

import { toTargetType, lengthForType } from './fieldMatch.js'

/** 取列名：兼容 `{ name }` 对象与直接字符串；无名称返回空串。 */
export function columnNameOf(column) {
  if (column == null) return ''
  if (typeof column === 'string') return column
  return column.name == null ? '' : String(column.name)
}

/** 列名数组（过滤空名）。 */
export function columnNamesOf(columns) {
  return (columns || []).map(columnNameOf).filter(name => name !== '')
}

/**
 * 按列名查找列，忽略首尾空白与大小写。
 * 未命中或列名为空时返回 null。
 */
export function findColumnByName(columns, name) {
  const target = String(name == null ? '' : name).trim().toLowerCase()
  if (!target) return null
  return (columns || []).find(c => columnNameOf(c).trim().toLowerCase() === target) || null
}

/**
 * 目标字段是否属于列集合（忽略大小写与首尾空白）。
 */
export function isTargetFieldValid(columns, targetField) {
  return findColumnByName(columns, targetField) != null
}

/**
 * 该列是否为主键。读取列接口的 `primaryKey` 标记，兼容布尔 `true` 与字符串 `'true'`
 * （去首尾空白、忽略大小写）；缺失/非对象/其它值一律视为非主键。
 */
export function isPrimaryKeyColumn(column) {
  if (column == null || typeof column !== 'object') return false
  const raw = column.primaryKey
  if (raw === true) return true
  if (typeof raw === 'string') return raw.trim().toLowerCase() === 'true'
  return false
}

/**
 * 主键列名集合（顺序与入参一致，过滤空名）。
 * 用于「关闭自动建表 + 选择已有表」时把目标表主键推导为合并主键。
 */
export function primaryKeyColumnsOf(columns) {
  return (columns || [])
    .filter(isPrimaryKeyColumn)
    .map(columnNameOf)
    .filter(name => name !== '')
}

/**
 * 目标表主键列中，未被任何映射行目标字段命中的列名（顺序同 {@link primaryKeyColumnsOf}）。
 *
 * 用于 MERGE 前置的**非阻断**提示：返回非空表示合并主键未被写入，合并可能不生效。
 * 列名比较忽略首尾空白与大小写。**空 `targetFields` 视为「尚未映射」并返回空数组**——
 * 表刚选定/切换、映射尚未配置时不应误报（否则「选择有主键的表」也会提示未映射）。
 *
 * @param {Array} columns 目标表列（含 `primaryKey` 标记）
 * @param {Array<string>} targetFields 映射行的目标字段
 * @returns {string[]}
 */
export function unmappedPrimaryKeyColumns(columns, targetFields) {
  const mapped = new Set(
    (targetFields || [])
      .map(field => (field == null ? '' : String(field)).trim().toLowerCase())
      .filter(field => field !== '')
  )
  if (mapped.size === 0) return []
  return primaryKeyColumnsOf(columns).filter(pk => !mapped.has(String(pk).trim().toLowerCase()))
}

/**
 * 依据列元数据推导映射行的目标类型与长度。
 * - 类型：`toTargetType(column.type)`（与后端 TargetType 取值一致）。
 * - 长度：仅 `VARCHAR` 有意义，取列声明长度 `size`；缺失/非法时回退默认长度（255）；其它类型为 null。
 */
export function typeAndLengthForColumn(column) {
  const raw = column && typeof column === 'object' ? column : {}
  const targetType = toTargetType(raw.type)
  let targetLength = null
  if (targetType === 'VARCHAR') {
    const size = raw.size == null ? NaN : parseInt(raw.size, 10)
    targetLength = (!isNaN(size) && size > 0) ? size : lengthForType('VARCHAR')
  }
  return { targetType, targetLength }
}

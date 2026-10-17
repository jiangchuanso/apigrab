/**
 * JSONPath / 路径片段拼接（design C9）。
 *
 * 路径拼接的**唯一实现点**：响应树（数据提取路径）与字段映射面板（源字段）共用本模块。
 * 此前两处各自拼接，导致 XML 属性键 `@id` 被拼成 `$.@id`——求值失败被静默吞成 null，
 * 用户界面一切正常但每行取到空值。集中实现以避免再次分叉。
 */

// 标识符：首字符为字母或下划线，其余为字母、数字、下划线。符合的点号写法才可读且可求值
const IDENTIFIER = /^[A-Za-z_][A-Za-z0-9_]*$/

function base(path) {
  return path === null || path === undefined || path === '' ? '$' : String(path)
}

/**
 * 在既有路径后追加一个键。
 * 标识符键用点号写法；其余（`@id`、`#text`、含连字符/空格/数字开头等）改用括号写法。
 */
export function appendSegment(path, key) {
  const root = base(path)
  if (IDENTIFIER.test(key)) {
    return `${root}.${key}`
  }
  const escaped = String(key).replace(/\\/g, '\\\\').replace(/'/g, "\\'")
  return `${root}['${escaped}']`
}

/**
 * 追加数组展平标记。
 * 尾段不带 [*] 时取到的是「数组的数组」，每条记录是数组而非对象：能通过解析，
 * 但下游字段映射取不到值，且报错点远离配置处（见 docs/current/design/unify-response-path.md）。
 */
export function appendFlatten(path) {
  return `${base(path)}[*]`
}

/**
 * 从路径推导目标字段名（取末段）。
 * 不能按 '.' 切分：改用括号写法后 `$.data['@id']` 会切出 `data['@id']` 这类垃圾名。
 */
export function leafName(path) {
  // 不复用 base()：空路径应得到空名而非 '$'
  const text = path === null || path === undefined ? '' : String(path)
  if (text === '') return ''
  if (text === '$') return '$'
  // 去掉数组下标后，取最后一个括号片段
  const withoutIndex = text.replace(/\[\d+\]/g, '')
  const bracket = withoutIndex.match(/\['([^']*)'\](?!.*\[')/)
  if (bracket) return bracket[1]
  const parts = withoutIndex.split('.')
  return parts[parts.length - 1] || ''
}
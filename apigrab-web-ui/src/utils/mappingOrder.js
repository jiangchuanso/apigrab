/**
 * 映射行排序（design C8 / P2）。
 *
 * 顺序由 `sort_order` 持久化，读取时 `ORDER BY sort_order ASC`。重排后按**界面顺序**
 * 写回从 0 起的连续序号，不使用稀疏值。
 *
 * 两个函数都**返回新数组与新对象、不修改入参**：面板里的行是 Vue 的响应式对象，
 * 就地改写会让界面、表单与后端在各处出现不一致。
 */

/**
 * 按当前数组顺序写回连续 sortOrder（0..n-1），其余字段原样保留。
 * @param {Array<Object>|null|undefined} rows
 * @returns {Array<Object>}
 */
export function withSequentialSortOrder(rows) {
  if (!rows) return []
  return rows.map((row, index) => ({ ...row, sortOrder: index }))
}

/**
 * 把 fromIndex 处的行移动到 toIndex。越界或原地不动时返回等值副本。
 * @param {Array<Object>|null|undefined} rows
 * @param {number} fromIndex
 * @param {number} toIndex
 * @returns {Array<Object>}
 */
export function moveRow(rows, fromIndex, toIndex) {
  const list = rows ? rows.slice() : []
  if (fromIndex < 0 || fromIndex >= list.length || toIndex < 0 || toIndex >= list.length || fromIndex === toIndex) {
    return list
  }
  const [moved] = list.splice(fromIndex, 1)
  list.splice(toIndex, 0, moved)
  return list
}

/**
 * URL 查询串抽取（design C4 / C5）。
 *
 * 纯函数，不触碰 DOM、不引入依赖。URL 是输入源：把查询参数拆出来交给调用方填入参数表，
 * 并从 URL 上抹除，使参数表成为唯一事实来源——否则查询参数会在最终请求中出现两次。
 */

/**
 * 把地址拆为「去掉查询串的地址」与「查询参数列表」。
 * 空值键（`?flag`）保留键、值为空串；方括号键（`a[]`）保持字面量而非嵌套结构。
 */
export function extractQuery(url) {
  const raw = url === null || url === undefined ? '' : String(url)
  const mark = raw.indexOf('?')
  if (mark < 0) {
    return { baseUrl: raw, params: [] }
  }
  const baseUrl = raw.slice(0, mark)
  let query = raw.slice(mark + 1)
  const fragment = query.indexOf('#')
  if (fragment >= 0) {
    query = query.slice(0, fragment)
  }
  const params = []
  new URLSearchParams(query).forEach((value, key) => {
    params.push({ key, value })
  })
  return { baseUrl, params }
}

/**
 * 只补不覆盖：参数表中已存在的键一律保留，URL 同名键不参与写入。
 * 参数表是唯一事实来源，用户可能已在其中配置过页码/游标等参数，被覆盖会造成静默数据丢失。
 *
 * @returns {{ toAdd: Array<{key: string, value: string}>, skipped: string[] }}
 */
export function mergeParams(existing, incoming) {
  const known = new Set((existing || []).map(p => p.key))
  const toAdd = []
  const skipped = []
  ;(incoming || []).forEach(param => {
    if (known.has(param.key)) {
      if (!skipped.includes(param.key)) {
        skipped.push(param.key)
      }
      return
    }
    known.add(param.key)
    toAdd.push(param)
  })
  return { toAdd, skipped }
}
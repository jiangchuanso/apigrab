/**
 * FORM 请求体序列化（design D1 / D3）。
 *
 * FORM 的正文契约是 application/x-www-form-urlencoded 编码串（k1=v1&k2=v2）。
 * 采用浏览器标准表单序列化（URLSearchParams）：空格→`+`、`&`→`%26`、`=`→`%3D`、
 * `+`→`%2B`、`%`→`%25`、非 ASCII 按 UTF-8 百分号编码；与后端解码严格对称，
 * 因此键/值中的保留字符不会破坏分隔、也不会双重编码。
 */

/**
 * 把键值对象序列化为编码串。键值按对象顺序输出。
 * `null`/`undefined` 值序列化为空串。
 */
export function serializeFormParams(params) {
  const usp = new URLSearchParams()
  Object.keys(params || {}).forEach(key => {
    const value = params[key]
    usp.append(key, value === null || value === undefined ? '' : String(value))
  })
  return usp.toString()
}

/**
 * 把编码串反序列化为键值对象（自动解码）。空输入返回空对象。
 * 重复键取最后一次出现的值（对象语义）。
 */
export function parseFormParams(content) {
  const params = {}
  if (!content) return params
  new URLSearchParams(String(content)).forEach((value, key) => {
    params[key] = value
  })
  return params
}

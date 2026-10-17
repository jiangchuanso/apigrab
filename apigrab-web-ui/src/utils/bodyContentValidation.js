/**
 * JSON / XML 请求体正文格式校验（design D10 / B9）。
 *
 * 纯前端、无新增依赖。规则：
 * - 空正文不报错：与既有「允许发送空正文」行为一致（表单允许 JSON/XML 正文留空）。
 * - JSON 用 JSON.parse 判定。
 * - XML 良构判定依赖浏览器 DOMParser；环境无解析器时（如 Node 单测）不误报，
 *   并把「解析结果 → 结论」拆成纯函数 {@link xmlVerdictFromDocument} 以便单测。
 */

/**
 * 校验 JSON 正文。返回空串表示通过，否则返回可读中文错误。
 */
export function validateJsonBody(text) {
  if (isBlank(text)) return ''
  try {
    JSON.parse(text)
    return ''
  } catch (e) {
    const detail = e && e.message ? e.message : '无法解析'
    return `JSON 格式不正确：${detail}`
  }
}

/**
 * 纯函数：由已解析的 XML 文档给出结论。
 * 含 `parsererror` 元素即良构失败；文档不可用（无解析器）时返回空串，不误报。
 */
export function xmlVerdictFromDocument(doc) {
  if (!doc || typeof doc.getElementsByTagName !== 'function') {
    return ''
  }
  const errors = doc.getElementsByTagName('parsererror')
  if (errors && errors.length) {
    const node = errors[0]
    const detail = node && node.textContent ? String(node.textContent).trim() : ''
    return detail ? `XML 格式不正确：${detail}` : 'XML 格式不正确'
  }
  return ''
}

/**
 * 校验 XML 正文。返回空串表示通过（含「当前环境无法校验」），否则返回可读中文错误。
 */
export function validateXmlBody(text) {
  if (isBlank(text)) return ''
  if (typeof DOMParser === 'undefined') {
    return ''
  }
  let doc
  try {
    doc = new DOMParser().parseFromString(text, 'application/xml')
  } catch (e) {
    return 'XML 格式不正确'
  }
  return xmlVerdictFromDocument(doc)
}

/**
 * 按 Body 类型分派校验：仅 JSON / XML 校验，其它类型（NONE / FORM）不校验。
 */
export function validateBodyContent(bodyType, content) {
  if (bodyType === 'JSON') return validateJsonBody(content)
  if (bodyType === 'XML') return validateXmlBody(content)
  return ''
}

function isBlank(text) {
  return text === null || text === undefined || String(text).trim() === ''
}

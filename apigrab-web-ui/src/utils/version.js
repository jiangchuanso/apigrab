/**
 * 版本展示工具。
 *
 * 后端 `GET /api/version` 返回统一响应体 `{ code, message, data: { version } }`。
 * 展示层需要两个纯函数：从响应体安全取值（缺失时降级为空串），以及把版本号
 * 统一格式化为带小写 v 前缀的展示形态（如 `1.0.0` -> `v1.0.0`）。
 */

export function formatVersion(raw) {
  if (raw === null || raw === undefined) return ''
  const trimmed = String(raw).trim()
  if (!trimmed) return ''
  const withoutPrefix = /^v(?=\d)/i.test(trimmed) ? trimmed.slice(1) : trimmed
  return 'v' + withoutPrefix
}

export function extractVersion(payload) {
  if (!payload || typeof payload !== 'object') return ''
  const data = payload.data
  if (!data || typeof data !== 'object') return ''
  const version = data.version
  if (version === null || version === undefined) return ''
  return String(version).trim()
}

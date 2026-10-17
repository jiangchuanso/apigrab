import request from '@/utils/request'

export function getAuthConfigList(params) {
  return request({ url: '/auth-configs', method: 'get', params })
}

export function getAuthConfigDetail(id) {
  return request({ url: `/auth-configs/${id}`, method: 'get' })
}

export function createAuthConfig(data) {
  return request({ url: '/auth-configs', method: 'post', data })
}

export function updateAuthConfig(id, data) {
  return request({ url: `/auth-configs/${id}`, method: 'put', data })
}

export function deleteAuthConfig(id) {
  return request({ url: `/auth-configs/${id}`, method: 'delete' })
}

/**
 * 获取 Token 测试（仅 OAuth2 / 账号密码换 Token）。
 * 返回 `{ rawResponse, token, message }`：rawResponse 为原始 Token 响应（供展示结构、点选提取路径），
 * token 为按路径提取到的 Token（未设置路径或响应非 JSON 时为空），message 为可读说明（可空）。
 */
export function testAuthConfig(data) {
  return request({ url: '/auth-configs/test', method: 'post', data })
}

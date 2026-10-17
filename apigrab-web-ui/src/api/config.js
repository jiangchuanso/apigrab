import request from '@/utils/request'

export function getConfigList(params) {
  return request({ url: '/configs', method: 'get', params })
}

export function getConfigDetail(id) {
  return request({ url: `/configs/${id}`, method: 'get' })
}

export function createConfig(data) {
  return request({ url: '/configs', method: 'post', data })
}

export function updateConfig(id, data) {
  return request({ url: `/configs/${id}`, method: 'put', data })
}

export function deleteConfig(id) {
  return request({ url: `/configs/${id}`, method: 'delete' })
}

export function testConfig(id, data) {
  return request({ url: `/configs/${id}/test`, method: 'post', data })
}

export function testConfigTransient(data) {
  return request({ url: '/configs/test', method: 'post', data })
}

export function testDetailConfig(id, data) {
  return request({ url: `/configs/${id}/test-detail`, method: 'post', data })
}

export function testDetailConfigTransient(data) {
  return request({ url: '/configs/test-detail', method: 'post', data })
}

export function debugScript(id, data) {
  return request({ url: `/configs/${id}/debug-script`, method: 'post', data })
}

export function previewMapping(id, data) {
  return request({ url: `/configs/${id}/preview-mapping`, method: 'post', data })
}

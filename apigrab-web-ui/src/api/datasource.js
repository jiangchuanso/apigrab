import request from '@/utils/request'

export function getDatasourceList(params) {
  return request({ url: '/datasources', method: 'get', params })
}

export function getDatasourceDetail(id) {
  return request({ url: `/datasources/${id}`, method: 'get' })
}

export function createDatasource(data) {
  return request({ url: '/datasources', method: 'post', data })
}

export function updateDatasource(id, data) {
  return request({ url: `/datasources/${id}`, method: 'put', data })
}

export function deleteDatasource(id) {
  return request({ url: `/datasources/${id}`, method: 'delete' })
}

export function testDatasource(id) {
  return request({ url: `/datasources/${id}/test`, method: 'post' })
}

export function testDatasourceTransient(data) {
  return request({ url: '/datasources/test', method: 'post', data })
}

export function getDatasourceSchemas(id) {
  return request({ url: `/datasources/${id}/schemas`, method: 'get' })
}

export function getDatasourceTables(id, schema) {
  return request({ url: `/datasources/${id}/tables`, method: 'get', params: { schema } })
}

export function getDatasourceColumns(id, schema, tableName) {
  return request({ url: `/datasources/${id}/columns`, method: 'get', params: { schema, tableName } })
}

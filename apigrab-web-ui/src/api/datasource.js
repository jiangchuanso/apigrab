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

// 数据库类型清单由后端 DbType 枚举驱动
export function getDbTypes() {
  return request({ url: '/datasource-meta/db-types', method: 'get' })
}

// 外部驱动包清单由后端扫描 drivers/ 目录得到
export function getDriverPackages() {
  return request({ url: '/datasource-meta/drivers', method: 'get' })
}

export function refreshDriverPackages() {
  return request({ url: '/datasource-meta/drivers/refresh', method: 'post' })
}

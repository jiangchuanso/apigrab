import request from '@/utils/request'

export function getExecutionList(params) {
  return request({ url: '/executions', method: 'get', params })
}

export function getExecutionDetail(id) {
  return request({ url: `/executions/${id}`, method: 'get' })
}

export function getExecutionData(id) {
  return request({ url: `/executions/${id}/data`, method: 'get' })
}

export function getExecutionStages(id) {
  return request({ url: `/executions/${id}/stages`, method: 'get' })
}

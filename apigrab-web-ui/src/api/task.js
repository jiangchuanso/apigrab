import request from '@/utils/request'

export function getTaskList(params) {
  return request({ url: '/tasks', method: 'get', params })
}

export function getTaskDetail(id) {
  return request({ url: `/tasks/${id}`, method: 'get' })
}

export function createTask(data) {
  return request({ url: '/tasks', method: 'post', data })
}

export function updateTask(id, data) {
  return request({ url: `/tasks/${id}`, method: 'put', data })
}

export function deleteTask(id) {
  return request({ url: `/tasks/${id}`, method: 'delete' })
}

export function executeTask(id) {
  return request({ url: `/tasks/${id}/execute`, method: 'post' })
}

export function pauseTask(id) {
  return request({ url: `/tasks/${id}/pause`, method: 'put' })
}

export function resumeTask(id) {
  return request({ url: `/tasks/${id}/resume`, method: 'put' })
}

export function previewTask(id) {
  return request({ url: `/tasks/${id}/preview`, method: 'get' })
}

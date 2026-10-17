import request from '@/utils/request'

export function getUserList(params) {
  return request({ url: '/users', method: 'get', params })
}

export function getUserDetail(id) {
  return request({ url: `/users/${id}`, method: 'get' })
}

export function createUser(data) {
  return request({ url: '/users', method: 'post', data })
}

export function updateUser(id, data) {
  return request({ url: `/users/${id}`, method: 'put', data })
}

export function deleteUser(id) {
  return request({ url: `/users/${id}`, method: 'delete' })
}

export function resetUserPassword(id, data) {
  return request({ url: `/users/${id}/password`, method: 'put', data })
}

export function updateUserStatus(id, data) {
  return request({ url: `/users/${id}/status`, method: 'put', data })
}

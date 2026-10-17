import request from '@/utils/request'

export function getAlarmConfig() {
  return request({ url: '/alarm/config', method: 'get' })
}

export function saveAlarmConfig(data) {
  return request({ url: '/alarm/config', method: 'post', data })
}

export function testAlarm(data) {
  return request({ url: '/alarm/test', method: 'post', data })
}

export function getAlarmExample() {
  return request({ url: '/alarm/example', method: 'get' })
}

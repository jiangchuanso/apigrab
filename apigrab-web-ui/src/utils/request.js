import axios from 'axios'
import { Message } from 'element-ui'
import store from '@/store'
import Qs from 'qs'

const service = axios.create({
  baseURL: '/api',
  timeout: 60000,
  paramsSerializer: params => Qs.stringify(params, { indices: false })
})

// 请求拦截 - 携带 Token
service.interceptors.request.use(
  config => {
    if (store.getters.isLoggedIn) {
      config.headers['Authorization'] = 'Bearer ' + store.state.token
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 0 || res.code === 200) {
      return res
    }
    Message.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  error => {
    const status = error.response?.status
    const url = error.config?.url || ''
    if (status === 401) {
      if (url.indexOf('/auth/login') !== -1) {
        // 登录接口的 401 是凭据错误，直接提示服务端消息
        Message.error(error.response?.data?.message || '用户名或密码错误')
      } else {
        // Token 过期或未授权，清除本地登录态并跳转登录页（避免再次调用登出接口造成循环）
        Message.error('登录已过期，请重新登录')
        store.commit('CLEAR_AUTH')
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
    } else {
      const msg = error.response?.data?.message || error.message || '网络异常'
      Message.error(msg)
    }
    return Promise.reject(error)
  }
)

export default service

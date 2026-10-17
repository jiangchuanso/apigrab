import Vue from 'vue'
import Vuex from 'vuex'
import { login, logout, getUserInfo } from '@/api/auth'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    realName: '',
    role: localStorage.getItem('role') || ''
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    SET_USERNAME(state, username) {
      state.username = username
      localStorage.setItem('username', username || '')
    },
    SET_REALNAME(state, realName) {
      state.realName = realName
    },
    SET_ROLE(state, role) {
      state.role = role
      localStorage.setItem('role', role || '')
    },
    CLEAR_AUTH(state) {
      state.token = ''
      state.username = ''
      state.realName = ''
      state.role = ''
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
    }
  },
  actions: {
    async login({ commit, dispatch }, loginForm) {
      const res = await login(loginForm)
      const data = res.data || {}
      const token = data.token || res.data
      if (token) {
        commit('SET_TOKEN', token)
      }
      await dispatch('getUserInfo')
      return res
    },
    async getUserInfo({ commit }) {
      const res = await getUserInfo()
      const data = res.data || {}
      commit('SET_USERNAME', data.username || '')
      commit('SET_REALNAME', data.realName || '')
      commit('SET_ROLE', data.role || '')
      return data
    },
    async logout({ commit }) {
      try {
        await logout()
      } catch (e) {
        // 忽略登出失败
      }
      commit('CLEAR_AUTH')
    }
  },
  getters: {
    isLoggedIn: state => !!state.token,
    username: state => state.username,
    role: state => state.role,
    isAdmin: state => state.role === 'ADMIN'
  }
})

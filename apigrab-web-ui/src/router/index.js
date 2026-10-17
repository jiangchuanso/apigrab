import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '@/store'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Index.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue'),
        meta: { title: '仪表盘' }
      },
      // 接口配置
      {
        path: '/api-config/list',
        name: 'ApiConfigList',
        component: () => import('@/views/api-config/List.vue'),
        meta: { title: '接口配置列表' }
      },
      {
        path: '/api-config/edit/:id?',
        name: 'ApiConfigEdit',
        component: () => import('@/views/api-config/Edit.vue'),
        meta: { title: '接口配置编辑' }
      },
      // 认证配置
      {
        path: '/auth-config/list',
        name: 'AuthConfigList',
        component: () => import('@/views/auth-config/List.vue'),
        meta: { title: '认证配置列表' }
      },
      {
        path: '/auth-config/edit/:id?',
        name: 'AuthConfigEdit',
        component: () => import('@/views/auth-config/Edit.vue'),
        meta: { title: '认证配置编辑' }
      },
      // 数据源
      {
        path: '/datasource/list',
        name: 'DatasourceList',
        component: () => import('@/views/datasource/List.vue'),
        meta: { title: '数据源列表' }
      },
      {
        path: '/datasource/edit/:id?',
        name: 'DatasourceEdit',
        component: () => import('@/views/datasource/Edit.vue'),
        meta: { title: '数据源编辑' }
      },
      // 采集任务
      {
        path: '/task/list',
        name: 'TaskList',
        component: () => import('@/views/task/List.vue'),
        meta: { title: '任务列表' }
      },
      {
        path: '/task/edit/:id?',
        name: 'TaskEdit',
        component: () => import('@/views/task/Edit.vue'),
        meta: { title: '任务编辑' }
      },
      // 执行监控
      {
        path: '/monitor/history',
        name: 'ExecutionHistory',
        component: () => import('@/views/monitor/History.vue'),
        meta: { title: '执行历史' }
      },
      {
        path: '/monitor/log/:executionId',
        name: 'ExecutionLog',
        component: () => import('@/views/monitor/LogDetail.vue'),
        meta: { title: '执行详情' }
      },
      // 告警配置
      {
        path: '/alarm',
        name: 'AlarmConfig',
        component: () => import('@/views/alarm/Index.vue'),
        meta: { title: '告警配置' }
      },
      // 用户管理
      {
        path: '/user/list',
        name: 'UserList',
        component: () => import('@/views/user/List.vue'),
        meta: { title: '用户管理', requiresAdmin: true }
      },
      // 个人中心
      {
        path: '/user/profile',
        name: 'UserProfile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  }
]

const originalPush = VueRouter.prototype.push
const originalReplace = VueRouter.prototype.replace

VueRouter.prototype.push = function push (location) {
  return originalPush.call(this, location).catch(err => {
    if (err.name !== 'NavigationDuplicated' && !err.message.includes('Redirected')) {
      return Promise.reject(err)
    }
  })
}

VueRouter.prototype.replace = function replace (location) {
  return originalReplace.call(this, location).catch(err => {
    if (err.name !== 'NavigationDuplicated' && !err.message.includes('Redirected')) {
      return Promise.reject(err)
    }
  })
}

const router = new VueRouter({
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = (to.meta.title || 'ApiGrab') + ' - ApiGrab'

  if (to.meta.noAuth) {
    // 已登录用户访问登录页，重定向到统计主页
    if (to.path === '/login' && store.getters.isLoggedIn) {
      next({ path: '/dashboard' })
    } else {
      next()
    }
  } else {
    // 需要认证的页面
    if (store.getters.isLoggedIn) {
      if (to.meta.requiresAdmin && !store.getters.isAdmin) {
        next({ path: '/dashboard' })
      } else {
        next()
      }
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }
})

export default router

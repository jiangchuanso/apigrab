<template>
  <div class="rc-layout">
    <el-container class="rc-layout-container">
      <!-- 侧边栏 -->
      <el-aside class="rc-aside" :width="isCollapse ? '64px' : '220px'">
        <div class="rc-logo">
          <span class="rc-logo__mark" aria-hidden="true">AG</span>
          <span v-if="!isCollapse" class="rc-logo__name">ApiGrab</span>
        </div>
        <el-menu
          class="rc-sidebar"
          :default-active="activeMenu"
          :collapse="isCollapse"
          background-color="transparent"
          text-color="#bfcbd9"
          active-text-color="#ffffff"
          unique-opened
          router>
          <el-menu-item index="/dashboard">
            <i class="el-icon-s-data"></i>
            <span slot="title">仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/datasource/list">
            <i class="el-icon-bank-card"></i>
            <span slot="title">数据源管理</span>
          </el-menu-item>
          <el-submenu index="api-management">
            <template slot="title">
              <i class="el-icon-s-management"></i>
              <span>接口管理</span>
            </template>
            <el-menu-item index="/auth-config/list">
              <i class="el-icon-key"></i>
              <span slot="title">认证配置</span>
            </el-menu-item>
            <el-menu-item index="/api-config/list">
              <i class="el-icon-connection"></i>
              <span slot="title">接口配置</span>
            </el-menu-item>
          </el-submenu>
          <el-submenu index="task-management">
            <template slot="title">
              <i class="el-icon-s-cooperation"></i>
              <span>任务管理</span>
            </template>
            <el-menu-item index="/task/list">
              <i class="el-icon-s-operation"></i>
              <span slot="title">采集任务</span>
            </el-menu-item>
            <el-menu-item index="/monitor/history">
              <i class="el-icon-document"></i>
              <span slot="title">执行监控</span>
            </el-menu-item>
          </el-submenu>
          <el-menu-item index="/alarm">
            <i class="el-icon-bell"></i>
            <span slot="title">告警配置</span>
          </el-menu-item>
          <el-menu-item v-if="isAdmin" index="/user/list">
            <i class="el-icon-user-solid"></i>
            <span slot="title">用户管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <!-- 顶部栏 -->
        <el-header class="rc-header">
          <button
            type="button"
            class="rc-collapse-btn"
            :aria-label="isCollapse ? '展开侧边栏' : '收起侧边栏'"
            :aria-expanded="String(!isCollapse)"
            @click="isCollapse = !isCollapse">
            <i :class="isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
          </button>
          <div class="rc-header-title">接口数据采集入库系统</div>
          <div class="rc-header-right">
            <el-dropdown trigger="click" @command="handleCommand">
              <span class="rc-user-link" role="button" tabindex="0">
                <i class="el-icon-user"></i>
                <span class="rc-user-name">{{ username || '未登录' }}</span>
                <i class="el-icon-arrow-down rc-user-caret"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="about">关于系统</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 主内容 -->
        <el-main class="rc-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <about-dialog :visible.sync="aboutVisible" />
  </div>
</template>

<script>
import AboutDialog from '@/components/AboutDialog'

export default {
  name: 'Layout',
  components: {
    AboutDialog
  },
  data() {
    return {
      isCollapse: false,
      aboutVisible: false
    }
  },
  computed: {
    activeMenu() {
      return this.$route.path
    },
    username() {
      return this.$store.getters.username
    },
    isAdmin() {
      return this.$store.getters.isAdmin
    }
  },
  methods: {
    async handleCommand(command) {
      if (command === 'profile') {
        this.$router.push('/user/profile')
      } else if (command === 'about') {
        this.aboutVisible = true
      } else if (command === 'logout') {
        await this.$store.dispatch('logout')
        this.$router.push('/login')
      }
    }
  }
}
</script>

<style scoped>
.rc-layout {
  height: 100vh;
}

.rc-layout-container {
  height: 100%;
}

.rc-aside {
  background-color: var(--rc-sidebar);
  transition: width 0.25s ease;
  overflow: hidden;
}

.rc-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--rc-space-2);
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
}
/* Same brand mark as the login page: one product, one identity */
.rc-logo__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--rc-primary), var(--rc-primary-strong));
  font-size: var(--rc-text-xs);
  font-weight: 700;
  letter-spacing: 0.5px;
}
.rc-logo__name {
  font-size: var(--rc-text-base);
  font-weight: 600;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

.rc-sidebar {
  border-right: none;
  background-color: transparent;
}

.rc-header {
  display: flex;
  align-items: center;
  gap: var(--rc-space-4);
  height: 56px;
  padding: 0 var(--rc-space-5);
  background: #fff;
  border-bottom: 1px solid var(--rc-border);
  box-shadow: var(--rc-shadow-header);
}

.rc-collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: var(--rc-radius);
  color: var(--rc-text-secondary);
  font-size: 20px;
  cursor: pointer;
  transition: background-color var(--rc-transition), color var(--rc-transition);
}
.rc-collapse-btn:hover {
  background: #f2f3f5;
  color: var(--rc-text);
}

.rc-header-title {
  font-size: var(--rc-text-base);
  color: var(--rc-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rc-header-right {
  margin-left: auto;
}

/* Comfortable hit target, and a surface so it reads as interactive */
.rc-user-link {
  display: inline-flex;
  align-items: center;
  gap: var(--rc-space-2);
  height: 36px;
  padding: 0 var(--rc-space-3);
  border-radius: var(--rc-radius);
  color: var(--rc-text-secondary);
  font-size: var(--rc-text-base);
  cursor: pointer;
  transition: background-color var(--rc-transition), color var(--rc-transition);
}
.rc-user-link:hover {
  background: #f2f3f5;
  color: var(--rc-text);
}
.rc-user-caret {
  font-size: var(--rc-text-xs);
  color: var(--rc-text-placeholder);
}

.rc-user-name {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rc-main {
  background-color: var(--rc-bg);
  padding: var(--rc-space-5);
  overflow-y: auto;
}
</style>

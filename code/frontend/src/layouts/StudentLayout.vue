<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo">健身分析系统</div>
      <el-menu :default-active="$route.path" router class="menu">
        <el-menu-item index="/student/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>个人仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/student/calendar">
          <el-icon><Calendar /></el-icon>
          <span>运动日历</span>
        </el-menu-item>
        <el-menu-item index="/student/checkin">
          <el-icon><EditPen /></el-icon>
          <span>运动打卡</span>
        </el-menu-item>
        <el-menu-item index="/student/progress">
          <el-icon><TrendCharts /></el-icon>
          <span>效果追踪</span>
        </el-menu-item>
        <el-menu-item index="/student/plan">
          <el-icon><Document /></el-icon>
          <span>训练计划</span>
        </el-menu-item>
        <el-menu-item index="/student/achievements">
          <el-icon><Trophy /></el-icon>
          <span>成就排行榜</span>
        </el-menu-item>
        <el-menu-item index="/student/exercise-library">
          <el-icon><Reading /></el-icon>
          <span>运动库</span>
        </el-menu-item>
        <el-menu-item index="/student/settings">
          <el-icon><Setting /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-content">
          <span class="welcome">欢迎，{{ realName }}</span>
          <el-button class="logout-btn" @click="handleLogout" text>退出登录</el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const realName = computed(() => localStorage.getItem('realName') || '用户')

const handleLogout = () => {
  localStorage.clear()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: #f3f7ff;
}

.sidebar {
  color: #fff;
  background: linear-gradient(180deg, #4a90ff 0%, #6f63ff 100%);
  box-shadow: 2px 0 18px rgba(74, 112, 255, 0.2);
}

.logo {
  height: 64px;
  line-height: 64px;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
  background: rgba(255, 255, 255, 0.12);
}

.menu {
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  margin: 8px 10px;
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.9);
}

.menu :deep(.el-menu-item:hover),
.menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: rgba(255, 255, 255, 0.22);
}

.layout-header {
  background: #fff;
  box-shadow: 0 1px 8px rgba(74, 112, 255, 0.12);
}

.header-content {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.welcome {
  font-weight: 600;
  color: #2f3f63;
}

.logout-btn {
  color: #4a90ff;
}

.layout-main {
  background: #f3f7ff;
  padding: 20px;
}
</style>


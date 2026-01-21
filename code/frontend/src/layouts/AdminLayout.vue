<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">健身分析系统</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/admin/monitoring">
          <el-icon><Monitor /></el-icon>
          <span>全局监控</span>
        </el-menu-item>
        <el-menu-item index="/admin/coaches">
          <el-icon><UserFilled /></el-icon>
          <span>教练管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/students">
          <el-icon><User /></el-icon>
          <span>学员管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/analytics">
          <el-icon><DataAnalysis /></el-icon>
          <span>行为分析</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-content">
          <span>欢迎，管理员 {{ realName }}</span>
          <el-button @click="handleLogout" text>退出登录</el-button>
        </div>
      </el-header>
      <el-main>
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
}

.el-aside {
  background-color: #304156;
  color: #fff;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  background-color: #1f2d3d;
}

.el-menu {
  border-right: none;
  background-color: #304156;
}

.el-menu-item {
  color: #bfcbd9;
}

.el-menu-item:hover,
.el-menu-item.is-active {
  background-color: #263445 !important;
  color: #409eff !important;
}

.el-header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  display: flex;
  align-items: center;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>

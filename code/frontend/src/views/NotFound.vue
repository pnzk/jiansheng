<template>
  <div class="not-found-page">
    <div class="content">
      <div class="error-code">404</div>
      <div class="error-title">页面未找到</div>
      <div class="error-desc">抱歉，您访问的页面不存在或已被移除</div>
      <div class="actions">
        <el-button type="primary" size="large" @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        <el-button size="large" @click="goBack">
          <el-icon><Back /></el-icon>
          返回上页
        </el-button>
      </div>
    </div>
    <div class="illustration">
      <svg viewBox="0 0 400 300" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="grad1" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color:#409eff;stop-opacity:1" />
            <stop offset="100%" style="stop-color:#67c23a;stop-opacity:1" />
          </linearGradient>
        </defs>
        <circle cx="200" cy="150" r="80" fill="url(#grad1)" opacity="0.1"/>
        <circle cx="200" cy="150" r="60" fill="url(#grad1)" opacity="0.2"/>
        <circle cx="200" cy="150" r="40" fill="url(#grad1)" opacity="0.3"/>
        <text x="200" y="160" text-anchor="middle" fill="#409eff" font-size="24" font-weight="bold">
          🏋️
        </text>
        <path d="M100,250 Q200,200 300,250" stroke="#409eff" stroke-width="3" fill="none" opacity="0.5"/>
      </svg>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  const role = localStorage.getItem('role')
  if (role === 'ADMIN') {
    router.push('/admin/coaches')
  } else if (role === 'COACH') {
    router.push('/coach/dashboard')
  } else if (role === 'STUDENT') {
    router.push('/student/dashboard')
  } else {
    router.push('/login')
  }
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef4ff 0%, #f4f0ff 100%);
  padding: 20px;
}

.content {
  text-align: center;
  z-index: 1;
}

.error-code {
  font-size: 120px;
  font-weight: bold;
  background: linear-gradient(135deg, #4a90ff, #6f63ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 20px;
}

.error-title {
  font-size: 32px;
  color: #303133;
  margin-bottom: 15px;
}

.error-desc {
  font-size: 16px;
  color: #909399;
  margin-bottom: 40px;
}

.actions {
  display: flex;
  gap: 15px;
  justify-content: center;
}

.illustration {
  position: absolute;
  width: 400px;
  height: 300px;
  opacity: 0.5;
}

.illustration svg {
  width: 100%;
  height: 100%;
}
</style>

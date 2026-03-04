<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
    <el-card class="login-card">
      <div class="logo-section">
        <div class="logo-icon">💪</div>
        <h2 class="title">健身房分析系统</h2>
        <p class="subtitle">科学健身，数据驱动</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <div class="form-options">
          <el-checkbox v-model="rememberMe">记住登录账号</el-checkbox>
          <el-button text type="primary" size="small">忘记密码？</el-button>
        </div>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" size="large" style="width: 100%">
            登 录
          </el-button>
        </el-form-item>
        <div class="divider">
          <span>或使用以下方式登录</span>
        </div>
        <div class="quick-login">
          <el-button @click="quickLogin('user00001')" round>
            <span class="quick-icon">🎓</span> 学员体验
          </el-button>
          <el-button @click="quickLogin('coach_auto_001')" round>
            <span class="quick-icon">🏋️</span> 教练体验
          </el-button>
          <el-button @click="quickLogin('admin_auto_001')" round>
            <span class="quick-icon">👨‍💼</span> 管理员
          </el-button>
        </div>
        <div class="register-link">
          <span>还没有账号？</span>
          <el-button text type="primary" @click="$router.push('/register')">立即注册</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(() => {
  const savedUsername = localStorage.getItem('savedUsername')
  if (savedUsername) {
    form.username = savedUsername
    rememberMe.value = true
  }
})

const handleLogin = async () => {
  await formRef.value.validate()
  
  loading.value = true
  try {
    const res = await login(form)
    
    localStorage.setItem('token', res.token)
    localStorage.setItem('userId', res.userId)
    localStorage.setItem('username', res.username)
    localStorage.setItem('role', res.role)
    localStorage.setItem('realName', res.realName)
    
    if (rememberMe.value) {
      localStorage.setItem('savedUsername', form.username)
    } else {
      localStorage.removeItem('savedUsername')
    }
    
    ElMessage.success('登录成功')
    
    if (res.role === 'STUDENT') {
      router.push('/student/dashboard')
    } else if (res.role === 'COACH') {
      router.push('/coach/dashboard')
    } else if (res.role === 'ADMIN') {
      router.push('/admin/coaches')
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const quickLogin = async (username) => {
  form.username = username
  form.password = '123456'
  await handleLogin()
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #4a90ff 0%, #6f63ff 100%);
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  left: -100px;
  animation: float 6s ease-in-out infinite;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: -50px;
  right: -50px;
  animation: float 8s ease-in-out infinite reverse;
}

.circle-3 {
  width: 150px;
  height: 150px;
  top: 50%;
  right: 10%;
  animation: float 7s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.login-card {
  width: 420px;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 20px 48px rgba(74, 112, 255, 0.24);
  z-index: 1;
}

.logo-section {
  text-align: center;
  margin-bottom: 30px;
}

.logo-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.title {
  margin: 0;
  color: #303133;
  font-size: 24px;
}

.subtitle {
  color: #909399;
  margin-top: 8px;
  font-size: 14px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.divider {
  text-align: center;
  margin: 20px 0;
  position: relative;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 30%;
  height: 1px;
  background: #dcdfe6;
}

.divider::before {
  left: 0;
}

.divider::after {
  right: 0;
}

.divider span {
  background: white;
  padding: 0 15px;
  color: #909399;
  font-size: 12px;
}

.quick-login {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.quick-login .el-button {
  font-size: 12px;
}

.quick-icon {
  margin-right: 4px;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  color: #909399;
  font-size: 14px;
}
</style>

<template>
  <div class="register-container">
    <div class="register-bg">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
    </div>
    <el-card class="register-card">
      <div class="logo-section">
        <div class="logo-icon">🏋️</div>
        <h2 class="title">加入我们</h2>
        <p class="subtitle">开启您的健身之旅</p>
      </div>
      
      <!-- 步骤指示器 -->
      <el-steps :active="currentStep" simple style="margin-bottom: 30px">
        <el-step title="账号信息" />
        <el-step title="个人资料" />
        <el-step title="健身目标" />
      </el-steps>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <!-- 步骤1: 账号信息 -->
        <div v-show="currentStep === 0">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="3-20个字符" prefix-icon="User" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="6-20个字符" prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" prefix-icon="Lock" size="large" show-password />
          </el-form-item>
        </div>

        <!-- 步骤2: 个人资料 -->
        <div v-show="currentStep === 1">
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" size="large" />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="请输入邮箱" size="large" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" placeholder="请输入手机号" size="large" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="年龄" prop="age">
                <el-input-number v-model="form.age" :min="15" :max="80" size="large" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="性别" prop="gender">
                <el-radio-group v-model="form.gender" size="large">
                  <el-radio-button label="MALE">👨 男</el-radio-button>
                  <el-radio-button label="FEMALE">👩 女</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 步骤3: 健身目标 -->
        <div v-show="currentStep === 2">
          <el-form-item label="我是" prop="role">
            <div class="role-cards">
              <div class="role-card" :class="{ active: form.role === 'STUDENT' }" @click="form.role = 'STUDENT'">
                <div class="role-icon">🎓</div>
                <div class="role-name">学员</div>
                <div class="role-desc">我想科学健身</div>
              </div>
              <div class="role-card" :class="{ active: form.role === 'COACH' }" @click="form.role = 'COACH'">
                <div class="role-icon">🏋️</div>
                <div class="role-name">教练</div>
                <div class="role-desc">我想指导学员</div>
              </div>
            </div>
          </el-form-item>
          <el-form-item label="健身目标" prop="fitnessGoal" v-if="form.role === 'STUDENT'">
            <div class="goal-cards">
              <div class="goal-card" :class="{ active: form.fitnessGoal === 'WEIGHT_LOSS' }" @click="form.fitnessGoal = 'WEIGHT_LOSS'">
                <div class="goal-icon">⚖️</div>
                <div class="goal-name">减重</div>
              </div>
              <div class="goal-card" :class="{ active: form.fitnessGoal === 'FAT_LOSS' }" @click="form.fitnessGoal = 'FAT_LOSS'">
                <div class="goal-icon">🔥</div>
                <div class="goal-name">减脂</div>
              </div>
              <div class="goal-card" :class="{ active: form.fitnessGoal === 'MUSCLE_GAIN' }" @click="form.fitnessGoal = 'MUSCLE_GAIN'">
                <div class="goal-icon">💪</div>
                <div class="goal-name">增肌</div>
              </div>
            </div>
          </el-form-item>
        </div>

        <!-- 按钮区域 -->
        <div class="form-actions">
          <el-button v-if="currentStep > 0" @click="prevStep" size="large">上一步</el-button>
          <el-button v-if="currentStep < 2" type="primary" @click="nextStep" size="large" style="flex: 1">下一步</el-button>
          <el-button v-else type="primary" @click="handleRegister" :loading="loading" size="large" style="flex: 1">
            完成注册
          </el-button>
        </div>

        <div class="login-link">
          <span>已有账号？</span>
          <el-button text type="primary" @click="$router.push('/login')">立即登录</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const currentStep = ref(0)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  email: '',
  phone: '',
  age: 25,
  gender: 'MALE',
  role: 'STUDENT',
  fitnessGoal: 'WEIGHT_LOSS'
})

const validatePassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const step1Fields = ['username', 'password', 'confirmPassword']
const step2Fields = ['realName', 'email', 'phone', 'age', 'gender']

const nextStep = async () => {
  const fields = currentStep.value === 0 ? step1Fields : step2Fields
  try {
    await formRef.value.validateField(fields)
    currentStep.value++
  } catch (e) {
    ElMessage.warning('请完善当前步骤的信息')
  }
}

const prevStep = () => {
  currentStep.value--
}

const handleRegister = async () => {
  loading.value = true
  try {
    const { confirmPassword, ...data } = form
    await register(data)
    
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.register-bg {
  position: absolute;
  width: 100%;
  height: 100%;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.circle-1 {
  width: 400px;
  height: 400px;
  top: -150px;
  right: -150px;
  animation: float 8s ease-in-out infinite;
}

.circle-2 {
  width: 300px;
  height: 300px;
  bottom: -100px;
  left: -100px;
  animation: float 6s ease-in-out infinite reverse;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.register-card {
  width: 520px;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  z-index: 1;
}

.logo-section {
  text-align: center;
  margin-bottom: 20px;
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

.role-cards {
  display: flex;
  gap: 20px;
}

.role-card {
  flex: 1;
  padding: 20px;
  border: 2px solid #dcdfe6;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.role-card:hover {
  border-color: #409eff;
}

.role-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.role-icon {
  font-size: 40px;
  margin-bottom: 10px;
}

.role-name {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.role-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.goal-cards {
  display: flex;
  gap: 15px;
}

.goal-card {
  flex: 1;
  padding: 15px;
  border: 2px solid #dcdfe6;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.goal-card:hover {
  border-color: #409eff;
}

.goal-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.goal-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.goal-name {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
}

.form-actions {
  display: flex;
  gap: 15px;
  margin-top: 30px;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  color: #909399;
  font-size: 14px;
}
</style>

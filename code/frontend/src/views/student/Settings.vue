<template>
  <div class="settings-page">
    <h2>个人中心</h2>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="个人信息" name="profile">
        <el-form :model="profileForm" label-width="120px">
          <el-form-item label="用户名">
            <el-input v-model="profileForm.username" disabled />
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="profileForm.realName" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" />
          </el-form-item>
          <el-form-item label="手机">
            <el-input v-model="profileForm.phone" />
          </el-form-item>
          <el-form-item label="年龄">
            <el-input-number v-model="profileForm.age" :min="1" :max="120" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="profileForm.gender">
              <el-radio label="MALE">男</el-radio>
              <el-radio label="FEMALE">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="健身目标">
            <el-select v-model="profileForm.fitnessGoal">
              <el-option label="减重" value="WEIGHT_LOSS" />
              <el-option label="减脂" value="FAT_LOSS" />
              <el-option label="增肌" value="MUSCLE_GAIN" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="updateProfile">保存</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="修改密码" name="password">
        <el-form :model="passwordForm" label-width="120px">
          <el-form-item label="原密码">
            <el-input v-model="passwordForm.oldPassword" type="password" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="changePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="隐私设置" name="privacy">
        <el-form :model="privacyForm" label-width="180px">
          <el-form-item label="在排行榜中显示">
            <el-switch v-model="privacyForm.showInLeaderboard" />
          </el-form-item>
          <el-form-item label="允许教练查看数据">
            <el-switch v-model="privacyForm.allowCoachView" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="updatePrivacy">保存</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserProfile, updateUserProfile, changePassword as changePasswordApi, updatePrivacySettings } from '@/api/user'

const activeTab = ref('profile')
const profileForm = ref({})
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const privacyForm = ref({
  showInLeaderboard: true,
  allowCoachView: true
})

const loadProfile = async () => {
  try {
    const data = await getUserProfile()
    profileForm.value = data
    privacyForm.value = {
      showInLeaderboard: data.showInLeaderboard,
      allowCoachView: data.allowCoachView
    }
  } catch (error) {
    ElMessage.error('加载个人信息失败')
  }
}

const updateProfile = async () => {
  try {
    await updateUserProfile(profileForm.value)
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const changePassword = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.error('两次密码输入不一致')
    return
  }
  try {
    await changePasswordApi(passwordForm.value)
    ElMessage.success('密码修改成功')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (error) {
    ElMessage.error('密码修改失败')
  }
}

const updatePrivacy = async () => {
  try {
    await updatePrivacySettings(privacyForm.value)
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.settings-page {
  padding: 20px;
}
</style>

<template>
  <div class="coaches-page">
    <h2>教练管理</h2>

    <el-card style="margin-top: 20px">
      <el-form :inline="true">
        <el-form-item>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索教练姓名或用户名"
            clearable
            style="width: 300px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button type="success" @click="handleAdd">添加教练</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px">
      <el-table :data="coachList" border style="width: 100%" v-loading="loading">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" width="220" />
        <el-table-column prop="phone" label="电话" width="150" />
        <el-table-column prop="studentCount" label="学员数" width="120" align="center" />
        <el-table-column prop="genderLabel" label="性别" width="80" align="center" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" prop="password" :required="!isEdit">
          <el-input
            v-model="formData.password"
            type="password"
            :placeholder="isEdit ? '不修改请留空' : '请输入密码'"
            show-password
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="formData.age" :min="18" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio label="MALE">男</el-radio>
            <el-radio label="FEMALE">女</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminCoaches,
  createAdminCoach,
  updateAdminCoach,
  deleteAdminCoach
} from '@/api/admin'

const searchKeyword = ref('')
const loading = ref(false)
const coachList = ref([])
const allCoachList = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('添加教练')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const formData = reactive({
  userId: null,
  realName: '',
  username: '',
  password: '',
  email: '',
  phone: '',
  age: 28,
  gender: 'MALE'
})

const validatePassword = (rule, value, callback) => {
  if (!isEdit.value && (!value || value.length < 6)) {
    callback(new Error('新增教练时密码至少6位'))
    return
  }
  if (value && value.length < 6) {
    callback(new Error('密码至少6位'))
    return
  }
  callback()
}

const formRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3-20之间', trigger: 'blur' }
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
}

onMounted(() => {
  loadCoachList()
})

const toGenderLabel = (gender) => {
  const value = `${gender || ''}`.toUpperCase()
  if (value === 'FEMALE') {
    return '女'
  }
  return '男'
}

const normalizeCoach = (item) => ({
  userId: item.id,
  realName: item.realName || item.username,
  username: item.username,
  email: item.email || '-',
  phone: item.phone || '-',
  age: item.age || null,
  studentCount: item.studentCount ?? 0,
  gender: `${item.gender || 'MALE'}`.toUpperCase(),
  genderLabel: toGenderLabel(item.gender)
})

const loadCoachList = async () => {
  loading.value = true
  try {
    const data = await getAdminCoaches()
    allCoachList.value = (data || []).map(normalizeCoach)
    applyFilters()
  } catch (error) {
    ElMessage.error('加载教练列表失败')
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  const keyword = (searchKeyword.value || '').trim().toLowerCase()
  let filtered = [...allCoachList.value]

  if (keyword) {
    filtered = filtered.filter((item) =>
      (item.realName || '').toLowerCase().includes(keyword) ||
      (item.username || '').toLowerCase().includes(keyword) ||
      (item.email || '').toLowerCase().includes(keyword) ||
      (item.phone || '').toLowerCase().includes(keyword) ||
      `${item.userId}`.includes(keyword)
    )
  }

  total.value = filtered.length
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  coachList.value = filtered.slice(start, end)
}

const handleSearch = () => {
  currentPage.value = 1
  applyFilters()
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '添加教练'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑教练'
  Object.assign(formData, {
    userId: row.userId,
    realName: row.realName,
    username: row.username,
    password: '',
    email: row.email === '-' ? '' : row.email,
    phone: row.phone === '-' ? '' : row.phone,
    age: row.age || 28,
    gender: row.gender || 'MALE'
  })
  dialogVisible.value = true
}

const buildPayload = () => ({
  username: formData.username,
  password: formData.password || null,
  realName: formData.realName,
  email: formData.email,
  phone: formData.phone,
  age: formData.age,
  gender: formData.gender
})

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该教练吗？此操作不可恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })

    await deleteAdminCoach(row.userId)
    ElMessage.success('删除成功')
    await loadCoachList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    const payload = buildPayload()
    if (isEdit.value) {
      await updateAdminCoach(formData.userId, payload)
      ElMessage.success('更新成功')
    } else {
      await createAdminCoach(payload)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    await loadCoachList()
  } catch (error) {
    if (error !== false && error !== 'cancel') {
      ElMessage.error(error?.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleDialogClose = () => {
  resetForm()
}

const resetForm = () => {
  Object.assign(formData, {
    userId: null,
    realName: '',
    username: '',
    password: '',
    email: '',
    phone: '',
    age: 28,
    gender: 'MALE'
  })
  formRef.value?.clearValidate()
}

const handleSizeChange = () => {
  applyFilters()
}

const handleCurrentChange = () => {
  applyFilters()
}
</script>

<style scoped>
.coaches-page h2 {
  margin-bottom: 20px;
}
</style>


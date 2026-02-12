<template>
  <div class="students-management-page">
    <h2>学员管理</h2>

    <el-card style="margin-top: 20px">
      <el-form :inline="true">
        <el-form-item>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索学员姓名或用户名"
            clearable
            style="width: 300px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="健身目标">
          <el-select v-model="filterGoal" placeholder="全部" clearable style="width: 160px">
            <el-option label="减重" value="WEIGHT_LOSS" />
            <el-option label="减脂" value="FAT_LOSS" />
            <el-option label="增肌" value="MUSCLE_GAIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责教练">
          <el-select v-model="filterCoach" placeholder="全部" clearable style="width: 160px">
            <el-option
              v-for="coach in coachList"
              :key="coach.userId"
              :label="coach.realName"
              :value="coach.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button type="success" @click="handleAdd">添加学员</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px">
      <el-table :data="studentList" border style="width: 100%" v-loading="loading">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" width="220" />
        <el-table-column prop="age" label="年龄" width="80" align="center" />
        <el-table-column prop="genderLabel" label="性别" width="80" align="center" />
        <el-table-column prop="fitnessGoal" label="健身目标" width="120">
          <template #default="{ row }">
            <el-tag :type="getGoalTagType(row.fitnessGoal)">{{ getGoalLabel(row.fitnessGoal) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="coachName" label="负责教练" width="120" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="handleAssignCoach(row)">分配教练</el-button>
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
          <el-input-number v-model="formData.age" :min="10" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio label="MALE">男</el-radio>
            <el-radio label="FEMALE">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="健身目标" prop="fitnessGoal">
          <el-select v-model="formData.fitnessGoal" style="width: 100%">
            <el-option label="减重" value="WEIGHT_LOSS" />
            <el-option label="减脂" value="FAT_LOSS" />
            <el-option label="增肌" value="MUSCLE_GAIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责教练" prop="coachId">
          <el-select v-model="formData.coachId" placeholder="可不选" clearable style="width: 100%">
            <el-option
              v-for="coach in coachList"
              :key="coach.userId"
              :label="coach.realName"
              :value="coach.userId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" title="分配教练" width="400px">
      <el-form label-width="80px">
        <el-form-item label="学员">
          <span>{{ currentStudent?.realName }}</span>
        </el-form-item>
        <el-form-item label="教练">
          <el-select v-model="newCoachId" placeholder="请选择教练" style="width: 100%">
            <el-option
              v-for="coach in coachList"
              :key="coach.userId"
              :label="coach.realName"
              :value="coach.userId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmAssign" :loading="assigning">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminStudents,
  getAdminCoaches,
  createAdminStudent,
  updateAdminStudent,
  deleteAdminStudent,
  assignStudentCoach
} from '@/api/admin'

const searchKeyword = ref('')
const filterGoal = ref('')
const filterCoach = ref('')
const loading = ref(false)
const studentList = ref([])
const allStudentList = ref([])
const coachList = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('添加学员')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const assignDialogVisible = ref(false)
const currentStudent = ref(null)
const newCoachId = ref(null)
const assigning = ref(false)

const formData = reactive({
  userId: null,
  realName: '',
  username: '',
  password: '',
  email: '',
  phone: '',
  age: 25,
  gender: 'MALE',
  fitnessGoal: 'WEIGHT_LOSS',
  coachId: null
})

const validatePassword = (rule, value, callback) => {
  if (!isEdit.value && (!value || value.length < 6)) {
    callback(new Error('新增学员时密码至少6位'))
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
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  fitnessGoal: [{ required: true, message: '请选择健身目标', trigger: 'change' }]
}

onMounted(async () => {
  await loadCoachList()
  await loadStudentList()
})

const toGenderLabel = (gender) => {
  const value = `${gender || ''}`.toUpperCase()
  if (value === 'FEMALE') {
    return '女'
  }
  return '男'
}

const goalLabelMap = {
  WEIGHT_LOSS: '减重',
  FAT_LOSS: '减脂',
  MUSCLE_GAIN: '增肌'
}

const normalizeGoal = (goal) => {
  const raw = `${goal || ''}`.trim().toUpperCase()
  if (raw === 'BODY_SHAPING') {
    return 'FAT_LOSS'
  }
  if (raw === 'HEALTH') {
    return 'WEIGHT_LOSS'
  }
  return raw || 'WEIGHT_LOSS'
}

const normalizeStudent = (item) => ({
  userId: item.id,
  realName: item.realName || item.username,
  username: item.username,
  email: item.email || '-',
  phone: item.phone || '',
  age: item.age || 25,
  gender: `${item.gender || 'MALE'}`.toUpperCase(),
  genderLabel: toGenderLabel(item.gender),
  fitnessGoal: normalizeGoal(item.fitnessGoal),
  coachId: item.coachId || null,
  coachName: item.coachName || '未分配'
})

const loadCoachList = async () => {
  try {
    const data = await getAdminCoaches()
    coachList.value = (data || []).map((item) => ({
      userId: item.id,
      realName: item.realName || item.username
    }))
  } catch (error) {
    ElMessage.error('加载教练列表失败')
  }
}

const loadStudentList = async () => {
  loading.value = true
  try {
    const data = await getAdminStudents()
    allStudentList.value = (data || []).map(normalizeStudent)
    applyFilters()
  } catch (error) {
    ElMessage.error('加载学员列表失败')
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  const keyword = (searchKeyword.value || '').trim().toLowerCase()
  let filtered = [...allStudentList.value]

  if (keyword) {
    filtered = filtered.filter((item) =>
      (item.realName || '').toLowerCase().includes(keyword) ||
      (item.username || '').toLowerCase().includes(keyword) ||
      (item.email || '').toLowerCase().includes(keyword) ||
      `${item.userId}`.includes(keyword)
    )
  }

  if (filterGoal.value) {
    filtered = filtered.filter((item) => item.fitnessGoal === filterGoal.value)
  }

  if (filterCoach.value) {
    filtered = filtered.filter((item) => item.coachId === filterCoach.value)
  }

  total.value = filtered.length
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  studentList.value = filtered.slice(start, end)
}

const handleSearch = () => {
  currentPage.value = 1
  applyFilters()
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '添加学员'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑学员'
  Object.assign(formData, {
    userId: row.userId,
    realName: row.realName,
    username: row.username,
    password: '',
    email: row.email === '-' ? '' : row.email,
    phone: row.phone,
    age: row.age,
    gender: row.gender,
    fitnessGoal: row.fitnessGoal,
    coachId: row.coachId
  })
  dialogVisible.value = true
}

const handleAssignCoach = (row) => {
  currentStudent.value = row
  newCoachId.value = row.coachId
  assignDialogVisible.value = true
}

const handleConfirmAssign = async () => {
  if (!newCoachId.value) {
    ElMessage.warning('请选择教练')
    return
  }
  if (!currentStudent.value?.userId) {
    ElMessage.warning('学员信息无效')
    return
  }

  assigning.value = true
  try {
    await assignStudentCoach(currentStudent.value.userId, newCoachId.value)
    ElMessage.success('分配教练成功')
    assignDialogVisible.value = false
    await loadStudentList()
  } catch (error) {
    ElMessage.error(error?.message || '分配教练失败')
  } finally {
    assigning.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该学员吗？此操作不可恢复。', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })

    await deleteAdminStudent(row.userId)
    ElMessage.success('删除成功')
    await loadStudentList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const buildPayload = () => ({
  username: formData.username,
  password: formData.password || null,
  realName: formData.realName,
  email: formData.email,
  phone: formData.phone,
  age: formData.age,
  gender: formData.gender,
  fitnessGoal: formData.fitnessGoal,
  coachId: formData.coachId || null
})

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    const payload = buildPayload()
    if (isEdit.value) {
      await updateAdminStudent(formData.userId, payload)
      ElMessage.success('更新成功')
    } else {
      await createAdminStudent(payload)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    await loadStudentList()
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
    age: 25,
    gender: 'MALE',
    fitnessGoal: 'WEIGHT_LOSS',
    coachId: null
  })
  formRef.value?.clearValidate()
}

const handleSizeChange = () => {
  applyFilters()
}

const handleCurrentChange = () => {
  applyFilters()
}

const getGoalLabel = (goal) => goalLabelMap[goal] || goal

const getGoalTagType = (goal) => {
  const types = {
    WEIGHT_LOSS: 'danger',
    FAT_LOSS: 'warning',
    MUSCLE_GAIN: 'success'
  }
  return types[goal] || ''
}
</script>

<style scoped>
.students-management-page h2 {
  margin-bottom: 20px;
}
</style>


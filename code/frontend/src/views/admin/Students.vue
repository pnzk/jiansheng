<template>
  <div class="students-management-page">
    <h2>学员管理</h2>

    <!-- 搜索和操作区 -->
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
          <el-select v-model="filterGoal" placeholder="全部" clearable style="width: 150px">
            <el-option label="减重" value="weight_loss" />
            <el-option label="增肌" value="muscle_gain" />
            <el-option label="塑形" value="body_shaping" />
            <el-option label="保持健康" value="health" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责教练">
          <el-select v-model="filterCoach" placeholder="全部" clearable style="width: 150px">
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

    <!-- 学员列表 -->
    <el-card style="margin-top: 20px">
      <el-table :data="studentList" border style="width: 100%" v-loading="loading">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="age" label="年龄" width="80" align="center" />
        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template #default="{ row }">
            {{ row.gender === 'male' ? '男' : '女' }}
          </template>
        </el-table-column>
        <el-table-column prop="fitnessGoal" label="健身目标" width="120">
          <template #default="{ row }">
            <el-tag :type="getGoalTagType(row.fitnessGoal)">
              {{ getGoalLabel(row.fitnessGoal) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="coachName" label="负责教练" width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="handleAssignCoach(row)">分配教练</el-button>
            <el-button
              size="small"
              :type="row.status === 'active' ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 添加/编辑对话框 -->
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
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="formData.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入电话" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="formData.age" :min="10" :max="100" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio label="male">男</el-radio>
            <el-radio label="female">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="健身目标" prop="fitnessGoal">
          <el-select v-model="formData.fitnessGoal" placeholder="请选择">
            <el-option label="减重" value="weight_loss" />
            <el-option label="增肌" value="muscle_gain" />
            <el-option label="塑形" value="body_shaping" />
            <el-option label="保持健康" value="health" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责教练" prop="coachId">
          <el-select v-model="formData.coachId" placeholder="请选择教练">
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

    <!-- 分配教练对话框 -->
    <el-dialog v-model="assignDialogVisible" title="分配教练" width="400px">
      <el-form label-width="100px">
        <el-form-item label="学员姓名">
          <span>{{ currentStudent?.realName }}</span>
        </el-form-item>
        <el-form-item label="当前教练">
          <span>{{ currentStudent?.coachName || '未分配' }}</span>
        </el-form-item>
        <el-form-item label="新教练">
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

const searchKeyword = ref('')
const filterGoal = ref('')
const filterCoach = ref('')
const loading = ref(false)
const studentList = ref([])
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
  gender: 'male',
  fitnessGoal: 'weight_loss',
  coachId: null
})

const formRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
  fitnessGoal: [{ required: true, message: '请选择健身目标', trigger: 'change' }]
}

onMounted(() => {
  loadCoachList()
  loadStudentList()
})

const loadCoachList = () => {
  coachList.value = [
    { userId: 501, realName: '王教练' },
    { userId: 502, realName: '李教练' },
    { userId: 503, realName: '张教练' }
  ]
}

const loadStudentList = async () => {
  loading.value = true
  try {
    // 模拟数据
    studentList.value = [
      {
        userId: 1,
        realName: '张三',
        username: 'student001',
        email: 'student001@gym.com',
        age: 25,
        gender: 'male',
        fitnessGoal: 'weight_loss',
        coachId: 501,
        coachName: '王教练',
        status: 'active'
      },
      {
        userId: 2,
        realName: '李四',
        username: 'student002',
        email: 'student002@gym.com',
        age: 28,
        gender: 'female',
        fitnessGoal: 'muscle_gain',
        coachId: 502,
        coachName: '李教练',
        status: 'active'
      },
      {
        userId: 3,
        realName: '王五',
        username: 'student003',
        email: 'student003@gym.com',
        age: 30,
        gender: 'male',
        fitnessGoal: 'body_shaping',
        coachId: 503,
        coachName: '张教练',
        status: 'active'
      }
    ]
    total.value = studentList.value.length
  } catch (error) {
    ElMessage.error('加载学员列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadStudentList()
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
    email: row.email,
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
  
  assigning.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    
    const student = studentList.value.find(s => s.userId === currentStudent.value.userId)
    if (student) {
      student.coachId = newCoachId.value
      const coach = coachList.value.find(c => c.userId === newCoachId.value)
      student.coachName = coach?.realName || ''
    }
    
    ElMessage.success('分配教练成功')
    assignDialogVisible.value = false
  } catch (error) {
    ElMessage.error('分配教练失败')
  } finally {
    assigning.value = false
  }
}

const handleToggleStatus = async (row) => {
  const action = row.status === 'active' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该学员吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    row.status = row.status === 'active' ? 'inactive' : 'active'
    ElMessage.success(`${action}成功`)
  } catch {
    // 取消操作
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该学员吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    studentList.value = studentList.value.filter(s => s.userId !== row.userId)
    total.value--
    ElMessage.success('删除成功')
  } catch {
    // 取消操作
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    
    await new Promise(resolve => setTimeout(resolve, 500))
    
    if (isEdit.value) {
      const index = studentList.value.findIndex(s => s.userId === formData.userId)
      if (index !== -1) {
        const coach = coachList.value.find(c => c.userId === formData.coachId)
        studentList.value[index] = {
          ...studentList.value[index],
          ...formData,
          coachName: coach?.realName || ''
        }
      }
      ElMessage.success('更新成功')
    } else {
      const coach = coachList.value.find(c => c.userId === formData.coachId)
      studentList.value.push({
        ...formData,
        userId: Date.now(),
        coachName: coach?.realName || '',
        status: 'active'
      })
      total.value++
      ElMessage.success('添加成功')
    }
    
    dialogVisible.value = false
  } catch (error) {
    if (error !== false) {
      ElMessage.error('操作失败')
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
    gender: 'male',
    fitnessGoal: 'weight_loss',
    coachId: null
  })
  formRef.value?.clearValidate()
}

const handleSizeChange = () => {
  loadStudentList()
}

const handleCurrentChange = () => {
  loadStudentList()
}

const getGoalLabel = (goal) => {
  const labels = {
    weight_loss: '减重',
    muscle_gain: '增肌',
    body_shaping: '塑形',
    health: '保持健康'
  }
  return labels[goal] || goal
}

const getGoalTagType = (goal) => {
  const types = {
    weight_loss: 'danger',
    muscle_gain: 'success',
    body_shaping: 'warning',
    health: 'info'
  }
  return types[goal] || ''
}
</script>

<style scoped>
.students-management-page h2 {
  margin-bottom: 20px;
}
</style>

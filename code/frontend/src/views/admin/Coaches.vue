<template>
  <div class="coaches-page">
    <h2>教练管理</h2>

    <!-- 搜索和操作区 -->
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

    <!-- 教练列表 -->
    <el-card style="margin-top: 20px">
      <el-table :data="coachList" border style="width: 100%" v-loading="loading">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="phone" label="电话" width="150" />
        <el-table-column prop="studentCount" label="负责学员数" width="120" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
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
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio label="male">男</el-radio>
            <el-radio label="female">女</el-radio>
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

const searchKeyword = ref('')
const loading = ref(false)
const coachList = ref([])
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
  gender: 'male'
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
  ]
}

onMounted(() => {
  loadCoachList()
})

const loadCoachList = async () => {
  loading.value = true
  try {
    // 模拟数据
    coachList.value = [
      {
        userId: 501,
        realName: '王教练',
        username: 'coach001',
        email: 'coach001@gym.com',
        phone: '13800138001',
        studentCount: 25,
        status: 'active'
      },
      {
        userId: 502,
        realName: '李教练',
        username: 'coach002',
        email: 'coach002@gym.com',
        phone: '13800138002',
        studentCount: 30,
        status: 'active'
      },
      {
        userId: 503,
        realName: '张教练',
        username: 'coach003',
        email: 'coach003@gym.com',
        phone: '13800138003',
        studentCount: 20,
        status: 'active'
      }
    ]
    total.value = coachList.value.length
  } catch (error) {
    ElMessage.error('加载教练列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadCoachList()
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
    email: row.email,
    phone: row.phone,
    gender: row.gender || 'male'
  })
  dialogVisible.value = true
}

const handleToggleStatus = async (row) => {
  const action = row.status === 'active' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该教练吗？`, '提示', {
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
    await ElMessageBox.confirm('确定要删除该教练吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    coachList.value = coachList.value.filter(c => c.userId !== row.userId)
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
    
    // 模拟提交
    await new Promise(resolve => setTimeout(resolve, 500))
    
    if (isEdit.value) {
      const index = coachList.value.findIndex(c => c.userId === formData.userId)
      if (index !== -1) {
        coachList.value[index] = { ...coachList.value[index], ...formData }
      }
      ElMessage.success('更新成功')
    } else {
      coachList.value.push({
        ...formData,
        userId: Date.now(),
        studentCount: 0,
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
    gender: 'male'
  })
  formRef.value?.clearValidate()
}

const handleSizeChange = () => {
  loadCoachList()
}

const handleCurrentChange = () => {
  loadCoachList()
}
</script>

<style scoped>
.coaches-page h2 {
  margin-bottom: 20px;
}
</style>

<template>
  <div class="plans-page">
    <h2>训练计划管理</h2>

    <!-- 操作栏 -->
    <el-card style="margin-top: 20px">
      <el-row :gutter="20" align="middle">
        <el-col :span="5">
          <el-select v-model="filterStudent" placeholder="选择学员" clearable @change="loadPlans" style="width: 100%">
            <el-option label="全部学员" value="" />
            <el-option v-for="s in studentList" :key="s.id" :label="s.realName" :value="s.id" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filterGoal" placeholder="目标类型" clearable @change="loadPlans" style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="减重" value="WEIGHT_LOSS" />
            <el-option label="减脂" value="FAT_LOSS" />
            <el-option label="增肌" value="MUSCLE_GAIN" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filterStatus" placeholder="计划状态" clearable @change="loadPlans" style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="进行中" value="ACTIVE" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="未开始" value="PENDING" />
          </el-select>
        </el-col>
        <el-col :span="11" style="text-align: right">
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon> 添加新计划
          </el-button>
          <el-button @click="loadPlans">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 计划列表 -->
    <el-card style="margin-top: 20px">
      <el-table :data="plansList" style="width: 100%" stripe>
        <el-table-column prop="studentName" label="学员" width="100" />
        <el-table-column prop="planName" label="计划名称" width="150" />
        <el-table-column prop="goalType" label="目标类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getGoalTagType(row.goalType)" size="small">{{ getGoalText(row.goalType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetValue" label="目标值" width="100">
          <template #default="{ row }">
            {{ row.targetValue }} {{ row.goalType === 'MUSCLE_GAIN' ? 'kg' : 'kg' }}
          </template>
        </el-table-column>
        <el-table-column label="计划周期" width="180">
          <template #default="{ row }">
            {{ row.startDate }} ~ {{ row.endDate }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completionRate" label="完成率" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.completionRate" :stroke-width="8" :color="getProgressColor(row.completionRate)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="viewPlan(row)">
              <el-icon><View /></el-icon>
            </el-button>
            <el-button size="small" text type="warning" @click="editPlan(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button size="small" text type="info" @click="copyPlan(row)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
            <el-button size="small" text type="danger" @click="deletePlan(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑计划对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="planForm" label-width="100px">
        <el-form-item label="选择学员">
          <el-select v-model="planForm.studentId" placeholder="请选择学员">
            <el-option label="张三" value="1" />
            <el-option label="李四" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划名称">
          <el-input v-model="planForm.planName" placeholder="请输入计划名称" />
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="planForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item label="减重目标">
          <el-input-number v-model="planForm.weightGoal" :min="0" :max="50" :step="0.5" />
          <span style="margin-left: 10px">kg</span>
        </el-form-item>
        <el-form-item label="体脂目标">
          <el-input-number v-model="planForm.bodyFatGoal" :min="0" :max="50" :step="0.5" />
          <span style="margin-left: 10px">%</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePlan">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const filterStudent = ref('')
const filterGoal = ref('')
const filterStatus = ref('')
const plansList = ref([])
const studentList = ref([
  { id: 1, realName: '张三' },
  { id: 2, realName: '李四' },
  { id: 3, realName: '王五' },
  { id: 4, realName: '赵六' }
])
const dialogVisible = ref(false)
const dialogTitle = ref('创建训练计划')
const planForm = reactive({
  studentId: '',
  planName: '',
  goalType: 'WEIGHT_LOSS',
  targetValue: 5,
  dateRange: [],
  description: ''
})

const getGoalTagType = (type) => {
  const types = { 'WEIGHT_LOSS': 'danger', 'FAT_LOSS': 'warning', 'MUSCLE_GAIN': 'success' }
  return types[type] || 'info'
}

const getGoalText = (type) => {
  const texts = { 'WEIGHT_LOSS': '减重', 'FAT_LOSS': '减脂', 'MUSCLE_GAIN': '增肌' }
  return texts[type] || type
}

const getStatusTagType = (status) => {
  const types = { 'ACTIVE': 'success', 'COMPLETED': 'info', 'PENDING': 'warning' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 'ACTIVE': '进行中', 'COMPLETED': '已完成', 'PENDING': '未开始' }
  return texts[status] || status
}

const getProgressColor = (percentage) => {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 50) return '#e6a23c'
  return '#f56c6c'
}

onMounted(() => {
  loadPlans()
})

const loadPlans = async () => {
  try {
    plansList.value = [
      { planId: 1, studentName: '张三', planName: '减脂塑形计划', goalType: 'FAT_LOSS', targetValue: 5, startDate: '2025-01-01', endDate: '2025-03-31', completionRate: 65, status: 'ACTIVE' },
      { planId: 2, studentName: '李四', planName: '增肌训练计划', goalType: 'MUSCLE_GAIN', targetValue: 3, startDate: '2025-01-15', endDate: '2025-04-15', completionRate: 45, status: 'ACTIVE' },
      { planId: 3, studentName: '王五', planName: '减重计划', goalType: 'WEIGHT_LOSS', targetValue: 8, startDate: '2024-12-01', endDate: '2025-02-28', completionRate: 85, status: 'ACTIVE' },
      { planId: 4, studentName: '赵六', planName: '新手入门计划', goalType: 'WEIGHT_LOSS', targetValue: 5, startDate: '2025-02-01', endDate: '2025-04-30', completionRate: 0, status: 'PENDING' }
    ]
  } catch (error) {
    ElMessage.error('加载计划列表失败')
  }
}

const copyPlan = (row) => {
  dialogTitle.value = '复制训练计划'
  Object.assign(planForm, {
    studentId: '',
    planName: row.planName + ' (副本)',
    goalType: row.goalType,
    targetValue: row.targetValue,
    dateRange: [],
    description: ''
  })
  dialogVisible.value = true
}

const showCreateDialog = () => {
  dialogTitle.value = '创建训练计划'
  Object.assign(planForm, {
    studentId: '',
    planName: '',
    dateRange: [],
    weightGoal: 5,
    bodyFatGoal: 3
  })
  dialogVisible.value = true
}

const viewPlan = (row) => {
  ElMessage.info('查看计划详情')
}

const editPlan = (row) => {
  dialogTitle.value = '编辑训练计划'
  Object.assign(planForm, {
    planId: row.planId,
    studentId: row.studentId,
    planName: row.planName,
    dateRange: [row.startDate, row.endDate]
  })
  dialogVisible.value = true
}

const deletePlan = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这个训练计划吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // TODO: 调用API删除计划
    ElMessage.success('删除成功')
    loadPlans()
  } catch {
    // 用户取消
  }
}

const savePlan = async () => {
  try {
    // TODO: 调用API保存计划
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadPlans()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}
</script>

<style scoped>
.plans-page h2 {
  margin-bottom: 20px;
}
</style>

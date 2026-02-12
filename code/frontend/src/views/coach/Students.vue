<template>
  <div class="students-page">
    <div class="page-header">
      <div>
        <h2>学员管理</h2>
        <p class="page-subtitle">查看学员训练状态与近期数据，快速进入详情分析</p>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="8">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-label">学员总数</div>
          <div class="metric-value">{{ total }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-label">进行中</div>
          <div class="metric-value success">{{ activeCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-label">未开始</div>
          <div class="metric-value warning">{{ inactiveCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="filter-card" shadow="never">
      <el-row :gutter="16" align="middle">
        <el-col :span="10">
          <el-input
            v-model="searchQuery"
            placeholder="搜索学员姓名或用户名"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="5">
          <el-select v-model="filterGoal" placeholder="健身目标" clearable style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="减重" value="减重" />
            <el-option label="减脂" value="减脂" />
            <el-option label="增肌" value="增肌" />
            <el-option label="塑形" value="塑形" />
            <el-option label="保持" value="保持" />
          </el-select>
        </el-col>
        <el-col :span="5">
          <el-select v-model="filterStatus" placeholder="训练状态" clearable style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="进行中" value="active" />
            <el-option label="未开始" value="inactive" />
          </el-select>
        </el-col>
        <el-col :span="4" class="filter-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="reload">刷新</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table :data="studentsList" style="width: 100%" @row-click="viewStudentDetail">
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="age" label="年龄" width="80" align="center" />
        <el-table-column prop="gender" label="性别" width="80" align="center" />
        <el-table-column prop="fitnessGoal" label="健身目标" width="110" />
        <el-table-column prop="currentWeight" label="当前体重(kg)" width="130" align="center" />
        <el-table-column prop="planStatus" label="计划状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.planStatus === 'active'" type="success">进行中</el-tag>
            <el-tag v-else-if="row.planStatus === 'completed'" type="info">已完成</el-tag>
            <el-tag v-else type="warning">未开始</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="planProgress" label="完成率" width="170">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round(Number(row.planProgress || 0))"
              :stroke-width="8"
              :show-text="false"
            />
            <span class="progress-text">{{ Math.round(Number(row.planProgress || 0)) }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastExerciseDate" label="最后运动时间" min-width="180" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click.stop="viewStudentDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCoachStudents } from '@/api/user'

const router = useRouter()

const searchQuery = ref('')
const filterGoal = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const studentsList = ref([])
const allStudents = ref([])

const goalMap = {
  WEIGHT_LOSS: '减重',
  FAT_LOSS: '减脂',
  MUSCLE_GAIN: '增肌',
  BODY_SHAPING: '塑形',
  HEALTH: '保持'
}

const activeCount = computed(() => allStudents.value.filter((item) => item.planStatus === 'active').length)
const inactiveCount = computed(() => allStudents.value.filter((item) => item.planStatus !== 'active').length)

onMounted(() => {
  loadStudents()
})

const loadStudents = async () => {
  try {
    const data = await getCoachStudents()

    allStudents.value = (data || []).map((item) => ({
      userId: item.id,
      username: item.username,
      realName: item.realName || item.username,
      age: item.age || '-',
      gender: normalizeGender(item.gender),
      fitnessGoal: normalizeGoal(item.fitnessGoal),
      currentWeight: item.currentWeight != null ? item.currentWeight : '-',
      planStatus: normalizePlanStatus(item.trainingStatus),
      planProgress: item.planProgress != null ? Number(item.planProgress) : 0,
      lastExerciseDate: item.lastExerciseTime || '-'
    }))

    handleSearch()
  } catch (error) {
    ElMessage.error('加载学员列表失败')
  }
}

const normalizeGender = (gender) => {
  const value = `${gender || ''}`.toUpperCase()
  if (value === 'FEMALE' || gender === '女') {
    return '女'
  }
  return '男'
}

const normalizeGoal = (goal) => {
  if (!goal) {
    return '-'
  }
  const key = `${goal}`.trim().toUpperCase()
  return goalMap[key] || `${goal}`
}

const normalizePlanStatus = (status) => {
  const raw = `${status || ''}`.trim().toLowerCase()
  if (raw.includes('进行') || raw.includes('active') || raw === 'in_progress') {
    return 'active'
  }
  if (raw.includes('完成') || raw.includes('complete')) {
    return 'completed'
  }
  return 'inactive'
}

const applyFilters = () => {
  let filtered = [...allStudents.value]

  const keyword = (searchQuery.value || '').trim().toLowerCase()
  if (keyword) {
    filtered = filtered.filter((item) =>
      (item.realName || '').toLowerCase().includes(keyword) ||
      (item.username || '').toLowerCase().includes(keyword) ||
      `${item.userId}`.includes(keyword)
    )
  }

  if (filterGoal.value) {
    filtered = filtered.filter((item) => item.fitnessGoal === filterGoal.value)
  }

  if (filterStatus.value) {
    filtered = filtered.filter((item) => item.planStatus === filterStatus.value)
  }

  total.value = filtered.length
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  studentsList.value = filtered.slice(start, end)
}

const handleSearch = () => {
  currentPage.value = 1
  applyFilters()
}

const handlePageChange = () => {
  applyFilters()
}

const reload = async () => {
  await loadStudents()
}

const viewStudentDetail = (row) => {
  router.push(`/coach/students/${row.userId}`)
}
</script>

<style scoped>
.students-page {
  padding-bottom: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.students-page h2 {
  margin: 0;
  font-size: 34px;
  line-height: 1.2;
}

.page-subtitle {
  margin-top: 8px;
  color: #8c93a7;
  font-size: 14px;
}

.stats-row {
  margin-bottom: 16px;
}

.metric-card {
  border-radius: 12px;
}

.metric-label {
  color: #8c93a7;
  font-size: 13px;
}

.metric-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.metric-value.success {
  color: #67c23a;
}

.metric-value.warning {
  color: #e6a23c;
}

.filter-card,
.table-card {
  border-radius: 12px;
  margin-bottom: 16px;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.progress-text {
  margin-left: 8px;
  color: #606266;
  font-size: 12px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.el-table {
  cursor: pointer;
}

:deep(.el-table__header th) {
  background: #f7f9fc;
  color: #60697a;
}

:deep(.el-table__row:hover > td) {
  background-color: #f5f9ff !important;
}
</style>


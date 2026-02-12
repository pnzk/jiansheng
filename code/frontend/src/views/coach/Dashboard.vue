<template>
  <div class="dashboard">
    <h2>学员总览仪表盘</h2>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="6">
        <el-card class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #409eff, #66b1ff)">
              <el-icon size="30"><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalStudents }}</div>
              <div class="stat-label">学员总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-icon male" style="background: linear-gradient(135deg, #409eff, #79bbff)">
              <el-icon size="30"><Male /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.maleStudents }}</div>
              <div class="stat-label">男生学员</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-icon female" style="background: linear-gradient(135deg, #f56c6c, #fab6b6)">
              <el-icon size="30"><Female /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.femaleStudents }}</div>
              <div class="stat-label">女生学员</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #e6a23c, #f0c78a)">
              <el-icon size="30"><Calendar /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.avgAge }}</div>
              <div class="stat-label">平均年龄</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 健身目标分析 + 待办事项 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>🎯 学员健身目标分析</span>
          </template>
          <div ref="goalChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="todo-card">
          <template #header>
            <div class="todo-header">
              <span>📋 待办事项提醒</span>
              <el-badge :value="todoList.length" class="todo-badge" />
            </div>
          </template>
          <div class="todo-list">
            <div 
              v-for="item in todoList" 
              :key="item.id" 
              class="todo-item"
              :class="item.priority"
            >
              <div class="todo-icon">
                <el-icon v-if="item.priority === 'high'" color="#f56c6c"><WarningFilled /></el-icon>
                <el-icon v-else-if="item.priority === 'medium'" color="#e6a23c"><Bell /></el-icon>
                <el-icon v-else color="#409eff"><InfoFilled /></el-icon>
              </div>
              <div class="todo-content">
                <div class="todo-title">{{ item.title }}</div>
                <div class="todo-desc">{{ item.description }}</div>
              </div>
              <el-button
                size="small"
                type="primary"
                text
                :disabled="processingTodoIds.has(item.id)"
                @click="handleTodo(item)"
              >
                {{ processingTodoIds.has(item.id) ? '处理中...' : '处理' }}
              </el-button>
            </div>
            <el-empty v-if="!todoList.length" description="暂无待办事项" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>📈 学员体重变化趋势</span>
          </template>
          <div ref="weightChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>🏃 运动类型分布</span>
          </template>
          <div ref="exerciseChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 学员列表 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>最近活跃学员</span>
          </template>
          <el-table :data="activeStudentsList" style="width: 100%">
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="lastExerciseDate" label="最后运动时间" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" @click="viewStudent(row.userId)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>需要关注的学员</span>
          </template>
          <el-table :data="attentionStudentsList" style="width: 100%">
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="reason" label="原因" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" type="warning" @click="viewStudent(row.userId)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getCoachDashboard } from '@/api/analytics'
import { getCoachStudents, handleCoachTodo } from '@/api/user'
import { CHART_COLORS, initChart } from '@/utils/chartTheme'

const router = useRouter()
const weightChartRef = ref(null)
const exerciseChartRef = ref(null)
const goalChartRef = ref(null)

const stats = reactive({
  totalStudents: 0,
  maleStudents: 0,
  femaleStudents: 0,
  avgAge: 0,
  activeStudents: 0
})

const dashboardData = ref(null)

const todoList = ref([])

const activeStudentsList = ref([])
const attentionStudentsList = ref([])
const processingTodoIds = ref(new Set())
const inactiveThresholdDays = 7

const normalizePlanStatus = (status) => {
  const raw = `${status || ''}`.trim().toLowerCase()
  if (raw.includes('active') || raw.includes('进行') || raw === 'in_progress') {
    return 'active'
  }
  if (raw.includes('complete') || raw.includes('完成')) {
    return 'completed'
  }
  return 'inactive'
}

const toDateValue = (value) => {
  if (!value) {
    return null
  }
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

const formatDateTime = (value) => {
  const date = toDateValue(value)
  if (!date) {
    return '-'
  }
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')} ${`${date.getHours()}`.padStart(2, '0')}:${`${date.getMinutes()}`.padStart(2, '0')}`
}

const getInactiveDays = (value) => {
  const date = toDateValue(value)
  if (!date) {
    return null
  }
  const diff = Date.now() - date.getTime()
  return Math.max(0, Math.floor(diff / (24 * 60 * 60 * 1000)))
}

const handleTodo = async (item) => {
  if (processingTodoIds.value.has(item.id)) {
    return
  }

  processingTodoIds.value.add(item.id)

  try {
    await handleCoachTodo({
      studentId: item.userId,
      todoKey: item.todoKey,
      todoTitle: item.title,
      todoDescription: item.description
    })

    todoList.value = todoList.value.filter((todo) => todo.id !== item.id)
    ElMessage.success(`已处理: ${item.title}`)

    if (item.route) {
      await router.push(item.route)
    }
  } finally {
    processingTodoIds.value.delete(item.id)
  }
}

onMounted(async () => {
  await loadData()
  initGoalChart()
  initWeightChart()
  initExerciseChart()
})

const loadData = async () => {
  try {
    const [coachDashboard, coachStudents] = await Promise.all([
      getCoachDashboard(),
      getCoachStudents()
    ])

    dashboardData.value = coachDashboard || {}
    stats.totalStudents = Number(coachDashboard?.totalStudents || 0)
    stats.activeStudents = Number(coachDashboard?.activeStudents || 0)
    const students = Array.isArray(coachStudents) ? coachStudents : []

    const maleFromApi = Number(coachDashboard?.maleStudents || 0)
    stats.maleStudents = maleFromApi > 0 ? maleFromApi : students.filter((item) => {
      const gender = `${item.gender || ''}`.toUpperCase()
      return gender === 'MALE'
    }).length
    const femaleFromApi = Number(coachDashboard?.femaleStudents || 0)
    stats.femaleStudents = femaleFromApi > 0 ? femaleFromApi : students.filter((item) => {
      const gender = `${item.gender || ''}`.toUpperCase()
      return gender === 'FEMALE'
    }).length

    const avgAgeFromApi = Number(coachDashboard?.avgAge || 0)
    if (Number.isFinite(avgAgeFromApi) && avgAgeFromApi > 0) {
      stats.avgAge = Math.round(avgAgeFromApi)
    } else {
      const ageValues = students
        .map((item) => Number(item.age))
        .filter((value) => Number.isFinite(value) && value > 0)
      stats.avgAge = ageValues.length
        ? Math.round(ageValues.reduce((sum, value) => sum + value, 0) / ageValues.length)
        : 0
    }

    activeStudentsList.value = students
      .filter((item) => item.lastExerciseTime)
      .sort((left, right) => {
        const leftTs = toDateValue(left.lastExerciseTime)?.getTime() || 0
        const rightTs = toDateValue(right.lastExerciseTime)?.getTime() || 0
        return rightTs - leftTs
      })
      .slice(0, 5)
      .map((item) => ({
        userId: item.id,
        realName: item.realName || item.username,
        lastExerciseDate: formatDateTime(item.lastExerciseTime)
      }))

    attentionStudentsList.value = students
      .map((item) => {
        const status = normalizePlanStatus(item.trainingStatus)
        const inactiveDays = getInactiveDays(item.lastExerciseTime)

        if (inactiveDays == null || inactiveDays >= inactiveThresholdDays) {
          return {
            userId: item.id,
            realName: item.realName || item.username,
            reason: inactiveDays == null ? '暂无运动记录' : `${inactiveDays}天未运动`
          }
        }

        if (status === 'inactive') {
          return {
            userId: item.id,
            realName: item.realName || item.username,
            reason: '暂无进行中训练计划'
          }
        }

        const progress = Number(item.planProgress)
        if (Number.isFinite(progress) && progress > 0 && progress < 30) {
          return {
            userId: item.id,
            realName: item.realName || item.username,
            reason: `计划完成率偏低（${Math.round(progress)}%）`
          }
        }

        return null
      })
      .filter(Boolean)
      .slice(0, 5)

    todoList.value = attentionStudentsList.value.map((item, index) => ({
      id: `todo-${item.userId}-${index}`,
      todoKey: item.reason,
      userId: item.userId,
      title: `${item.realName}需要跟进`,
      description: item.reason,
      priority: item.reason.includes('未运动') || item.reason.includes('暂无运动记录') ? 'high' : 'medium',
      route: '/coach/students'
    }))
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const initGoalChart = () => {
  if (!goalChartRef.value) return
  const chart = initChart(goalChartRef.value)

  const goalMap = dashboardData.value?.goalDistribution || {}
  const goalLabels = {
    WEIGHT_LOSS: '减重',
    FAT_LOSS: '减脂',
    MUSCLE_GAIN: '增肌'
  }
  const colors = {
    WEIGHT_LOSS: '#f56c6c',
    FAT_LOSS: '#e6a23c',
    MUSCLE_GAIN: '#67c23a'
  }
  const data = ['WEIGHT_LOSS', 'FAT_LOSS', 'MUSCLE_GAIN'].map((key) => ({
    value: Number(goalMap[key] || 0),
    name: goalLabels[key],
    itemStyle: { color: colors[key] }
  }))

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['60%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}人' },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data
    }]
  })
}

const initWeightChart = () => {
  if (!weightChartRef.value) return
  const chart = initChart(weightChartRef.value)

  const trend = Array.isArray(dashboardData.value?.weightTrend)
    ? dashboardData.value.weightTrend
    : []
  const dates = trend.map((item) => item.date)
  const values = trend.map((item) => Number(item.avgWeight || 0))

  const safeMin = values.length ? Math.max(Math.floor(Math.min(...values) - 3), 30) : 40
  const safeMax = values.length ? Math.ceil(Math.max(...values) + 3) : 100

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['平均体重'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: dates
    },
    yAxis: { type: 'value', name: '体重(kg)', min: safeMin, max: safeMax },
    series: [
      {
        name: '平均体重',
        data: values,
        type: 'line',
        smooth: true,
        itemStyle: { color: CHART_COLORS[0] },
        areaStyle: { opacity: 0.2 }
      }
    ]
  })
}

const initExerciseChart = () => {
  if (!exerciseChartRef.value) return
  const chart = initChart(exerciseChartRef.value)

  const distribution = dashboardData.value?.exerciseTypeDistribution || {}
  const data = Object.entries(distribution).map(([name, value]) => ({
    name,
    value: Number(value || 0)
  }))

  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '60%',
      data,
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  })
}

const viewStudent = (userId) => {
  router.push(`/coach/students/${userId}`)
}
</script>

<style scoped>
.dashboard h2 {
  margin-bottom: 20px;
  color: #303133;
}

.stat-card-wrapper {
  transition: all 0.3s ease;
}

.stat-card-wrapper:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 65px;
  height: 65px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-right: 15px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.todo-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.todo-list {
  max-height: 240px;
  overflow-y: auto;
}

.todo-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 10px;
  background: #f5f7fa;
  transition: all 0.2s ease;
}

.todo-item:hover {
  background: #e4e7ed;
}

.todo-item.high {
  border-left: 3px solid #f56c6c;
}

.todo-item.medium {
  border-left: 3px solid #e6a23c;
}

.todo-item.low {
  border-left: 3px solid #409eff;
}

.todo-icon {
  margin-right: 12px;
  font-size: 20px;
}

.todo-content {
  flex: 1;
}

.todo-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.todo-desc {
  font-size: 12px;
  color: #909399;
}
</style>

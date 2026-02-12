<template>
  <div class="mobile-dashboard" v-loading="loading">
    <!-- 顶部状态栏 -->
    <div class="status-bar">
      <span class="time">{{ currentTime }}</span>
      <div class="status-icons">
        <span>📶</span>
        <span>🔋</span>
      </div>
    </div>

    <!-- 教练头部 -->
    <div class="coach-header">
      <div class="coach-avatar">
        <el-avatar :size="60" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
        <div class="coach-badge">教练</div>
      </div>
      <div class="coach-info">
        <div class="coach-name">{{ coachName }}</div>
        <div class="coach-title">高级健身教练</div>
      </div>
      <div class="header-actions">
        <el-badge :value="todoList.length" :max="99">
          <el-icon size="24"><Bell /></el-icon>
        </el-badge>
      </div>
    </div>

    <!-- 学员概览 -->
    <div class="stats-overview">
      <div class="stat-item">
        <div class="stat-value">{{ stats.totalStudents }}</div>
        <div class="stat-label">学员总数</div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-value active">{{ stats.activeStudents }}</div>
        <div class="stat-label">本周活跃</div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-value warning">{{ stats.needAttention }}</div>
        <div class="stat-label">需关注</div>
      </div>
    </div>

    <!-- 今日待办 -->
    <div class="section">
      <div class="section-header">
        <span class="section-title">📋 今日待办</span>
        <el-badge :value="todoList.length" type="danger" />
      </div>
      <div class="todo-list">
        <div
          class="todo-item"
          v-for="item in todoList"
          :key="item.id"
          :class="item.priority"
          @click="handleTodoClick(item)"
        >
          <div class="todo-priority"></div>
          <div class="todo-content">
            <div class="todo-title">{{ item.title }}</div>
            <div class="todo-desc">{{ item.description }}</div>
          </div>
          <el-icon class="todo-action"><ArrowRight /></el-icon>
        </div>
        <el-empty v-if="!todoList.length" description="暂无待办事项" :image-size="50" />
      </div>
    </div>

    <!-- 学员健身目标分布 -->
    <div class="section">
      <div class="section-header">
        <span class="section-title">🎯 学员目标分布</span>
      </div>
      <div class="goal-distribution">
        <div
          class="goal-item"
          v-for="item in goalDistribution"
          :key="item.name"
        >
          <div class="goal-bar" :style="{ '--color': item.color, '--width': item.width }"></div>
          <div class="goal-info">
            <span class="goal-name">{{ item.name }}</span>
            <span class="goal-count">{{ item.count }}人</span>
          </div>
        </div>
        <el-empty v-if="!goalDistribution.length" description="暂无学员目标数据" :image-size="50" />
      </div>
    </div>

    <!-- 最近活跃学员 -->
    <div class="section">
      <div class="section-header">
        <span class="section-title">🏃 最近活跃</span>
        <span class="section-more" @click="router.push('/coach/students')">全部 ></span>
      </div>
      <div class="student-list">
        <div class="student-item" v-for="student in activeStudents" :key="student.id">
          <el-avatar :size="40">{{ student.name.charAt(0) }}</el-avatar>
          <div class="student-info">
            <div class="student-name">{{ student.name }}</div>
            <div class="student-activity">{{ student.lastActivity }}</div>
          </div>
          <el-tag size="small" :type="student.tagType">
            {{ student.status }}
          </el-tag>
        </div>
        <el-empty v-if="!activeStudents.length" description="暂无运动记录" :image-size="50" />
      </div>
    </div>

    <!-- 需要关注的学员 -->
    <div class="section warning-section">
      <div class="section-header">
        <span class="section-title">⚠️ 需要关注</span>
      </div>
      <div class="attention-list">
        <div class="attention-item" v-for="student in attentionStudents" :key="student.id">
          <el-avatar :size="36">{{ student.name.charAt(0) }}</el-avatar>
          <div class="attention-info">
            <div class="attention-name">{{ student.name }}</div>
            <div class="attention-reason">{{ student.reason }}</div>
          </div>
          <el-button size="small" type="primary" round @click="viewStudent(student.id)">查看</el-button>
        </div>
        <el-empty v-if="!attentionStudents.length" description="暂无重点关注学员" :image-size="50" />
      </div>
    </div>

    <!-- 本周数据统计 -->
    <div class="section">
      <div class="section-header">
        <span class="section-title">📊 本周统计</span>
      </div>
      <div class="week-summary">
        <div class="summary-item">
          <div class="summary-icon">⏱️</div>
          <div class="summary-value">{{ stats.weekDuration }}</div>
          <div class="summary-label">总运动时长(分)</div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">🔥</div>
          <div class="summary-value">{{ stats.weekCalories }}</div>
          <div class="summary-label">总消耗(卡)</div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">📝</div>
          <div class="summary-value">{{ stats.plansCreated }}</div>
          <div class="summary-label">新建计划</div>
        </div>
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <div class="nav-item active">
        <el-icon size="24"><HomeFilled /></el-icon>
        <span>首页</span>
      </div>
      <div class="nav-item" @click="$router.push('/coach/students')">
        <el-icon size="24"><User /></el-icon>
        <span>学员</span>
      </div>
      <div class="nav-item add-btn" @click="$router.push('/coach/plans')">
        <el-icon size="28"><Plus /></el-icon>
      </div>
      <div class="nav-item" @click="$router.push('/coach/reports')">
        <el-icon size="24"><DataAnalysis /></el-icon>
        <span>报告</span>
      </div>
      <div class="nav-item" @click="$router.push('/coach/settings')">
        <el-icon size="24"><Setting /></el-icon>
        <span>设置</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, HomeFilled, User, Plus, DataAnalysis, Setting, ArrowRight } from '@element-plus/icons-vue'
import { getCoachStudentReport } from '@/api/analytics'
import { getCoachStudents, handleCoachTodo } from '@/api/user'
import { getCoachTrainingPlans } from '@/api/trainingPlan'

const router = useRouter()

const goalMap = {
  WEIGHT_LOSS: '减重',
  FAT_LOSS: '减脂',
  MUSCLE_GAIN: '增肌',
  BODY_SHAPING: '塑形',
  HEALTH: '保持'
}

const goalColors = ['#f56c6c', '#e6a23c', '#67c23a', '#409eff', '#909399']

const loading = ref(false)
const currentTime = ref(formatClock(new Date()))
const coachName = ref(localStorage.getItem('realName') || '教练')

const stats = reactive({
  totalStudents: 0,
  activeStudents: 0,
  needAttention: 0,
  weekDuration: 0,
  weekCalories: 0,
  plansCreated: 0
})

const goalDistribution = ref([])
const todoList = ref([])
const activeStudents = ref([])
const attentionStudents = ref([])

let timerId = null

const loadData = async () => {
  loading.value = true
  try {
    const [students, plans] = await Promise.all([
      getCoachStudents(),
      getCoachTrainingPlans()
    ])

    const normalizedStudents = (students || []).map((item) => normalizeStudent(item))
    const today = new Date()
    const weekStart = getWeekStart(today)

    stats.totalStudents = normalizedStudents.length
    stats.activeStudents = normalizedStudents.filter((item) => item.daysSinceLastExercise != null && item.daysSinceLastExercise <= 7).length

    const weeklyReport = await loadWeeklyReport(normalizedStudents, weekStart, today)
    stats.weekDuration = weeklyReport.reduce((sum, item) => sum + Number(item.totalDuration || 0), 0)
    stats.weekCalories = Math.round(weeklyReport.reduce((sum, item) => sum + Number(item.totalCalories || 0), 0))
    stats.plansCreated = countPlansCreatedThisWeek(plans || [], weekStart, today)

    goalDistribution.value = buildGoalDistribution(normalizedStudents)
    activeStudents.value = buildActiveStudents(normalizedStudents)
    attentionStudents.value = buildAttentionStudents(normalizedStudents)
    stats.needAttention = attentionStudents.value.length
    todoList.value = buildTodoList(attentionStudents.value)
  } catch (error) {
    ElMessage.error('加载移动端看板数据失败')
  } finally {
    loading.value = false
  }
}

const loadWeeklyReport = async (students, startDate, endDate) => {
  if (!students.length) {
    return []
  }

  const data = await getCoachStudentReport({
    studentIds: students.map((item) => item.id).join(','),
    startDate: formatDate(startDate),
    endDate: formatDate(endDate)
  })

  return Array.isArray(data) ? data : []
}

const normalizeStudent = (item) => {
  const lastExerciseDate = parseDate(item.lastExerciseTime)
  const numericProgress = Number(item.planProgress || 0)

  return {
    id: item.id,
    name: item.realName || item.username || `学员${item.id}`,
    goal: normalizeGoal(item.fitnessGoal),
    trainingStatus: normalizePlanStatus(item.trainingStatus),
    progress: Number.isFinite(numericProgress) ? numericProgress : 0,
    lastExerciseDate,
    daysSinceLastExercise: getDaysDiff(lastExerciseDate, new Date())
  }
}

const normalizeGoal = (goal) => {
  if (!goal) {
    return '未设置'
  }
  const key = `${goal}`.trim().toUpperCase()
  return goalMap[key] || `${goal}`
}

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

const buildGoalDistribution = (students) => {
  if (!students.length) {
    return []
  }

  const goalCounter = students.reduce((acc, item) => {
    const key = item.goal || '未设置'
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, {})

  const total = students.length
  return Object.entries(goalCounter)
    .sort((a, b) => b[1] - a[1])
    .map(([name, count], index) => ({
      name,
      count,
      color: goalColors[index % goalColors.length],
      width: `${Math.max(Math.round((count / total) * 100), 10)}%`
    }))
}

const buildActiveStudents = (students) => {
  return students
    .filter((item) => item.lastExerciseDate)
    .sort((a, b) => b.lastExerciseDate - a.lastExerciseDate)
    .slice(0, 5)
    .map((item) => {
      const days = item.daysSinceLastExercise
      let status = '待跟进'
      let tagType = 'info'

      if (days != null && days <= 1) {
        status = '活跃'
        tagType = 'success'
      } else if (days != null && days <= 3) {
        status = '近期'
        tagType = 'warning'
      }

      return {
        id: item.id,
        name: item.name,
        lastActivity: `最近一次运动：${formatRelativeDay(days)}`,
        status,
        tagType
      }
    })
}

const buildAttentionStudents = (students) => {
  return students
    .map((item) => {
      if (item.daysSinceLastExercise == null) {
        return {
          id: item.id,
          name: item.name,
          reason: '暂无运动记录'
        }
      }

      if (item.daysSinceLastExercise > 7) {
        return {
          id: item.id,
          name: item.name,
          reason: `连续${item.daysSinceLastExercise}天未运动`
        }
      }

      if (item.trainingStatus === 'inactive') {
        return {
          id: item.id,
          name: item.name,
          reason: '暂无进行中训练计划'
        }
      }

      if (item.progress > 0 && item.progress < 30) {
        return {
          id: item.id,
          name: item.name,
          reason: `计划完成率偏低（${Math.round(item.progress)}%）`
        }
      }

      return null
    })
    .filter(Boolean)
    .slice(0, 6)
}

const buildTodoList = (attentionList) => {
  return attentionList.map((item, index) => ({
    id: `todo-${item.id}-${index}`,
    todoKey: item.reason,
    title: `${item.name}需要跟进`,
    description: item.reason,
    priority: getTodoPriority(item.reason),
    userId: item.id
  }))
}

const getTodoPriority = (reason) => {
  if (reason.includes('未运动') || reason.includes('暂无运动记录')) {
    return 'high'
  }
  if (reason.includes('暂无进行中训练计划')) {
    return 'medium'
  }
  return 'low'
}

const countPlansCreatedThisWeek = (plans, startDate, endDate) => {
  return plans.filter((plan) => {
    const createdAt = parseDate(plan.createdAt)
    return createdAt && createdAt >= startDate && createdAt <= endDate
  }).length
}

const handleTodoClick = async (item) => {
  try {
    await handleCoachTodo({
      studentId: item.userId,
      todoKey: item.todoKey,
      todoTitle: item.title,
      todoDescription: item.description
    })
    todoList.value = todoList.value.filter((todo) => todo.id !== item.id)
    ElMessage.success(`已处理: ${item.title}`)
  } catch (error) {
    ElMessage.error(error?.message || '处理待办失败')
  }

  if (item.userId) {
    router.push(`/coach/students/${item.userId}`)
    return
  }
  router.push('/coach/students')
}

const viewStudent = (studentId) => {
  router.push(`/coach/students/${studentId}`)
}

const getWeekStart = (date) => {
  const d = new Date(date)
  const day = d.getDay()
  const diff = day === 0 ? 6 : day - 1
  d.setDate(d.getDate() - diff)
  d.setHours(0, 0, 0, 0)
  return d
}

const getDaysDiff = (date, reference) => {
  if (!date) {
    return null
  }
  const target = new Date(date)
  target.setHours(0, 0, 0, 0)
  const current = new Date(reference)
  current.setHours(0, 0, 0, 0)
  return Math.max(Math.round((current - target) / (24 * 60 * 60 * 1000)), 0)
}

const formatRelativeDay = (days) => {
  if (days == null) {
    return '暂无记录'
  }
  if (days === 0) {
    return '今天'
  }
  if (days === 1) {
    return '1天前'
  }
  return `${days}天前`
}

const formatDate = (value) => {
  const date = new Date(value)
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatClock = (date) => {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const parseDate = (value) => {
  if (!value) {
    return null
  }
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return null
  }
  return date
}

onMounted(() => {
  loadData()
  timerId = setInterval(() => {
    currentTime.value = formatClock(new Date())
  }, 1000)
})

onUnmounted(() => {
  if (timerId) {
    clearInterval(timerId)
  }
})
</script>

<style scoped>
.mobile-dashboard {
  background: #f5f5f5;
  min-height: 100%;
  padding-bottom: 80px;
}

.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 20px;
  background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
  color: white;
  font-size: 14px;
}

.status-icons {
  display: flex;
  gap: 8px;
}

.coach-header {
  display: flex;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
  color: white;
}

.coach-avatar {
  position: relative;
}

.coach-badge {
  position: absolute;
  bottom: -5px;
  left: 50%;
  transform: translateX(-50%);
  background: #e6a23c;
  color: white;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
}

.coach-info {
  flex: 1;
  margin-left: 15px;
}

.coach-name {
  font-size: 20px;
  font-weight: bold;
}

.coach-title {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 4px;
}

.header-actions {
  cursor: pointer;
}

.stats-overview {
  display: flex;
  justify-content: space-around;
  align-items: center;
  background: white;
  margin: -20px 15px 15px;
  padding: 20px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-value.active {
  color: #67c23a;
}

.stat-value.warning {
  color: #f56c6c;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: #eee;
}

.section {
  margin: 15px;
  background: white;
  border-radius: 16px;
  padding: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.section-more {
  font-size: 12px;
  color: #909399;
  cursor: pointer;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 12px;
  cursor: pointer;
}

.todo-priority {
  width: 4px;
  height: 40px;
  border-radius: 2px;
  margin-right: 12px;
}

.todo-item.high .todo-priority {
  background: #f56c6c;
}

.todo-item.medium .todo-priority {
  background: #e6a23c;
}

.todo-item.low .todo-priority {
  background: #67c23a;
}

.todo-content {
  flex: 1;
}

.todo-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.todo-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.todo-action {
  color: #c0c4cc;
}

.goal-distribution {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.goal-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.goal-bar {
  height: 8px;
  background: var(--color);
  border-radius: 4px;
  width: var(--width);
  transition: width 0.5s ease;
}

.goal-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.goal-name {
  color: #606266;
}

.goal-count {
  color: #909399;
}

.student-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.student-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.student-info {
  flex: 1;
}

.student-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.student-activity {
  font-size: 12px;
  color: #909399;
}

.warning-section {
  background: linear-gradient(135deg, #fff5f5 0%, #fff 100%);
}

.attention-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.attention-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.attention-info {
  flex: 1;
}

.attention-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.attention-reason {
  font-size: 12px;
  color: #f56c6c;
}

.week-summary {
  display: flex;
  justify-content: space-around;
}

.summary-item {
  text-align: center;
}

.summary-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}

.summary-label {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  display: flex;
  justify-content: space-around;
  align-items: center;
  background: white;
  padding: 10px 0 25px;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.1);
  border-radius: 20px 20px 0 0;
  z-index: 100;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 11px;
  cursor: pointer;
}

.nav-item.active {
  color: #3498db;
}

.nav-item.add-btn {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
  border-radius: 50%;
  color: white;
  justify-content: center;
  margin-top: -25px;
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.4);
}
</style>

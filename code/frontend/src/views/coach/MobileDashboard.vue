<template>
  <div class="mobile-dashboard" v-loading="loading">
    <div class="status-bar">
      <span class="time">{{ currentTime }}</span>
      <div class="status-icons">
        <span>5G</span>
        <span>100%</span>
      </div>
    </div>

    <div class="coach-header">
      <div class="coach-avatar">
        <el-avatar :size="60">{{ coachName.slice(0, 1) }}</el-avatar>
        <div class="coach-badge">教练</div>
      </div>
      <div class="coach-info">
        <div class="coach-name">{{ coachName }}</div>
        <div class="coach-title">学员训练管理</div>
      </div>
      <div class="header-actions">
        <el-badge :value="todoList.length" :max="99">
          <el-icon size="24"><Bell /></el-icon>
        </el-badge>
      </div>
    </div>

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

    <div class="section">
      <div class="section-header">
        <span class="section-title">今日待办</span>
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

    <div class="section">
      <div class="section-header">
        <span class="section-title">学员目标分布</span>
      </div>
      <div class="goal-distribution">
        <div class="goal-item" v-for="item in goalDistribution" :key="item.name">
          <div class="goal-bar" :style="{ '--color': item.color, '--width': item.width }"></div>
          <div class="goal-info">
            <span class="goal-name">{{ item.name }}</span>
            <span class="goal-count">{{ item.count }}人</span>
          </div>
        </div>
        <el-empty v-if="!goalDistribution.length" description="暂无学员目标数据" :image-size="50" />
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <span class="section-title">最近活跃</span>
        <span class="section-more" @click="router.push('/coach/students')">全部 ></span>
      </div>
      <div class="student-list">
        <div class="student-item" v-for="student in activeStudents" :key="student.id">
          <el-avatar :size="40">{{ student.name.charAt(0) }}</el-avatar>
          <div class="student-info">
            <div class="student-name">{{ student.name }}</div>
            <div class="student-activity">{{ student.lastActivity }}</div>
          </div>
          <el-tag size="small" :type="student.tagType">{{ student.status }}</el-tag>
        </div>
        <el-empty v-if="!activeStudents.length" description="暂无运动记录" :image-size="50" />
      </div>
    </div>

    <div class="section warning-section">
      <div class="section-header">
        <span class="section-title">需要关注</span>
      </div>
      <div class="attention-list">
        <div class="attention-item" v-for="student in attentionStudents" :key="`${student.id}-${student.reason}`">
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
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Bell, DataAnalysis, HomeFilled, Plus, User } from '@element-plus/icons-vue'
import { getCoachStudents, getHandledCoachTodos, handleCoachTodo } from '@/api/user'

const router = useRouter()
const loading = ref(false)
const currentTime = ref(formatClock(new Date()))
const coachName = ref(localStorage.getItem('realName') || '教练')
const goalColors = ['#f56c6c', '#e6a23c', '#67c23a', '#409eff', '#909399']

const stats = reactive({
  totalStudents: 0,
  activeStudents: 0,
  needAttention: 0
})

const goalDistribution = ref([])
const todoList = ref([])
const activeStudents = ref([])
const attentionStudents = ref([])
const handledTodoKeys = ref(new Set())

const createTodoReasonKey = (type, userId) => `${type}::${userId}`

let timerId = null

const buildTodoKey = (reason, userId) => `${userId}::${reason || `FOLLOW_UP_${userId}`}`

const loadData = async () => {
  loading.value = true
  try {
    const [students, handledTodos] = await Promise.all([
      getCoachStudents(),
      getHandledCoachTodos()
    ])

    handledTodoKeys.value = new Set(
      (handledTodos || []).map((item) => buildTodoKey(item.todoKey || item.todoDescription, item.studentId))
    )

    const normalizedStudents = (students || []).map((item) => normalizeStudent(item))
    stats.totalStudents = normalizedStudents.length
    stats.activeStudents = normalizedStudents.filter((item) => item.daysSinceLastExercise != null && item.daysSinceLastExercise <= 7).length

    goalDistribution.value = buildGoalDistribution(normalizedStudents)
    activeStudents.value = buildActiveStudents(normalizedStudents)
    attentionStudents.value = buildAttentionStudents(normalizedStudents).filter((item) => {
      const reasonKey = item.reasonKey || item.reason
      return !handledTodoKeys.value.has(buildTodoKey(reasonKey, item.id))
        && !handledTodoKeys.value.has(buildTodoKey(item.reason, item.id))
        && !handledTodoKeys.value.has(buildTodoKey(`FOLLOW_UP_${item.id}`, item.id))
    })
    stats.needAttention = attentionStudents.value.length
    todoList.value = buildTodoList(attentionStudents.value)
  } catch (error) {
    ElMessage.error(error?.message || '加载移动端看板失败')
  } finally {
    loading.value = false
  }
}

const normalizeStudent = (item) => {
  const lastExerciseDate = parseDate(item.lastExerciseTime)
  return {
    id: item.id,
    name: item.realName || item.username || `学员${item.id}`,
    goal: normalizeGoal(item.fitnessGoal),
    trainingStatus: normalizePlanStatus(item.trainingStatus),
    progress: Number(item.planProgress || 0),
    lastExerciseDate,
    daysSinceLastExercise: getDaysDiff(lastExerciseDate, new Date())
  }
}

const normalizeGoal = (goal) => {
  const goalMap = {
    WEIGHT_LOSS: '减重',
    FAT_LOSS: '减脂',
    MUSCLE_GAIN: '增肌'
  }
  return goalMap[`${goal || ''}`.trim().toUpperCase()] || '未设置'
}

const normalizePlanStatus = (status) => {
  const raw = `${status || ''}`.trim().toLowerCase()
  if (raw.includes('active') || raw.includes('进行')) return 'active'
  if (raw.includes('complete') || raw.includes('完成')) return 'completed'
  return 'inactive'
}

const buildGoalDistribution = (students) => {
  if (!students.length) return []
  const goalCounter = students.reduce((acc, item) => {
    acc[item.goal] = (acc[item.goal] || 0) + 1
    return acc
  }, {})
  const total = students.length
  return Object.entries(goalCounter).map(([name, count], index) => ({
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
        return { id: item.id, name: item.name, reason: '暂无运动记录', reasonKey: createTodoReasonKey('NO_EXERCISE_RECORD', item.id) }
      }
      if (item.daysSinceLastExercise > 7) {
        return { id: item.id, name: item.name, reason: `连续${item.daysSinceLastExercise}天未运动`, reasonKey: createTodoReasonKey('INACTIVE_DAYS', item.id) }
      }
      if (item.trainingStatus === 'inactive') {
        return { id: item.id, name: item.name, reason: '暂无进行中的训练计划', reasonKey: createTodoReasonKey('NO_ACTIVE_PLAN', item.id) }
      }
      if (item.progress > 0 && item.progress < 30) {
        return { id: item.id, name: item.name, reason: `计划完成率偏低（${Math.round(item.progress)}%）`, reasonKey: createTodoReasonKey('LOW_PROGRESS', item.id) }
      }
      return null
    })
    .filter(Boolean)
    .slice(0, 6)
}

const buildTodoList = (attentionList) => {
  return attentionList.map((item, index) => ({
    id: `todo-${item.id}-${index}`,
    todoKey: item.reasonKey || item.reason,
    title: `${item.name}需要跟进`,
    description: item.reason,
    priority: item.reason.includes('未运动') || item.reason.includes('暂无运动记录') ? 'high' : 'medium',
    userId: item.id
  }))
}

const handleTodoClick = async (item) => {
  try {
    await handleCoachTodo({
      studentId: item.userId,
      todoKey: item.todoKey,
      todoTitle: item.title,
      todoDescription: item.description
    })
    handledTodoKeys.value.add(buildTodoKey(item.todoKey, item.userId))
    todoList.value = todoList.value.filter((todo) => todo.id !== item.id)
    attentionStudents.value = attentionStudents.value.filter((student) => !(student.id === item.userId && student.reason === item.todoKey))
    ElMessage.success(`已处理: ${item.title}`)
  } catch (error) {
    ElMessage.error(error?.message || '处理待办失败')
  }

  router.push(`/coach/students/${item.userId}`)
}

const viewStudent = (studentId) => {
  router.push(`/coach/students/${studentId}`)
}

const getDaysDiff = (date, reference) => {
  if (!date) return null
  const target = new Date(date)
  target.setHours(0, 0, 0, 0)
  const current = new Date(reference)
  current.setHours(0, 0, 0, 0)
  return Math.max(Math.round((current - target) / (24 * 60 * 60 * 1000)), 0)
}

const formatRelativeDay = (days) => {
  if (days == null) return '暂无记录'
  if (days === 0) return '今天'
  if (days === 1) return '1天前'
  return `${days}天前`
}

const formatClock = (date) => {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const parseDate = (value) => {
  if (!value) return null
  const date = value instanceof Date ? value : new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

onMounted(() => {
  loadData()
  timerId = setInterval(() => {
    currentTime.value = formatClock(new Date())
  }, 1000)
})

onUnmounted(() => {
  if (timerId) clearInterval(timerId)
})
</script>

<style scoped>
.mobile-dashboard {
  background: #f5f5f5;
  min-height: 100%;
  padding-bottom: 80px;
}

.status-bar,
.coach-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
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
  opacity: 0.85;
  margin-top: 4px;
}

.stats-overview {
  display: flex;
  justify-content: space-around;
  align-items: center;
  background: white;
  margin: -20px 15px 15px;
  padding: 20px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
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

.todo-list,
.goal-distribution,
.student-list,
.attention-list {
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

.todo-content,
.student-info,
.attention-info {
  flex: 1;
}

.todo-title,
.student-name,
.attention-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.todo-desc,
.student-activity {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.attention-reason {
  font-size: 12px;
  color: #f56c6c;
  margin-top: 4px;
}

.goal-bar {
  height: 8px;
  background: var(--color);
  border-radius: 4px;
  width: var(--width);
}

.goal-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.warning-section {
  background: linear-gradient(135deg, #fff5f5 0%, #fff 100%);
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
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
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

<template>
  <div class="mobile-dashboard">
    <div class="status-bar">
      <span class="time">{{ currentTime }}</span>
      <div class="status-icons">
        <span>5G</span>
        <span>100%</span>
      </div>
    </div>

    <div class="user-header">
      <div class="user-avatar">
        <el-avatar :size="60">{{ userName.slice(0, 1) }}</el-avatar>
      </div>
      <div class="user-info">
        <div class="user-name">{{ userName }}</div>
        <div class="user-goal">
          <el-tag size="small" :type="goalTagType">{{ fitnessGoal }}</el-tag>
        </div>
      </div>
      <div class="notification-icon">
        <el-badge :value="0" :max="9">
          <el-icon size="24"><Bell /></el-icon>
        </el-badge>
      </div>
    </div>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon">体</div>
        <div class="stat-value">{{ stats.weight }}<span class="unit">kg</span></div>
        <div class="stat-label">当前体重</div>
        <div class="stat-change" :class="stats.weightChange < 0 ? 'down' : 'up'">
          {{ stats.weightChange > 0 ? '+' : '' }}{{ stats.weightChange }}kg
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">脂</div>
        <div class="stat-value">{{ stats.bodyFat }}<span class="unit">%</span></div>
        <div class="stat-label">体脂率</div>
        <div class="stat-change" :class="stats.bodyFatChange < 0 ? 'down' : 'up'">
          {{ stats.bodyFatChange > 0 ? '+' : '' }}{{ stats.bodyFatChange }}%
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">BMI</div>
        <div class="stat-value">{{ stats.bmi }}</div>
        <div class="stat-label">身体状态</div>
        <div class="stat-change normal">{{ getBmiText(stats.bmi) }}</div>
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <span class="section-title">本周运动</span>
      </div>
      <div class="week-stats">
        <div class="week-stat-item">
          <div class="week-stat-value">{{ weekStats.exerciseCount }}</div>
          <div class="week-stat-label">运动次数</div>
        </div>
        <div class="week-stat-item">
          <div class="week-stat-value">{{ weekStats.totalDuration }}</div>
          <div class="week-stat-label">总时长(分钟)</div>
        </div>
        <div class="week-stat-item">
          <div class="week-stat-value">{{ weekStats.totalCalories }}</div>
          <div class="week-stat-label">消耗(kcal)</div>
        </div>
      </div>
      <div class="week-chart" ref="weekChartRef"></div>
    </div>

    <div class="section warning-section">
      <div class="section-header">
        <span class="section-title">健身房状态</span>
      </div>
      <div class="gym-status" :class="peakWarning.level">
        <div class="status-indicator"></div>
        <div class="status-info">
          <div class="status-text">{{ peakWarning.currentStatus }}</div>
          <div class="status-count">当前在馆: {{ peakWarning.currentCount }}人</div>
        </div>
        <div class="peak-time">
          <div class="peak-label">高峰时段</div>
          <div class="peak-value">{{ peakWarning.peakHours }}</div>
        </div>
      </div>
    </div>

    <div class="section" v-if="trainingPlan">
      <div class="section-header">
        <span class="section-title">我的训练计划</span>
      </div>
      <div class="plan-card">
        <div class="plan-name">{{ trainingPlan.planName }}</div>
        <div class="plan-meta">
          <el-tag size="small" effect="light" :type="goalTagType">{{ formatGoalType(trainingPlan.goalType) }}</el-tag>
          <span>{{ Math.round(trainingPlan.completionRate || 0) }}%</span>
        </div>
        <el-progress :percentage="Math.round(trainingPlan.completionRate || 0)" :stroke-width="10" />
        <div class="schedule-title">今日安排</div>
        <div class="schedule-items" v-if="todaySchedule.length">
          <div class="schedule-item" v-for="(item, index) in todaySchedule" :key="`${item}-${index}`">
            <el-icon><Check /></el-icon>
            <span>{{ item }}</span>
          </div>
        </div>
        <div class="schedule-item empty" v-else>今日休息</div>
      </div>
    </div>

    <div class="bottom-nav">
      <div class="nav-item active">
        <el-icon size="24"><HomeFilled /></el-icon>
        <span>首页</span>
      </div>
      <div class="nav-item" @click="$router.push('/student/calendar')">
        <el-icon size="24"><Calendar /></el-icon>
        <span>日历</span>
      </div>
      <div class="nav-item add-btn" @click="$router.push('/student/checkin')">
        <el-icon size="28"><Plus /></el-icon>
      </div>
      <div class="nav-item" @click="$router.push('/student/achievements')">
        <el-icon size="24"><Trophy /></el-icon>
        <span>成就</span>
      </div>
      <div class="nav-item" @click="$router.push('/student/settings')">
        <el-icon size="24"><User /></el-icon>
        <span>我的</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { Bell, Calendar, Check, HomeFilled, Plus, Trophy, User } from '@element-plus/icons-vue'
import { getLatestBodyMetric, getBodyMetricHistory } from '@/api/bodyMetric'
import { getExerciseStatistics, getUserExerciseRecords } from '@/api/exercise'
import { getPeakHourWarning } from '@/api/analytics'
import { getMyTrainingPlan } from '@/api/trainingPlan'
import { initChart } from '@/utils/chartTheme'

const weekChartRef = ref(null)
const currentTime = ref(formatClock(new Date()))
const userName = ref(localStorage.getItem('realName') || '学员')
const fitnessGoal = ref('未设置目标')
const goalTagType = ref('info')
const trainingPlan = ref(null)
const weekChart = ref(null)

const stats = reactive({
  weight: 0,
  bodyFat: 0,
  bmi: 0,
  weightChange: 0,
  bodyFatChange: 0
})

const weekStats = reactive({
  exerciseCount: 0,
  totalDuration: 0,
  totalCalories: 0
})

const peakWarning = reactive({
  level: 'normal',
  currentStatus: '当前空闲',
  currentCount: 0,
  peakHours: '--:--'
})

const todaySchedule = computed(() => {
  const raw = trainingPlan.value?.weeklySchedule
  if (!raw) return []
  try {
    const schedule = typeof raw === 'string' ? JSON.parse(raw) : raw
    const days = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
    const todayKey = days[new Date().getDay()]
    const value = schedule?.[todayKey]
    if (Array.isArray(value)) return value
    if (typeof value === 'string') return value.split(/[；;\n]/).map((item) => item.trim()).filter(Boolean)
    return []
  } catch {
    return []
  }
})

let timerId = null

function formatClock(date) {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function getBmiText(bmi) {
  if (bmi < 18.5) return '偏瘦'
  if (bmi < 24) return '正常'
  if (bmi < 28) return '偏胖'
  return '肥胖'
}

function formatGoalType(goalType) {
  const map = {
    WEIGHT_LOSS: '减重',
    FAT_LOSS: '减脂',
    MUSCLE_GAIN: '增肌'
  }
  return map[`${goalType || ''}`.trim().toUpperCase()] || '未设置目标'
}

function resolveGoalTagType(goalType) {
  const map = {
    WEIGHT_LOSS: 'danger',
    FAT_LOSS: 'warning',
    MUSCLE_GAIN: 'success'
  }
  return map[`${goalType || ''}`.trim().toUpperCase()] || 'info'
}

function getCurrentWeekRange() {
  const now = new Date()
  const weekday = now.getDay()
  const diff = weekday === 0 ? 6 : weekday - 1

  const start = new Date(now)
  start.setDate(now.getDate() - diff)
  start.setHours(0, 0, 0, 0)

  const end = new Date(start)
  end.setDate(start.getDate() + 6)
  end.setHours(23, 59, 59, 999)

  return { start, end }
}

function formatDateParam(date) {
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}`
}

function initWeekChartWith(records) {
  if (!weekChartRef.value) return
  if (!weekChart.value) weekChart.value = initChart(weekChartRef.value)

  const labels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const values = Array(7).fill(0)
  ;(records || []).forEach((record) => {
    if (!record?.exerciseDate) return
    const date = new Date(record.exerciseDate)
    if (Number.isNaN(date.getTime())) return
    const day = date.getDay()
    const index = day === 0 ? 6 : day - 1
    values[index] += Number(record.durationMinutes || 0)
  })

  weekChart.value.setOption({
    grid: { left: 20, right: 10, top: 10, bottom: 20 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { fontSize: 10, color: '#999' }
    },
    yAxis: { type: 'value', show: false },
    series: [{
      type: 'bar',
      data: values,
      barWidth: 20,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#409eff' },
          { offset: 1, color: '#66b1ff' }
        ])
      }
    }]
  })
}

async function loadData() {
  const { start, end } = getCurrentWeekRange()
  const weekParams = {
    startDate: formatDateParam(start),
    endDate: formatDateParam(end)
  }

  try {
    const latestMetric = await getLatestBodyMetric()
    if (latestMetric) {
      stats.weight = Number(latestMetric.weightKg || 0)
      stats.bodyFat = Number(latestMetric.bodyFatPercentage || 0)
      stats.bmi = Number(latestMetric.bmi || 0)
    }
  } catch {}

  try {
    const history = await getBodyMetricHistory()
    const sorted = (Array.isArray(history) ? history : [])
      .filter((item) => item && item.measurementDate && item.weightKg != null)
      .sort((left, right) => new Date(left.measurementDate) - new Date(right.measurementDate))
    if (sorted.length >= 2) {
      stats.weightChange = Number((Number(sorted.at(-1).weightKg) - Number(sorted.at(-2).weightKg)).toFixed(1))
      const latestFat = Number(sorted.at(-1).bodyFatPercentage)
      const previousFat = Number(sorted.at(-2).bodyFatPercentage)
      if (Number.isFinite(latestFat) && Number.isFinite(previousFat)) {
        stats.bodyFatChange = Number((latestFat - previousFat).toFixed(1))
      }
    }
  } catch {}

  try {
    const exerciseStats = await getExerciseStatistics(weekParams)
    weekStats.exerciseCount = Number(exerciseStats?.totalRecords || 0)
    weekStats.totalDuration = Number(exerciseStats?.totalDurationMinutes || 0)
    weekStats.totalCalories = Math.round(Number(exerciseStats?.totalCaloriesBurned || 0))
  } catch {}

  try {
    const records = await getUserExerciseRecords(weekParams)
    initWeekChartWith(records)
  } catch {
    initWeekChartWith([])
  }

  try {
    const plan = await getMyTrainingPlan()
    trainingPlan.value = plan
    fitnessGoal.value = formatGoalType(plan?.goalType)
    goalTagType.value = resolveGoalTagType(plan?.goalType)
  } catch {
    trainingPlan.value = null
    fitnessGoal.value = '未设置目标'
    goalTagType.value = 'info'
  }

  try {
    const warning = await getPeakHourWarning()
    const peakHour = Number(warning?.peakHour)
    peakWarning.level = warning?.isPeakHour ? 'crowded' : 'normal'
    peakWarning.currentStatus = warning?.isPeakHour ? '当前高峰' : '当前空闲'
    peakWarning.currentCount = Number(warning?.currentCount || 0)
    peakWarning.peakHours = Number.isFinite(peakHour)
      ? `${String(peakHour).padStart(2, '0')}:00-${String((peakHour + 1) % 24).padStart(2, '0')}:59`
      : '--:--'
  } catch {}
}

onMounted(() => {
  loadData()
  timerId = setInterval(() => {
    currentTime.value = formatClock(new Date())
  }, 1000)
})

onUnmounted(() => {
  if (timerId) clearInterval(timerId)
  weekChart.value?.dispose()
  weekChart.value = null
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 14px;
  font-weight: 500;
}

.status-icons {
  display: flex;
  gap: 8px;
}

.user-header {
  display: flex;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.user-info {
  flex: 1;
  margin-left: 15px;
}

.user-name {
  font-size: 20px;
  font-weight: bold;
}

.user-goal {
  margin-top: 5px;
}

.stats-cards {
  display: flex;
  gap: 10px;
  padding: 0 15px;
  margin-top: -30px;
}

.stat-card {
  flex: 1;
  background: white;
  border-radius: 16px;
  padding: 15px 10px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  font-size: 18px;
  margin-bottom: 8px;
  color: #409eff;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-value .unit {
  font-size: 12px;
  color: #909399;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.stat-change {
  font-size: 11px;
  margin-top: 4px;
  padding: 2px 6px;
  border-radius: 10px;
}

.stat-change.down {
  color: #67c23a;
  background: #f0f9eb;
}

.stat-change.up {
  color: #f56c6c;
  background: #fef0f0;
}

.stat-change.normal {
  color: #409eff;
  background: #ecf5ff;
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

.week-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 15px;
}

.week-stat-item {
  text-align: center;
}

.week-stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.week-stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.week-chart {
  height: 120px;
}

.warning-section {
  background: linear-gradient(135deg, #fff5f5 0%, #fff 100%);
}

.gym-status {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 12px;
  background: #f0f9eb;
}

.gym-status.crowded {
  background: #fef0f0;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #67c23a;
  margin-right: 12px;
}

.gym-status.crowded .status-indicator {
  background: #f56c6c;
}

.status-info {
  flex: 1;
}

.status-text {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.status-count {
  font-size: 12px;
  color: #909399;
}

.peak-time {
  text-align: right;
}

.peak-label {
  font-size: 11px;
  color: #909399;
}

.peak-value {
  font-size: 14px;
  font-weight: bold;
  color: #f56c6c;
}

.plan-card {
  border-radius: 12px;
  padding: 15px;
  background: linear-gradient(135deg, #eef4ff 0%, #ffffff 100%);
  border: 1px solid #dfe9ff;
}

.plan-name {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.plan-meta {
  margin: 8px 0 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #606266;
}

.schedule-title {
  margin: 12px 0 8px;
  font-size: 14px;
  color: #606266;
}

.schedule-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
  color: #303133;
}

.schedule-item.empty {
  color: #909399;
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
  color: #409eff;
}

.nav-item.add-btn {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  color: white;
  justify-content: center;
  margin-top: -25px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}
</style>

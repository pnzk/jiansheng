<template>
  <div class="mobile-dashboard">
    <!-- 顶部状态栏 -->
    <div class="status-bar">
      <span class="time">{{ currentTime }}</span>
      <div class="status-icons">
        <span>📶</span>
        <span>🔋</span>
      </div>
    </div>

    <!-- 用户头部 -->
    <div class="user-header">
      <div class="user-avatar">
        <el-avatar :size="60" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
      </div>
      <div class="user-info">
        <div class="user-name">{{ userName }}</div>
        <div class="user-goal">
          <el-tag size="small" :type="goalType">{{ fitnessGoal }}</el-tag>
        </div>
      </div>
      <div class="notification-icon">
        <el-badge :value="3" :max="9">
          <el-icon size="24"><Bell /></el-icon>
        </el-badge>
      </div>
    </div>

    <!-- 核心数据卡片 -->
    <div class="stats-cards">
      <div class="stat-card weight">
        <div class="stat-icon">⚖️</div>
        <div class="stat-value">{{ stats.weight }}<span class="unit">kg</span></div>
        <div class="stat-label">体重</div>
        <div class="stat-change" :class="stats.weightChange < 0 ? 'down' : 'up'">
          {{ stats.weightChange > 0 ? '↑' : '↓' }}{{ Math.abs(stats.weightChange) }}kg
        </div>
      </div>
      <div class="stat-card fat">
        <div class="stat-icon">📊</div>
        <div class="stat-value">{{ stats.bodyFat }}<span class="unit">%</span></div>
        <div class="stat-label">体脂率</div>
        <div class="stat-change" :class="stats.bodyFatChange < 0 ? 'down' : 'up'">
          {{ stats.bodyFatChange > 0 ? '↑' : '↓' }}{{ Math.abs(stats.bodyFatChange) }}%
        </div>
      </div>
      <div class="stat-card bmi">
        <div class="stat-icon">💪</div>
        <div class="stat-value">{{ stats.bmi }}</div>
        <div class="stat-label">BMI</div>
        <div class="stat-change normal">{{ getBmiStatus(stats.bmi) }}</div>
      </div>
    </div>

    <!-- 今日目标进度 -->
    <div class="section">
      <div class="section-header">
        <span class="section-title">今日目标</span>
        <span class="section-more">查看详情 ></span>
      </div>
      <div class="goal-progress">
        <div class="goal-item">
          <div class="goal-icon">🏃</div>
          <div class="goal-info">
            <div class="goal-name">运动时长</div>
            <el-progress :percentage="60" :stroke-width="8" color="#409eff" />
          </div>
          <div class="goal-value">36/60分钟</div>
        </div>
        <div class="goal-item">
          <div class="goal-icon">🔥</div>
          <div class="goal-info">
            <div class="goal-name">消耗热量</div>
            <el-progress :percentage="75" :stroke-width="8" color="#f56c6c" />
          </div>
          <div class="goal-value">375/500卡</div>
        </div>
        <div class="goal-item">
          <div class="goal-icon">👣</div>
          <div class="goal-info">
            <div class="goal-name">步数</div>
            <el-progress :percentage="85" :stroke-width="8" color="#67c23a" />
          </div>
          <div class="goal-value">8500/10000</div>
        </div>
      </div>
    </div>

    <!-- 本周运动统计 -->
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
          <div class="week-stat-label">总时长(分)</div>
        </div>
        <div class="week-stat-item">
          <div class="week-stat-value">{{ weekStats.totalCalories }}</div>
          <div class="week-stat-label">消耗(卡)</div>
        </div>
      </div>
      <div class="week-chart" ref="weekChartRef"></div>
    </div>

    <!-- 高峰期预警 -->
    <div class="section warning-section">
      <div class="section-header">
        <span class="section-title">🚨 健身房状态</span>
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

    <!-- 训练计划 -->
    <div class="section" v-if="trainingPlan">
      <div class="section-header">
        <span class="section-title">📋 我的训练计划</span>
      </div>
      <div class="plan-card">
        <div class="plan-name">{{ trainingPlan.planName }}</div>
        <div class="plan-progress">
          <el-progress :percentage="trainingPlan.completionRate" :stroke-width="10" />
        </div>
        <div class="plan-schedule">
          <div class="schedule-title">今日安排</div>
          <div class="schedule-items">
            <div class="schedule-item" v-for="(item, index) in todaySchedule" :key="index">
              <el-icon><Check /></el-icon>
              <span>{{ item }}</span>
            </div>
            <div class="schedule-item empty" v-if="todaySchedule.length === 0">
              今日休息
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <div class="nav-item active">
        <el-icon size="24"><HomeFilled /></el-icon>
        <span>首页</span>
      </div>
      <div class="nav-item" @click="$router.push('/student/calendar')">
        <el-icon size="24"><Calendar /></el-icon>
        <span>记录</span>
      </div>
      <div class="nav-item add-btn" @click="$router.push('/student/calendar')">
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
import { ref, reactive, onMounted, computed } from 'vue'
import * as echarts from 'echarts'
import { Bell, HomeFilled, Calendar, Plus, Trophy, User, Check } from '@element-plus/icons-vue'
import { getLatestBodyMetric, getBodyMetricHistory } from '@/api/bodyMetric'
import { getExerciseStatistics } from '@/api/exercise'
import { getMyTrainingPlan } from '@/api/trainingPlan'
import { getPeakHourWarning } from '@/api/analytics'
import { initChart } from '@/utils/chartTheme'

const weekChartRef = ref(null)
const currentTime = ref(new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }))
const userName = ref(localStorage.getItem('realName') || '健身达人')
const fitnessGoal = ref('减重')
const goalType = ref('danger')

const stats = reactive({
  weight: 70,
  bodyFat: 20,
  bmi: 22,
  weightChange: -0.5,
  bodyFatChange: -0.3
})

const weekStats = reactive({
  exerciseCount: 5,
  totalDuration: 180,
  totalCalories: 1500
})

const peakWarning = reactive({
  level: 'normal',
  currentStatus: '当前空闲',
  currentCount: 15,
  peakHours: '18:00-20:00'
})

const trainingPlan = ref(null)

const todaySchedule = computed(() => {
  if (!trainingPlan.value?.weeklySchedule) return []
  try {
    const schedule = typeof trainingPlan.value.weeklySchedule === 'string' 
      ? JSON.parse(trainingPlan.value.weeklySchedule) 
      : trainingPlan.value.weeklySchedule
    const days = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
    const today = days[new Date().getDay()]
    return schedule[today] || []
  } catch {
    return []
  }
})

const getBmiStatus = (bmi) => {
  if (bmi < 18.5) return '偏瘦'
  if (bmi < 24) return '正常'
  if (bmi < 28) return '偏胖'
  return '肥胖'
}

const loadData = async () => {
  try {
    const latestMetric = await getLatestBodyMetric()
    if (latestMetric) {
      stats.weight = latestMetric.weightKg || 70
      stats.bodyFat = latestMetric.bodyFatPercentage || 20
      stats.bmi = latestMetric.bmi || 22
    }
  } catch (e) {}

  try {
    const exerciseStats = await getExerciseStatistics()
    if (exerciseStats) {
      weekStats.exerciseCount = exerciseStats.weekExerciseCount || 5
      weekStats.totalDuration = exerciseStats.weekTotalDuration || 180
      weekStats.totalCalories = exerciseStats.weekTotalCalories || 1500
    }
  } catch (e) {}

  try {
    const plan = await getMyTrainingPlan()
    trainingPlan.value = plan
  } catch (e) {}

  try {
    const warning = await getPeakHourWarning()
    if (warning) {
      Object.assign(peakWarning, warning)
    }
  } catch (e) {}
}

const initWeekChart = () => {
  if (!weekChartRef.value) return
  const chart = initChart(weekChartRef.value)
  chart.setOption({
    grid: { left: 30, right: 10, top: 10, bottom: 20 },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { fontSize: 10, color: '#999' }
    },
    yAxis: {
      type: 'value',
      show: false
    },
    series: [{
      type: 'bar',
      data: [30, 45, 0, 60, 45, 0, 0],
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#409eff' },
          { offset: 1, color: '#66b1ff' }
        ])
      },
      barWidth: 20
    }]
  })
}

onMounted(() => {
  loadData()
  setTimeout(initWeekChart, 100)
  setInterval(() => {
    currentTime.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }, 1000)
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

.notification-icon {
  cursor: pointer;
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
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.stat-icon {
  font-size: 24px;
  margin-bottom: 8px;
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
}

.goal-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.goal-item:last-child {
  border-bottom: none;
}

.goal-icon {
  font-size: 24px;
  margin-right: 12px;
}

.goal-info {
  flex: 1;
}

.goal-name {
  font-size: 14px;
  color: #606266;
  margin-bottom: 6px;
}

.goal-value {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
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
  background: #f5f5f5;
}

.gym-status.normal {
  background: #f0f9eb;
}

.gym-status.busy {
  background: #fdf6ec;
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
  animation: pulse 2s infinite;
}

.gym-status.busy .status-indicator {
  background: #e6a23c;
}

.gym-status.crowded .status-indicator {
  background: #f56c6c;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 15px;
  color: white;
}

.plan-name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
}

.plan-progress {
  margin-bottom: 15px;
}

.plan-progress :deep(.el-progress__text) {
  color: white;
}

.schedule-title {
  font-size: 14px;
  margin-bottom: 10px;
  opacity: 0.9;
}

.schedule-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
}

.schedule-item.empty {
  opacity: 0.7;
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

<template>
  <div class="dashboard">
    <h2>个人健身仪表盘</h2>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card class="metric-card">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #409eff, #66b1ff)">
              <el-icon size="32"><Scale /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.weight }} <span class="unit">kg</span></div>
              <div class="stat-label">当前体重</div>
              <div class="stat-change" :class="stats.weightChange < 0 ? 'positive' : 'negative'">
                {{ stats.weightChange > 0 ? '+' : '' }}{{ stats.weightChange }} kg 较上周
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="metric-card">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #67c23a, #85ce61)">
              <el-icon size="32"><TrendCharts /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.bodyFat }} <span class="unit">%</span></div>
              <div class="stat-label">体脂率</div>
              <div class="stat-change" :class="stats.bodyFatChange < 0 ? 'positive' : 'negative'">
                {{ stats.bodyFatChange > 0 ? '+' : '' }}{{ stats.bodyFatChange }}% 较上周
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="metric-card">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #e6a23c, #f0c78a)">
              <el-icon size="32"><DataAnalysis /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.bmi }}</div>
              <div class="stat-label">BMI指数</div>
              <div class="stat-change" :class="getBmiStatus(stats.bmi).class">
                {{ getBmiStatus(stats.bmi).text }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card class="warning-card">
          <template #header>
            <div class="card-header">
              <el-icon color="#f56c6c"><WarningFilled /></el-icon>
              <span>高峰期拥堵预警</span>
            </div>
          </template>
          <div class="peak-warning">
            <div class="current-status" :class="peakWarning.level">
              <div class="status-icon">
                <el-icon size="40"><Clock /></el-icon>
              </div>
              <div class="status-text">
                <div class="status-title">{{ peakWarning.currentStatus }}</div>
                <div class="status-desc">当前在馆人数: {{ peakWarning.currentCount }}人</div>
              </div>
            </div>
            <el-divider />
            <div class="peak-times">
              <div class="peak-item">
                <span class="peak-label">今日高峰时段:</span>
                <el-tag type="danger" size="small">{{ peakWarning.peakHours }}</el-tag>
              </div>
              <div class="peak-item">
                <span class="peak-label">建议运动时段:</span>
                <el-tag type="success" size="small">{{ peakWarning.suggestedHours }}</el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>本周运动概况</span>
          </template>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="week-stat">
                <div class="week-stat-value">{{ weekStats.exerciseCount }}</div>
                <div class="week-stat-label">运动次数</div>
                <el-progress :percentage="Math.min((weekStats.exerciseCount / 7) * 100, 100)" :show-text="false" />
              </div>
            </el-col>
            <el-col :span="8">
              <div class="week-stat">
                <div class="week-stat-value">{{ weekStats.totalDuration }}<span class="small-unit">分钟</span></div>
                <div class="week-stat-label">运动时长</div>
                <el-progress :percentage="Math.min((weekStats.totalDuration / 300) * 100, 100)" :show-text="false" status="success" />
              </div>
            </el-col>
            <el-col :span="8">
              <div class="week-stat">
                <div class="week-stat-value">{{ weekStats.totalCalories }}<span class="small-unit">kcal</span></div>
                <div class="week-stat-label">消耗卡路里</div>
                <el-progress :percentage="Math.min((weekStats.totalCalories / 3000) * 100, 100)" :show-text="false" status="warning" />
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>体重变化趋势</span>
          </template>
          <div ref="weightChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="plan-card">
          <template #header>
            <div class="card-header">
              <el-icon color="#409eff"><Calendar /></el-icon>
              <span>训练计划提醒</span>
            </div>
          </template>
          <div class="plan-reminder" v-if="trainingPlan">
            <div class="plan-name">{{ trainingPlan.planName }}</div>
            <div class="plan-goal">
              <el-tag :type="getGoalTagType(trainingPlan.goalType)">{{ formatGoalType(trainingPlan.goalType) }}</el-tag>
            </div>
            <el-divider />
            <div class="today-plan">
              <div class="today-title">今日训练安排</div>
              <div class="today-items" v-if="todaySchedule.length">
                <div class="today-item" v-for="(item, index) in todaySchedule" :key="`${item}-${index}`">
                  <el-icon><Check /></el-icon>
                  <span>{{ item }}</span>
                </div>
              </div>
              <div class="no-plan" v-else>
                <el-icon><Coffee /></el-icon>
                <span>今日休息日</span>
              </div>
            </div>
            <el-divider />
            <div class="plan-progress">
              <span>计划完成率</span>
              <el-progress :percentage="Math.round(trainingPlan.completionRate || 0)" :stroke-width="12" />
            </div>
          </div>
          <div class="no-plan-card" v-else>
            <el-empty description="暂无训练计划" :image-size="80">
              <el-button type="primary" size="small">联系教练制定计划</el-button>
            </el-empty>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>运动类型分布</span>
          </template>
          <div ref="exerciseChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>{{ heatmapTitle }}</span>
          </template>
          <div ref="heatmapChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { getBodyMetricHistory, getLatestBodyMetric } from '@/api/bodyMetric'
import { getUserExerciseRecords, getExerciseStatistics } from '@/api/exercise'
import { getPeakHourWarning } from '@/api/analytics'
import { getMyTrainingPlan } from '@/api/trainingPlan'
import { CHART_COLORS, HEATMAP_GRADIENT, initChart } from '@/utils/chartTheme'

const DAY_LABELS = {
  monday: '周一',
  tuesday: '周二',
  wednesday: '周三',
  thursday: '周四',
  friday: '周五',
  saturday: '周六',
  sunday: '周日'
}

const weightChartRef = ref(null)
const exerciseChartRef = ref(null)
const heatmapChartRef = ref(null)
const chartInstances = []
const heatmapTitle = ref('本周运动热力图')
const trainingPlan = ref(null)

const EXERCISE_TYPE_LABELS = {
  RUNNING: '跑步',
  CYCLING: '骑行',
  SWIMMING: '游泳',
  STRENGTH_TRAINING: '力量训练',
  YOGA: '瑜伽',
  HIIT: 'HIIT',
  WALKING: '步行',
  BOXING: '搏击',
  PILATES: '普拉提'
}

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
  peakHours: '--:--',
  suggestedHours: '--:--'
})

const todaySchedule = computed(() => {
  const raw = trainingPlan.value?.weeklySchedule
  if (!raw) return []

  try {
    const schedule = typeof raw === 'string' ? JSON.parse(raw) : raw
    const days = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
    const today = days[new Date().getDay()]
    const value = schedule?.[today]
    if (Array.isArray(value)) return value
    if (typeof value === 'string') return value.split(/[；;\n]/).map((item) => item.trim()).filter(Boolean)
    return []
  } catch {
    return []
  }
})

const getOrCreateChart = (domRef) => {
  if (!domRef) return null
  let chart = echarts.getInstanceByDom(domRef)
  if (!chart) chart = initChart(domRef)
  if (!chartInstances.includes(chart)) chartInstances.push(chart)
  return chart
}

const resizeCharts = () => {
  chartInstances.forEach((chart) => chart.resize())
}

const getBmiStatus = (bmi) => {
  if (bmi < 18.5) return { text: '偏瘦', class: 'warning' }
  if (bmi < 24) return { text: '正常', class: 'positive' }
  if (bmi < 28) return { text: '偏胖', class: 'warning' }
  return { text: '肥胖', class: 'negative' }
}

const formatExerciseType = (type) => {
  const raw = `${type || ''}`.trim().toUpperCase()
  return EXERCISE_TYPE_LABELS[raw] || type || '其他'
}

const getGoalTagType = (goalType) => {
  const types = { WEIGHT_LOSS: 'danger', FAT_LOSS: 'warning', MUSCLE_GAIN: 'success' }
  return types[`${goalType || ''}`.trim().toUpperCase()] || 'info'
}

const formatGoalType = (goalType) => {
  const texts = { WEIGHT_LOSS: '减重', FAT_LOSS: '减脂', MUSCLE_GAIN: '增肌' }
  return texts[`${goalType || ''}`.trim().toUpperCase()] || '未设置目标'
}

const getRecentDaysRange = (days = 30) => {
  const end = new Date()
  end.setHours(23, 59, 59, 999)
  const start = new Date(end)
  start.setDate(end.getDate() - (days - 1))
  start.setHours(0, 0, 0, 0)
  return {
    startDate: formatDateParam(start),
    endDate: formatDateParam(end)
  }
}

const getRecordHour = (record) => {
  const dateLike = record?.createdAt || record?.updatedAt || record?.exerciseDate
  if (!dateLike) return 18
  if (typeof dateLike === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(dateLike)) return 18
  const date = new Date(dateLike)
  if (Number.isNaN(date.getTime())) return 18
  return date.getHours()
}

const getHeatmapHourIndex = (hour) => {
  if (hour < 7) return 0
  if (hour < 9) return 1
  if (hour < 11) return 2
  if (hour < 13) return 3
  if (hour < 15) return 4
  if (hour < 17) return 5
  if (hour < 19) return 6
  if (hour < 21) return 7
  return 8
}

const normalizeRecords = (records) => {
  return (Array.isArray(records) ? records : []).filter((item) => item && item.exerciseDate)
}

const loadData = async () => {
  const { weekStart, weekEnd } = getCurrentWeekRange()

  try {
    const latestMetric = await getLatestBodyMetric()
    if (latestMetric) {
      stats.weight = Number(latestMetric.weightKg || 0)
      stats.bodyFat = Number(latestMetric.bodyFatPercentage || 0)
      stats.bmi = Number(latestMetric.bmi || 0)
    }
  } catch {
    stats.weight = 0
    stats.bodyFat = 0
    stats.bmi = 0
  }

  try {
    const exerciseStats = await getExerciseStatistics({
      startDate: formatDateParam(weekStart),
      endDate: formatDateParam(weekEnd)
    })
    weekStats.exerciseCount = Number(exerciseStats?.totalRecords || 0)
    weekStats.totalDuration = Number(exerciseStats?.totalDurationMinutes || 0)
    weekStats.totalCalories = Math.round(Number(exerciseStats?.totalCaloriesBurned || 0))
  } catch {
    weekStats.exerciseCount = 0
    weekStats.totalDuration = 0
    weekStats.totalCalories = 0
  }

  const { startDate: monthStart, endDate: monthEnd } = getRecentDaysRange(30)
  let weeklyRecords = []
  let monthlyRecords = []

  try {
    weeklyRecords = normalizeRecords(await getUserExerciseRecords({
      startDate: formatDateParam(weekStart),
      endDate: formatDateParam(weekEnd)
    }))
  } catch {
    weeklyRecords = []
  }

  try {
    monthlyRecords = normalizeRecords(await getUserExerciseRecords({
      startDate: monthStart,
      endDate: monthEnd
    }))
  } catch {
    monthlyRecords = []
  }

  const chartRecords = monthlyRecords.length ? monthlyRecords : weeklyRecords
  heatmapTitle.value = monthlyRecords.length ? '近30天运动热力图' : '本周运动热力图'
  initExerciseChart(chartRecords)
  initHeatmapChart(chartRecords)

  try {
    trainingPlan.value = await getMyTrainingPlan()
  } catch {
    trainingPlan.value = null
  }

  try {
    const warning = await getPeakHourWarning()
    const peakHour = Number(warning?.peakHour)
    const validPeakHour = Number.isFinite(peakHour) ? peakHour : null
    peakWarning.level = warning?.isPeakHour ? 'crowded' : 'normal'
    peakWarning.currentStatus = warning?.isPeakHour ? '当前高峰' : '当前空闲'
    peakWarning.currentCount = Number(warning?.currentCount || 0)
    if (validPeakHour != null) {
      const nextHour = (peakHour + 1) % 24
      const suggestedStart = (peakHour + 2) % 24
      const suggestedEnd = (peakHour + 3) % 24
      peakWarning.peakHours = `${String(peakHour).padStart(2, '0')}:00-${String(nextHour).padStart(2, '0')}:59`
      peakWarning.suggestedHours = `${String(suggestedStart).padStart(2, '0')}:00-${String(suggestedEnd).padStart(2, '0')}:59`
    } else {
      peakWarning.peakHours = '--:--'
      peakWarning.suggestedHours = '--:--'
    }
  } catch {
    peakWarning.level = 'normal'
    peakWarning.currentStatus = '当前空闲'
    peakWarning.currentCount = 0
    peakWarning.peakHours = '--:--'
    peakWarning.suggestedHours = '--:--'
  }

  try {
    const history = await getBodyMetricHistory()
    renderWeightChart(history)
  } catch {
    renderWeightChart([])
  }
}

onMounted(() => {
  loadData().then(() => {
    nextTick(() => {
      resizeCharts()
      setTimeout(resizeCharts, 200)
    })
  })
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  chartInstances.forEach((chart) => chart.dispose())
  chartInstances.length = 0
})

const renderWeightChart = (history) => {
  if (!weightChartRef.value) return
  const chart = getOrCreateChart(weightChartRef.value)
  if (!chart) return

  const sortedHistory = Array.isArray(history)
    ? [...history].filter((item) => item && item.measurementDate && item.weightKg != null)
      .sort((left, right) => new Date(left.measurementDate) - new Date(right.measurementDate))
    : []

  const dates = sortedHistory.map((item) => item.measurementDate)
  const weights = sortedHistory.map((item) => Number(item.weightKg || 0))

  if (weights.length >= 2) {
    const latest = weights.at(-1)
    const previous = weights.at(-2)
    stats.weightChange = Number((latest - previous).toFixed(1))
  } else {
    stats.weightChange = 0
  }

  const bodyFatValues = sortedHistory.map((item) => Number(item.bodyFatPercentage)).filter((value) => Number.isFinite(value))
  if (bodyFatValues.length >= 2) {
    const latest = bodyFatValues.at(-1)
    const previous = bodyFatValues.at(-2)
    stats.bodyFatChange = Number((latest - previous).toFixed(1))
  } else {
    stats.bodyFatChange = 0
  }

  if (!dates.length || !weights.length) {
    chart.clear()
    return
  }

  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value', name: '体重(kg)', min: 'dataMin', max: 'dataMax' },
    series: [{
      name: '体重',
      data: weights,
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      itemStyle: { color: CHART_COLORS[0] },
      lineStyle: { width: 3 }
    }]
  })
}

const initExerciseChart = (records) => {
  if (!exerciseChartRef.value) return
  const chart = getOrCreateChart(exerciseChartRef.value)
  if (!chart) return

  const grouped = (Array.isArray(records) ? records : []).reduce((acc, item) => {
    const type = formatExerciseType(item?.exerciseType)
    acc[type] = (acc[type] || 0) + 1
    return acc
  }, {})

  const pieData = Object.entries(grouped).map(([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value)
  if (!pieData.length) {
    chart.clear()
    return
  }

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}次 ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      data: pieData
    }]
  })
}

const initHeatmapChart = (records) => {
  if (!heatmapChartRef.value) return
  const chart = getOrCreateChart(heatmapChartRef.value)
  if (!chart) return

  const hours = ['6:00', '8:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00']
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const dayIndexMap = { 1: 0, 2: 1, 3: 2, 4: 3, 5: 4, 6: 5, 0: 6 }
  const matrix = Array.from({ length: days.length }, () => Array(hours.length).fill(0))

  ;(Array.isArray(records) ? records : []).forEach((record) => {
    const date = new Date(record.exerciseDate)
    if (Number.isNaN(date.getTime())) return
    const rowIndex = dayIndexMap[date.getDay()]
    if (rowIndex == null) return
    const hourIndex = getHeatmapHourIndex(getRecordHour(record))
    matrix[rowIndex][hourIndex] += Number(record.durationMinutes || 0)
  })

  const data = []
  for (let i = 0; i < days.length; i += 1) {
    for (let j = 0; j < hours.length; j += 1) {
      data.push([j, i, matrix[i][j]])
    }
  }
  const maxValue = data.reduce((max, item) => (item[2] > max ? item[2] : max), 0)

  chart.setOption({
    tooltip: { position: 'top', formatter: (p) => `${days[p.value[1]]} ${hours[p.value[0]]}: ${p.value[2]}分钟` },
    grid: { height: '60%', top: '10%' },
    xAxis: { type: 'category', data: hours, splitArea: { show: true } },
    yAxis: { type: 'category', data: days, splitArea: { show: true } },
    visualMap: {
      min: 0,
      max: Math.max(maxValue, 60),
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '5%',
      inRange: { color: HEATMAP_GRADIENT }
    },
    series: [{
      name: '运动时长',
      type: 'heatmap',
      data,
      label: { show: false },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.5)' } }
    }]
  })
}

const getCurrentWeekRange = () => {
  const now = new Date()
  const day = now.getDay()
  const diff = day === 0 ? 6 : day - 1
  const weekStart = new Date(now)
  weekStart.setDate(now.getDate() - diff)
  weekStart.setHours(0, 0, 0, 0)
  const weekEnd = new Date(weekStart)
  weekEnd.setDate(weekStart.getDate() + 6)
  weekEnd.setHours(23, 59, 59, 999)
  return { weekStart, weekEnd }
}

const formatDateParam = (date) => {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}
</script>

<style scoped>
.dashboard h2 {
  margin-bottom: 20px;
  color: #303133;
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 70px;
  height: 70px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-right: 15px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-value .unit {
  font-size: 14px;
  font-weight: normal;
  color: #909399;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.stat-change {
  font-size: 12px;
  margin-top: 4px;
}

.stat-change.positive {
  color: #67c23a;
}

.stat-change.negative {
  color: #f56c6c;
}

.stat-change.warning {
  color: #e6a23c;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.warning-card .peak-warning {
  padding: 10px 0;
}

.current-status {
  display: flex;
  align-items: center;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 10px;
}

.current-status.normal {
  background: linear-gradient(135deg, #67c23a20, #67c23a10);
  color: #67c23a;
}

.current-status.busy {
  background: linear-gradient(135deg, #e6a23c20, #e6a23c10);
  color: #e6a23c;
}

.current-status.crowded {
  background: linear-gradient(135deg, #f56c6c20, #f56c6c10);
  color: #f56c6c;
}

.status-icon {
  margin-right: 15px;
}

.status-title {
  font-size: 18px;
  font-weight: bold;
}

.status-desc {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 4px;
}

.peak-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.peak-label {
  color: #606266;
  font-size: 13px;
}

.week-stat {
  text-align: center;
  padding: 20px 10px;
}

.week-stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

.week-stat-value .small-unit {
  font-size: 14px;
  font-weight: normal;
  color: #909399;
}

.week-stat-label {
  font-size: 14px;
  color: #909399;
  margin: 8px 0;
}

.plan-reminder {
  padding: 10px 0;
}

.plan-name {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.today-title {
  font-size: 14px;
  font-weight: bold;
  color: #606266;
  margin-bottom: 10px;
}

.today-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  color: #409eff;
  font-size: 14px;
}

.no-plan {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
  padding: 10px 0;
}

.plan-progress {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.plan-progress span {
  font-size: 13px;
  color: #606266;
}

.no-plan-card {
  padding: 20px 0;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}
</style>

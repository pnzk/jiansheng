<template>
  <div class="monitoring-page">
    <h2>全局监控</h2>

    <el-card style="margin-top: 20px">
      <el-form :inline="true">
        <el-form-item label="时段活跃口径">
          <el-radio-group v-model="hourlyRangeType" @change="loadData">
            <el-radio-button label="today">当天</el-radio-button>
            <el-radio-button label="week">近7天</el-radio-button>
            <el-radio-button label="month">近30天</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <span class="range-tip">
            当前展示区间：{{ resolvedRange.startDate || '--' }} 至 {{ resolvedRange.endDate || '--' }}
            <el-tag v-if="resolvedRange.fallbackApplied" size="small" type="warning" effect="plain">已自动回退到最近有数据区间</el-tag>
          </span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px" v-loading="loadingStates.summary">
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background: #409eff">
              <el-icon size="30"><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background: #67c23a">
              <el-icon size="30"><Check /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.activeUsers }}</div>
              <div class="stat-label">活跃用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background: #e6a23c">
              <el-icon size="30"><Timer /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalDuration }}</div>
              <div class="stat-label">累计运动时长(分钟)</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background: #f56c6c">
              <el-icon size="30"><TrendCharts /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalCalories }}</div>
              <div class="stat-label">累计消耗卡路里</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px" v-loading="loadingStates.heatmap">
      <el-col :span="8">
        <el-card class="warning-card">
          <template #header>
            <div class="card-header-warning">
              <el-icon color="#f56c6c" size="20"><WarningFilled /></el-icon>
              <span>高峰期拥堵预警</span>
            </div>
          </template>
          <div class="peak-warning-content">
            <div class="current-status" :class="peakWarning.level">
              <div class="status-indicator"></div>
              <div class="status-info">
                <div class="status-title">{{ peakWarning.statusText }}</div>
                <div class="status-count">当前活跃: {{ peakWarning.currentCount }} 人</div>
              </div>
            </div>
            <el-divider />
            <div class="peak-info-list">
              <div class="peak-info-item">
                <span class="label">识别高峰时段</span>
                <el-tag type="danger">{{ peakWarning.peakHours }}</el-tag>
              </div>
              <div class="peak-info-item">
                <span class="label">高峰人数</span>
                <span class="value">{{ peakWarning.peakCount }} 人</span>
              </div>
              <div class="peak-info-item">
                <span class="label">建议阈值</span>
                <span class="value">{{ peakWarning.threshold }} 人</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>{{ heatmapTitle }}</span>
          </template>
          <div ref="heatmapChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px" v-loading="loadingStates.charts">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>用户增长趋势</span>
          </template>
          <div ref="userGrowthChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>运动类型分布</span>
          </template>
          <div ref="exerciseTypeChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px" v-loading="loadingStates.charts">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>{{ peakHourTitle }}</span>
          </template>
          <div ref="peakHourChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>器材使用率</span>
          </template>
          <div ref="equipmentUsageChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px" v-loading="loadingStates.workload">
      <template #header>
        <span>教练工作量统计</span>
      </template>
      <el-table :data="coachWorkload" style="width: 100%">
        <el-table-column prop="coachName" label="教练姓名" min-width="150" />
        <el-table-column prop="studentCount" label="负责学员数" width="130" align="center" />
        <el-table-column prop="planCount" label="创建计划数" width="130" align="center" />
        <el-table-column prop="activeStudents" label="活跃学员数" width="130" align="center" />
        <el-table-column prop="avgProgress" label="平均完成率" min-width="220">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(Number(row.avgProgress || 0))" :stroke-width="10" />
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="coachWorkload.length === 0" description="暂无教练工作量数据" />
    </el-card>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { CHART_COLORS, HEATMAP_GRADIENT, initChart } from '@/utils/chartTheme'
import {
  getCoachWorkload,
  getDashboardStatistics,
  getEquipmentUsage,
  getExercisePreference,
  getHourlyActivity,
  getHourlyHeatmap,
  getPeakHourWarning
} from '@/api/analytics'

const userGrowthChartRef = ref(null)
const exerciseTypeChartRef = ref(null)
const peakHourChartRef = ref(null)
const equipmentUsageChartRef = ref(null)
const heatmapChartRef = ref(null)

const stats = reactive({
  totalUsers: 0,
  activeUsers: 0,
  totalDuration: 0,
  totalCalories: 0
})

const peakWarning = reactive({
  level: 'normal',
  statusText: '当前空闲',
  currentCount: 0,
  peakHours: '--:00- --:59',
  peakCount: 0,
  threshold: 50
})

const resolvedRange = reactive({
  startDate: '',
  endDate: '',
  fallbackApplied: false
})

const coachWorkload = ref([])
const exercisePreferenceData = ref([])
const hourlyData = ref([])
const equipmentUsageRows = ref([])
const userGrowthSeries = ref([])
const heatmapData = ref([])
const heatmapDayLabels = ref([])
const hourlyRangeType = ref('week')
const peakHourTitle = ref('高峰期时段分析（近7天）')
const heatmapTitle = ref('近7天各时段活跃热力图')
const chartInstances = []
const loadingStates = reactive({
  summary: false,
  heatmap: false,
  charts: false,
  workload: false
})

const hours = Array.from({ length: 24 }, (_, hour) => `${hour}:00`)
onMounted(async () => {
  await loadData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  chartInstances.forEach((chart) => chart.dispose())
  chartInstances.length = 0
})

const getRangeMeta = (rangeType) => {
  if (rangeType === 'today') {
    return {
      mode: 'today',
      days: 1,
      peakTitle: '高峰期时段分析（当天）',
      heatmapTitleText: '当天各时段活跃热力图'
    }
  }
  if (rangeType === 'month') {
    return {
      mode: 'month',
      days: 30,
      peakTitle: '高峰期时段分析（近30天）',
      heatmapTitleText: '近30天各时段活跃热力图'
    }
  }
  return {
    mode: 'week',
    days: 7,
    peakTitle: '高峰期时段分析（近7天）',
    heatmapTitleText: '近7天各时段活跃热力图'
  }
}

const formatDate = (date) => {
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}`
}

const buildDateList = (daysCount) => {
  const today = new Date()
  return Array.from({ length: daysCount }, (_, index) => {
    const date = new Date(today)
    date.setDate(today.getDate() - (daysCount - 1 - index))
    return formatDate(date)
  })
}

const toHourlySeries = (hourlyDataList) => {
  const map = new Map((hourlyDataList || []).map((item) => [Number(item.hour), Number(item.count || 0)]))
  return Array.from({ length: 24 }, (_, hour) => ({
    hour,
    count: map.get(hour) || 0
  }))
}

const resolveEffectiveRange = (...responses) => {
  const candidates = responses
    .map((item) => ({
      startDate: item?.periodStart,
      endDate: item?.periodEnd,
      fallbackApplied: Boolean(item?.fallbackApplied)
    }))
    .filter((item) => item.startDate && item.endDate)

  if (!candidates.length) {
    return { startDate: '', endDate: '', fallbackApplied: false }
  }

  return candidates.find((item) => item.fallbackApplied) || candidates[0]
}

const loadData = async () => {
  const meta = getRangeMeta(hourlyRangeType.value)
  const dateList = buildDateList(meta.days)
  const startDate = dateList[0]
  const endDate = dateList[dateList.length - 1]
  const params = { startDate, endDate }

  peakHourTitle.value = meta.peakTitle
  heatmapTitle.value = meta.heatmapTitleText

  try {
    loadingStates.summary = true
    loadingStates.heatmap = true
    loadingStates.charts = true
    loadingStates.workload = true
    const [
      dashboardResult,
      peakResult,
      equipmentResult,
      preferenceResult,
      workloadResult,
      hourlyResult
    ] = await Promise.allSettled([
      getDashboardStatistics(params),
      getPeakHourWarning(params),
      getEquipmentUsage(params),
      getExercisePreference(params),
      getCoachWorkload(),
      getHourlyActivity(params)
    ])

    const dashboardData = dashboardResult.status === 'fulfilled' ? dashboardResult.value : {}
    const peakData = peakResult.status === 'fulfilled' ? peakResult.value : {}
    const equipmentData = equipmentResult.status === 'fulfilled' ? equipmentResult.value : {}
    const preferenceData = preferenceResult.status === 'fulfilled' ? preferenceResult.value : {}
    const workloadData = workloadResult.status === 'fulfilled' ? workloadResult.value : []
    const hourlyResultData = hourlyResult.status === 'fulfilled' ? hourlyResult.value : {}

    const effectiveRange = resolveEffectiveRange(
      dashboardData,
      peakData,
      equipmentData,
      preferenceData,
      hourlyResultData
    )
    resolvedRange.startDate = effectiveRange.startDate || startDate
    resolvedRange.endDate = effectiveRange.endDate || endDate
    resolvedRange.fallbackApplied = effectiveRange.fallbackApplied

    stats.totalUsers = Number(dashboardData?.totalUsers || 0)
    stats.activeUsers = Number(dashboardData?.activeUsers || 0)
    stats.totalDuration = Number(dashboardData?.totalDurationMinutes || 0)
    stats.totalCalories = Math.round(Number(dashboardData?.totalCaloriesBurned || 0))

    peakWarning.currentCount = Number(peakData?.currentCount || 0)
    peakWarning.threshold = Number(peakData?.threshold || 50)
    peakWarning.peakCount = Number(peakData?.peakCount || 0)
    const peakHour = Number(peakData?.peakHour)
    if (Number.isFinite(peakHour)) {
      peakWarning.peakHours = `${String(peakHour).padStart(2, '0')}:00-${String(peakHour).padStart(2, '0')}:59`
    } else {
      peakWarning.peakHours = '--:00- --:59'
    }

    if (peakData?.isPeakHour) {
      peakWarning.level = 'crowded'
      peakWarning.statusText = '当前拥挤'
    } else if (peakWarning.currentCount >= Math.floor(peakWarning.threshold * 0.7)) {
      peakWarning.level = 'busy'
      peakWarning.statusText = '当前较忙'
    } else {
      peakWarning.level = 'normal'
      peakWarning.statusText = '当前空闲'
    }

    exercisePreferenceData.value = preferenceData?.preferences || []
    equipmentUsageRows.value = Object.entries(equipmentData?.equipmentUsage || {})
      .map(([name, count]) => ({ name, count: Number(count || 0) }))
      .sort((left, right) => right.count - left.count)
    coachWorkload.value = (workloadData || []).map((item) => ({
      ...item,
      avgProgress: Number(item.avgProgress || 0)
    }))
    hourlyData.value = toHourlySeries(hourlyResultData?.hourlyData || [])
    userGrowthSeries.value = buildUserGrowthSeries(stats.totalUsers, stats.activeUsers)

    await loadHeatmapByRange(params)
    loadingStates.summary = false
    loadingStates.heatmap = false
    loadingStates.charts = false
    loadingStates.workload = false
    await nextTick()
    initCharts()
    resizeCharts()
    setTimeout(resizeCharts, 200)
  } catch (error) {
    loadingStates.summary = false
    loadingStates.heatmap = false
    loadingStates.charts = false
    loadingStates.workload = false
    ElMessage.error('加载监控数据失败')
  }
}

const loadHeatmapByRange = async (params) => {
  try {
    const result = await getHourlyHeatmap(params)
    const rawLabels = result?.dayLabels || []
    const rawPoints = result?.points || []

    if (!rawLabels.length) {
      heatmapDayLabels.value = []
      heatmapData.value = []
      return
    }

    heatmapDayLabels.value = rawLabels.map((label) => label.slice(5))
    heatmapData.value = rawPoints.map((point) => [
      Number(point.hour || 0),
      Number(point.dayIndex || 0),
      Number(point.count || 0)
    ])
  } catch {
    heatmapDayLabels.value = []
    heatmapData.value = []
  }
}

const buildUserGrowthSeries = (totalUsers, activeUsers) => {
  const points = 6
  const safeTotal = Math.max(0, Number(totalUsers || 0))
  const safeActive = Math.max(0, Number(activeUsers || 0))
  const estimatedStart = Math.max(0, safeTotal - safeActive)
  const step = points > 1 ? (safeTotal - estimatedStart) / (points - 1) : safeTotal

  return Array.from({ length: points }, (_, index) => Math.round(estimatedStart + step * index))
}

const resizeCharts = () => {
  chartInstances.forEach((chart) => chart.resize())
}

const getOrCreateChart = (refEl) => {
  if (!refEl) return null
  const existing = echarts.getInstanceByDom(refEl)
  const chart = existing || initChart(refEl)
  if (!chartInstances.includes(chart)) {
    chartInstances.push(chart)
  }
  return chart
}

const initCharts = () => {
  initUserGrowthChart()
  initExerciseTypeChart()
  initPeakHourChart()
  initEquipmentUsageChart()
  initHeatmapChart()
}

const initUserGrowthChart = () => {
  if (!userGrowthChartRef.value) return
  const chart = getOrCreateChart(userGrowthChartRef.value)
  if (!chart) return

  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['T-5', 'T-4', 'T-3', 'T-2', 'T-1', '今日'] },
    yAxis: { type: 'value', name: '用户数' },
    series: [{
      data: userGrowthSeries.value,
      type: 'line',
      smooth: true,
      itemStyle: { color: CHART_COLORS[0] },
      areaStyle: { color: 'rgba(74, 144, 255, 0.2)' }
    }]
  })
}

const initExerciseTypeChart = () => {
  if (!exerciseTypeChartRef.value) return
  const chart = getOrCreateChart(exerciseTypeChartRef.value)
  if (!chart) return

  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: exercisePreferenceData.value.map((item) => ({
        value: Number(item.count || 0),
        name: item.exerciseType || '未分类'
      }))
    }]
  })
}

const initPeakHourChart = () => {
  if (!peakHourChartRef.value) return
  const chart = getOrCreateChart(peakHourChartRef.value)
  if (!chart) return

  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: hourlyData.value.map((item) => `${item.hour}:00`) },
    yAxis: { type: 'value', name: '人数' },
    series: [{
      type: 'bar',
      data: hourlyData.value.map((item) => item.count),
      itemStyle: { color: CHART_COLORS[1] }
    }]
  })
}

const initEquipmentUsageChart = () => {
  if (!equipmentUsageChartRef.value) return
  const chart = getOrCreateChart(equipmentUsageChartRef.value)
  if (!chart) return

  const topRows = equipmentUsageRows.value.slice(0, 10)
  const maxCount = Math.max(...topRows.map((item) => item.count), 1)

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', max: 100, name: '使用率(%)' },
    yAxis: { type: 'category', data: topRows.map((item) => item.name) },
    series: [{
      type: 'bar',
      data: topRows.map((item) => Math.round((item.count / maxCount) * 100)),
      itemStyle: {
        color: (params) => CHART_COLORS[params.dataIndex % CHART_COLORS.length]
      },
      label: { show: true, position: 'right', formatter: '{c}%' }
    }]
  })
}

const initHeatmapChart = () => {
  if (!heatmapChartRef.value) return
  const chart = getOrCreateChart(heatmapChartRef.value)
  if (!chart) return

  const maxValue = Math.max(...heatmapData.value.map((item) => item[2]), 1)

  chart.setOption({
    tooltip: {
      position: 'top',
      formatter: (params) => {
        const [hourIndex, dayIndex, value] = params.value || [0, 0, 0]
        return `${heatmapDayLabels.value[dayIndex] || ''} ${hours[hourIndex] || '0:00'}<br/>活跃人数: <b>${value || 0}</b> 人`
      }
    },
    grid: { height: '70%', top: '5%', left: '10%', right: '6%' },
    xAxis: {
      type: 'category',
      data: hours,
      splitArea: { show: true },
      axisLabel: { interval: 1, fontSize: 10 }
    },
    yAxis: {
      type: 'category',
      data: heatmapDayLabels.value,
      splitArea: { show: true }
    },
    visualMap: {
      min: 0,
      max: maxValue,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      inRange: { color: HEATMAP_GRADIENT },
      text: ['拥挤', '空闲'],
      textStyle: { fontSize: 11 }
    },
    series: [{
      name: '活跃人数',
      type: 'heatmap',
      data: heatmapData.value,
      label: { show: false },
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.5)' }
      }
    }]
  })
}
</script>

<style scoped>
.monitoring-page h2 {
  margin-bottom: 20px;
}

.range-tip {
  font-size: 13px;
  color: #606266;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-right: 15px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.card-header-warning {
  display: flex;
  align-items: center;
  gap: 8px;
}

.peak-warning-content {
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
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.1), rgba(103, 194, 58, 0.05));
}

.current-status.busy {
  background: linear-gradient(135deg, rgba(230, 162, 60, 0.1), rgba(230, 162, 60, 0.05));
}

.current-status.crowded {
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.1), rgba(245, 108, 108, 0.05));
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 12px;
}

.current-status.normal .status-indicator {
  background: #67c23a;
}

.current-status.busy .status-indicator {
  background: #e6a23c;
}

.current-status.crowded .status-indicator {
  background: #f56c6c;
}

.status-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.status-count {
  font-size: 13px;
  color: #606266;
  margin-top: 4px;
}

.peak-info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.peak-info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.peak-info-item .label {
  color: #606266;
  font-size: 13px;
}

.peak-info-item .value {
  color: #303133;
  font-weight: 600;
}
</style>

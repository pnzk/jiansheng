<template>
  <div class="analytics-page">
    <h2>用户行为分析</h2>

    <el-card style="margin-top: 20px">
      <el-form :inline="true">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item label="时段活跃口径">
          <el-radio-group v-model="hourlyRangeType" @change="loadAnalytics">
            <el-radio-button value="today">当天</el-radio-button>
            <el-radio-button value="week">近7天</el-radio-button>
            <el-radio-button value="month">近30天</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAnalytics">查询</el-button>
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
              <div class="stat-value">{{ analytics.activeRate }}%</div>
              <div class="stat-label">区间日均活跃率</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background: #67c23a">
              <el-icon size="30"><TrendCharts /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ analytics.retentionRate }}%</div>
              <div class="stat-label">7日留存率</div>
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
              <div class="stat-value">{{ analytics.avgDuration }}</div>
              <div class="stat-label">平均运动时长(分钟)</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-icon" style="background: #f56c6c">
              <el-icon size="30"><Check /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ analytics.completionRate }}%</div>
              <div class="stat-label">计划完成率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px" v-loading="loadingStates.trends">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>用户活跃趋势</span>
          </template>
          <div ref="activityTrendChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>用户留存分析</span>
          </template>
          <div ref="retentionChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px" v-loading="loadingStates.preference">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>运动偏好分析</span>
          </template>
          <div ref="exercisePreferenceChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>{{ hourlyChartTitle }}</span>
          </template>
          <div ref="hourlyActivityChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px" v-loading="loadingStates.leaderboards">
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="leaderboard-title">
              <el-icon color="#ffd700" size="20"><Trophy /></el-icon>
              <span>运动时长排行榜</span>
            </div>
          </template>
          <el-table :data="leaderboards.duration" :show-header="false" style="width: 100%">
            <el-table-column width="50">
              <template #default="{ $index }">
                <el-tag :type="$index === 0 ? 'danger' : $index === 1 ? 'warning' : 'info'" size="small">
                  {{ $index + 1 }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="userName" label="姓名" />
            <el-table-column prop="value" label="时长" align="right">
              <template #default="{ row }">
                <span class="value-strong">{{ row.value }}分钟</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="leaderboard-title">
              <el-icon color="#ffd700" size="20"><Trophy /></el-icon>
              <span>卡路里消耗排行榜</span>
            </div>
          </template>
          <el-table :data="leaderboards.calories" :show-header="false" style="width: 100%">
            <el-table-column width="50">
              <template #default="{ $index }">
                <el-tag :type="$index === 0 ? 'danger' : $index === 1 ? 'warning' : 'info'" size="small">
                  {{ $index + 1 }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="userName" label="姓名" />
            <el-table-column prop="value" label="卡路里" align="right">
              <template #default="{ row }">
                <span class="value-strong">{{ row.value }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="leaderboard-title">
              <el-icon color="#ffd700" size="20"><Trophy /></el-icon>
              <span>减重效果排行榜</span>
            </div>
          </template>
          <el-table :data="leaderboards.weightLoss" :show-header="false" style="width: 100%">
            <el-table-column width="50">
              <template #default="{ $index }">
                <el-tag :type="$index === 0 ? 'danger' : $index === 1 ? 'warning' : 'info'" size="small">
                  {{ $index + 1 }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="userName" label="姓名" />
            <el-table-column prop="value" label="减重" align="right">
              <template #default="{ row }">
                <span class="value-strong weight-loss">-{{ row.value }}kg</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { CHART_COLORS, initChart } from '@/utils/chartTheme'
import {
  getDashboardStatistics,
  getExercisePreference,
  getHourlyActivity,
  getLeaderboard,
  getUserBehaviorAnalysis
} from '@/api/analytics'

const dateRange = ref([])
const hourlyRangeType = ref('week')
const hourlyChartTitle = ref('时段活跃度分布（近7天）')
const activityTrendChartRef = ref(null)
const retentionChartRef = ref(null)
const exercisePreferenceChartRef = ref(null)
const hourlyActivityChartRef = ref(null)
const chartInstances = []

const analytics = reactive({
  activeRate: 0,
  retentionRate: 0,
  avgDuration: 0,
  completionRate: 0
})

const leaderboards = reactive({
  duration: [],
  calories: [],
  weightLoss: []
})

const behaviorTrend = ref([])
const retentionSeries = ref([])
const preferenceSeries = ref([])
const hourlySeries = ref([])
const resolvedRange = reactive({
  startDate: '',
  endDate: '',
  fallbackApplied: false
})
const loadingStates = reactive({
  summary: false,
  trends: false,
  preference: false,
  leaderboards: false
})

onMounted(async () => {
  await loadAnalytics()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  chartInstances.forEach((chart) => chart.dispose())
  chartInstances.length = 0
})

const loadAnalytics = async () => {
  const queryRange = resolveQueryRange()
  const params = {
    startDate: queryRange.startDate,
    endDate: queryRange.endDate
  }

  try {
    loadingStates.summary = true
    loadingStates.trends = true
    loadingStates.preference = true
    const [
      dashboardResult,
      behaviorResult,
      preferenceResult,
      hourlyResult
    ] = await Promise.allSettled([
      getDashboardStatistics(params),
      getUserBehaviorAnalysis(params),
      getExercisePreference(params),
      getHourlyActivity(params)
    ])
    loadingStates.summary = false
    loadingStates.trends = false
    loadingStates.preference = false

    loadingStates.leaderboards = true
    const [
      durationResult,
      caloriesResult,
      weightLossResult
    ] = await Promise.allSettled([
      getLeaderboard('TOTAL_DURATION', 10, params),
      getLeaderboard('TOTAL_CALORIES', 10, params),
      getLeaderboard('WEIGHT_LOSS', 10, params)
    ])
    loadingStates.leaderboards = false

    const dashboardData = dashboardResult.status === 'fulfilled' ? dashboardResult.value : {}
    const behaviorData = behaviorResult.status === 'fulfilled' ? behaviorResult.value : {}
    const preferenceData = preferenceResult.status === 'fulfilled' ? preferenceResult.value : {}
    const hourlyData = hourlyResult.status === 'fulfilled' ? hourlyResult.value : {}
    const durationData = durationResult.status === 'fulfilled' ? durationResult.value : {}
    const caloriesData = caloriesResult.status === 'fulfilled' ? caloriesResult.value : {}
    const weightLossData = weightLossResult.status === 'fulfilled' ? weightLossResult.value : {}

    const effectiveRange = resolveEffectiveRange(
      queryRange,
      behaviorData,
      preferenceData,
      hourlyData,
      durationData,
      caloriesData,
      weightLossData
    )

    resolvedRange.startDate = effectiveRange.startDate
    resolvedRange.endDate = effectiveRange.endDate
    resolvedRange.fallbackApplied = effectiveRange.fallbackApplied
    hourlyChartTitle.value = getHourlyTitle(effectiveRange)

    analytics.activeRate = Math.round(Number(behaviorData?.averageActiveRate || 0))
    analytics.avgDuration = Math.round(Number(behaviorData?.averageDurationMinutes || 0))
    analytics.retentionRate = getPrimaryRetentionRate(behaviorData?.retentionRates || [])

    leaderboards.duration = normalizeLeaderboardEntries(durationData)
    leaderboards.calories = normalizeLeaderboardEntries(caloriesData)
    leaderboards.weightLoss = normalizeLeaderboardEntries(weightLossData)
    analytics.completionRate = Math.round(Number(behaviorData?.averagePlanCompletionRate || 0))

    behaviorTrend.value = buildBehaviorTrend(behaviorData?.dailyActivity || [])
    retentionSeries.value = buildRetentionSeries(behaviorData?.retentionRates || [])
    preferenceSeries.value = (preferenceData?.preferences || []).map((item) => ({
      name: item.exerciseType || '未分类',
      value: Number(item.count || 0)
    }))
    hourlySeries.value = (hourlyData?.hourlyData || []).map((item) => ({
      hour: Number(item.hour),
      count: Number(item.count || 0)
    }))

    await nextTick()
    initCharts()
    resizeCharts()
    setTimeout(resizeCharts, 200)
  } catch {
    loadingStates.summary = false
    loadingStates.trends = false
    loadingStates.preference = false
    loadingStates.leaderboards = false
    analytics.activeRate = 0
    analytics.retentionRate = 0
    analytics.avgDuration = 0
    analytics.completionRate = 0
    leaderboards.duration = []
    leaderboards.calories = []
    leaderboards.weightLoss = []
    behaviorTrend.value = []
    retentionSeries.value = []
    preferenceSeries.value = []
    hourlySeries.value = []
    resolvedRange.startDate = queryRange.startDate
    resolvedRange.endDate = queryRange.endDate
    resolvedRange.fallbackApplied = false
    hourlyChartTitle.value = getHourlyTitle(queryRange)

    await nextTick()
    initCharts()
    resizeCharts()
    ElMessage.error('数据分析加载失败')
  }
}

const getHourlyTitle = (queryRange) => {
  if (queryRange.mode === 'custom' || queryRange.fallbackApplied) {
    return `时段活跃度分布（${queryRange.startDate} 至 ${queryRange.endDate}）`
  }
  if (queryRange.mode === 'today') return '时段活跃度分布（当天）'
  if (queryRange.mode === 'month') return '时段活跃度分布（近30天）'
  return '时段活跃度分布（近7天）'
}

const resolveQueryRange = () => {
  const [startDate, endDate] = dateRange.value || []
  if (startDate && endDate) {
    return {
      mode: 'custom',
      startDate: formatDate(startDate),
      endDate: formatDate(endDate)
    }
  }

  const preset = buildHourlyParams(hourlyRangeType.value)
  return {
    mode: hourlyRangeType.value,
    startDate: preset.startDate,
    endDate: preset.endDate
  }
}

const buildHourlyParams = (rangeType) => {
  const today = new Date()
  const todayText = formatDate(today)

  if (rangeType === 'today') {
    return { startDate: todayText, endDate: todayText }
  }

  const days = rangeType === 'month' ? 29 : 6
  const start = new Date(today)
  start.setDate(today.getDate() - days)
  return {
    startDate: formatDate(start),
    endDate: todayText
  }
}

const formatDate = (value) => {
  const date = new Date(value)
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}`
}

const normalizeLeaderboardEntries = (data) => {
  return (data?.entries || []).map((item) => ({
    userName: item.realName || item.username || `用户${item.userId}`,
    value: Number(item.value || 0)
  }))
}

const resolveEffectiveRange = (queryRange, ...responses) => {
  const candidates = responses
    .map((item) => ({
      startDate: item?.periodStart,
      endDate: item?.periodEnd,
      fallbackApplied: Boolean(item?.fallbackApplied)
    }))
    .filter((item) => item.startDate && item.endDate)

  if (!candidates.length) {
    return { ...queryRange, fallbackApplied: false }
  }

  return candidates.find((item) => item.fallbackApplied) || candidates[0]
}

const getPrimaryRetentionRate = (retentionRates) => {
  const day7 = (Array.isArray(retentionRates) ? retentionRates : []).find((item) => Number(item?.days) === 7)
  return Number(day7?.retentionRate || 0).toFixed(0)
}

const buildBehaviorTrend = (dailyActivity) => {
  return (Array.isArray(dailyActivity) ? dailyActivity : []).map((item) => {
    const date = item?.date ? new Date(`${item.date}T00:00:00`) : null
    const label = date && !Number.isNaN(date.getTime())
      ? `${date.getMonth() + 1}/${date.getDate()}`
      : '未知'

    return {
      day: label,
      daily: Number(item?.activeUserCount || 0),
      avgDuration: Number(item?.averageDurationMinutes || 0)
    }
  })
}

const buildRetentionSeries = (retentionRates) => {
  return (Array.isArray(retentionRates) ? retentionRates : [])
    .filter((item) => Number(item?.cohortSize || 0) > 0)
    .map((item) => ({
      label: item?.label || `${item?.days || 0}日`,
      value: Number(item?.retentionRate || 0),
      retainedUsers: Number(item?.retainedUsers || 0),
      cohortSize: Number(item?.cohortSize || 0)
    }))
}

const resizeCharts = () => {
  chartInstances.forEach((chart) => chart.resize())
}

const getOrCreateChart = (domRef) => {
  if (!domRef) return null
  const existing = echarts.getInstanceByDom(domRef)
  const chart = existing || initChart(domRef)
  if (!chartInstances.includes(chart)) {
    chartInstances.push(chart)
  }
  return chart
}

const initCharts = () => {
  initActivityTrendChart()
  initRetentionChart()
  initExercisePreferenceChart()
  initHourlyActivityChart()
}

const initActivityTrendChart = () => {
  if (!activityTrendChartRef.value) return
  const chart = getOrCreateChart(activityTrendChartRef.value)
  if (!chart) return

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['日活跃用户', '日均时长'] },
    xAxis: { type: 'category', data: behaviorTrend.value.map((item) => item.day) },
    yAxis: [
      { type: 'value', name: '人数' },
      { type: 'value', name: '分钟' }
    ],
    series: [
      {
        name: '日活跃用户',
        type: 'line',
        smooth: true,
        data: behaviorTrend.value.map((item) => item.daily),
        itemStyle: { color: CHART_COLORS[0] }
      },
      {
        name: '日均时长',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: behaviorTrend.value.map((item) => item.avgDuration),
        itemStyle: { color: CHART_COLORS[1] }
      }
    ]
  })
}

const initRetentionChart = () => {
  if (!retentionChartRef.value) return
  const chart = getOrCreateChart(retentionChartRef.value)
  if (!chart) return

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const current = params?.[0]
        if (!current) return ''
        const data = retentionSeries.value[current.dataIndex] || {}
        return `${data.label}<br/>留存率: <b>${Number(data.value || 0).toFixed(2)}%</b><br/>留存用户: ${data.retainedUsers || 0}/${data.cohortSize || 0}`
      }
    },
    xAxis: { type: 'category', data: retentionSeries.value.map((item) => item.label) },
    yAxis: { type: 'value', name: '留存率(%)', max: 100 },
    series: [{
      type: 'bar',
      data: retentionSeries.value.map((item) => item.value),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: CHART_COLORS[0] },
          { offset: 1, color: CHART_COLORS[1] }
        ])
      }
    }],
    graphic: retentionSeries.value.length
      ? []
      : [{
          type: 'text',
          left: 'center',
          top: 'middle',
          style: {
            text: '当前时间范围暂无可计算的留存周期',
            fill: '#909399',
            fontSize: 14
          }
        }]
  })
}

const initExercisePreferenceChart = () => {
  if (!exercisePreferenceChartRef.value) return
  const chart = getOrCreateChart(exercisePreferenceChartRef.value)
  if (!chart) return

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}次 ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      data: preferenceSeries.value
    }]
  })
}

const initHourlyActivityChart = () => {
  if (!hourlyActivityChartRef.value) return
  const chart = getOrCreateChart(hourlyActivityChartRef.value)
  if (!chart) return

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: hourlySeries.value.map((item) => `${item.hour}:00`) },
    yAxis: { type: 'value', name: '活跃人数' },
    series: [{
      type: 'bar',
      data: hourlySeries.value.map((item) => item.count),
      itemStyle: {
        color: (params) => CHART_COLORS[params.dataIndex % CHART_COLORS.length]
      }
    }]
  })
}
</script>

<style scoped>
.analytics-page h2 {
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

.leaderboard-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.value-strong {
  font-weight: bold;
}

.weight-loss {
  color: #67c23a;
}
</style>

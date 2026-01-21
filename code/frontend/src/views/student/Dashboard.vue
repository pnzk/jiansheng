<template>
  <div class="dashboard">
    <h2>个人健身仪表盘</h2>
    
    <!-- 三个核心数值卡片 -->
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

    <!-- 高峰期预警 + 本周运动概况 -->
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
                <el-progress :percentage="weekStats.exerciseCount / 7 * 100" :show-text="false" />
              </div>
            </el-col>
            <el-col :span="8">
              <div class="week-stat">
                <div class="week-stat-value">{{ weekStats.totalDuration }}<span class="small-unit">分钟</span></div>
                <div class="week-stat-label">运动时长</div>
                <el-progress :percentage="Math.min(weekStats.totalDuration / 300 * 100, 100)" :show-text="false" status="success" />
              </div>
            </el-col>
            <el-col :span="8">
              <div class="week-stat">
                <div class="week-stat-value">{{ weekStats.totalCalories }}<span class="small-unit">kcal</span></div>
                <div class="week-stat-label">消耗卡路里</div>
                <el-progress :percentage="Math.min(weekStats.totalCalories / 3000 * 100, 100)" :show-text="false" status="warning" />
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- 体重变化图 + 训练计划提醒 -->
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
              <el-tag :type="getGoalTagType(trainingPlan.goalType)">{{ trainingPlan.goalType }}</el-tag>
            </div>
            <el-divider />
            <div class="today-plan">
              <div class="today-title">今日训练安排</div>
              <div class="today-items" v-if="todaySchedule.length">
                <div class="today-item" v-for="(item, index) in todaySchedule" :key="index">
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
              <el-progress :percentage="trainingPlan.completionRate" :stroke-width="12" />
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

    <!-- 运动类型分布 -->
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
            <span>本周运动热力图</span>
          </template>
          <div ref="heatmapChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import * as echarts from 'echarts'
import { getLatestBodyMetric, getBodyMetricHistory } from '@/api/bodyMetric'
import { getExerciseStatistics } from '@/api/exercise'
import { getMyTrainingPlan } from '@/api/trainingPlan'
import { getPeakHourWarning } from '@/api/analytics'

const weightChartRef = ref(null)
const exerciseChartRef = ref(null)
const heatmapChartRef = ref(null)

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
  peakHours: '18:00-20:00',
  suggestedHours: '10:00-12:00'
})

const trainingPlan = ref(null)

const todaySchedule = computed(() => {
  if (!trainingPlan.value?.weeklySchedule) return []
  const days = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday']
  const today = days[new Date().getDay()]
  return trainingPlan.value.weeklySchedule[today] || []
})

const getBmiStatus = (bmi) => {
  if (bmi < 18.5) return { text: '偏瘦', class: 'warning' }
  if (bmi < 24) return { text: '正常', class: 'positive' }
  if (bmi < 28) return { text: '偏胖', class: 'warning' }
  return { text: '肥胖', class: 'negative' }
}

const getGoalTagType = (goalType) => {
  const types = { '减重': 'danger', '减脂': 'warning', '增肌': 'success' }
  return types[goalType] || 'info'
}

const loadData = async () => {
  // 加载最新身体指标
  try {
    const latestMetric = await getLatestBodyMetric()
    if (latestMetric) {
      stats.weight = latestMetric.weightKg || 70
      stats.bodyFat = latestMetric.bodyFatPercentage || 20
      stats.bmi = latestMetric.bmi || 22
    }
  } catch (e) {
    stats.weight = 70
    stats.bodyFat = 20
    stats.bmi = 22
  }
  
  // 加载运动统计
  try {
    const exerciseStats = await getExerciseStatistics()
    if (exerciseStats) {
      weekStats.exerciseCount = exerciseStats.weekExerciseCount || 5
      weekStats.totalDuration = exerciseStats.weekTotalDuration || 180
      weekStats.totalCalories = exerciseStats.weekTotalCalories || 1500
    }
  } catch (e) {
    weekStats.exerciseCount = 5
    weekStats.totalDuration = 180
    weekStats.totalCalories = 1500
  }
  
  // 加载训练计划
  try {
    const plan = await getMyTrainingPlan()
    trainingPlan.value = plan
  } catch (e) {
    trainingPlan.value = null
  }
  
  // 加载高峰期预警
  try {
    const warning = await getPeakHourWarning()
    if (warning) {
      Object.assign(peakWarning, warning)
    }
  } catch (e) {
    // 使用默认值
  }
  
  // 加载体重历史并渲染图表
  try {
    const history = await getBodyMetricHistory()
    renderWeightChart(history)
  } catch (e) {
    renderWeightChart(null)
  }
}

onMounted(() => {
  loadData()
  initExerciseChart()
  initHeatmapChart()
})

const renderWeightChart = (history) => {
  if (!weightChartRef.value) return
  const chart = echarts.init(weightChartRef.value)
  
  const dates = history?.map(m => m.measurementDate) || ['1月', '2月', '3月', '4月', '5月', '6月']
  const weights = history?.map(m => m.weightKg) || [70, 68, 67, 66.5, 66, 65.5]
  
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
      itemStyle: { color: '#409eff' },
      lineStyle: { width: 3 }
    }]
  })
}

const initExerciseChart = () => {
  if (!exerciseChartRef.value) return
  const chart = echarts.init(exerciseChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}次 ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: {
        label: { show: true, fontSize: 20, fontWeight: 'bold' }
      },
      labelLine: { show: false },
      data: [
        { value: 5, name: '跑步', itemStyle: { color: '#409eff' } },
        { value: 3, name: '动感单车', itemStyle: { color: '#67c23a' } },
        { value: 2, name: '游泳', itemStyle: { color: '#e6a23c' } },
        { value: 2, name: '力量训练', itemStyle: { color: '#f56c6c' } },
        { value: 1, name: '瑜伽', itemStyle: { color: '#909399' } }
      ]
    }]
  })
}

const initHeatmapChart = () => {
  if (!heatmapChartRef.value) return
  const chart = echarts.init(heatmapChartRef.value)
  
  const hours = ['6:00', '8:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00']
  const days = ['周日', '周六', '周五', '周四', '周三', '周二', '周一']
  
  // 模拟热力图数据
  const data = []
  for (let i = 0; i < days.length; i++) {
    for (let j = 0; j < hours.length; j++) {
      data.push([j, i, Math.floor(Math.random() * 60)])
    }
  }
  
  chart.setOption({
    tooltip: { position: 'top', formatter: (p) => `${days[p.value[1]]} ${hours[p.value[0]]}: ${p.value[2]}分钟` },
    grid: { height: '60%', top: '10%' },
    xAxis: { type: 'category', data: hours, splitArea: { show: true } },
    yAxis: { type: 'category', data: days, splitArea: { show: true } },
    visualMap: {
      min: 0, max: 60, calculable: true, orient: 'horizontal', left: 'center', bottom: '5%',
      inRange: { color: ['#ebeef5', '#409eff', '#67c23a', '#e6a23c', '#f56c6c'] }
    },
    series: [{
      name: '运动时长',
      type: 'heatmap',
      data: data,
      label: { show: false },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.5)' } }
    }]
  })
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

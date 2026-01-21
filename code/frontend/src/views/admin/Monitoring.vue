<template>
  <div class="monitoring-page">
    <h2>健身房全局监控</h2>

    <!-- 关键指标卡片 -->
    <el-row :gutter="20" style="margin-top: 20px">
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
              <div class="stat-value">{{ stats.weekDuration }}</div>
              <div class="stat-label">本周总时长(分钟)</div>
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
              <div class="stat-value">{{ stats.weekCalories }}</div>
              <div class="stat-label">本周总卡路里</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 高峰期拥堵预警 -->
    <el-row :gutter="20" style="margin-top: 20px">
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
                <div class="status-count">当前在馆: {{ peakWarning.currentCount }} 人</div>
              </div>
            </div>
            <el-divider />
            <div class="peak-info-list">
              <div class="peak-info-item">
                <span class="label">今日高峰时段</span>
                <el-tag type="danger">{{ peakWarning.peakHours }}</el-tag>
              </div>
              <div class="peak-info-item">
                <span class="label">预计高峰人数</span>
                <span class="value">{{ peakWarning.peakCount }} 人</span>
              </div>
              <div class="peak-info-item">
                <span class="label">建议运动时段</span>
                <el-tag type="success">{{ peakWarning.suggestedHours }}</el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>各时段在线人数热力图</span>
          </template>
          <div ref="heatmapChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" style="margin-top: 20px">
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

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>高峰期时段分析</span>
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

    <!-- 教练工作量统计 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span>教练工作量统计</span>
      </template>
      <el-table :data="coachWorkload" style="width: 100%">
        <el-table-column prop="coachName" label="教练姓名" width="150" />
        <el-table-column prop="studentCount" label="负责学员数" width="150" />
        <el-table-column prop="planCount" label="创建计划数" width="150" />
        <el-table-column prop="activeStudents" label="活跃学员数" width="150" />
        <el-table-column prop="avgProgress" label="平均完成率" width="150">
          <template #default="{ row }">
            <el-progress :percentage="row.avgProgress" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getDashboardStatistics, getUserBehaviorAnalysis, getPeakHourWarning, getEquipmentUsage } from '@/api/analytics'

const userGrowthChartRef = ref(null)
const exerciseTypeChartRef = ref(null)
const peakHourChartRef = ref(null)
const equipmentUsageChartRef = ref(null)
const heatmapChartRef = ref(null)

const stats = reactive({
  totalUsers: 0,
  activeUsers: 0,
  weekDuration: 0,
  weekCalories: 0
})

const peakWarning = reactive({
  level: 'normal',
  statusText: '当前空闲',
  currentCount: 45,
  peakHours: '18:00-20:00',
  peakCount: 120,
  suggestedHours: '10:00-12:00'
})

const coachWorkload = ref([])

onMounted(async () => {
  await loadData()
  initCharts()
  initHeatmapChart()
})

const loadData = async () => {
  try {
    const dashboardData = await getDashboardStatistics()
    stats.totalUsers = dashboardData.totalUsers || 0
    stats.activeUsers = dashboardData.activeUsers || 0
    stats.weekDuration = dashboardData.totalDuration || 0
    stats.weekCalories = dashboardData.totalCalories || 0

    // 模拟教练工作量数据
    coachWorkload.value = [
      { coachName: '王教练', studentCount: 25, planCount: 18, activeStudents: 20, avgProgress: 75 },
      { coachName: '李教练', studentCount: 30, planCount: 22, activeStudents: 25, avgProgress: 68 },
      { coachName: '张教练', studentCount: 20, planCount: 15, activeStudents: 18, avgProgress: 82 }
    ]
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const initCharts = () => {
  // 用户增长趋势图
  const userGrowthChart = echarts.init(userGrowthChartRef.value)
  userGrowthChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
    yAxis: { type: 'value', name: '用户数' },
    series: [{
      data: [450, 480, 500, 520, 540, 560],
      type: 'line',
      smooth: true,
      itemStyle: { color: '#409eff' },
      areaStyle: { color: 'rgba(64, 158, 255, 0.2)' }
    }]
  })

  // 运动类型分布图
  const exerciseTypeChart = echarts.init(exerciseTypeChartRef.value)
  exerciseTypeChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: [
        { value: 35, name: '跑步' },
        { value: 25, name: '动感单车' },
        { value: 20, name: '力量训练' },
        { value: 15, name: '游泳' },
        { value: 5, name: '其他' }
      ]
    }]
  })

  // 高峰期分析图
  const peakHourChart = echarts.init(peakHourChartRef.value)
  peakHourChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['6:00', '8:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00']
    },
    yAxis: { type: 'value', name: '人数' },
    series: [{
      data: [20, 45, 30, 25, 35, 40, 80, 90, 50],
      type: 'bar',
      itemStyle: { color: '#67c23a' }
    }]
  })

  // 器材使用率图
  const equipmentUsageChart = echarts.init(equipmentUsageChartRef.value)
  equipmentUsageChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', max: 100, name: '使用率(%)' },
    yAxis: {
      type: 'category',
      data: ['跑步机', '动感单车', '椭圆机', '划船机', '哑铃', '杠铃', '史密斯机']
    },
    series: [{
      type: 'bar',
      data: [85, 75, 60, 45, 90, 70, 55],
      itemStyle: {
        color: (params) => {
          const colors = ['#f56c6c', '#e6a23c', '#67c23a']
          if (params.value >= 80) return colors[0]
          if (params.value >= 60) return colors[1]
          return colors[2]
        }
      },
      label: { show: true, position: 'right', formatter: '{c}%' }
    }]
  })
}

const initHeatmapChart = () => {
  if (!heatmapChartRef.value) return
  const chart = echarts.init(heatmapChartRef.value)
  
  const hours = ['6:00', '7:00', '8:00', '9:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00', '21:00', '22:00']
  const days = ['周日', '周六', '周五', '周四', '周三', '周二', '周一']
  
  // 生成热力图数据
  const data = []
  for (let i = 0; i < days.length; i++) {
    for (let j = 0; j < hours.length; j++) {
      // 模拟真实的健身房人流规律
      let value = 20
      if (j >= 11 && j <= 13) value = 60 + Math.random() * 30 // 晚高峰
      else if (j >= 5 && j <= 7) value = 40 + Math.random() * 20 // 早高峰
      else if (j >= 3 && j <= 5) value = 30 + Math.random() * 15 // 午间
      else value = 15 + Math.random() * 20
      
      // 周末人流更多
      if (i <= 1) value *= 1.2
      
      data.push([j, i, Math.round(value)])
    }
  }
  
  chart.setOption({
    tooltip: {
      position: 'top',
      formatter: (params) => `${days[params.value[1]]} ${hours[params.value[0]]}<br/>在馆人数: <b>${params.value[2]}</b> 人`
    },
    grid: { height: '70%', top: '5%', left: '15%', right: '10%' },
    xAxis: {
      type: 'category',
      data: hours,
      splitArea: { show: true },
      axisLabel: { interval: 1, fontSize: 10 }
    },
    yAxis: {
      type: 'category',
      data: days,
      splitArea: { show: true }
    },
    visualMap: {
      min: 0,
      max: 100,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      inRange: {
        color: ['#ebeef5', '#b3d8ff', '#409eff', '#e6a23c', '#f56c6c']
      },
      text: ['拥挤', '空闲'],
      textStyle: { fontSize: 11 }
    },
    series: [{
      name: '在馆人数',
      type: 'heatmap',
      data: data,
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
  animation: pulse 2s infinite;
}

.current-status.normal .status-indicator {
  background: #67c23a;
  box-shadow: 0 0 8px #67c23a;
}

.current-status.busy .status-indicator {
  background: #e6a23c;
  box-shadow: 0 0 8px #e6a23c;
}

.current-status.crowded .status-indicator {
  background: #f56c6c;
  box-shadow: 0 0 8px #f56c6c;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-info {
  flex: 1;
}

.status-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.status-count {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.peak-info-list {
  padding: 10px 0;
}

.peak-info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.peak-info-item .label {
  color: #606266;
  font-size: 13px;
}

.peak-info-item .value {
  font-weight: bold;
  color: #303133;
}
</style>

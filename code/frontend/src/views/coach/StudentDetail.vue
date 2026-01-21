<template>
  <div class="student-detail">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="page-title">学员详细分析</span>
      </template>
    </el-page-header>

    <!-- 学员基本信息 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span>基本信息</span>
      </template>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="姓名">{{ studentInfo.realName }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ studentInfo.age }}岁</el-descriptions-item>
        <el-descriptions-item label="性别">{{ studentInfo.gender }}</el-descriptions-item>
        <el-descriptions-item label="身高">{{ studentInfo.heightCm }}cm</el-descriptions-item>
        <el-descriptions-item label="健身目标">{{ studentInfo.fitnessGoal }}</el-descriptions-item>
        <el-descriptions-item label="初始体重">{{ studentInfo.initialWeight }}kg</el-descriptions-item>
        <el-descriptions-item label="当前体重">{{ studentInfo.currentWeight }}kg</el-descriptions-item>
        <el-descriptions-item label="体重变化">
          <span :style="{ color: weightChange >= 0 ? '#f56c6c' : '#67c23a' }">
            {{ weightChange >= 0 ? '+' : '' }}{{ weightChange }}kg
          </span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 当前身体指标 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-value">{{ metrics.weight }} kg</div>
            <div class="stat-label">当前体重</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-value">{{ metrics.bodyFat }}%</div>
            <div class="stat-label">体脂率</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-value">{{ metrics.bmi }}</div>
            <div class="stat-label">BMI指数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-card">
            <div class="stat-value">{{ metrics.muscleMass }} kg</div>
            <div class="stat-label">肌肉量</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>体重变化趋势</span>
          </template>
          <div ref="weightChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>体脂率变化趋势</span>
          </template>
          <div ref="bodyFatChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>运动类型分布</span>
          </template>
          <div ref="exerciseTypeChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>运动统计</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="总运动时长">{{ exerciseStats.totalDuration }}分钟</el-descriptions-item>
            <el-descriptions-item label="总卡路里消耗">{{ exerciseStats.totalCalories }}千卡</el-descriptions-item>
            <el-descriptions-item label="平均心率">{{ exerciseStats.avgHeartRate }}次/分</el-descriptions-item>
            <el-descriptions-item label="运动次数">{{ exerciseStats.totalCount }}次</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <!-- 运动记录表格 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span>最近30天运动记录</span>
      </template>
      <el-table :data="exerciseRecords" style="width: 100%">
        <el-table-column prop="exerciseDate" label="日期" width="120" />
        <el-table-column prop="exerciseType" label="运动类型" width="120" />
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="120" />
        <el-table-column prop="caloriesBurned" label="卡路里" width="100" />
        <el-table-column prop="averageHeartRate" label="平均心率" width="100" />
        <el-table-column prop="equipmentUsed" label="器材" />
      </el-table>
    </el-card>

    <!-- 当前训练计划 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>当前训练计划</span>
          <el-button type="primary" size="small" @click="createPlan">创建新计划</el-button>
        </div>
      </template>
      <div v-if="currentPlan">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="计划名称">{{ currentPlan.planName }}</el-descriptions-item>
          <el-descriptions-item label="完成率">{{ currentPlan.completionRate }}%</el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ currentPlan.startDate }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ currentPlan.endDate }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-else description="暂无训练计划" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const studentId = route.params.id

const weightChartRef = ref(null)
const bodyFatChartRef = ref(null)
const exerciseTypeChartRef = ref(null)

const studentInfo = reactive({
  realName: '张三',
  age: 25,
  gender: '男',
  heightCm: 175,
  fitnessGoal: '减脂',
  initialWeight: 80.0,
  currentWeight: 75.5
})

const metrics = reactive({
  weight: 75.5,
  bodyFat: 18.5,
  bmi: 24.7,
  muscleMass: 32.5
})

const exerciseStats = reactive({
  totalDuration: 1250,
  totalCalories: 8500,
  avgHeartRate: 135,
  totalCount: 28
})

const exerciseRecords = ref([])
const currentPlan = ref(null)

const weightChange = computed(() => {
  return (studentInfo.currentWeight - studentInfo.initialWeight).toFixed(1)
})

onMounted(() => {
  loadStudentData()
  initCharts()
})

const loadStudentData = async () => {
  try {
    // TODO: 调用API获取学员数据
    exerciseRecords.value = [
      { exerciseDate: '2024-01-19', exerciseType: '跑步', durationMinutes: 45, caloriesBurned: 450, averageHeartRate: 135, equipmentUsed: '跑步机' },
      { exerciseDate: '2024-01-18', exerciseType: '动感单车', durationMinutes: 30, caloriesBurned: 320, averageHeartRate: 142, equipmentUsed: '动感单车' },
      { exerciseDate: '2024-01-17', exerciseType: '力量训练', durationMinutes: 60, caloriesBurned: 280, averageHeartRate: 125, equipmentUsed: '哑铃' }
    ]

    currentPlan.value = {
      planName: '减脂计划',
      completionRate: 65,
      startDate: '2024-01-01',
      endDate: '2024-03-31'
    }
  } catch (error) {
    ElMessage.error('加载学员数据失败')
  }
}

const initCharts = () => {
  // 体重变化趋势图
  const weightChart = echarts.init(weightChartRef.value)
  weightChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
    yAxis: { type: 'value', name: '体重(kg)' },
    series: [{
      data: [80, 78.5, 77, 76.5, 76, 75.5],
      type: 'line',
      smooth: true,
      itemStyle: { color: '#409eff' }
    }]
  })

  // 体脂率变化趋势图
  const bodyFatChart = echarts.init(bodyFatChartRef.value)
  bodyFatChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
    yAxis: { type: 'value', name: '体脂率(%)' },
    series: [{
      data: [25, 23, 21.5, 20, 19, 18.5],
      type: 'line',
      smooth: true,
      itemStyle: { color: '#67c23a' }
    }]
  })

  // 运动类型分布图
  const exerciseTypeChart = echarts.init(exerciseTypeChartRef.value)
  exerciseTypeChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: [
        { value: 12, name: '跑步' },
        { value: 8, name: '动感单车' },
        { value: 5, name: '力量训练' },
        { value: 3, name: '游泳' }
      ]
    }]
  })
}

const goBack = () => {
  router.back()
}

const createPlan = () => {
  router.push('/coach/plans?studentId=' + studentId)
}
</script>

<style scoped>
.student-detail {
  padding: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: bold;
}

.stat-card {
  text-align: center;
  padding: 10px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}
</style>

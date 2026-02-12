<template>
  <div class="student-detail">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="page-title">学员详细分析</span>
      </template>
    </el-page-header>

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
          <span :style="{ color: Number(weightChange) >= 0 ? '#f56c6c' : '#67c23a' }">
            {{ Number(weightChange) >= 0 ? '+' : '' }}{{ weightChange }}kg
          </span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

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
      <el-empty v-if="exerciseRecords.length === 0" description="暂无运动记录" />
    </el-card>

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
          <el-descriptions-item label="完成率">{{ Math.round(Number(currentPlan.completionRate || 0)) }}%</el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ currentPlan.startDate }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ currentPlan.endDate }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-empty v-else description="暂无训练计划" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getCoachStudents } from '@/api/user'
import { getCoachStudentBodyMetrics, getCoachStudentExerciseRecords } from '@/api/analytics'
import { getStudentTrainingPlan } from '@/api/trainingPlan'
import { CHART_COLORS, initChart } from '@/utils/chartTheme'

const router = useRouter()
const route = useRoute()
const studentId = Number(route.params.id)

const weightChartRef = ref(null)
const bodyFatChartRef = ref(null)
const exerciseTypeChartRef = ref(null)

const studentInfo = reactive({
  realName: '-',
  age: '-',
  gender: '-',
  heightCm: '-',
  fitnessGoal: '-',
  initialWeight: '-',
  currentWeight: '-'
})

const metrics = reactive({
  weight: '-',
  bodyFat: '-',
  bmi: '-',
  muscleMass: '-'
})

const exerciseStats = reactive({
  totalDuration: 0,
  totalCalories: 0,
  avgHeartRate: 0,
  totalCount: 0
})

const exerciseRecords = ref([])
const currentPlan = ref(null)
const bodyMetrics = ref([])

const weightChange = computed(() => {
  const start = Number(studentInfo.initialWeight)
  const current = Number(studentInfo.currentWeight)
  if (!Number.isFinite(start) || !Number.isFinite(current)) {
    return '0.0'
  }
  return (current - start).toFixed(1)
})

onMounted(async () => {
  await loadStudentData()
  initCharts()
})

const loadStudentData = async () => {
  try {
    const [students, metricsData, recordsData] = await Promise.all([
      getCoachStudents(),
      getCoachStudentBodyMetrics({ studentId }),
      getCoachStudentExerciseRecords({ studentId })
    ])

    const student = (students || []).find((item) => Number(item.id) === studentId)
    if (!student) {
      ElMessage.warning('未找到该学员或无访问权限')
      return
    }

    studentInfo.realName = student.realName || student.username || '-'
    studentInfo.age = student.age || '-'
    studentInfo.gender = normalizeGender(student.gender)
    studentInfo.fitnessGoal = normalizeGoal(student.fitnessGoal)

    bodyMetrics.value = (metricsData || []).map((item) => ({
      measurementDate: item.measurementDate,
      weightKg: Number(item.weightKg || 0),
      bodyFatPercentage: Number(item.bodyFatPercentage || 0),
      heightCm: Number(item.heightCm || 0),
      bmi: Number(item.bmi || 0),
      muscleMassKg: Number(item.muscleMassKg || 0)
    }))

    if (bodyMetrics.value.length > 0) {
      const firstMetric = bodyMetrics.value[0]
      const lastMetric = bodyMetrics.value[bodyMetrics.value.length - 1]

      studentInfo.initialWeight = firstMetric.weightKg.toFixed(1)
      studentInfo.currentWeight = lastMetric.weightKg.toFixed(1)
      studentInfo.heightCm = (lastMetric.heightCm || firstMetric.heightCm || 0).toFixed(1)

      metrics.weight = lastMetric.weightKg.toFixed(1)
      metrics.bodyFat = lastMetric.bodyFatPercentage.toFixed(1)
      metrics.bmi = lastMetric.bmi.toFixed(2)
      metrics.muscleMass = lastMetric.muscleMassKg.toFixed(1)
    }

    exerciseRecords.value = (recordsData || []).slice(0, 100).map((item) => ({
      exerciseDate: item.exerciseDate,
      exerciseType: item.exerciseType,
      durationMinutes: item.durationMinutes,
      caloriesBurned: Math.round(Number(item.caloriesBurned || 0)),
      averageHeartRate: item.averageHeartRate || 0,
      equipmentUsed: item.equipmentUsed || '-'
    }))

    exerciseStats.totalCount = exerciseRecords.value.length
    exerciseStats.totalDuration = exerciseRecords.value.reduce((sum, item) => sum + Number(item.durationMinutes || 0), 0)
    exerciseStats.totalCalories = exerciseRecords.value.reduce((sum, item) => sum + Number(item.caloriesBurned || 0), 0)
    exerciseStats.avgHeartRate = exerciseRecords.value.length
      ? Math.round(exerciseRecords.value.reduce((sum, item) => sum + Number(item.averageHeartRate || 0), 0) / exerciseRecords.value.length)
      : 0

    try {
      currentPlan.value = await getStudentTrainingPlan(studentId)
    } catch (error) {
      currentPlan.value = null
    }
  } catch (error) {
    ElMessage.error('加载学员数据失败')
  }
}

const normalizeGender = (gender) => {
  const value = `${gender || ''}`.toUpperCase()
  if (value === 'FEMALE' || gender === '女') {
    return '女'
  }
  if (value === 'MALE' || gender === '男') {
    return '男'
  }
  return '-'
}

const normalizeGoal = (goal) => {
  const value = `${goal || ''}`.toUpperCase()
  const mapping = {
    WEIGHT_LOSS: '减重',
    FAT_LOSS: '减脂',
    MUSCLE_GAIN: '增肌'
  }
  return mapping[value] || goal || '-'
}

const initCharts = () => {
  initWeightChart()
  initBodyFatChart()
  initExerciseTypeChart()
}

const initWeightChart = () => {
  if (!weightChartRef.value) return
  const chart = initChart(weightChartRef.value)

  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: bodyMetrics.value.map((item) => item.measurementDate)
    },
    yAxis: { type: 'value', name: '体重(kg)' },
    series: [{
      data: bodyMetrics.value.map((item) => item.weightKg),
      type: 'line',
      smooth: true,
      itemStyle: { color: CHART_COLORS[0] }
    }]
  })
}

const initBodyFatChart = () => {
  if (!bodyFatChartRef.value) return
  const chart = initChart(bodyFatChartRef.value)

  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: bodyMetrics.value.map((item) => item.measurementDate)
    },
    yAxis: { type: 'value', name: '体脂率(%)' },
    series: [{
      data: bodyMetrics.value.map((item) => item.bodyFatPercentage),
      type: 'line',
      smooth: true,
      itemStyle: { color: CHART_COLORS[1] }
    }]
  })
}

const initExerciseTypeChart = () => {
  if (!exerciseTypeChartRef.value) return
  const chart = initChart(exerciseTypeChartRef.value)

  const typeCountMap = exerciseRecords.value.reduce((map, item) => {
    const key = item.exerciseType || '未分类'
    map[key] = (map[key] || 0) + 1
    return map
  }, {})

  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: Object.entries(typeCountMap).map(([name, value]) => ({ name, value }))
    }]
  })
}

const goBack = () => {
  router.back()
}

const createPlan = () => {
  router.push(`/coach/plans?studentId=${studentId}`)
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

<template>
  <div class="progress-page">
    <h2>健身效果追踪</h2>
    
    <el-card class="filter-card">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        @change="loadData"
      />
      <el-button type="primary" @click="showAddDialog = true" style="margin-left: 10px">
        添加身体指标
      </el-button>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="chart-card">
          <div ref="weightChartRef" style="width: 100%; height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <div ref="bodyFatChartRef" style="width: 100%; height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="stats-card">
      <h3>变化统计</h3>
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="stat-item">
            <div class="stat-label">体重变化</div>
            <div class="stat-value" :class="{ positive: analysis.weightChange > 0, negative: analysis.weightChange < 0 }">
              {{ analysis.weightChange > 0 ? '+' : '' }}{{ analysis.weightChange?.toFixed(2) || 0 }} kg
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-item">
            <div class="stat-label">体脂率变化</div>
            <div class="stat-value" :class="{ positive: analysis.bodyFatChange > 0, negative: analysis.bodyFatChange < 0 }">
              {{ analysis.bodyFatChange > 0 ? '+' : '' }}{{ analysis.bodyFatChange?.toFixed(2) || 0 }} %
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-item">
            <div class="stat-label">BMI变化</div>
            <div class="stat-value" :class="{ positive: analysis.bmiChange > 0, negative: analysis.bmiChange < 0 }">
              {{ analysis.bmiChange > 0 ? '+' : '' }}{{ analysis.bmiChange?.toFixed(2) || 0 }}
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog v-model="showAddDialog" title="添加身体指标" width="500px">
      <el-form :model="addForm" label-width="120px">
        <el-form-item label="测量日期">
          <el-date-picker v-model="addForm.measurementDate" type="date" />
        </el-form-item>
        <el-form-item label="体重(kg)">
          <el-input-number v-model="addForm.weightKg" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="体脂率(%)">
          <el-input-number v-model="addForm.bodyFatPercentage" :min="0" :max="100" :precision="2" />
        </el-form-item>
        <el-form-item label="身高(cm)">
          <el-input-number v-model="addForm.heightCm" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="肌肉量(kg)">
          <el-input-number v-model="addForm.muscleMassKg" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addMetric">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getBodyMetricHistory, addBodyMetric } from '@/api/bodyMetric'
import { getFitnessEffectAnalysis } from '@/api/analytics'
import { initChart } from '@/utils/chartTheme'

const dateRange = ref([])
const showAddDialog = ref(false)
const analysis = ref({})
const weightChartRef = ref(null)
const bodyFatChartRef = ref(null)
const addingMetric = ref(false)
let weightChart = null
let bodyFatChart = null

const addForm = ref({
  measurementDate: new Date(),
  weightKg: 70,
  bodyFatPercentage: 20,
  heightCm: 170,
  muscleMassKg: 50
})

const loadData = async () => {
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0].toISOString().split('T')[0]
      params.endDate = dateRange.value[1].toISOString().split('T')[0]
    }
    
    const metrics = await getBodyMetricHistory(params)
    const analysisData = await getFitnessEffectAnalysis(params)
    analysis.value = analysisData
    
    renderCharts(metrics)
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const addMetric = async () => {
  if (addingMetric.value) {
    return
  }

  addingMetric.value = true
  try {
    const payload = {
      ...addForm.value,
      measurementDate: formatDateValue(addForm.value.measurementDate)
    }

    await addBodyMetric(payload)
    ElMessage.success('添加成功')
    showAddDialog.value = false
    await loadData()
  } catch (error) {
    ElMessage.error('添加失败')
  } finally {
    addingMetric.value = false
  }
}

const renderCharts = (metrics) => {
  if (!weightChart) {
    weightChart = initChart(weightChartRef.value)
  }
  if (!bodyFatChart) {
    bodyFatChart = initChart(bodyFatChartRef.value)
  }
  
  const sortedMetrics = [...(metrics || [])].sort((left, right) => {
    const leftDate = new Date(left.measurementDate).getTime()
    const rightDate = new Date(right.measurementDate).getTime()
    return leftDate - rightDate
  })

  const dates = sortedMetrics.map(m => m.measurementDate)
  const weights = sortedMetrics.map(m => m.weightKg)
  const bodyFats = sortedMetrics.map(m => m.bodyFatPercentage)
  
  weightChart.setOption({
    title: { text: '体重变化趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', name: '体重(kg)' },
    series: [{ type: 'line', data: weights, smooth: true }]
  })
  
  bodyFatChart.setOption({
    title: { text: '体脂率变化趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', name: '体脂率(%)' },
    series: [{ type: 'line', data: bodyFats, smooth: true }]
  })
}

onMounted(() => {
  loadData()
})

const formatDateValue = (value) => {
  if (!value) {
    return null
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return null
  }

  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}
</script>

<style scoped>
.progress-page {
  padding: 20px;
}

.filter-card, .chart-card, .stats-card {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 20px;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
}

.stat-value.positive {
  color: #f56c6c;
}

.stat-value.negative {
  color: #67c23a;
}
</style>

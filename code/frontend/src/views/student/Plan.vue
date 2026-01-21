<template>
  <div class="plan-page">
    <h2>我的训练计划</h2>
    
    <el-card v-if="plan" class="plan-card">
      <h3>{{ plan.planName }}</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="目标类型">{{ plan.goalType }}</el-descriptions-item>
        <el-descriptions-item label="目标值">{{ plan.targetValue }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ plan.startDate }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ plan.endDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ plan.status }}</el-descriptions-item>
        <el-descriptions-item label="完成率">{{ plan.completionRate }}%</el-descriptions-item>
      </el-descriptions>
      
      <div style="margin-top: 20px">
        <h4>周训练安排</h4>
        <pre>{{ plan.weeklySchedule }}</pre>
      </div>
      
      <div style="margin-top: 20px">
        <h4>计划描述</h4>
        <p>{{ plan.description }}</p>
      </div>
      
      <div ref="chartRef" style="width: 100%; height: 300px; margin-top: 20px"></div>
    </el-card>
    
    <el-empty v-else description="暂无训练计划" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getMyTrainingPlan } from '@/api/trainingPlan'

const plan = ref(null)
const chartRef = ref(null)

const loadPlan = async () => {
  try {
    const data = await getMyTrainingPlan()
    if (data) {
      plan.value = data
      setTimeout(() => renderChart(), 100)
    }
  } catch (error) {
    console.log('加载训练计划失败，可能暂无计划')
    plan.value = null
  }
}

const renderChart = () => {
  if (!chartRef.value || !plan.value) return
  const chart = echarts.init(chartRef.value)
  chart.setOption({
    title: { text: '计划完成率' },
    series: [{
      type: 'gauge',
      detail: { formatter: '{value}%' },
      data: [{ value: plan.value?.completionRate || 0, name: '完成率' }]
    }]
  })
}

onMounted(() => {
  loadPlan()
})
</script>

<style scoped>
.plan-page {
  padding: 20px;
}

.plan-card {
  margin-bottom: 20px;
}
</style>

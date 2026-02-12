<template>
  <div class="reports-page">
    <h2>学员效果对比报告</h2>

    <el-card style="margin-top: 20px">
      <el-form :inline="true">
        <el-form-item label="选择学员">
          <el-select
            v-model="selectedStudents"
            multiple
            placeholder="最多选择5个学员"
            style="width: 420px"
            :max-collapse-tags="3"
          >
            <el-option
              v-for="student in studentList"
              :key="student.userId"
              :label="student.realName"
              :value="student.userId"
              :disabled="selectedStudents.length >= 5 && !selectedStudents.includes(student.userId)"
            />
          </el-select>
        </el-form-item>
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
        <el-form-item>
          <el-button type="primary" @click="generateReport">生成报告</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="reportData.length > 0" style="margin-top: 20px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>学员对比数据</span>
          <el-button size="small" @click="exportReport">导出CSV</el-button>
        </div>
      </template>
      <el-table :data="reportData" border style="width: 100%">
        <el-table-column prop="studentName" label="学员姓名" width="120" fixed />
        <el-table-column prop="startWeight" label="初始体重(kg)" width="120" />
        <el-table-column prop="currentWeight" label="当前体重(kg)" width="120" />
        <el-table-column prop="weightChange" label="体重变化(kg)" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.weightChange < 0 ? '#67c23a' : '#f56c6c' }">
              {{ row.weightChange > 0 ? '+' : '' }}{{ row.weightChange }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="totalDuration" label="总运动时长(分钟)" width="150" />
        <el-table-column prop="totalCalories" label="总消耗卡路里" width="150" />
        <el-table-column prop="exerciseCount" label="运动次数" width="120" />
        <el-table-column prop="avgDuration" label="平均时长(分钟)" width="150" />
        <el-table-column prop="planProgress" label="计划完成率" width="150">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(Number(row.planProgress || 0))" :color="getProgressColor(row.planProgress)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-row v-if="reportData.length > 0" :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>体重变化对比</span>
          </template>
          <div ref="weightCompareChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>运动时长对比</span>
          </template>
          <div ref="durationCompareChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="reportData.length > 0" :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>卡路里消耗对比</span>
          </template>
          <div ref="caloriesCompareChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>计划完成率对比</span>
          </template>
          <div ref="progressCompareChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getCoachStudents } from '@/api/user'
import { getCoachStudentReport } from '@/api/analytics'
import { CHART_COLORS, initChart } from '@/utils/chartTheme'

const selectedStudents = ref([])
const dateRange = ref([])
const studentList = ref([])
const reportData = ref([])

const weightCompareChartRef = ref(null)
const durationCompareChartRef = ref(null)
const caloriesCompareChartRef = ref(null)
const progressCompareChartRef = ref(null)

onMounted(async () => {
  await loadStudentList()
})

const loadStudentList = async () => {
  try {
    const students = await getCoachStudents()
    studentList.value = (students || []).map((item) => ({
      userId: item.id,
      realName: item.realName || item.username
    }))
  } catch (error) {
    ElMessage.error('加载学员列表失败')
  }
}

const generateReport = async () => {
  if (selectedStudents.value.length === 0) {
    ElMessage.warning('请至少选择一个学员')
    return
  }

  try {
    const [startDate, endDate] = dateRange.value || []
    const params = {
      studentIds: selectedStudents.value.join(',')
    }

    if (startDate && endDate) {
      params.startDate = formatDate(startDate)
      params.endDate = formatDate(endDate)
    }

    const data = await getCoachStudentReport(params)
    reportData.value = (data || []).map((item) => ({
      ...item,
      startWeight: formatNumber(item.startWeight),
      currentWeight: formatNumber(item.currentWeight),
      weightChange: formatNumber(item.weightChange),
      totalCalories: Math.round(Number(item.totalCalories || 0)),
      planProgress: Number(item.planProgress || 0)
    }))

    if (reportData.value.length === 0) {
      ElMessage.warning('当前筛选条件下暂无可对比数据')
      return
    }

    await nextTick()
    initCharts()
    ElMessage.success('报告生成成功')
  } catch (error) {
    ElMessage.error('生成报告失败')
  }
}

const resetFilters = () => {
  selectedStudents.value = []
  dateRange.value = []
  reportData.value = []
}

const formatDate = (value) => {
  const date = new Date(value)
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}`
}

const formatNumber = (value) => {
  if (value == null || value === '') {
    return '-'
  }
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '-'
  }
  return Number(numeric.toFixed(1))
}

const initCharts = () => {
  const colors = CHART_COLORS

  const weightChart = initChart(weightCompareChartRef.value)
  weightChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: reportData.value.map((item) => item.studentName) },
    xAxis: { type: 'category', data: ['初始', '当前'] },
    yAxis: { type: 'value', name: '体重(kg)' },
    series: reportData.value.map((item, index) => ({
      name: item.studentName,
      type: 'line',
      data: [
        item.startWeight === '-' ? 0 : Number(item.startWeight),
        item.currentWeight === '-' ? 0 : Number(item.currentWeight)
      ],
      itemStyle: { color: colors[index % colors.length] }
    }))
  })

  const durationChart = initChart(durationCompareChartRef.value)
  durationChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: reportData.value.map((item) => item.studentName) },
    yAxis: { type: 'value', name: '时长(分钟)' },
    series: [{
      type: 'bar',
      data: reportData.value.map((item, index) => ({
        value: Number(item.totalDuration || 0),
        itemStyle: { color: colors[index % colors.length] }
      }))
    }]
  })

  const caloriesChart = initChart(caloriesCompareChartRef.value)
  caloriesChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: reportData.value.map((item) => item.studentName) },
    yAxis: { type: 'value', name: '卡路里' },
    series: [{
      type: 'bar',
      data: reportData.value.map((item, index) => ({
        value: Number(item.totalCalories || 0),
        itemStyle: { color: colors[index % colors.length] }
      }))
    }]
  })

  const progressChart = initChart(progressCompareChartRef.value)
  progressChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: reportData.value.map((item, index) => ({
        value: Number(item.planProgress || 0),
        name: item.studentName,
        itemStyle: { color: colors[index % colors.length] }
      }))
    }]
  })
}

const getProgressColor = (progress) => {
  const value = Number(progress || 0)
  if (value >= 80) return '#67c23a'
  if (value >= 60) return '#e6a23c'
  return '#f56c6c'
}

const exportReport = () => {
  if (!reportData.value.length) {
    ElMessage.warning('暂无可导出数据')
    return
  }

  const headers = [
    '学员姓名',
    '初始体重(kg)',
    '当前体重(kg)',
    '体重变化(kg)',
    '总运动时长(分钟)',
    '总消耗卡路里',
    '运动次数',
    '平均时长(分钟)',
    '计划完成率(%)'
  ]

  const rows = reportData.value.map((item) => [
    item.studentName,
    item.startWeight,
    item.currentWeight,
    item.weightChange,
    item.totalDuration,
    item.totalCalories,
    item.exerciseCount,
    item.avgDuration,
    Math.round(Number(item.planProgress || 0))
  ])

  const csv = [headers, ...rows].map((row) => row.join(',')).join('\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `coach_report_${Date.now()}.csv`
  link.click()
  URL.revokeObjectURL(link.href)

  ElMessage.success('导出成功')
}
</script>

<style scoped>
.reports-page h2 {
  margin-bottom: 20px;
}
</style>

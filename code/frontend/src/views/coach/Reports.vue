<template>
  <div class="reports-page">
    <h2>学员效果对比报告</h2>

    <!-- 筛选区 -->
    <el-card style="margin-top: 20px">
      <el-form :inline="true">
        <el-form-item label="选择学员">
          <el-select
            v-model="selectedStudents"
            multiple
            placeholder="最多选择5个学员"
            style="width: 400px"
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

    <!-- 对比表格 -->
    <el-card v-if="reportData.length > 0" style="margin-top: 20px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>学员对比数据</span>
          <el-button size="small" @click="exportReport">导出报告</el-button>
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
        <el-table-column prop="planProgress" label="计划完成率" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.planProgress" :color="getProgressColor(row.planProgress)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 对比图表 -->
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
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getBodyMetricHistory } from '@/api/bodyMetric'
import { getExerciseStatistics } from '@/api/exercise'

const selectedStudents = ref([])
const dateRange = ref([])
const studentList = ref([])
const reportData = ref([])

const weightCompareChartRef = ref(null)
const durationCompareChartRef = ref(null)
const caloriesCompareChartRef = ref(null)
const progressCompareChartRef = ref(null)

onMounted(() => {
  loadStudentList()
})

const loadStudentList = () => {
  // 模拟学员列表数据
  studentList.value = [
    { userId: 1, realName: '张三' },
    { userId: 2, realName: '李四' },
    { userId: 3, realName: '王五' },
    { userId: 4, realName: '赵六' },
    { userId: 5, realName: '孙七' },
    { userId: 6, realName: '周八' },
    { userId: 7, realName: '吴九' },
    { userId: 8, realName: '郑十' }
  ]
}

const generateReport = async () => {
  if (selectedStudents.value.length === 0) {
    ElMessage.warning('请至少选择一个学员')
    return
  }

  try {
    // 模拟生成报告数据
    reportData.value = selectedStudents.value.map(userId => {
      const student = studentList.value.find(s => s.userId === userId)
      const startWeight = 70 + Math.random() * 20
      const weightChange = -2 - Math.random() * 8
      return {
        studentName: student.realName,
        startWeight: startWeight.toFixed(1),
        currentWeight: (startWeight + weightChange).toFixed(1),
        weightChange: weightChange.toFixed(1),
        totalDuration: Math.floor(1000 + Math.random() * 2000),
        totalCalories: Math.floor(5000 + Math.random() * 10000),
        exerciseCount: Math.floor(20 + Math.random() * 40),
        avgDuration: Math.floor(30 + Math.random() * 30),
        planProgress: Math.floor(60 + Math.random() * 40)
      }
    })

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

const initCharts = () => {
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399']
  
  // 体重变化对比图
  const weightChart = echarts.init(weightCompareChartRef.value)
  weightChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: reportData.value.map(d => d.studentName) },
    xAxis: { type: 'category', data: ['初始', '当前'] },
    yAxis: { type: 'value', name: '体重(kg)' },
    series: reportData.value.map((data, index) => ({
      name: data.studentName,
      type: 'line',
      data: [parseFloat(data.startWeight), parseFloat(data.currentWeight)],
      itemStyle: { color: colors[index % colors.length] }
    }))
  })

  // 运动时长对比图
  const durationChart = echarts.init(durationCompareChartRef.value)
  durationChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: reportData.value.map(d => d.studentName) },
    yAxis: { type: 'value', name: '时长(分钟)' },
    series: [{
      type: 'bar',
      data: reportData.value.map((d, i) => ({
        value: d.totalDuration,
        itemStyle: { color: colors[i % colors.length] }
      }))
    }]
  })

  // 卡路里消耗对比图
  const caloriesChart = echarts.init(caloriesCompareChartRef.value)
  caloriesChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: reportData.value.map(d => d.studentName) },
    yAxis: { type: 'value', name: '卡路里' },
    series: [{
      type: 'bar',
      data: reportData.value.map((d, i) => ({
        value: d.totalCalories,
        itemStyle: { color: colors[i % colors.length] }
      }))
    }]
  })

  // 计划完成率对比图
  const progressChart = echarts.init(progressCompareChartRef.value)
  progressChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: reportData.value.map((d, i) => ({
        value: d.planProgress,
        name: d.studentName,
        itemStyle: { color: colors[i % colors.length] }
      }))
    }]
  })
}

const getProgressColor = (progress) => {
  if (progress >= 80) return '#67c23a'
  if (progress >= 60) return '#e6a23c'
  return '#f56c6c'
}

const exportReport = () => {
  ElMessage.info('导出功能开发中...')
}
</script>

<style scoped>
.reports-page h2 {
  margin-bottom: 20px;
}
</style>

<template>
  <div class="dashboard">
    <h2>学员总览仪表盘</h2>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="6">
        <el-card class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #409eff, #66b1ff)">
              <el-icon size="30"><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalStudents }}</div>
              <div class="stat-label">学员总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-icon male" style="background: linear-gradient(135deg, #409eff, #79bbff)">
              <el-icon size="30"><Male /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.maleStudents }}</div>
              <div class="stat-label">男生学员</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-icon female" style="background: linear-gradient(135deg, #f56c6c, #fab6b6)">
              <el-icon size="30"><Female /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.femaleStudents }}</div>
              <div class="stat-label">女生学员</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #e6a23c, #f0c78a)">
              <el-icon size="30"><Calendar /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.avgAge }}</div>
              <div class="stat-label">平均年龄</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 健身目标分析 + 待办事项 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>🎯 学员健身目标分析</span>
          </template>
          <div ref="goalChartRef" style="height: 280px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="todo-card">
          <template #header>
            <div class="todo-header">
              <span>📋 待办事项提醒</span>
              <el-badge :value="todoList.length" class="todo-badge" />
            </div>
          </template>
          <div class="todo-list">
            <div 
              v-for="(item, index) in todoList" 
              :key="index" 
              class="todo-item"
              :class="item.priority"
            >
              <div class="todo-icon">
                <el-icon v-if="item.priority === 'high'" color="#f56c6c"><WarningFilled /></el-icon>
                <el-icon v-else-if="item.priority === 'medium'" color="#e6a23c"><Bell /></el-icon>
                <el-icon v-else color="#409eff"><InfoFilled /></el-icon>
              </div>
              <div class="todo-content">
                <div class="todo-title">{{ item.title }}</div>
                <div class="todo-desc">{{ item.description }}</div>
              </div>
              <el-button size="small" type="primary" text @click="handleTodo(item)">处理</el-button>
            </div>
            <el-empty v-if="!todoList.length" description="暂无待办事项" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>📈 学员体重变化趋势</span>
          </template>
          <div ref="weightChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>🏃 运动类型分布</span>
          </template>
          <div ref="exerciseChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 学员列表 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>最近活跃学员</span>
          </template>
          <el-table :data="activeStudentsList" style="width: 100%">
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="lastExerciseDate" label="最后运动时间" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" @click="viewStudent(row.userId)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>需要关注的学员</span>
          </template>
          <el-table :data="attentionStudentsList" style="width: 100%">
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="reason" label="原因" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" type="warning" @click="viewStudent(row.userId)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getDashboardStatistics, getUserBehaviorAnalysis } from '@/api/analytics'

const router = useRouter()
const weightChartRef = ref(null)
const exerciseChartRef = ref(null)
const goalChartRef = ref(null)

const stats = reactive({
  totalStudents: 25,
  maleStudents: 15,
  femaleStudents: 10,
  avgAge: 28
})

const todoList = ref([
  { title: '张三训练计划到期', description: '计划将于3天后到期，需要续期或制定新计划', priority: 'high' },
  { title: '李四体重异常', description: '本周体重增加2kg，需要关注', priority: 'medium' },
  { title: '王五7天未运动', description: '建议联系了解情况', priority: 'high' },
  { title: '新学员赵六入会', description: '需要制定初始训练计划', priority: 'low' }
])

const activeStudentsList = ref([])
const attentionStudentsList = ref([])

const handleTodo = (item) => {
  ElMessage.info(`处理: ${item.title}`)
}

onMounted(async () => {
  await loadData()
  initGoalChart()
  initWeightChart()
  initExerciseChart()
})

const loadData = async () => {
  try {
    // 获取统计数据
    const dashboardData = await getDashboardStatistics()
    stats.totalStudents = dashboardData.totalUsers || 0
    stats.activeStudents = dashboardData.activeUsers || 0
    stats.weekDuration = dashboardData.totalDuration || 0
    stats.weekCalories = dashboardData.totalCalories || 0

    // 模拟最近活跃学员数据
    activeStudentsList.value = [
      { userId: 1, realName: '张三', lastExerciseDate: '2024-01-19 10:30' },
      { userId: 2, realName: '李四', lastExerciseDate: '2024-01-19 09:15' },
      { userId: 3, realName: '王五', lastExerciseDate: '2024-01-18 18:45' }
    ]

    // 模拟需要关注的学员
    attentionStudentsList.value = [
      { userId: 4, realName: '赵六', reason: '7天未运动' },
      { userId: 5, realName: '孙七', reason: '体重异常增加' }
    ]
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const initGoalChart = () => {
  if (!goalChartRef.value) return
  const chart = echarts.init(goalChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['60%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}人' },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data: [
        { value: 12, name: '减重', itemStyle: { color: '#f56c6c' } },
        { value: 8, name: '减脂', itemStyle: { color: '#e6a23c' } },
        { value: 5, name: '增肌', itemStyle: { color: '#67c23a' } }
      ]
    }]
  })
}

const initWeightChart = () => {
  if (!weightChartRef.value) return
  const chart = echarts.init(weightChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['平均体重', '目标体重'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月']
    },
    yAxis: { type: 'value', name: '体重(kg)', min: 60, max: 80 },
    series: [
      {
        name: '平均体重',
        data: [72, 71, 70, 69.5, 69, 68.5],
        type: 'line',
        smooth: true,
        itemStyle: { color: '#409eff' },
        areaStyle: { opacity: 0.2 }
      },
      {
        name: '目标体重',
        data: [68, 68, 68, 68, 68, 68],
        type: 'line',
        lineStyle: { type: 'dashed' },
        itemStyle: { color: '#67c23a' }
      }
    ]
  })
}

const initExerciseChart = () => {
  const chart = echarts.init(exerciseChartRef.value)
  chart.setOption({
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
      ],
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  })
}

const viewStudent = (userId) => {
  router.push(`/coach/students/${userId}`)
}
</script>

<style scoped>
.dashboard h2 {
  margin-bottom: 20px;
  color: #303133;
}

.stat-card-wrapper {
  transition: all 0.3s ease;
}

.stat-card-wrapper:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 65px;
  height: 65px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-right: 15px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.todo-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.todo-list {
  max-height: 240px;
  overflow-y: auto;
}

.todo-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 10px;
  background: #f5f7fa;
  transition: all 0.2s ease;
}

.todo-item:hover {
  background: #e4e7ed;
}

.todo-item.high {
  border-left: 3px solid #f56c6c;
}

.todo-item.medium {
  border-left: 3px solid #e6a23c;
}

.todo-item.low {
  border-left: 3px solid #409eff;
}

.todo-icon {
  margin-right: 12px;
  font-size: 20px;
}

.todo-content {
  flex: 1;
}

.todo-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.todo-desc {
  font-size: 12px;
  color: #909399;
}
</style>

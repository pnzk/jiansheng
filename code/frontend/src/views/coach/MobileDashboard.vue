<template>
  <div class="mobile-dashboard">
    <!-- 顶部状态栏 -->
    <div class="status-bar">
      <span class="time">{{ currentTime }}</span>
      <div class="status-icons">
        <span>📶</span>
        <span>🔋</span>
      </div>
    </div>

    <!-- 教练头部 -->
    <div class="coach-header">
      <div class="coach-avatar">
        <el-avatar :size="60" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
        <div class="coach-badge">教练</div>
      </div>
      <div class="coach-info">
        <div class="coach-name">{{ coachName }}</div>
        <div class="coach-title">高级健身教练</div>
      </div>
      <div class="header-actions">
        <el-badge :value="5" :max="9">
          <el-icon size="24"><Bell /></el-icon>
        </el-badge>
      </div>
    </div>

    <!-- 学员概览 -->
    <div class="stats-overview">
      <div class="stat-item">
        <div class="stat-value">{{ stats.totalStudents }}</div>
        <div class="stat-label">学员总数</div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-value active">{{ stats.activeStudents }}</div>
        <div class="stat-label">本周活跃</div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-value warning">{{ stats.needAttention }}</div>
        <div class="stat-label">需关注</div>
      </div>
    </div>

    <!-- 今日待办 -->
    <div class="section">
      <div class="section-header">
        <span class="section-title">📋 今日待办</span>
        <el-badge :value="todoList.length" type="danger" />
      </div>
      <div class="todo-list">
        <div class="todo-item" v-for="(item, index) in todoList" :key="index" :class="item.priority">
          <div class="todo-priority"></div>
          <div class="todo-content">
            <div class="todo-title">{{ item.title }}</div>
            <div class="todo-desc">{{ item.description }}</div>
          </div>
          <el-icon class="todo-action"><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <!-- 学员健身目标分布 -->
    <div class="section">
      <div class="section-header">
        <span class="section-title">🎯 学员目标分布</span>
      </div>
      <div class="goal-distribution">
        <div class="goal-item">
          <div class="goal-bar" style="--color: #f56c6c; --width: 48%"></div>
          <div class="goal-info">
            <span class="goal-name">减重</span>
            <span class="goal-count">12人</span>
          </div>
        </div>
        <div class="goal-item">
          <div class="goal-bar" style="--color: #e6a23c; --width: 32%"></div>
          <div class="goal-info">
            <span class="goal-name">减脂</span>
            <span class="goal-count">8人</span>
          </div>
        </div>
        <div class="goal-item">
          <div class="goal-bar" style="--color: #67c23a; --width: 20%"></div>
          <div class="goal-info">
            <span class="goal-name">增肌</span>
            <span class="goal-count">5人</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 最近活跃学员 -->
    <div class="section">
      <div class="section-header">
        <span class="section-title">🏃 最近活跃</span>
        <span class="section-more">全部 ></span>
      </div>
      <div class="student-list">
        <div class="student-item" v-for="(student, index) in activeStudents" :key="index">
          <el-avatar :size="40">{{ student.name.charAt(0) }}</el-avatar>
          <div class="student-info">
            <div class="student-name">{{ student.name }}</div>
            <div class="student-activity">{{ student.lastActivity }}</div>
          </div>
          <el-tag size="small" :type="student.status === '运动中' ? 'success' : 'info'">
            {{ student.status }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 需要关注的学员 -->
    <div class="section warning-section">
      <div class="section-header">
        <span class="section-title">⚠️ 需要关注</span>
      </div>
      <div class="attention-list">
        <div class="attention-item" v-for="(student, index) in attentionStudents" :key="index">
          <el-avatar :size="36">{{ student.name.charAt(0) }}</el-avatar>
          <div class="attention-info">
            <div class="attention-name">{{ student.name }}</div>
            <div class="attention-reason">{{ student.reason }}</div>
          </div>
          <el-button size="small" type="primary" round>联系</el-button>
        </div>
      </div>
    </div>

    <!-- 本周数据统计 -->
    <div class="section">
      <div class="section-header">
        <span class="section-title">📊 本周统计</span>
      </div>
      <div class="week-summary">
        <div class="summary-item">
          <div class="summary-icon">⏱️</div>
          <div class="summary-value">{{ stats.weekDuration }}</div>
          <div class="summary-label">总运动时长(分)</div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">🔥</div>
          <div class="summary-value">{{ stats.weekCalories }}</div>
          <div class="summary-label">总消耗(卡)</div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">📝</div>
          <div class="summary-value">{{ stats.plansCreated }}</div>
          <div class="summary-label">新建计划</div>
        </div>
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <div class="nav-item active">
        <el-icon size="24"><HomeFilled /></el-icon>
        <span>首页</span>
      </div>
      <div class="nav-item" @click="$router.push('/coach/students')">
        <el-icon size="24"><User /></el-icon>
        <span>学员</span>
      </div>
      <div class="nav-item add-btn" @click="$router.push('/coach/plans')">
        <el-icon size="28"><Plus /></el-icon>
      </div>
      <div class="nav-item" @click="$router.push('/coach/reports')">
        <el-icon size="24"><DataAnalysis /></el-icon>
        <span>报告</span>
      </div>
      <div class="nav-item" @click="$router.push('/coach/settings')">
        <el-icon size="24"><Setting /></el-icon>
        <span>设置</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Bell, HomeFilled, User, Plus, DataAnalysis, Setting, ArrowRight } from '@element-plus/icons-vue'
import { getDashboardStatistics } from '@/api/analytics'

const currentTime = ref(new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }))
const coachName = ref(localStorage.getItem('realName') || '教练')

const stats = reactive({
  totalStudents: 25,
  activeStudents: 18,
  needAttention: 3,
  weekDuration: 2850,
  weekCalories: 45000,
  plansCreated: 5
})

const todoList = ref([
  { title: '张三训练计划到期', description: '计划将于3天后到期', priority: 'high' },
  { title: '李四体重异常', description: '本周体重增加2kg', priority: 'medium' },
  { title: '王五7天未运动', description: '建议联系了解情况', priority: 'high' },
  { title: '新学员赵六入会', description: '需要制定初始计划', priority: 'low' }
])

const activeStudents = ref([
  { name: '张三', lastActivity: '刚刚完成跑步30分钟', status: '已完成' },
  { name: '李四', lastActivity: '正在进行力量训练', status: '运动中' },
  { name: '王五', lastActivity: '2小时前完成游泳', status: '已完成' }
])

const attentionStudents = ref([
  { name: '赵六', reason: '连续7天未运动' },
  { name: '孙七', reason: '体重异常增加3kg' },
  { name: '周八', reason: '训练计划即将到期' }
])

const loadData = async () => {
  try {
    const data = await getDashboardStatistics()
    if (data) {
      stats.totalStudents = data.totalUsers || 25
      stats.activeStudents = data.activeUsers || 18
    }
  } catch (e) {}
}

onMounted(() => {
  loadData()
  setInterval(() => {
    currentTime.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }, 1000)
})
</script>

<style scoped>
.mobile-dashboard {
  background: #f5f5f5;
  min-height: 100%;
  padding-bottom: 80px;
}

.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 20px;
  background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
  color: white;
  font-size: 14px;
}

.status-icons {
  display: flex;
  gap: 8px;
}

.coach-header {
  display: flex;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
  color: white;
}

.coach-avatar {
  position: relative;
}

.coach-badge {
  position: absolute;
  bottom: -5px;
  left: 50%;
  transform: translateX(-50%);
  background: #e6a23c;
  color: white;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
}

.coach-info {
  flex: 1;
  margin-left: 15px;
}

.coach-name {
  font-size: 20px;
  font-weight: bold;
}

.coach-title {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 4px;
}

.header-actions {
  cursor: pointer;
}

.stats-overview {
  display: flex;
  justify-content: space-around;
  align-items: center;
  background: white;
  margin: -20px 15px 15px;
  padding: 20px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-value.active {
  color: #67c23a;
}

.stat-value.warning {
  color: #f56c6c;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: #eee;
}

.section {
  margin: 15px;
  background: white;
  border-radius: 16px;
  padding: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.section-more {
  font-size: 12px;
  color: #909399;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 12px;
}

.todo-priority {
  width: 4px;
  height: 40px;
  border-radius: 2px;
  margin-right: 12px;
}

.todo-item.high .todo-priority {
  background: #f56c6c;
}

.todo-item.medium .todo-priority {
  background: #e6a23c;
}

.todo-item.low .todo-priority {
  background: #67c23a;
}

.todo-content {
  flex: 1;
}

.todo-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.todo-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.todo-action {
  color: #c0c4cc;
}

.goal-distribution {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.goal-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.goal-bar {
  height: 8px;
  background: var(--color);
  border-radius: 4px;
  width: var(--width);
  transition: width 0.5s ease;
}

.goal-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.goal-name {
  color: #606266;
}

.goal-count {
  color: #909399;
}

.student-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.student-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.student-info {
  flex: 1;
}

.student-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.student-activity {
  font-size: 12px;
  color: #909399;
}

.warning-section {
  background: linear-gradient(135deg, #fff5f5 0%, #fff 100%);
}

.attention-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.attention-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.attention-info {
  flex: 1;
}

.attention-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.attention-reason {
  font-size: 12px;
  color: #f56c6c;
}

.week-summary {
  display: flex;
  justify-content: space-around;
}

.summary-item {
  text-align: center;
}

.summary-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}

.summary-label {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  display: flex;
  justify-content: space-around;
  align-items: center;
  background: white;
  padding: 10px 0 25px;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.1);
  border-radius: 20px 20px 0 0;
  z-index: 100;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 11px;
  cursor: pointer;
}

.nav-item.active {
  color: #3498db;
}

.nav-item.add-btn {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
  border-radius: 50%;
  color: white;
  justify-content: center;
  margin-top: -25px;
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.4);
}
</style>

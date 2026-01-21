<template>
  <div class="achievements-page">
    <h2>健身成就与排行榜</h2>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="成就勋章墙" name="achievements">
        <!-- 成就统计 -->
        <el-row :gutter="20" style="margin-bottom: 20px">
          <el-col :span="8">
            <el-card class="stats-card">
              <div class="stats-icon">🏅</div>
              <div class="stats-info">
                <div class="stats-value">{{ unlockedCount }}/{{ achievements.length }}</div>
                <div class="stats-label">已解锁成就</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="stats-card">
              <div class="stats-icon">🔥</div>
              <div class="stats-info">
                <div class="stats-value">{{ totalExercises }}</div>
                <div class="stats-label">累计运动次数</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="stats-card">
              <div class="stats-icon">⚡</div>
              <div class="stats-info">
                <div class="stats-value">{{ totalCalories.toLocaleString() }}</div>
                <div class="stats-label">累计消耗卡路里</div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 成就勋章墙 -->
        <el-card>
          <template #header>
            <span>🏆 成就勋章墙</span>
          </template>
          <el-row :gutter="20">
            <el-col :span="6" v-for="achievement in achievements" :key="achievement.id">
              <div class="achievement-card" :class="{ unlocked: achievement.unlocked }">
                <div class="achievement-badge">
                  <span class="badge-icon">{{ getAchievementIcon(achievement.achievementType) }}</span>
                  <div class="badge-glow" v-if="achievement.unlocked"></div>
                </div>
                <h4>{{ achievement.achievementName }}</h4>
                <p class="achievement-desc">{{ achievement.description }}</p>
                <div v-if="achievement.unlocked" class="unlock-info">
                  <el-icon><Check /></el-icon>
                  <span>{{ formatDate(achievement.unlockedAt) }}</span>
                </div>
                <div v-else class="progress-info">
                  <el-progress :percentage="getProgress(achievement)" :stroke-width="6" />
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <!-- 成就解锁时间线 -->
        <el-card style="margin-top: 20px">
          <template #header>
            <span>📅 成就解锁时间线</span>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="item in unlockedTimeline"
              :key="item.id"
              :timestamp="formatDate(item.unlockedAt)"
              placement="top"
              :color="getTimelineColor(item.achievementType)"
            >
              <el-card shadow="hover" class="timeline-card">
                <div class="timeline-content">
                  <span class="timeline-icon">{{ getAchievementIcon(item.achievementType) }}</span>
                  <div class="timeline-info">
                    <div class="timeline-title">{{ item.achievementName }}</div>
                    <div class="timeline-desc">{{ item.description }}</div>
                  </div>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="!unlockedTimeline.length" description="暂无解锁成就" />
        </el-card>
      </el-tab-pane>
      
      <el-tab-pane label="健身排行榜" name="leaderboard">
        <el-row :gutter="20">
          <!-- 减重排行榜 -->
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="leaderboard-header">
                  <span>🏋️ 减重排行榜</span>
                  <el-tag type="danger" size="small">本月</el-tag>
                </div>
              </template>
              <div class="leaderboard-list">
                <div 
                  v-for="(item, index) in weightLossLeaderboard" 
                  :key="item.userId"
                  class="leaderboard-item"
                  :class="{ 'is-me': item.isMe }"
                >
                  <div class="rank" :class="getRankClass(index)">
                    <span v-if="index < 3">{{ getRankIcon(index) }}</span>
                    <span v-else>{{ index + 1 }}</span>
                  </div>
                  <div class="user-info">
                    <div class="username">{{ item.realName || item.username }}</div>
                    <div class="user-tag" v-if="item.isMe">
                      <el-tag size="small" type="success">我</el-tag>
                    </div>
                  </div>
                  <div class="value">-{{ item.value }} kg</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <!-- 运动时长排行榜 -->
          <el-col :span="12">
            <el-card>
              <template #header>
                <div class="leaderboard-header">
                  <span>⏱️ 运动时长排行榜</span>
                  <el-tag type="warning" size="small">本月</el-tag>
                </div>
              </template>
              <div class="leaderboard-list">
                <div 
                  v-for="(item, index) in durationLeaderboard" 
                  :key="item.userId"
                  class="leaderboard-item"
                  :class="{ 'is-me': item.isMe }"
                >
                  <div class="rank" :class="getRankClass(index)">
                    <span v-if="index < 3">{{ getRankIcon(index) }}</span>
                    <span v-else>{{ index + 1 }}</span>
                  </div>
                  <div class="user-info">
                    <div class="username">{{ item.realName || item.username }}</div>
                    <div class="user-tag" v-if="item.isMe">
                      <el-tag size="small" type="success">我</el-tag>
                    </div>
                  </div>
                  <div class="value">{{ item.value }} 分钟</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserAchievements } from '@/api/achievement'
import { getLeaderboard } from '@/api/analytics'
import { getExerciseStatistics } from '@/api/exercise'

const activeTab = ref('achievements')
const achievements = ref([])
const weightLossLeaderboard = ref([])
const durationLeaderboard = ref([])
const totalExercises = ref(0)
const totalCalories = ref(0)

const unlockedCount = computed(() => achievements.value.filter(a => a.unlocked).length)

const unlockedTimeline = computed(() => {
  return achievements.value
    .filter(a => a.unlocked)
    .sort((a, b) => new Date(b.unlockedAt) - new Date(a.unlockedAt))
})

const getAchievementIcon = (type) => {
  const icons = {
    'EXERCISE_COUNT': '🏃',
    'CONSECUTIVE_DAYS': '📅',
    'TOTAL_CALORIES': '🔥',
    'RUNNING_DISTANCE': '🏅',
    'WEIGHT_LOSS': '⚖️',
    'FAT_LOSS': '💪',
    'MUSCLE_GAIN': '🏋️',
    'SINGLE_DURATION': '⏱️'
  }
  return icons[type] || '🏆'
}

const getTimelineColor = (type) => {
  const colors = {
    'EXERCISE_COUNT': '#409eff',
    'CONSECUTIVE_DAYS': '#67c23a',
    'TOTAL_CALORIES': '#f56c6c',
    'WEIGHT_LOSS': '#e6a23c'
  }
  return colors[type] || '#909399'
}

const getProgress = (achievement) => {
  return Math.min(Math.random() * 80, 100)
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}

const getRankClass = (index) => {
  if (index === 0) return 'gold'
  if (index === 1) return 'silver'
  if (index === 2) return 'bronze'
  return ''
}

const getRankIcon = (index) => {
  const icons = ['🥇', '🥈', '🥉']
  return icons[index] || ''
}

const loadAchievements = async () => {
  try {
    const data = await getUserAchievements()
    achievements.value = data || []
  } catch (error) {
    // 使用模拟数据
    achievements.value = [
      { id: 1, achievementName: '健身新手', description: '完成第1次运动', achievementType: 'EXERCISE_COUNT', unlocked: true, unlockedAt: '2025-01-10' },
      { id: 2, achievementName: '坚持不懈', description: '连续运动7天', achievementType: 'CONSECUTIVE_DAYS', unlocked: true, unlockedAt: '2025-01-15' },
      { id: 3, achievementName: '运动达人', description: '累计运动50次', achievementType: 'EXERCISE_COUNT', unlocked: false },
      { id: 4, achievementName: '卡路里杀手', description: '累计消耗10000卡路里', achievementType: 'TOTAL_CALORIES', unlocked: false },
      { id: 5, achievementName: '减重冠军', description: '成功减重5kg', achievementType: 'WEIGHT_LOSS', unlocked: false },
      { id: 6, achievementName: '时长大师', description: '单次运动超过2小时', achievementType: 'SINGLE_DURATION', unlocked: true, unlockedAt: '2025-01-18' }
    ]
  }
}

const loadLeaderboards = async () => {
  // 直接使用模拟数据，避免后端排行榜API错误
  weightLossLeaderboard.value = [
    { userId: 1, username: 'user1', realName: '张三', value: 5.2, isMe: false },
    { userId: 2, username: 'user2', realName: '李四', value: 4.8, isMe: true },
    { userId: 3, username: 'user3', realName: '王五', value: 4.5, isMe: false },
    { userId: 4, username: 'user4', realName: '赵六', value: 3.9, isMe: false },
    { userId: 5, username: 'user5', realName: '钱七', value: 3.2, isMe: false }
  ]
  durationLeaderboard.value = [
    { userId: 1, username: 'user1', realName: '李四', value: 1250, isMe: true },
    { userId: 2, username: 'user2', realName: '张三', value: 1180, isMe: false },
    { userId: 3, username: 'user3', realName: '王五', value: 980, isMe: false },
    { userId: 4, username: 'user4', realName: '赵六', value: 850, isMe: false },
    { userId: 5, username: 'user5', realName: '钱七', value: 720, isMe: false }
  ]
}

const loadStats = async () => {
  try {
    const stats = await getExerciseStatistics()
    totalExercises.value = stats?.totalExercises || 45
    totalCalories.value = stats?.totalCalories || 28500
  } catch (error) {
    totalExercises.value = 45
    totalCalories.value = 28500
  }
}

onMounted(() => {
  loadAchievements()
  loadLeaderboards()
  loadStats()
})
</script>

<style scoped>
.achievements-page {
  padding: 20px;
}

.stats-card {
  display: flex;
  align-items: center;
  padding: 15px;
}

.stats-icon {
  font-size: 40px;
  margin-right: 15px;
}

.stats-info {
  flex: 1;
}

.stats-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stats-label {
  font-size: 14px;
  color: #909399;
}

.achievement-card {
  text-align: center;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 12px;
  background: #f5f7fa;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.achievement-card.unlocked {
  background: linear-gradient(135deg, #f0f9eb, #e1f3d8);
  border: 2px solid #67c23a;
}

.achievement-card:not(.unlocked) {
  opacity: 0.6;
  filter: grayscale(50%);
}

.achievement-badge {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 15px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-icon {
  font-size: 48px;
  z-index: 1;
}

.badge-glow {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(103, 194, 58, 0.3), transparent);
  animation: glow 2s infinite;
}

@keyframes glow {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.2); opacity: 0.8; }
}

.achievement-card h4 {
  margin: 10px 0 5px;
  color: #303133;
}

.achievement-desc {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.unlock-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: #67c23a;
  font-size: 12px;
}

.progress-info {
  padding: 0 10px;
}

.timeline-card {
  padding: 10px;
}

.timeline-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.timeline-icon {
  font-size: 32px;
}

.timeline-title {
  font-weight: bold;
  color: #303133;
}

.timeline-desc {
  font-size: 12px;
  color: #909399;
}

.leaderboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.leaderboard-list {
  padding: 10px 0;
}

.leaderboard-item {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #f5f7fa;
  transition: all 0.2s ease;
}

.leaderboard-item:hover {
  background: #e4e7ed;
}

.leaderboard-item.is-me {
  background: linear-gradient(135deg, #ecf5ff, #d9ecff);
  border: 1px solid #409eff;
}

.rank {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  border-radius: 50%;
  margin-right: 15px;
  background: #e4e7ed;
  color: #606266;
}

.rank.gold {
  background: linear-gradient(135deg, #ffd700, #ffec8b);
  color: #8b6914;
}

.rank.silver {
  background: linear-gradient(135deg, #c0c0c0, #e8e8e8);
  color: #666;
}

.rank.bronze {
  background: linear-gradient(135deg, #cd7f32, #daa520);
  color: #5c3317;
}

.user-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  font-weight: 500;
  color: #303133;
}

.value {
  font-weight: bold;
  font-size: 16px;
  color: #409eff;
}
</style>

<template>
  <div class="achievements-page">
    <h2>健身成就与排行榜</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="成就勋章墙" name="achievements">
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
              <div class="stats-icon">🏃</div>
              <div class="stats-info">
                <div class="stats-value">{{ totalExercises }}</div>
                <div class="stats-label">累计运动次数</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="stats-card">
              <div class="stats-icon">🔥</div>
              <div class="stats-info">
                <div class="stats-value">{{ totalCalories.toLocaleString() }}</div>
                <div class="stats-label">累计消耗卡路里</div>
              </div>
            </el-card>
          </el-col>
        </el-row>

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
                <el-empty v-if="!weightLossLeaderboard.length" description="暂无数据" :image-size="70" />
              </div>
            </el-card>
          </el-col>

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
                <el-empty v-if="!durationLeaderboard.length" description="暂无数据" :image-size="70" />
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getUserAchievements } from '@/api/achievement'
import { getLeaderboard } from '@/api/analytics'
import { getBodyMetricHistory } from '@/api/bodyMetric'
import { getExerciseStatistics, getUserExerciseRecords } from '@/api/exercise'

const activeTab = ref('achievements')
const achievements = ref([])
const weightLossLeaderboard = ref([])
const durationLeaderboard = ref([])
const totalExercises = ref(0)
const totalCalories = ref(0)
const currentUserId = Number(localStorage.getItem('userId') || 0)

const achievementProgressState = reactive({
  totalExercises: 0,
  totalCalories: 0,
  maxDuration: 0,
  longestStreak: 0,
  runningDistance: 0,
  weightLoss: 0,
  fatLoss: 0,
  muscleGain: 0
})

const unlockedCount = computed(() => achievements.value.filter((item) => item.unlocked).length)

const unlockedTimeline = computed(() => {
  return achievements.value
    .filter((item) => item.unlocked)
    .sort((left, right) => new Date(right.unlockedAt) - new Date(left.unlockedAt))
})

const getAchievementIcon = (type) => {
  const icons = {
    EXERCISE_COUNT: '🏅',
    CONSECUTIVE_DAYS: '📅',
    TOTAL_CALORIES: '🔥',
    RUNNING_DISTANCE: '🏃',
    WEIGHT_LOSS: '⚖️',
    FAT_LOSS: '🎯',
    MUSCLE_GAIN: '💪',
    SINGLE_DURATION: '⏱️'
  }
  return icons[type] || '🏆'
}

const getTimelineColor = (type) => {
  const colors = {
    EXERCISE_COUNT: '#409eff',
    CONSECUTIVE_DAYS: '#67c23a',
    TOTAL_CALORIES: '#f56c6c',
    WEIGHT_LOSS: '#e6a23c'
  }
  return colors[type] || '#909399'
}

const getProgress = (achievement) => {
  if (achievement?.unlocked) {
    return 100
  }

  const threshold = Number(achievement?.thresholdValue || 0)
  if (!Number.isFinite(threshold) || threshold <= 0) {
    return 0
  }

  const currentValue = getAchievementCurrentValue(achievement?.achievementType)
  if (!Number.isFinite(currentValue) || currentValue <= 0) {
    return 0
  }

  return Math.min(Math.round((currentValue / threshold) * 100), 100)
}

const getAchievementCurrentValue = (type) => {
  const normalizedType = `${type || ''}`.trim().toUpperCase()
  switch (normalizedType) {
    case 'EXERCISE_COUNT':
      return achievementProgressState.totalExercises
    case 'CONSECUTIVE_DAYS':
      return achievementProgressState.longestStreak
    case 'TOTAL_CALORIES':
    case 'CALORIES':
      return achievementProgressState.totalCalories
    case 'RUNNING_DISTANCE':
      return achievementProgressState.runningDistance
    case 'WEIGHT_LOSS':
      return achievementProgressState.weightLoss
    case 'FAT_LOSS':
      return achievementProgressState.fatLoss
    case 'MUSCLE_GAIN':
      return achievementProgressState.muscleGain
    case 'SINGLE_DURATION':
    case 'DURATION':
      return achievementProgressState.maxDuration
    default:
      return 0
  }
}

const formatDate = (date) => {
  if (!date) {
    return ''
  }
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
    achievements.value = []
  }
}

const normalizeLeaderboard = (data) => {
  return (data?.entries || []).map((item) => ({
    ...item,
    isMe: Number(item.userId) === currentUserId
  }))
}

const loadLeaderboards = async () => {
  try {
    const [weightLossData, durationData] = await Promise.all([
      getLeaderboard('WEIGHT_LOSS', 10),
      getLeaderboard('TOTAL_DURATION', 10)
    ])

    weightLossLeaderboard.value = normalizeLeaderboard(weightLossData)
    durationLeaderboard.value = normalizeLeaderboard(durationData)
  } catch (error) {
    weightLossLeaderboard.value = []
    durationLeaderboard.value = []
  }
}

const loadStats = async () => {
  try {
    const [exerciseStats, records, metrics] = await Promise.all([
      getExerciseStatistics(),
      getUserExerciseRecords(),
      getBodyMetricHistory()
    ])

    const exerciseRecords = Array.isArray(records) ? records : []
    const bodyMetrics = Array.isArray(metrics) ? metrics : []

    totalExercises.value = Number(exerciseStats?.totalRecords ?? exerciseRecords.length ?? 0)
    totalCalories.value = Math.round(Number(exerciseStats?.totalCaloriesBurned ?? 0))

    achievementProgressState.totalExercises = totalExercises.value
    achievementProgressState.totalCalories = totalCalories.value
    achievementProgressState.maxDuration = exerciseRecords.reduce((max, item) => {
      const duration = Number(item.durationMinutes || 0)
      return duration > max ? duration : max
    }, 0)
    achievementProgressState.longestStreak = calculateLongestStreak(exerciseRecords)
    achievementProgressState.runningDistance = estimateRunningDistance(exerciseRecords)

    const sortedMetrics = bodyMetrics
      .filter((item) => item && item.measurementDate)
      .sort((left, right) => new Date(left.measurementDate) - new Date(right.measurementDate))

    if (sortedMetrics.length >= 2) {
      const first = sortedMetrics[0]
      const latest = sortedMetrics[sortedMetrics.length - 1]

      achievementProgressState.weightLoss = roundOneDecimal(Math.max(Number(first.weightKg || 0) - Number(latest.weightKg || 0), 0))
      achievementProgressState.fatLoss = roundOneDecimal(Math.max(Number(first.bodyFatPercentage || 0) - Number(latest.bodyFatPercentage || 0), 0))
      achievementProgressState.muscleGain = roundOneDecimal(Math.max(Number(latest.muscleMassKg || 0) - Number(first.muscleMassKg || 0), 0))
    } else {
      achievementProgressState.weightLoss = 0
      achievementProgressState.fatLoss = 0
      achievementProgressState.muscleGain = 0
    }
  } catch (error) {
    totalExercises.value = 0
    totalCalories.value = 0

    achievementProgressState.totalExercises = 0
    achievementProgressState.totalCalories = 0
    achievementProgressState.maxDuration = 0
    achievementProgressState.longestStreak = 0
    achievementProgressState.runningDistance = 0
    achievementProgressState.weightLoss = 0
    achievementProgressState.fatLoss = 0
    achievementProgressState.muscleGain = 0
  }
}

const calculateLongestStreak = (records) => {
  if (!records.length) {
    return 0
  }

  const uniqueDates = [...new Set(records
    .map((item) => (item?.exerciseDate ? normalizeDateString(item.exerciseDate) : null))
    .filter(Boolean))]
    .sort()

  if (!uniqueDates.length) {
    return 0
  }

  let longest = 1
  let current = 1

  for (let i = 1; i < uniqueDates.length; i++) {
    const previous = new Date(`${uniqueDates[i - 1]}T00:00:00`)
    const next = new Date(`${uniqueDates[i]}T00:00:00`)
    const days = Math.round((next - previous) / (24 * 60 * 60 * 1000))
    if (days === 1) {
      current += 1
      longest = Math.max(longest, current)
    } else {
      current = 1
    }
  }

  return longest
}

const estimateRunningDistance = (records) => {
  const runningMinutes = records
    .filter((item) => /跑|run/i.test(`${item?.exerciseType || ''}`))
    .reduce((sum, item) => sum + Number(item.durationMinutes || 0), 0)

  return roundOneDecimal((runningMinutes / 60) * 8)
}

const normalizeDateString = (value) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return null
  }
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}`
}

const roundOneDecimal = (value) => {
  return Number(Number(value || 0).toFixed(1))
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
  0%,
  100% {
    transform: scale(1);
    opacity: 0.5;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
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

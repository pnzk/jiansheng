<template>
  <div :class="['plan-page', `theme-${uiTheme}`]" :style="pageStyleVars">
    <div class="page-banner">
      <div>
        <h2>我的训练计划</h2>
        <p class="banner-subtitle">按目标稳步推进，实时查看训练进度与安排</p>
      </div>
      <div class="banner-right">
        <el-switch
          v-model="isDarkTheme"
          inline-prompt
          active-text="深色"
          inactive-text="浅色"
          class="theme-switch"
        />
        <el-tag
          v-if="plan"
          :type="getStatusTagType(plan.status)"
          effect="dark"
          round
          class="status-tag"
        >
          {{ getStatusText(plan.status) }}
        </el-tag>
      </div>
    </div>

    <el-card v-if="plan" class="plan-card" shadow="never">
      <div class="stat-grid">
        <div class="stat-item goal">
          <div class="stat-label">目标类型</div>
          <div class="stat-value">{{ getGoalText(plan.goalType) }}</div>
        </div>
        <div class="stat-item target">
          <div class="stat-label">目标值</div>
          <div class="stat-value">{{ plan.targetValue }} kg</div>
        </div>
        <div class="stat-item progress">
          <div class="stat-label">完成率</div>
          <div class="stat-value">{{ completionRate }}%</div>
        </div>
      </div>

      <h3 class="plan-name">{{ plan.planName }}</h3>

      <el-descriptions :column="2" border class="plan-descriptions">
        <el-descriptions-item label="开始日期">{{ plan.startDate }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ plan.endDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(plan.status)" effect="light" round>
            {{ getStatusText(plan.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前进度">
          <el-progress
            :percentage="completionRate"
            :stroke-width="10"
            :color="getProgressColor(completionRate)"
            :show-text="false"
          />
        </el-descriptions-item>
      </el-descriptions>

      <el-row :gutter="16" class="section-row">
        <el-col :span="14">
          <div class="plan-section">
            <div class="section-title">周训练安排</div>
            <div v-if="weeklyScheduleItems.length" class="weekly-list">
              <div v-for="(item, index) in weeklyScheduleItems" :key="`${item}-${index}`" class="weekly-item">
                {{ item }}
              </div>
            </div>
            <el-empty v-else description="暂无周训练安排" :image-size="72" />
          </div>
        </el-col>
        <el-col :span="10">
          <div class="plan-section chart-section">
            <div class="section-title">进度仪表盘</div>
            <div ref="chartRef" class="chart-box"></div>
          </div>
        </el-col>
      </el-row>

      <div class="plan-section description-section">
        <div class="section-title">计划描述</div>
        <div class="description-box">{{ plan.description || '暂无计划描述' }}</div>
      </div>
    </el-card>

    <el-empty v-else description="暂无训练计划" class="empty-panel" />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getMyTrainingPlan } from '@/api/trainingPlan'
import { initChart } from '@/utils/chartTheme'

const GOAL_PALETTES = {
  WEIGHT_LOSS: {
    bannerStart: '#ff9a52',
    bannerEnd: '#ff6a55',
    accent: '#ff8b42',
    accentSoft: '#fff2e8',
    accentBorder: '#ffd8c2',
    progressStrong: '#ff8b42',
    progressMedium: '#ffac5f',
    progressWeak: '#ff7a45',
    bannerShadow: 'rgba(255, 124, 76, 0.32)'
  },
  FAT_LOSS: {
    bannerStart: '#3f8bff',
    bannerEnd: '#6f63ff',
    accent: '#4a90ff',
    accentSoft: '#ebf3ff',
    accentBorder: '#cfe0ff',
    progressStrong: '#4a90ff',
    progressMedium: '#6da6ff',
    progressWeak: '#3983ff',
    bannerShadow: 'rgba(76, 110, 240, 0.3)'
  },
  MUSCLE_GAIN: {
    bannerStart: '#2fbe77',
    bannerEnd: '#1ca56b',
    accent: '#27b36d',
    accentSoft: '#e8f8f0',
    accentBorder: '#bfe9d1',
    progressStrong: '#27b36d',
    progressMedium: '#45c788',
    progressWeak: '#1fa865',
    bannerShadow: 'rgba(34, 178, 110, 0.3)'
  }
}

const plan = ref(null)
const chartRef = ref(null)
const weeklyScheduleText = ref('')
const chartInstance = ref(null)
const uiTheme = ref('light')

const completionRate = computed(() => Math.round(Number(plan.value?.completionRate || 0)))

const isDarkTheme = computed({
  get: () => uiTheme.value === 'dark',
  set: (enabled) => {
    uiTheme.value = enabled ? 'dark' : 'light'
  }
})

const currentPalette = computed(() => GOAL_PALETTES[plan.value?.goalType] || GOAL_PALETTES.FAT_LOSS)

const pageStyleVars = computed(() => {
  const palette = currentPalette.value
  const isDark = uiTheme.value === 'dark'

  const common = {
    '--banner-start': palette.bannerStart,
    '--banner-end': palette.bannerEnd,
    '--banner-shadow': palette.bannerShadow,
    '--accent': palette.accent,
    '--accent-soft': palette.accentSoft,
    '--accent-border': palette.accentBorder,
    '--progress-track': isDark ? '#2b3d61' : '#e9eef7',
    '--pointer-color': isDark ? '#dce6ff' : '#5b6b84'
  }

  if (isDark) {
    return {
      ...common,
      '--page-bg-start': '#0f1525',
      '--page-bg-mid': '#111a2e',
      '--page-bg-end': '#0d1424',
      '--card-bg': '#121c33',
      '--card-border': '#223154',
      '--card-shadow': 'rgba(4, 10, 25, 0.55)',
      '--section-bg': '#101a30',
      '--section-border': '#24365c',
      '--desc-bg': '#0d172d',
      '--desc-border': '#273a62',
      '--text-main': '#e7eefc',
      '--text-sub': '#a9b6cf',
      '--text-muted': '#8fa0c3',
      '--detail-text': '#f2f6ff',
      '--label-bg': '#16233f',
      '--table-cell-bg': '#121c33',
      '--empty-bg': '#121c33',
      '--empty-border': '#273b60',
      '--empty-shadow': 'rgba(3, 10, 24, 0.52)'
    }
  }

  return {
    ...common,
    '--page-bg-start': '#f3f7ff',
    '--page-bg-mid': '#f8fbff',
    '--page-bg-end': '#ffffff',
    '--card-bg': '#ffffff',
    '--card-border': '#e8efff',
    '--card-shadow': 'rgba(33, 79, 173, 0.08)',
    '--section-bg': '#f9fbff',
    '--section-border': '#e7eefc',
    '--desc-bg': '#ffffff',
    '--desc-border': '#e5ecf9',
    '--text-main': '#27324a',
    '--text-sub': '#6f7c95',
    '--text-muted': '#7c879b',
    '--detail-text': '#25324d',
    '--label-bg': '#f5f8ff',
    '--table-cell-bg': '#ffffff',
    '--empty-bg': '#ffffff',
    '--empty-border': '#e9eef8',
    '--empty-shadow': 'rgba(33, 79, 173, 0.08)'
  }
})

const weeklyScheduleItems = computed(() => {
  return weeklyScheduleText.value
    .split(/[；;\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
})

const getGoalText = (goalType) => {
  const texts = {
    WEIGHT_LOSS: '减重',
    FAT_LOSS: '减脂',
    MUSCLE_GAIN: '增肌'
  }
  return texts[goalType] || goalType || '未设置'
}

const getStatusText = (status) => {
  const texts = {
    ACTIVE: '进行中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return texts[status] || status || '未知状态'
}

const getStatusTagType = (status) => {
  const types = {
    ACTIVE: 'success',
    COMPLETED: 'info',
    CANCELLED: 'danger'
  }
  return types[status] || 'warning'
}

const getProgressColor = (percentage) => {
  const palette = currentPalette.value
  if (percentage >= 80) return palette.progressStrong
  if (percentage >= 50) return palette.progressMedium
  return palette.progressWeak
}

const parseWeeklySchedule = (weeklySchedule) => {
  if (!weeklySchedule) {
    return ''
  }
  if (typeof weeklySchedule !== 'string') {
    return String(weeklySchedule)
  }

  try {
    const json = JSON.parse(weeklySchedule)
    if (Array.isArray(json)) {
      return json.join('；')
    }
    if (json && typeof json === 'object') {
      return Object.entries(json)
        .map(([day, items]) => `${day}：${Array.isArray(items) ? items.join('、') : items}`)
        .join('；')
    }
    return weeklySchedule
  } catch {
    return weeklySchedule
  }
}

const resizeChart = () => {
  chartInstance.value?.resize()
}

const renderChart = () => {
  if (!chartRef.value || !plan.value) return

  if (!chartInstance.value) {
    chartInstance.value = initChart(chartRef.value)
  }

  chartInstance.value.setOption({
    series: [
      {
        type: 'gauge',
        center: ['50%', '58%'],
        startAngle: 210,
        endAngle: -30,
        min: 0,
        max: 100,
        splitNumber: 10,
        progress: {
          show: true,
          width: 12,
          itemStyle: {
            color: getProgressColor(completionRate.value)
          }
        },
        axisLine: {
          lineStyle: {
            width: 12,
            color: [[1, pageStyleVars.value['--progress-track']]]
          }
        },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: {
          show: true,
          length: '60%',
          width: 4,
          itemStyle: { color: pageStyleVars.value['--pointer-color'] }
        },
        anchor: {
          show: true,
          size: 8,
          itemStyle: { color: pageStyleVars.value['--pointer-color'] }
        },
        detail: {
          valueAnimation: true,
          offsetCenter: [0, '42%'],
          formatter: '{value}%',
          color: pageStyleVars.value['--detail-text'],
          fontSize: 24,
          fontWeight: 700
        },
        title: {
          offsetCenter: [0, '72%'],
          color: pageStyleVars.value['--text-sub'],
          fontSize: 13
        },
        data: [{ value: completionRate.value, name: '计划完成率' }]
      }
    ]
  })
}

const loadPlan = async () => {
  try {
    const data = await getMyTrainingPlan()
    if (data) {
      plan.value = data
      weeklyScheduleText.value = parseWeeklySchedule(data.weeklySchedule)
      setTimeout(renderChart, 120)
    } else {
      plan.value = null
      weeklyScheduleText.value = ''
    }
  } catch (error) {
    plan.value = null
    weeklyScheduleText.value = ''
    const message = String(error?.message || '')
    if (message && !message.includes('暂无')) {
      ElMessage.error(message)
    }
  }
}

watch(
  () => [completionRate.value, plan.value?.goalType, uiTheme.value],
  async () => {
    await nextTick()
    renderChart()
  }
)

onMounted(() => {
  loadPlan()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance.value?.dispose()
  chartInstance.value = null
})
</script>

<style scoped>
.plan-page {
  padding: 20px;
  border-radius: 16px;
  color: var(--text-main);
  transition: all 0.3s ease;
  background: linear-gradient(180deg, var(--page-bg-start) 0%, var(--page-bg-mid) 40%, var(--page-bg-end) 100%);
}

.page-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-radius: 14px;
  margin-bottom: 16px;
  color: #fff;
  background: linear-gradient(120deg, var(--banner-start), var(--banner-end));
  box-shadow: 0 10px 26px var(--banner-shadow);
}

.page-banner h2 {
  margin: 0;
  font-size: 30px;
}

.banner-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.banner-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.status-tag {
  border: 1px solid rgba(255, 255, 255, 0.45);
}

.plan-card {
  border-radius: 14px;
  border: 1px solid var(--card-border);
  background: var(--card-bg);
  box-shadow: 0 8px 24px var(--card-shadow);
}

.plan-name {
  margin: 2px 0 14px;
  font-size: 24px;
  color: var(--text-main);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.stat-item {
  padding: 14px;
  border-radius: 12px;
  color: var(--text-main);
}

.stat-item.goal {
  background: linear-gradient(135deg, var(--accent-soft), color-mix(in srgb, var(--accent-soft) 75%, var(--accent) 25%));
}

.stat-item.target {
  background: linear-gradient(135deg, color-mix(in srgb, var(--accent-soft) 55%, #ffffff 45%), color-mix(in srgb, var(--accent-soft) 72%, var(--accent) 12%));
}

.stat-item.progress {
  background: linear-gradient(135deg, color-mix(in srgb, var(--accent-soft) 60%, #ffffff 40%), color-mix(in srgb, var(--accent-soft) 70%, var(--accent) 16%));
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
}

.stat-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
}

.plan-descriptions {
  margin-bottom: 16px;
}

.section-row {
  margin-bottom: 16px;
}

.plan-section {
  height: 100%;
  border-radius: 12px;
  padding: 14px;
  background: var(--section-bg);
  border: 1px solid var(--section-border);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 12px;
}

.weekly-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.weekly-item {
  border-radius: 10px;
  padding: 10px 12px;
  color: var(--text-main);
  line-height: 1.5;
  background: color-mix(in srgb, var(--card-bg) 88%, var(--accent-soft) 12%);
  border: 1px solid var(--accent-border);
  border-left: 4px solid var(--accent);
}

.chart-section {
  display: flex;
  flex-direction: column;
}

.chart-box {
  width: 100%;
  height: 260px;
}

.description-section {
  margin-top: 4px;
}

.description-box {
  border-radius: 10px;
  border: 1px solid var(--desc-border);
  background: var(--desc-bg);
  line-height: 1.7;
  min-height: 64px;
  padding: 12px;
  color: var(--text-sub);
}

.empty-panel {
  margin-top: 14px;
  border-radius: 14px;
  background: var(--empty-bg);
  border: 1px solid var(--empty-border);
  box-shadow: 0 8px 22px var(--empty-shadow);
}

:deep(.plan-descriptions .el-descriptions__cell) {
  color: var(--text-main);
  background: var(--table-cell-bg);
}

:deep(.plan-descriptions .el-descriptions__label) {
  color: var(--text-sub);
  background: var(--label-bg);
}

:deep(.el-progress-bar__outer) {
  background-color: var(--progress-track);
}

.theme-dark :deep(.el-empty__description p) {
  color: var(--text-sub);
}

.theme-dark :deep(.theme-switch .el-switch__core) {
  background: rgba(255, 255, 255, 0.2);
}
</style>

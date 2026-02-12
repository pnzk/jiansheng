<template>
  <div class="checkin-page">
    <div class="checkin-header">
      <div>
        <h2>运动打卡</h2>
        <p>选择运动类型并填写时长，即可完成今日打卡</p>
      </div>
      <div class="header-tags">
        <el-tag type="success" effect="dark" round>真实落库</el-tag>
        <el-tag type="warning" effect="dark" round>连续 {{ streakDays }} 天</el-tag>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card class="form-card" shadow="never">
          <template #header>
            <span>打卡信息</span>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
            <el-form-item label="运动类型" prop="exerciseType">
              <el-select
                v-model="form.exerciseType"
                filterable
                clearable
                placeholder="请选择运动类型"
                style="width: 100%"
              >
                <el-option
                  v-for="item in exerciseTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="打卡日期" prop="exerciseDate">
              <el-date-picker
                v-model="form.exerciseDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="时长(分钟)" prop="durationMinutes">
              <el-input-number v-model="form.durationMinutes" :min="1" :max="480" :step="5" />
              <div class="quick-duration">
                <el-button size="small" plain @click="setDuration(20)">20分</el-button>
                <el-button size="small" plain @click="setDuration(30)">30分</el-button>
                <el-button size="small" plain @click="setDuration(45)">45分</el-button>
                <el-button size="small" plain @click="setDuration(60)">60分</el-button>
              </div>
            </el-form-item>

            <el-form-item label="备注">
              <el-input
                v-model="form.notes"
                type="textarea"
                :rows="3"
                maxlength="200"
                show-word-limit
                placeholder="可选：例如今天状态、训练感受"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="submitCheckin">提交打卡</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card class="tips-card" shadow="never">
          <template #header>
            <span>提示</span>
          </template>
          <ul class="tips-list">
            <li>打卡提交后会写入数据库的 <code>exercise_records</code> 表。</li>
            <li>仅需填写“运动类型 + 时长”即可完成打卡记录。</li>
            <li>系统会自动估算卡路里、心率等辅助数据。</li>
            <li>提交后下方“最近打卡记录”会实时刷新。</li>
          </ul>
        </el-card>

        <el-card class="today-card" shadow="never">
          <template #header>
            <span>今日汇总</span>
          </template>
          <div class="today-stats">
            <div class="stat-item">
              <div class="label">今日记录</div>
              <div class="value">{{ todayCount }}</div>
            </div>
            <div class="stat-item">
              <div class="label">今日累计时长</div>
              <div class="value">{{ todayDuration }} 分钟</div>
            </div>
          </div>
          <div class="extra-stat">
            <span>最近打卡：</span>
            <strong>{{ latestCheckinDate || '暂无' }}</strong>
          </div>
        </el-card>

        <el-card class="trend-card" shadow="never">
          <template #header>
            <span>近7日时长趋势</span>
          </template>
          <div class="trend-list">
            <div v-for="item in recent7DayStats" :key="item.date" class="trend-item">
              <div class="trend-label">{{ item.label }}</div>
              <div class="trend-bar-wrap">
                <div class="trend-bar" :style="{ width: `${item.percent}%` }"></div>
              </div>
              <div class="trend-value">{{ item.duration }} 分</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <span>最近打卡记录</span>
          <el-button text @click="loadRecentRecords">刷新</el-button>
        </div>
      </template>

      <el-table :data="recentRecords" v-loading="loadingRecords" style="width: 100%">
        <el-table-column prop="exerciseDate" label="日期" width="120" />
        <el-table-column prop="exerciseType" label="运动类型" min-width="160" />
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="120" />
        <el-table-column prop="caloriesBurned" label="卡路里" width="110">
          <template #default="{ row }">{{ Number(row.caloriesBurned || 0).toFixed(1) }}</template>
        </el-table-column>
        <el-table-column prop="averageHeartRate" label="平均心率" width="110" />
        <el-table-column prop="notes" label="备注" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { addExerciseRecord, getUserExerciseRecords } from '@/api/exercise'
import { getExerciseTypes } from '@/api/exerciseReference'

const formRef = ref(null)
const submitting = ref(false)
const loadingRecords = ref(false)
const recentRecords = ref([])
const exerciseTypeOptions = ref([])

const form = ref({
  exerciseType: '',
  exerciseDate: formatDate(new Date()),
  durationMinutes: 30,
  notes: ''
})

const rules = {
  exerciseType: [{ required: true, message: '请选择运动类型', trigger: 'change' }],
  exerciseDate: [{ required: true, message: '请选择打卡日期', trigger: 'change' }],
  durationMinutes: [{ required: true, message: '请输入时长', trigger: 'change' }]
}

const todayText = computed(() => formatDate(new Date()))

const todayRecords = computed(() => {
  return recentRecords.value.filter((item) => String(item.exerciseDate) === todayText.value)
})

const todayCount = computed(() => todayRecords.value.length)
const todayDuration = computed(() => {
  return todayRecords.value.reduce((sum, item) => sum + Number(item.durationMinutes || 0), 0)
})

const normalizedRecordDates = computed(() => {
  return Array.from(new Set(
    recentRecords.value
      .map((item) => String(item.exerciseDate || ''))
      .filter(Boolean)
  )).sort((left, right) => right.localeCompare(left))
})

const latestCheckinDate = computed(() => normalizedRecordDates.value[0] || '')

const streakDays = computed(() => {
  const dateSet = new Set(normalizedRecordDates.value)
  if (!dateSet.size) {
    return 0
  }

  const today = new Date()
  const todayStr = formatDate(today)
  const yesterday = new Date(today)
  yesterday.setDate(today.getDate() - 1)
  const yesterdayStr = formatDate(yesterday)

  let cursor = null
  if (dateSet.has(todayStr)) {
    cursor = today
  } else if (dateSet.has(yesterdayStr)) {
    cursor = yesterday
  } else {
    return 0
  }

  let count = 0
  while (true) {
    const currentText = formatDate(cursor)
    if (!dateSet.has(currentText)) {
      break
    }
    count += 1
    cursor = new Date(cursor)
    cursor.setDate(cursor.getDate() - 1)
  }
  return count
})

const recent7DayStats = computed(() => {
  const statsMap = new Map()
  for (const item of recentRecords.value) {
    const date = String(item.exerciseDate || '')
    if (!date) continue
    const duration = Number(item.durationMinutes || 0)
    statsMap.set(date, (statsMap.get(date) || 0) + duration)
  }

  const rows = []
  const now = new Date()
  for (let offset = 6; offset >= 0; offset -= 1) {
    const date = new Date(now)
    date.setDate(now.getDate() - offset)
    const dateText = formatDate(date)
    rows.push({
      date: dateText,
      label: dateText.slice(5),
      duration: Number(statsMap.get(dateText) || 0)
    })
  }

  const maxDuration = Math.max(1, ...rows.map((item) => item.duration))
  return rows.map((item) => ({
    ...item,
    percent: Math.round((item.duration / maxDuration) * 100)
  }))
})

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const loadExerciseTypes = async () => {
  try {
    const types = await getExerciseTypes()
    const values = (types || []).filter((item) => item && String(item).trim())
    if (!values.length) {
      exerciseTypeOptions.value = [
        { label: 'RUNNING', value: 'RUNNING' },
        { label: 'CYCLING', value: 'CYCLING' },
        { label: 'SWIMMING', value: 'SWIMMING' },
        { label: 'STRENGTH_TRAINING', value: 'STRENGTH_TRAINING' },
        { label: 'YOGA', value: 'YOGA' }
      ]
      return
    }

    exerciseTypeOptions.value = values.map((item) => ({
      label: String(item),
      value: String(item)
    }))
  } catch {
    exerciseTypeOptions.value = [
      { label: 'RUNNING', value: 'RUNNING' },
      { label: 'CYCLING', value: 'CYCLING' },
      { label: 'SWIMMING', value: 'SWIMMING' },
      { label: 'STRENGTH_TRAINING', value: 'STRENGTH_TRAINING' },
      { label: 'YOGA', value: 'YOGA' }
    ]
  }
}

const loadRecentRecords = async () => {
  loadingRecords.value = true
  try {
    const endDate = todayText.value
    const startDateObj = new Date()
    startDateObj.setDate(startDateObj.getDate() - 29)
    const startDate = formatDate(startDateObj)
    const data = await getUserExerciseRecords({ startDate, endDate })
    recentRecords.value = (data || []).slice(0, 50)
  } catch (error) {
    ElMessage.error(error?.message || '加载打卡记录失败')
  } finally {
    loadingRecords.value = false
  }
}

const submitCheckin = async () => {
  if (submitting.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await addExerciseRecord({
      exerciseType: form.value.exerciseType,
      exerciseDate: form.value.exerciseDate,
      durationMinutes: Number(form.value.durationMinutes),
      notes: form.value.notes || null
    })

    ElMessage.success('打卡成功，已写入数据库')
    resetForm()
    await loadRecentRecords()
  } catch (error) {
    ElMessage.error(error?.message || '打卡失败')
  } finally {
    submitting.value = false
  }
}

const setDuration = (minutes) => {
  form.value.durationMinutes = Number(minutes)
}

const resetForm = () => {
  form.value = {
    exerciseType: '',
    exerciseDate: todayText.value,
    durationMinutes: 30,
    notes: ''
  }
  formRef.value?.clearValidate()
}

onMounted(async () => {
  await loadExerciseTypes()
  await loadRecentRecords()
})
</script>

<style scoped>
.checkin-page {
  padding: 20px;
}

.checkin-header {
  margin-bottom: 16px;
  padding: 18px 20px;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(120deg, #409eff, #6f63ff);
  box-shadow: 0 10px 24px rgba(74, 112, 255, 0.25);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.header-tags {
  display: flex;
  gap: 8px;
}

.checkin-header h2 {
  margin: 0;
  font-size: 28px;
}

.checkin-header p {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.9);
}

.form-card,
.tips-card,
.today-card,
.trend-card,
.table-card {
  margin-bottom: 16px;
  border: 1px solid #e8eefc;
}

.quick-duration {
  margin-left: 12px;
  display: inline-flex;
  gap: 6px;
}

.tips-list {
  margin: 0;
  padding-left: 18px;
  color: #4c5d7d;
  line-height: 1.8;
}

.today-stats {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.stat-item {
  flex: 1;
  border-radius: 10px;
  border: 1px solid #dce7ff;
  background: #f5f9ff;
  padding: 12px;
}

.stat-item .label {
  font-size: 12px;
  color: #7b8aa6;
}

.stat-item .value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 700;
  color: #2d3f63;
}

.extra-stat {
  margin-top: 10px;
  color: #5e6f8f;
  font-size: 13px;
}

.extra-stat strong {
  color: #304a7d;
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trend-item {
  display: grid;
  grid-template-columns: 48px 1fr 56px;
  align-items: center;
  gap: 8px;
}

.trend-label {
  font-size: 12px;
  color: #6f7f9c;
}

.trend-bar-wrap {
  height: 10px;
  border-radius: 999px;
  background: #edf3ff;
  overflow: hidden;
}

.trend-bar {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #5c8dff, #47c7ff);
}

.trend-value {
  text-align: right;
  font-size: 12px;
  color: #5a6a88;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

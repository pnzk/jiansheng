<template>
  <div :class="['plans-page', `theme-${uiTheme}`]" :style="pageStyleVars">
    <div class="page-header gradient-header">
      <div>
        <h2>计划管理</h2>
        <p class="page-subtitle">集中维护训练计划，支持筛选、查看、编辑与复制</p>
      </div>
      <div class="header-tools">
        <el-switch
          v-model="isDarkTheme"
          inline-prompt
          active-text="深色"
          inactive-text="浅色"
          class="theme-switch"
        />
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-row :gutter="20" align="middle">
        <el-col :span="5">
          <el-select v-model="filterStudent" placeholder="选择学员" clearable style="width: 100%">
            <el-option label="全部学员" value="" />
            <el-option
              v-for="student in studentList"
              :key="student.id"
              :label="student.realName"
              :value="student.id"
            />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filterGoal" placeholder="目标类型" clearable style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="减重" value="WEIGHT_LOSS" />
            <el-option label="减脂" value="FAT_LOSS" />
            <el-option label="增肌" value="MUSCLE_GAIN" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filterStatus" placeholder="计划状态" clearable style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="进行中" value="ACTIVE" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-col>
        <el-col :span="11" class="filter-actions">
          <el-button type="primary" @click="applyFilters">查询</el-button>
          <el-button @click="reloadData" plain>
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            添加新计划
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table :data="plansList" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="studentName" label="学员" width="120" />
        <el-table-column prop="planName" label="计划名称" width="200">
          <template #default="{ row }">
            <div class="plan-name-cell">
              <div class="plan-name-main" :style="{ color: getGoalPalette(row.goalType).accent }">
                {{ row.planName }}
              </div>
              <div class="plan-name-sub">{{ row.studentName }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="goalType" label="目标类型" width="110">
          <template #default="{ row }">
            <el-tag :type="getGoalTagType(row.goalType)" size="small" class="goal-tag" :style="getGoalTagStyle(row.goalType)">
              {{ getGoalText(row.goalType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetValue" label="目标值" width="100">
          <template #default="{ row }">{{ row.targetValue }} kg</template>
        </el-table-column>
        <el-table-column label="计划周期" width="220">
          <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completionRate" label="完成率" width="170">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress
                :percentage="Math.round(row.completionRate || 0)"
                :stroke-width="10"
                :color="getProgressColor(row.completionRate || 0, row.goalType)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="viewPlan(row)">
              <el-icon><View /></el-icon>
            </el-button>
            <el-button size="small" text type="warning" @click="editPlan(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button size="small" text type="info" @click="copyPlan(row)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
            <el-button size="small" text type="danger" @click="deletePlan(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" @close="resetPlanForm" class="nice-dialog">
      <el-form :model="planForm" label-width="110px">
        <el-form-item label="选择学员" required>
          <el-select v-model="planForm.studentId" placeholder="请选择学员" :disabled="isEdit" style="width: 100%">
            <el-option
              v-for="student in studentList"
              :key="student.id"
              :label="student.realName"
              :value="student.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="计划名称" required>
          <el-input v-model="planForm.planName" placeholder="请输入计划名称" />
        </el-form-item>
        <el-form-item label="目标类型" required>
          <el-select v-model="planForm.goalType" style="width: 100%">
            <el-option label="减重" value="WEIGHT_LOSS" />
            <el-option label="减脂" value="FAT_LOSS" />
            <el-option label="增肌" value="MUSCLE_GAIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标值" required>
          <el-input-number v-model="planForm.targetValue" :min="0" :max="100" :step="0.5" />
          <span style="margin-left: 10px">kg</span>
        </el-form-item>
        <el-form-item label="日期范围" required>
          <el-date-picker
            v-model="planForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="周训练安排">
          <el-input
            v-model="planForm.weeklyScheduleText"
            type="textarea"
            :rows="4"
            placeholder="例如：周一跑步30分钟；周三力量训练45分钟；周五游泳40分钟"
          />
        </el-form-item>
        <el-form-item label="计划描述">
          <el-input v-model="planForm.description" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePlan" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="计划详情" width="640px" class="nice-dialog">
      <el-descriptions :column="2" border v-if="detailPlan">
        <el-descriptions-item label="学员">{{ detailPlan.studentName }}</el-descriptions-item>
        <el-descriptions-item label="计划名称">{{ detailPlan.planName }}</el-descriptions-item>
        <el-descriptions-item label="目标类型">{{ getGoalText(detailPlan.goalType) }}</el-descriptions-item>
        <el-descriptions-item label="目标值">{{ detailPlan.targetValue }} kg</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ detailPlan.startDate }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ detailPlan.endDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusText(detailPlan.status) }}</el-descriptions-item>
        <el-descriptions-item label="完成率">{{ Math.round(detailPlan.completionRate || 0) }}%</el-descriptions-item>
      </el-descriptions>

      <div v-if="detailPlan" style="margin-top: 16px">
        <h4>周训练安排</h4>
        <el-empty v-if="!detailPlan.weeklyScheduleText" description="暂无周训练安排" :image-size="80" />
        <div v-else class="weekly-plan-box">{{ detailPlan.weeklyScheduleText }}</div>
      </div>

      <div v-if="detailPlan" style="margin-top: 16px">
        <h4>计划描述</h4>
        <div class="weekly-plan-box">{{ detailPlan.description || '暂无描述' }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createTrainingPlan,
  updateTrainingPlan,
  deleteTrainingPlan,
  getCoachTrainingPlans
} from '@/api/trainingPlan'
import { getCoachStudents } from '@/api/user'

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

const route = useRoute()

const filterStudent = ref('')
const filterGoal = ref('')
const filterStatus = ref('')
const uiTheme = ref('light')
const loading = ref(false)
const saving = ref(false)

const allPlans = ref([])
const plansList = ref([])
const studentList = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('创建训练计划')
const isEdit = ref(false)

const detailDialogVisible = ref(false)
const detailPlan = ref(null)

const isDarkTheme = computed({
  get: () => uiTheme.value === 'dark',
  set: (enabled) => {
    uiTheme.value = enabled ? 'dark' : 'light'
  }
})

const currentPalette = computed(() => GOAL_PALETTES[filterGoal.value] || GOAL_PALETTES.FAT_LOSS)

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
    '--progress-track': isDark ? '#263a5c' : '#edf2ff'
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
      '--table-header-bg': '#162544',
      '--table-header-text': '#b8c6e4',
      '--table-hover-bg': '#1a2a48',
      '--text-main': '#e7eefc',
      '--text-sub': '#9fb0cf',
      '--dialog-border': '#24375b',
      '--weekly-bg': '#0f1b32',
      '--weekly-border': '#2a3f65'
    }
  }

  return {
    ...common,
    '--page-bg-start': '#f2f7ff',
    '--page-bg-mid': '#f8fbff',
    '--page-bg-end': '#ffffff',
    '--card-bg': '#ffffff',
    '--card-border': '#e6edfc',
    '--card-shadow': 'rgba(58, 90, 170, 0.08)',
    '--table-header-bg': '#f2f7ff',
    '--table-header-text': '#5c6780',
    '--table-hover-bg': '#f3f8ff',
    '--text-main': '#273655',
    '--text-sub': '#8a95ab',
    '--dialog-border': '#eef2f7',
    '--weekly-bg': '#f8fbff',
    '--weekly-border': '#dfe9fc'
  }
})

const planForm = reactive({
  planId: null,
  studentId: '',
  planName: '',
  goalType: 'WEIGHT_LOSS',
  targetValue: 5,
  dateRange: [],
  weeklyScheduleText: '',
  description: ''
})

onMounted(async () => {
  const studentIdFromQuery = Number(route.query.studentId)
  if (!Number.isNaN(studentIdFromQuery) && studentIdFromQuery > 0) {
    filterStudent.value = studentIdFromQuery
  }
  await reloadData()
})

const safeParseWeeklySchedule = (weeklySchedule) => {
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
        .map(([day, items]) => `${day}: ${Array.isArray(items) ? items.join('、') : items}`)
        .join('；')
    }
    return weeklySchedule
  } catch {
    return weeklySchedule
  }
}

const normalizePlan = (plan) => {
  const student = studentList.value.find((item) => String(item.id) === String(plan.studentId))
  return {
    ...plan,
    studentName: student?.realName || `学员#${plan.studentId}`,
    weeklyScheduleText: safeParseWeeklySchedule(plan.weeklySchedule)
  }
}

const loadStudents = async () => {
  const students = await getCoachStudents()
  studentList.value = (students || []).map((item) => ({
    id: item.id,
    realName: item.realName || item.username
  }))
}

const loadPlans = async () => {
  loading.value = true
  try {
    const plans = await getCoachTrainingPlans()
    allPlans.value = (plans || []).map(normalizePlan)
    applyFilters()
  } catch (error) {
    allPlans.value = []
    plansList.value = []
    ElMessage.error(error?.message || '加载训练计划失败')
  } finally {
    loading.value = false
  }
}

const reloadData = async () => {
  try {
    await loadStudents()
    await loadPlans()
  } catch (error) {
    ElMessage.error(error?.message || '刷新失败')
  }
}

const applyFilters = () => {
  let filtered = [...allPlans.value]

  if (filterStudent.value) {
    filtered = filtered.filter((plan) => String(plan.studentId) === String(filterStudent.value))
  }
  if (filterGoal.value) {
    filtered = filtered.filter((plan) => plan.goalType === filterGoal.value)
  }
  if (filterStatus.value) {
    filtered = filtered.filter((plan) => plan.status === filterStatus.value)
  }

  plansList.value = filtered
}

const getGoalPalette = (goalType) => GOAL_PALETTES[goalType] || GOAL_PALETTES.FAT_LOSS

const getGoalTagType = (type) => {
  const types = { WEIGHT_LOSS: 'danger', FAT_LOSS: 'warning', MUSCLE_GAIN: 'success' }
  return types[type] || 'info'
}

const getGoalTagStyle = (goalType) => {
  const palette = getGoalPalette(goalType)
  return {
    borderColor: palette.accentBorder,
    color: palette.accent,
    background: palette.accentSoft
  }
}

const getGoalText = (type) => {
  const texts = { WEIGHT_LOSS: '减重', FAT_LOSS: '减脂', MUSCLE_GAIN: '增肌' }
  return texts[type] || type
}

const getStatusTagType = (status) => {
  const types = { ACTIVE: 'success', COMPLETED: 'info', CANCELLED: 'danger' }
  return types[status] || 'warning'
}

const getStatusText = (status) => {
  const texts = { ACTIVE: '进行中', COMPLETED: '已完成', CANCELLED: '已取消' }
  return texts[status] || status
}

const getProgressColor = (percentage, goalType) => {
  const palette = getGoalPalette(goalType)
  if (percentage >= 80) return palette.progressStrong
  if (percentage >= 50) return palette.progressMedium
  return palette.progressWeak
}

const resetPlanForm = () => {
  Object.assign(planForm, {
    planId: null,
    studentId: '',
    planName: '',
    goalType: 'WEIGHT_LOSS',
    targetValue: 5,
    dateRange: [],
    weeklyScheduleText: '',
    description: ''
  })
}

const showCreateDialog = () => {
  isEdit.value = false
  dialogTitle.value = '创建训练计划'
  resetPlanForm()
  if (filterStudent.value) {
    planForm.studentId = filterStudent.value
  }
  dialogVisible.value = true
}

const viewPlan = (row) => {
  detailPlan.value = { ...row }
  detailDialogVisible.value = true
}

const editPlan = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑训练计划'
  Object.assign(planForm, {
    planId: row.id,
    studentId: row.studentId,
    planName: row.planName,
    goalType: row.goalType,
    targetValue: row.targetValue,
    dateRange: [row.startDate, row.endDate],
    weeklyScheduleText: row.weeklyScheduleText || '',
    description: row.description || ''
  })
  dialogVisible.value = true
}

const copyPlan = (row) => {
  isEdit.value = false
  dialogTitle.value = '复制训练计划'
  Object.assign(planForm, {
    planId: null,
    studentId: row.studentId,
    planName: `${row.planName}（副本）`,
    goalType: row.goalType,
    targetValue: row.targetValue,
    dateRange: [],
    weeklyScheduleText: row.weeklyScheduleText || '',
    description: row.description || ''
  })
  dialogVisible.value = true
}

const validatePlanForm = () => {
  if (!planForm.studentId) {
    ElMessage.warning('请选择学员')
    return false
  }
  if (!planForm.planName) {
    ElMessage.warning('请输入计划名称')
    return false
  }
  if (!planForm.dateRange || planForm.dateRange.length !== 2) {
    ElMessage.warning('请选择开始和结束日期')
    return false
  }
  return true
}

const savePlan = async () => {
  if (!validatePlanForm()) {
    return
  }

  saving.value = true
  try {
    const payload = {
      studentId: Number(planForm.studentId),
      planName: planForm.planName,
      goalType: planForm.goalType,
      targetValue: Number(planForm.targetValue),
      startDate: planForm.dateRange[0],
      endDate: planForm.dateRange[1],
      weeklySchedule: planForm.weeklyScheduleText || null,
      description: planForm.description || null
    }

    if (isEdit.value) {
      await updateTrainingPlan(planForm.planId, {
        planName: payload.planName,
        goalType: payload.goalType,
        targetValue: payload.targetValue,
        endDate: payload.endDate,
        weeklySchedule: payload.weeklySchedule,
        description: payload.description
      })
      ElMessage.success('计划更新成功')
    } else {
      await createTrainingPlan(payload)
      ElMessage.success('计划创建成功')
    }

    dialogVisible.value = false
    await loadPlans()
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const deletePlan = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该训练计划吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTrainingPlan(row.id)
    ElMessage.success('删除成功')
    await loadPlans()
  } catch (error) {
    const message = String(error?.message || '')
    if (!message.includes('cancel') && !message.includes('取消')) {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}
</script>

<style scoped>
.plans-page {
  padding-bottom: 8px;
  border-radius: 16px;
  color: var(--text-main);
  transition: all 0.3s ease;
  background: linear-gradient(180deg, var(--page-bg-start) 0%, var(--page-bg-mid) 38%, var(--page-bg-end) 100%);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.gradient-header {
  padding: 20px 24px;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(120deg, var(--banner-start), var(--banner-end));
  box-shadow: 0 10px 26px var(--banner-shadow);
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.plans-page h2 {
  margin: 0;
  font-size: 34px;
  line-height: 1.2;
}

.page-subtitle {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.92);
  font-size: 14px;
}

.filter-card,
.table-card {
  border-radius: 12px;
  margin-bottom: 16px;
  border: 1px solid var(--card-border);
  background: var(--card-bg);
  box-shadow: 0 6px 20px var(--card-shadow);
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.plan-name-cell {
  display: flex;
  flex-direction: column;
}

.plan-name-main {
  font-weight: 600;
  color: var(--text-main);
}

.plan-name-sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-sub);
}

.progress-cell {
  padding-right: 8px;
}

.weekly-plan-box {
  background: var(--weekly-bg);
  border: 1px solid var(--weekly-border);
  border-radius: 8px;
  padding: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

:deep(.el-table__header th) {
  background: var(--table-header-bg);
  color: var(--table-header-text);
}

:deep(.el-table__row:hover > td) {
  background-color: var(--table-hover-bg) !important;
}

:deep(.nice-dialog .el-dialog__header) {
  border-bottom: 1px solid var(--dialog-border);
  margin-right: 0;
  padding: 18px 22px;
}

:deep(.nice-dialog .el-dialog__body) {
  padding: 18px 22px;
}

:deep(.el-progress-bar__outer) {
  background-color: var(--progress-track);
}

:deep(.filter-card .el-card__body),
:deep(.table-card .el-card__body),
:deep(.nice-dialog .el-dialog) {
  background: var(--card-bg);
}

:deep(.el-table),
:deep(.el-table tr),
:deep(.el-table td),
:deep(.el-table__inner-wrapper),
:deep(.el-table__body-wrapper),
:deep(.el-table__header-wrapper) {
  color: var(--text-main);
  background: var(--card-bg);
}

:deep(.el-dialog__title),
:deep(.el-form-item__label),
:deep(.el-descriptions__label),
:deep(.el-descriptions__content),
:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  color: var(--text-main);
}

.theme-dark :deep(.el-input__wrapper),
.theme-dark :deep(.el-select__wrapper),
.theme-dark :deep(.el-textarea__inner),
.theme-dark :deep(.el-input-number) {
  background-color: #111e38;
  box-shadow: 0 0 0 1px #2b3e64 inset;
}

.theme-dark :deep(.el-empty__description p) {
  color: var(--text-sub);
}

.theme-dark :deep(.theme-switch .el-switch__core) {
  background: rgba(255, 255, 255, 0.22);
}
</style>

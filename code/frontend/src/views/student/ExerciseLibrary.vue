<template>
  <div class="exercise-library-page">
    <div class="page-header">
      <div>
        <h2>运动库</h2>
        <p>按身体部位与难度快速筛选，查看标准动作说明</p>
      </div>
      <el-tag type="success" effect="dark" round>真实数据</el-tag>
    </div>

    <el-card class="search-card" shadow="never">
      <el-row :gutter="14" align="middle">
        <el-col :span="10">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索动作名称、部位或器材"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-select v-model="selectedBodyPart" clearable placeholder="身体部位" @change="handleFilterChange" style="width: 100%">
            <el-option label="全部部位" value="" />
            <el-option v-for="part in bodyParts" :key="part" :label="part" :value="part" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="selectedLevel" clearable placeholder="难度等级" @change="handleFilterChange" style="width: 100%">
            <el-option label="全部等级" value="" />
            <el-option v-for="level in levels" :key="level" :label="level" :value="level" />
          </el-select>
        </el-col>
        <el-col :span="2" class="refresh-col">
          <el-button @click="reload" plain>
            <el-icon><Refresh /></el-icon>
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="14" class="summary-row">
      <el-col :span="8">
        <el-card class="summary-card" shadow="never">
          <div class="summary-value">{{ total }}</div>
          <div class="summary-label">动作总数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="summary-card" shadow="never">
          <div class="summary-value">{{ bodyParts.length }}</div>
          <div class="summary-label">覆盖部位</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="summary-card" shadow="never">
          <div class="summary-value">{{ levels.length }}</div>
          <div class="summary-label">难度层级</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="list-card" shadow="never">
      <div class="card-title">动作列表</div>

      <el-empty v-if="!exercises.length && !loading" description="暂无匹配动作" :image-size="84" />

      <div v-else class="exercise-grid" v-loading="loading">
        <div class="exercise-item" v-for="exercise in exercises" :key="exercise.id" @click="showDetail(exercise)">
          <div class="exercise-top">
            <div class="exercise-icon">{{ getExerciseIcon(exercise.bodyPart) }}</div>
            <el-tag size="small" class="level-tag" :type="getLevelType(exercise.level)">{{ formatLevel(exercise.level) }}</el-tag>
          </div>

          <div class="exercise-name">{{ exercise.exerciseNameEn }}</div>
          <div class="exercise-sub">{{ formatExerciseType(exercise.exerciseType) }}</div>

          <div class="exercise-meta">
            <el-tag size="small" effect="plain">{{ exercise.bodyPart || '未分类' }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ exercise.equipment || '无需器材' }}</el-tag>
          </div>
        </div>
      </div>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadExercises"
        />
      </div>
    </el-card>

    <el-dialog v-model="showDetailDialog" :title="currentExercise?.exerciseNameEn || '动作详情'" width="640px">
      <div class="detail-content" v-if="currentExercise">
        <div class="detail-head">
          <div class="detail-icon">{{ getExerciseIcon(currentExercise.bodyPart) }}</div>
          <div class="detail-tags">
            <el-tag>{{ formatExerciseType(currentExercise.exerciseType) }}</el-tag>
            <el-tag type="success" effect="light">{{ currentExercise.bodyPart || '未知部位' }}</el-tag>
            <el-tag :type="getLevelType(currentExercise.level)" effect="light">{{ formatLevel(currentExercise.level) }}</el-tag>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">所需器材</div>
          <div class="section-content">{{ currentExercise.equipment || '无需器材' }}</div>
        </div>
        <div class="detail-section">
          <div class="section-title">动作说明</div>
          <div class="section-content">{{ currentExercise.description || '暂无动作说明' }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getExerciseList, getBodyParts, getLevels } from '@/api/exerciseReference'

const searchKeyword = ref('')
const selectedBodyPart = ref('')
const selectedLevel = ref('')
const exercises = ref([])
const bodyParts = ref([])
const levels = ref([])
const loading = ref(false)

const currentPage = ref(1)
const pageSize = ref(18)
const total = ref(0)

const showDetailDialog = ref(false)
const currentExercise = ref(null)

const bodyPartIcons = {
  Abdominals: '🎯',
  Biceps: '💪',
  Chest: '🫁',
  Back: '🔙',
  Shoulders: '🏋️',
  Legs: '🦵',
  Triceps: '💪',
  Glutes: '🍑',
  Quadriceps: '🦵',
  Hamstrings: '🦵',
  Calves: '🦶',
  Forearms: '💪',
  default: '🏃'
}

const getExerciseIcon = (bodyPart) => bodyPartIcons[bodyPart] || bodyPartIcons.default

const getLevelType = (level) => {
  if (level === 'Beginner') return 'success'
  if (level === 'Intermediate') return 'warning'
  if (level === 'Expert') return 'danger'
  return 'info'
}

const formatLevel = (level) => {
  if (level === 'Beginner') return '初级'
  if (level === 'Intermediate') return '中级'
  if (level === 'Expert') return '高级'
  return level || '未知'
}

const formatExerciseType = (exerciseType) => {
  if (!exerciseType) return '训练动作'
  return `${exerciseType}`.replaceAll('_', ' ')
}

const loadExercises = async () => {
  loading.value = true
  try {
    const response = await getExerciseList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      bodyPart: selectedBodyPart.value || undefined,
      level: selectedLevel.value || undefined
    })

    exercises.value = response?.records || []
    total.value = response?.total || 0
  } catch (error) {
    ElMessage.error(error?.message || '加载运动库失败')
  } finally {
    loading.value = false
  }
}

const loadFilters = async () => {
  try {
    const [partsResult, levelsResult] = await Promise.all([getBodyParts(), getLevels()])
    bodyParts.value = partsResult || []
    levels.value = levelsResult || []
  } catch (error) {
    ElMessage.warning(error?.message || '筛选项加载失败，已使用默认显示')
  }
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadExercises()
}

const handleSearch = () => {
  currentPage.value = 1
  loadExercises()
}

const reload = () => {
  loadExercises()
}

const showDetail = (exercise) => {
  currentExercise.value = exercise
  showDetailDialog.value = true
}

onMounted(async () => {
  await loadFilters()
  await loadExercises()
})
</script>

<style scoped>
.exercise-library-page {
  min-height: 100%;
}

.page-header {
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

.page-header h2 {
  margin: 0;
  font-size: 28px;
}

.page-header p {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.9);
}

.search-card {
  margin-bottom: 14px;
}

.refresh-col {
  display: flex;
  justify-content: flex-end;
}

.summary-row {
  margin-bottom: 14px;
}

.summary-card {
  text-align: center;
}

.summary-value {
  font-size: 28px;
  line-height: 1.2;
  font-weight: 700;
  color: #3155a6;
}

.summary-label {
  margin-top: 6px;
  color: #6b7ca1;
}

.card-title {
  margin-bottom: 14px;
  font-size: 16px;
  font-weight: 700;
  color: #2f477d;
}

.exercise-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.exercise-item {
  border: 1px solid #dde8ff;
  border-radius: 12px;
  background: #f8fbff;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.exercise-item:hover {
  border-color: #bcd2ff;
  box-shadow: 0 8px 20px rgba(74, 112, 255, 0.14);
  transform: translateY(-2px);
}

.exercise-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.exercise-icon {
  font-size: 28px;
}

.exercise-name {
  margin-top: 10px;
  font-size: 15px;
  color: #304774;
  font-weight: 700;
}

.exercise-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #7c8cab;
}

.exercise-meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.detail-content {
  padding-top: 2px;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.detail-icon {
  font-size: 42px;
}

.detail-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-section {
  margin-bottom: 12px;
  border: 1px solid #e5edff;
  border-radius: 10px;
  padding: 10px 12px;
  background: #f9fbff;
}

.section-title {
  font-size: 13px;
  color: #6a7ba0;
}

.section-content {
  margin-top: 6px;
  color: #304774;
  line-height: 1.6;
}

@media (max-width: 1280px) {
  .exercise-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 880px) {
  .exercise-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
    gap: 8px;
  }
}
</style>


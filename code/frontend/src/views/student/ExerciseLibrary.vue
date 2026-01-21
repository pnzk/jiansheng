<template>
  <div class="exercise-library">
    <!-- 顶部搜索栏 -->
    <div class="search-header">
      <div class="search-box">
        <el-input v-model="searchKeyword" placeholder="搜索运动..." prefix-icon="Search" size="large" @input="handleSearch" clearable />
      </div>
    </div>

    <!-- 筛选标签 -->
    <div class="filter-section">
      <div class="filter-group">
        <div class="filter-label">身体部位</div>
        <div class="filter-tags">
          <el-tag 
            v-for="part in bodyParts" 
            :key="part" 
            :type="selectedBodyPart === part ? '' : 'info'"
            @click="selectBodyPart(part)"
            class="filter-tag"
          >
            {{ part }}
          </el-tag>
        </div>
      </div>
      
      <div class="filter-group">
        <div class="filter-label">难度等级</div>
        <div class="filter-tags">
          <el-tag 
            v-for="level in levels" 
            :key="level" 
            :type="selectedLevel === level ? '' : 'info'"
            @click="selectLevel(level)"
            class="filter-tag"
          >
            {{ level }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 运动列表 -->
    <div class="exercise-list">
      <div class="exercise-card" v-for="exercise in exercises" :key="exercise.id" @click="showDetail(exercise)">
        <div class="exercise-icon">{{ getExerciseIcon(exercise.bodyPart) }}</div>
        <div class="exercise-info">
          <div class="exercise-name">{{ exercise.exerciseNameEn }}</div>
          <div class="exercise-meta">
            <el-tag size="small" type="info">{{ exercise.bodyPart }}</el-tag>
            <el-tag size="small" :type="getLevelType(exercise.level)">{{ exercise.level }}</el-tag>
          </div>
          <div class="exercise-equipment">
            <span class="equipment-label">器材:</span> {{ exercise.equipment || '无' }}
          </div>
        </div>
        <el-icon class="arrow-icon"><ArrowRight /></el-icon>
      </div>
      
      <el-empty v-if="exercises.length === 0" description="暂无运动数据" />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadExercises"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetailDialog" :title="currentExercise?.exerciseNameEn" width="90%">
      <div class="detail-content" v-if="currentExercise">
        <div class="detail-header">
          <div class="detail-icon">{{ getExerciseIcon(currentExercise.bodyPart) }}</div>
          <div class="detail-tags">
            <el-tag>{{ currentExercise.exerciseType }}</el-tag>
            <el-tag type="info">{{ currentExercise.bodyPart }}</el-tag>
            <el-tag :type="getLevelType(currentExercise.level)">{{ currentExercise.level }}</el-tag>
          </div>
        </div>
        <div class="detail-section">
          <div class="section-title">所需器材</div>
          <div class="section-content">{{ currentExercise.equipment || '无需器材' }}</div>
        </div>
        <div class="detail-section">
          <div class="section-title">动作说明</div>
          <div class="section-content description">{{ currentExercise.description || '暂无说明' }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import { getExerciseList, getBodyParts, getLevels } from '@/api/exerciseReference'

const searchKeyword = ref('')
const selectedBodyPart = ref('')
const selectedLevel = ref('')
const exercises = ref([])
const bodyParts = ref([])
const levels = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const showDetailDialog = ref(false)
const currentExercise = ref(null)

const bodyPartIcons = {
  'Abdominals': '🎯',
  'Biceps': '💪',
  'Chest': '🫁',
  'Back': '🔙',
  'Shoulders': '🏋️',
  'Legs': '🦵',
  'Triceps': '💪',
  'Glutes': '🍑',
  'Quadriceps': '🦵',
  'Hamstrings': '🦵',
  'Calves': '🦶',
  'Forearms': '💪',
  'default': '🏃'
}

const getExerciseIcon = (bodyPart) => {
  return bodyPartIcons[bodyPart] || bodyPartIcons.default
}

const getLevelType = (level) => {
  if (level === 'Beginner') return 'success'
  if (level === 'Intermediate') return 'warning'
  if (level === 'Expert') return 'danger'
  return 'info'
}

const loadExercises = async () => {
  try {
    const res = await getExerciseList({
      page: currentPage.value,
      size: pageSize.value,
      bodyPart: selectedBodyPart.value,
      level: selectedLevel.value
    })
    exercises.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error('加载运动数据失败', e)
  }
}

const loadFilters = async () => {
  try {
    const [partsRes, levelsRes] = await Promise.all([getBodyParts(), getLevels()])
    bodyParts.value = partsRes || []
    levels.value = levelsRes || []
  } catch (e) {
    console.error('加载筛选条件失败', e)
  }
}

const selectBodyPart = (part) => {
  selectedBodyPart.value = selectedBodyPart.value === part ? '' : part
  currentPage.value = 1
  loadExercises()
}

const selectLevel = (level) => {
  selectedLevel.value = selectedLevel.value === level ? '' : level
  currentPage.value = 1
  loadExercises()
}

const handleSearch = () => {
  currentPage.value = 1
  loadExercises()
}

const showDetail = (exercise) => {
  currentExercise.value = exercise
  showDetailDialog.value = true
}

onMounted(() => {
  loadFilters()
  loadExercises()
})
</script>

<style scoped>
.exercise-library {
  padding: 15px;
  background: #f5f5f5;
  min-height: 100%;
}

.search-header {
  margin-bottom: 15px;
}

.filter-section {
  background: white;
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 15px;
}

.filter-group {
  margin-bottom: 12px;
}

.filter-group:last-child {
  margin-bottom: 0;
}

.filter-label {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.filter-tag:hover {
  transform: scale(1.05);
}

.exercise-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.exercise-card {
  display: flex;
  align-items: center;
  background: white;
  border-radius: 12px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.exercise-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.exercise-icon {
  font-size: 32px;
  margin-right: 15px;
}

.exercise-info {
  flex: 1;
}

.exercise-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 6px;
}

.exercise-meta {
  display: flex;
  gap: 6px;
  margin-bottom: 4px;
}

.exercise-equipment {
  font-size: 12px;
  color: #909399;
}

.equipment-label {
  color: #606266;
}

.arrow-icon {
  color: #c0c4cc;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.detail-content {
  padding: 10px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.detail-icon {
  font-size: 48px;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}

.section-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.section-content.description {
  max-height: 200px;
  overflow-y: auto;
}
</style>

<template>
  <div class="calendar-page">
    <h2>运动日历</h2>
    
    <el-card class="filter-card">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        @change="loadExerciseRecords"
      />
      <el-button type="primary" @click="loadExerciseRecords" style="margin-left: 10px">
        查询
      </el-button>
      <el-button type="primary" @click="showAddDialog = true" style="margin-left: 10px">
        添加运动记录
      </el-button>
    </el-card>

    <el-card class="chart-card">
      <div ref="chartRef" style="width: 100%; height: 400px"></div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="exerciseRecords" style="width: 100%">
        <el-table-column prop="exerciseDate" label="日期" width="120" />
        <el-table-column prop="exerciseType" label="运动类型" width="120" />
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="100" />
        <el-table-column prop="caloriesBurned" label="消耗卡路里" width="120" />
        <el-table-column prop="averageHeartRate" label="平均心率" width="100" />
        <el-table-column prop="equipmentUsed" label="使用器材" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="deleteRecord(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showAddDialog" title="添加运动记录" width="500px">
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="运动类型">
          <el-select v-model="addForm.exerciseType" placeholder="请选择">
            <el-option label="跑步" value="RUNNING" />
            <el-option label="骑行" value="CYCLING" />
            <el-option label="游泳" value="SWIMMING" />
            <el-option label="力量训练" value="STRENGTH_TRAINING" />
            <el-option label="瑜伽" value="YOGA" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="addForm.exerciseDate" type="date" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="时长(分钟)">
          <el-input-number v-model="addForm.durationMinutes" :min="1" />
        </el-form-item>
        <el-form-item label="消耗卡路里">
          <el-input-number v-model="addForm.caloriesBurned" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="平均心率">
          <el-input-number v-model="addForm.averageHeartRate" :min="0" />
        </el-form-item>
        <el-form-item label="最大心率">
          <el-input-number v-model="addForm.maxHeartRate" :min="0" />
        </el-form-item>
        <el-form-item label="使用器材">
          <el-input v-model="addForm.equipmentUsed" placeholder="例如：跑步机" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.notes" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addRecord">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getUserExerciseRecords, addExerciseRecord, deleteExerciseRecord } from '@/api/exercise'
import { initChart } from '@/utils/chartTheme'

const dateRange = ref([])
const exerciseRecords = ref([])
const showAddDialog = ref(false)
const chartRef = ref(null)
let chart = null

const addForm = ref({
  exerciseType: '',
  exerciseDate: new Date(),
  durationMinutes: 30,
  caloriesBurned: 200,
  averageHeartRate: 120,
  maxHeartRate: 150,
  equipmentUsed: '',
  notes: ''
})

const loadExerciseRecords = async () => {
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const data = await getUserExerciseRecords(params)
    exerciseRecords.value = data
    renderChart()
  } catch (error) {
    ElMessage.error('加载运动记录失败')
  }
}

const addRecord = async () => {
  try {
    await addExerciseRecord(addForm.value)
    ElMessage.success('添加成功')
    showAddDialog.value = false
    loadExerciseRecords()
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

const deleteRecord = async (id) => {
  try {
    await deleteExerciseRecord(id)
    ElMessage.success('删除成功')
    loadExerciseRecords()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const renderChart = () => {
  if (!chart) {
    chart = initChart(chartRef.value)
  }
  
  const typeCount = {}
  exerciseRecords.value.forEach(record => {
    typeCount[record.exerciseType] = (typeCount[record.exerciseType] || 0) + 1
  })
  
  const option = {
    title: { text: '运动类型分布' },
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: Object.entries(typeCount).map(([name, value]) => ({ name, value }))
    }]
  }
  
  chart.setOption(option)
}

onMounted(() => {
  loadExerciseRecords()
})
</script>

<style scoped>
.calendar-page {
  padding: 20px;
}

.filter-card, .chart-card, .table-card {
  margin-bottom: 20px;
}
</style>

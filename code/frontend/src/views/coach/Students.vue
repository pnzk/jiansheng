<template>
  <div class="students-page">
    <h2>学员管理</h2>

    <!-- 搜索和筛选 -->
    <el-card style="margin-top: 20px">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input
            v-model="searchQuery"
            placeholder="搜索学员姓名或用户名"
            clearable
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterGoal" placeholder="健身目标" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="减脂" value="减脂" />
            <el-option label="增肌" value="增肌" />
            <el-option label="塑形" value="塑形" />
            <el-option label="保持" value="保持" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterStatus" placeholder="训练状态" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="进行中" value="active" />
            <el-option label="未开始" value="inactive" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 学员表格 -->
    <el-card style="margin-top: 20px">
      <el-table :data="studentsList" style="width: 100%" @row-click="viewStudentDetail">
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="fitnessGoal" label="健身目标" width="120" />
        <el-table-column prop="currentWeight" label="当前体重(kg)" width="130" />
        <el-table-column prop="planStatus" label="计划状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.planStatus === 'active'" type="success">进行中</el-tag>
            <el-tag v-else-if="row.planStatus === 'completed'" type="info">已完成</el-tag>
            <el-tag v-else type="warning">未开始</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="planProgress" label="完成率" width="100">
          <template #default="{ row }">
            <span>{{ row.planProgress }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastExerciseDate" label="最后运动时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click.stop="viewStudentDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const searchQuery = ref('')
const filterGoal = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const studentsList = ref([])

onMounted(() => {
  loadStudents()
})

const loadStudents = async () => {
  try {
    // TODO: 调用API获取学员列表
    // const data = await getCoachStudents({ searchQuery, filterGoal, filterStatus, page, pageSize })
    
    // 模拟数据
    studentsList.value = [
      {
        userId: 1,
        realName: '张三',
        age: 25,
        gender: '男',
        fitnessGoal: '减脂',
        currentWeight: 75.5,
        planStatus: 'active',
        planProgress: 65,
        lastExerciseDate: '2024-01-19 10:30'
      },
      {
        userId: 2,
        realName: '李四',
        age: 28,
        gender: '女',
        fitnessGoal: '塑形',
        currentWeight: 58.2,
        planStatus: 'active',
        planProgress: 80,
        lastExerciseDate: '2024-01-19 09:15'
      },
      {
        userId: 3,
        realName: '王五',
        age: 30,
        gender: '男',
        fitnessGoal: '增肌',
        currentWeight: 70.0,
        planStatus: 'completed',
        planProgress: 100,
        lastExerciseDate: '2024-01-18 18:45'
      },
      {
        userId: 4,
        realName: '赵六',
        age: 22,
        gender: '女',
        fitnessGoal: '减脂',
        currentWeight: 62.5,
        planStatus: 'inactive',
        planProgress: 0,
        lastExerciseDate: '2024-01-12 16:20'
      },
      {
        userId: 5,
        realName: '孙七',
        age: 35,
        gender: '男',
        fitnessGoal: '保持',
        currentWeight: 68.0,
        planStatus: 'active',
        planProgress: 45,
        lastExerciseDate: '2024-01-19 07:00'
      }
    ]
    total.value = studentsList.value.length
  } catch (error) {
    ElMessage.error('加载学员列表失败')
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadStudents()
}

const viewStudentDetail = (row) => {
  router.push(`/coach/students/${row.userId}`)
}
</script>

<style scoped>
.students-page h2 {
  margin-bottom: 20px;
}

.el-table {
  cursor: pointer;
}

.el-table__row:hover {
  background-color: #f5f7fa;
}
</style>

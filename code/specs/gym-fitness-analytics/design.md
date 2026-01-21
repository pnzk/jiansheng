# Design Document

## Overview

本系统是一个基于大数据技术的健身房运动行为与健身效果分析系统。系统采用分层架构设计，包括数据采集层、数据处理层、数据存储层、API服务层和前端展示层。

核心技术栈：
- 大数据处理：Hadoop HDFS、Apache Spark
- 后端服务：Spring Boot、MyBatis Plus
- 数据库：MySQL 8.0
- 前端框架：Vue.js 3、ECharts 5
- 认证授权：JWT

系统支持三种用户角色：
1. 管理员（Admin）：拥有最高权限，管理教练和学员，监控健身房运营
2. 教练（Coach）：管理学员信息，制定训练计划，查看学员效果
3. 学员（Student）：查看个人数据，追踪健身效果，完成训练计划

## Architecture

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        数据源层                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐          │
│  │公开数据集│  │爬虫数据  │  │模拟数据生成(<30%)│          │
│  └──────────┘  └──────────┘  └──────────────────┘          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    数据接入与预处理层                        │
│              Apache Spark (Spark SQL)                       │
│         数据清洗、去重、格式统一、缺失值处理                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      数据存储层                              │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │  HDFS            │         │  MySQL           │         │
│  │  (原始数据)      │         │  (分析结果)      │         │
│  └──────────────────┘         └──────────────────┘         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    数据处理与分析层                          │
│              Apache Spark (MLlib)                           │
│         用户行为分析、健身效果分析、关联分析                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      API服务层                               │
│              Spring Boot Application                        │
│         RESTful API、JWT认证、权限控制                      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      前端展示层                              │
│              Vue.js 3 + ECharts 5                           │
│         数据可视化、用户交互、角色页面                       │
└─────────────────────────────────────────────────────────────┘
```

### 技术架构分层

1. **数据采集层**
   - Web Scraper：使用Python + Scrapy框架爬取公开数据
   - Data Generator：使用Java Faker库生成模拟数据
   - Data Validator：验证数据完整性和合法性

2. **数据处理层**
   - Spark SQL：数据清洗、转换、聚合
   - Spark MLlib：机器学习分析（关联规则挖掘、聚类分析）
   - Spark Streaming：实时数据处理（可选）

3. **数据存储层**
   - HDFS：分布式存储原始数据和中间结果
   - MySQL：存储结构化分析结果、用户信息、训练计划等

4. **应用服务层**
   - Spring Boot：提供RESTful API
   - Spring Security + JWT：认证和授权
   - MyBatis Plus：数据访问层

5. **前端展示层**
   - Vue.js 3：组件化开发
   - Vue Router：路由管理
   - Pinia：状态管理
   - ECharts 5：数据可视化
   - Element Plus：UI组件库


## Components and Interfaces

### 1. 数据采集模块 (Data Collection Module)

#### 1.1 Web Scraper Component

**职责**：从公开平台爬取健身相关数据

**接口**：
```java
public interface WebScraperService {
    // 爬取Keep平台数据
    List<ExerciseData> scrapeKeepData(ScraperConfig config);
    
    // 爬取Kaggle数据集
    List<FitnessDataset> scrapeKaggleDatasets(String keyword);
    
    // 验证爬取数据的完整性
    ValidationResult validateScrapedData(List<ExerciseData> data);
    
    // 保存原始数据到HDFS
    void saveToHDFS(List<ExerciseData> data, String hdfsPath);
}
```

**关键类**：
- `ScraperConfig`: 爬虫配置（URL、请求头、代理等）
- `ExerciseData`: 运动数据实体
- `ValidationResult`: 数据验证结果

#### 1.2 Data Generator Component

**职责**：生成模拟健身数据

**接口**：
```java
public interface DataGeneratorService {
    // 生成模拟用户数据
    List<User> generateUsers(int count);
    
    // 生成模拟运动记录
    List<ExerciseRecord> generateExerciseRecords(List<User> users, int recordsPerUser);
    
    // 生成模拟身体指标数据
    List<BodyMetric> generateBodyMetrics(List<User> users, int metricsPerUser);
    
    // 确保模拟数据占比不超过30%
    boolean validateSimulatedDataRatio(int simulatedCount, int totalCount);
}
```

### 2. 数据处理模块 (Data Processing Module)

#### 2.1 Spark Data Cleaner

**职责**：使用Spark SQL清洗和预处理数据

**接口**：
```java
public interface SparkDataCleanerService {
    // 清洗原始数据
    Dataset<Row> cleanRawData(String hdfsInputPath);
    
    // 去重
    Dataset<Row> removeDuplicates(Dataset<Row> data);
    
    // 统一格式
    Dataset<Row> standardizeFormat(Dataset<Row> data);
    
    // 处理缺失值
    Dataset<Row> handleMissingValues(Dataset<Row> data);
    
    // 保存清洗后的数据
    void saveCleanedData(Dataset<Row> data, String hdfsOutputPath);
}
```

#### 2.2 Spark Analyzer

**职责**：使用Spark进行数据分析

**接口**：
```java
public interface SparkAnalyzerService {
    // 用户行为分析
    UserBehaviorAnalysisResult analyzeUserBehavior(Dataset<Row> exerciseData);
    
    // 健身效果分析
    FitnessEffectAnalysisResult analyzeFitnessEffect(Dataset<Row> bodyMetrics, Dataset<Row> exerciseData);
    
    // 运动关联分析
    AssociationRulesResult analyzeExerciseAssociation(Dataset<Row> exerciseData);
    
    // 生成排行榜数据
    LeaderboardData generateLeaderboard(Dataset<Row> data);
    
    // 保存分析结果到MySQL
    void saveAnalysisResults(AnalysisResult result);
}
```

### 3. 数据存储模块 (Data Storage Module)

#### 3.1 HDFS Storage Service

**职责**：管理HDFS存储

**接口**：
```java
public interface HDFSStorageService {
    // 写入数据到HDFS
    void writeToHDFS(String path, byte[] data);
    
    // 从HDFS读取数据
    byte[] readFromHDFS(String path);
    
    // 检查HDFS存储空间
    StorageStatus checkStorageStatus();
    
    // 删除HDFS文件
    void deleteFromHDFS(String path);
}
```

#### 3.2 MySQL Repository Layer

**职责**：MySQL数据访问

**主要Mapper接口**：
```java
// 用户Mapper
public interface UserMapper extends BaseMapper<User> {
    List<User> selectActiveUsers(@Param("days") int days);
    int countUsersByRole(@Param("role") String role);
}

// 运动记录Mapper
public interface ExerciseRecordMapper extends BaseMapper<ExerciseRecord> {
    List<ExerciseRecord> selectByUserId(@Param("userId") Long userId);
    List<ExerciseStatistics> selectExerciseStatistics(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}

// 身体指标Mapper
public interface BodyMetricMapper extends BaseMapper<BodyMetric> {
    List<BodyMetric> selectByUserIdOrderByDate(@Param("userId") Long userId);
    BodyMetric selectLatestByUserId(@Param("userId") Long userId);
}

// 训练计划Mapper
public interface TrainingPlanMapper extends BaseMapper<TrainingPlan> {
    List<TrainingPlan> selectByCoachId(@Param("coachId") Long coachId);
    List<TrainingPlan> selectByStudentId(@Param("studentId") Long studentId);
    TrainingPlan selectActiveByStudentId(@Param("studentId") Long studentId);
}

// 成就Mapper
public interface AchievementMapper extends BaseMapper<Achievement> {
    List<Achievement> selectByUserId(@Param("userId") Long userId);
    void unlockAchievement(@Param("userId") Long userId, @Param("achievementId") Long achievementId);
}
```

### 4. API服务模块 (API Service Module)

#### 4.1 Authentication Service

**职责**：用户认证和授权

**接口**：
```java
public interface AuthService {
    // 用户登录
    LoginResponse login(LoginRequest request);
    
    // 用户注册
    RegisterResponse register(RegisterRequest request);
    
    // 刷新Token
    TokenResponse refreshToken(String refreshToken);
    
    // 登出
    void logout(String token);
    
    // 验证Token
    boolean validateToken(String token);
}
```

#### 4.2 User Service

**职责**：用户管理

**接口**：
```java
public interface UserService {
    // 获取用户信息
    UserProfileResponse getUserProfile(Long userId);
    
    // 更新用户信息
    void updateUserProfile(Long userId, UpdateProfileRequest request);
    
    // 修改密码
    void changePassword(Long userId, ChangePasswordRequest request);
    
    // 更新隐私设置
    void updatePrivacySettings(Long userId, PrivacySettingsRequest request);
}
```

#### 4.3 Exercise Service

**职责**：运动记录管理

**接口**：
```java
public interface ExerciseService {
    // 添加运动记录
    ExerciseRecordResponse addExerciseRecord(Long userId, AddExerciseRequest request);
    
    // 获取用户运动记录
    List<ExerciseRecordResponse> getUserExerciseRecords(Long userId, DateRange dateRange);
    
    // 获取运动统计
    ExerciseStatisticsResponse getExerciseStatistics(Long userId, DateRange dateRange);
    
    // 删除运动记录
    void deleteExerciseRecord(Long userId, Long recordId);
}
```

#### 4.4 Body Metric Service

**职责**：身体指标管理

**接口**：
```java
public interface BodyMetricService {
    // 添加身体指标
    BodyMetricResponse addBodyMetric(Long userId, AddBodyMetricRequest request);
    
    // 获取身体指标历史
    List<BodyMetricResponse> getBodyMetricHistory(Long userId, DateRange dateRange);
    
    // 获取最新身体指标
    BodyMetricResponse getLatestBodyMetric(Long userId);
    
    // 计算BMI
    double calculateBMI(double height, double weight);
}
```

#### 4.5 Training Plan Service

**职责**：训练计划管理

**接口**：
```java
public interface TrainingPlanService {
    // 创建训练计划
    TrainingPlanResponse createTrainingPlan(Long coachId, CreatePlanRequest request);
    
    // 更新训练计划
    TrainingPlanResponse updateTrainingPlan(Long planId, UpdatePlanRequest request);
    
    // 删除训练计划
    void deleteTrainingPlan(Long planId);
    
    // 获取学员的训练计划
    TrainingPlanResponse getStudentTrainingPlan(Long studentId);
    
    // 获取教练的所有训练计划
    List<TrainingPlanResponse> getCoachTrainingPlans(Long coachId);
    
    // 更新计划进度
    void updatePlanProgress(Long planId, ProgressUpdateRequest request);
}
```

#### 4.6 Analytics Service

**职责**：数据分析和统计

**接口**：
```java
public interface AnalyticsService {
    // 获取主页统计数据
    DashboardStatisticsResponse getDashboardStatistics();
    
    // 获取用户行为分析
    UserBehaviorAnalysisResponse getUserBehaviorAnalysis(DateRange dateRange);
    
    // 获取健身效果分析
    FitnessEffectAnalysisResponse getFitnessEffectAnalysis(Long userId, DateRange dateRange);
    
    // 获取排行榜
    LeaderboardResponse getLeaderboard(LeaderboardType type, int limit);
    
    // 获取高峰期预警
    PeakHourWarningResponse getPeakHourWarning();
    
    // 获取器材使用率
    EquipmentUsageResponse getEquipmentUsage();
}
```

#### 4.7 Achievement Service

**职责**：成就系统管理

**接口**：
```java
public interface AchievementService {
    // 获取用户成就
    List<AchievementResponse> getUserAchievements(Long userId);
    
    // 检查并解锁成就
    void checkAndUnlockAchievements(Long userId);
    
    // 获取所有可用成就
    List<AchievementResponse> getAllAchievements();
}
```

### 5. 前端模块 (Frontend Module)

#### 5.1 Vue Router 配置

```javascript
const routes = [
  // 公共路由
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  { path: '/404', component: NotFound },
  
  // 管理员路由
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: 'dashboard', component: AdminDashboard },
      { path: 'coaches', component: CoachManagement },
      { path: 'students', component: StudentManagement },
      { path: 'monitoring', component: GymMonitoring },
      { path: 'analytics', component: AdvancedAnalytics }
    ]
  },
  
  // 教练路由
  {
    path: '/coach',
    component: CoachLayout,
    meta: { requiresAuth: true, role: 'COACH' },
    children: [
      { path: 'dashboard', component: CoachDashboard },
      { path: 'students', component: StudentList },
      { path: 'students/:id', component: StudentAnalytics },
      { path: 'plans', component: TrainingPlans },
      { path: 'reports', component: EffectReports }
    ]
  },
  
  // 学员路由
  {
    path: '/student',
    component: StudentLayout,
    meta: { requiresAuth: true, role: 'STUDENT' },
    children: [
      { path: 'dashboard', component: StudentDashboard },
      { path: 'calendar', component: ExerciseCalendar },
      { path: 'progress', component: ProgressTracking },
      { path: 'plan', component: MyTrainingPlan },
      { path: 'achievements', component: Achievements },
      { path: 'settings', component: Settings }
    ]
  }
];
```

#### 5.2 API Client

```javascript
// API客户端配置
import axios from 'axios';

const apiClient = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器 - 添加JWT Token
apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => Promise.reject(error)
);

// 响应拦截器 - 处理错误
apiClient.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      // Token过期，跳转登录
      router.push('/login');
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

#### 5.3 ECharts 图表组件

**主要图表类型**：
- 折线图：体重变化、体脂率变化
- 柱状图：运动时长分布、运动频率
- 饼图/环形图：运动类型分布、健身目标分布
- 散点图：运动时间与体重关联
- 热力图：各时段在线人数、周运动强度
- 矩形树图：运动类型参与人数
- 桑基图：运动偏好关联
- 3D曲面图：用户活跃时间段
- 双轴图：身体指标与运动数据对比
- 仪表盘：计划完成率


## Data Models

### 核心实体模型

#### 1. User (用户)

```java
@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    private String password; // BCrypt加密
    private String email;
    private String phone;
    
    @TableField("real_name")
    private String realName;
    
    private Integer age;
    private String gender; // MALE, FEMALE
    
    @TableField("user_role")
    private String role; // ADMIN, COACH, STUDENT
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    // 学员特有字段
    @TableField("coach_id")
    private Long coachId; // 所属教练ID
    
    @TableField("fitness_goal")
    private String fitnessGoal; // WEIGHT_LOSS, FAT_LOSS, MUSCLE_GAIN
    
    // 隐私设置
    @TableField("show_in_leaderboard")
    private Boolean showInLeaderboard;
    
    @TableField("allow_coach_view")
    private Boolean allowCoachView;
}
```

#### 2. ExerciseRecord (运动记录)

```java
@Data
@TableName("exercise_records")
public class ExerciseRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("exercise_type")
    private String exerciseType; // RUNNING, CYCLING, SWIMMING, STRENGTH_TRAINING, etc.
    
    @TableField("exercise_date")
    private LocalDate exerciseDate;
    
    @TableField("duration_minutes")
    private Integer durationMinutes;
    
    @TableField("calories_burned")
    private Double caloriesBurned;
    
    @TableField("average_heart_rate")
    private Integer averageHeartRate;
    
    @TableField("max_heart_rate")
    private Integer maxHeartRate;
    
    @TableField("equipment_used")
    private String equipmentUsed; // 使用的器材
    
    private String notes;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
```

#### 3. BodyMetric (身体指标)

```java
@Data
@TableName("body_metrics")
public class BodyMetric {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("measurement_date")
    private LocalDate measurementDate;
    
    @TableField("weight_kg")
    private Double weightKg;
    
    @TableField("body_fat_percentage")
    private Double bodyFatPercentage;
    
    @TableField("height_cm")
    private Double heightCm;
    
    @TableField("bmi")
    private Double bmi;
    
    @TableField("muscle_mass_kg")
    private Double muscleMassKg;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
```

#### 4. TrainingPlan (训练计划)

```java
@Data
@TableName("training_plans")
public class TrainingPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("student_id")
    private Long studentId;
    
    @TableField("coach_id")
    private Long coachId;
    
    @TableField("plan_name")
    private String planName;
    
    @TableField("goal_type")
    private String goalType; // WEIGHT_LOSS, FAT_LOSS, MUSCLE_GAIN
    
    @TableField("target_value")
    private Double targetValue; // 目标值（如减重5kg）
    
    @TableField("start_date")
    private LocalDate startDate;
    
    @TableField("end_date")
    private LocalDate endDate;
    
    @TableField("status")
    private String status; // ACTIVE, COMPLETED, CANCELLED
    
    @TableField("completion_rate")
    private Double completionRate;
    
    @TableField("weekly_schedule")
    private String weeklySchedule; // JSON格式存储周训练安排
    
    private String description;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
```

#### 5. Achievement (成就)

```java
@Data
@TableName("achievements")
public class Achievement {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("achievement_name")
    private String achievementName;
    
    private String description;
    
    @TableField("achievement_type")
    private String achievementType; // EXERCISE_COUNT, CALORIES, WEIGHT_LOSS, etc.
    
    @TableField("threshold_value")
    private Double thresholdValue;
    
    @TableField("icon_url")
    private String iconUrl;
}
```

#### 6. UserAchievement (用户成就)

```java
@Data
@TableName("user_achievements")
public class UserAchievement {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("achievement_id")
    private Long achievementId;
    
    @TableField("unlocked_at")
    private LocalDateTime unlockedAt;
}
```

#### 7. EquipmentUsage (器材使用记录)

```java
@Data
@TableName("equipment_usage")
public class EquipmentUsage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("equipment_name")
    private String equipmentName;
    
    @TableField("usage_date")
    private LocalDate usageDate;
    
    @TableField("usage_hour")
    private Integer usageHour; // 0-23
    
    @TableField("usage_count")
    private Integer usageCount;
    
    @TableField("total_duration_minutes")
    private Integer totalDurationMinutes;
}
```

### 分析结果数据模型

#### 8. UserBehaviorAnalysis (用户行为分析结果)

```java
@Data
@TableName("user_behavior_analysis")
public class UserBehaviorAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("analysis_date")
    private LocalDate analysisDate;
    
    @TableField("most_popular_exercise")
    private String mostPopularExercise;
    
    @TableField("peak_hour_start")
    private Integer peakHourStart;
    
    @TableField("peak_hour_end")
    private Integer peakHourEnd;
    
    @TableField("average_duration_minutes")
    private Double averageDurationMinutes;
    
    @TableField("active_user_count")
    private Integer activeUserCount;
    
    @TableField("exercise_type_distribution")
    private String exerciseTypeDistribution; // JSON格式
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
```

#### 9. Leaderboard (排行榜)

```java
@Data
@TableName("leaderboards")
public class Leaderboard {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("leaderboard_type")
    private String leaderboardType; // TOTAL_DURATION, TOTAL_CALORIES, WEIGHT_LOSS
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("rank")
    private Integer rank;
    
    @TableField("value")
    private Double value;
    
    @TableField("period_start")
    private LocalDate periodStart;
    
    @TableField("period_end")
    private LocalDate periodEnd;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
```

### 数据库表关系

```
users (1) ----< (N) exercise_records
users (1) ----< (N) body_metrics
users (1) ----< (N) user_achievements
users (COACH) (1) ----< (N) training_plans
users (STUDENT) (1) ----< (N) training_plans
achievements (1) ----< (N) user_achievements
```

### HDFS数据存储结构

```
/gym-fitness-data/
├── raw/                          # 原始数据
│   ├── scraped/                  # 爬取的数据
│   │   ├── keep/
│   │   │   └── YYYY-MM-DD/
│   │   └── kaggle/
│   │       └── dataset_name/
│   └── generated/                # 生成的模拟数据
│       └── YYYY-MM-DD/
├── cleaned/                      # 清洗后的数据
│   ├── exercise_records/
│   │   └── YYYY-MM-DD/
│   ├── body_metrics/
│   │   └── YYYY-MM-DD/
│   └── user_profiles/
│       └── YYYY-MM-DD/
└── analysis/                     # 分析结果
    ├── user_behavior/
    │   └── YYYY-MM-DD/
    ├── fitness_effect/
    │   └── YYYY-MM-DD/
    └── association_rules/
        └── YYYY-MM-DD/
```


## Correctness Properties

*属性(Property)是系统在所有有效执行中都应该保持为真的特征或行为——本质上是关于系统应该做什么的形式化陈述。属性是人类可读规范和机器可验证正确性保证之间的桥梁。*

### Property 1: 数据采集比例约束

*For any* 数据采集任务，模拟数据的数量不应超过总数据量的30%

**Validates: Requirements 1.3**

### Property 2: 数据去重正确性

*For any* 清洗后的数据集，不应存在完全相同的记录（基于用户ID、日期、运动类型的组合）

**Validates: Requirements 2.2**

### Property 3: 用户认证令牌有效性

*For any* 有效的JWT令牌，在过期时间之前应该能够通过验证

**Validates: Requirements 10.3, 10.4**

### Property 4: 密码加密不可逆

*For any* 用户密码，存储在数据库中的密码哈希值不应该能够反向解密得到原始密码

**Validates: Requirements 27.1**

### Property 5: BMI计算正确性

*For any* 有效的身高(cm)和体重(kg)值，BMI = 体重 / (身高/100)²，结果应在合理范围内(10-50)

**Validates: Requirements 14.1**

### Property 6: 排行榜排序正确性

*For any* 排行榜数据，排名应该按照对应指标值降序排列，且排名连续无重复

**Validates: Requirements 9.1, 9.2, 9.3**

### Property 7: 隐私设置生效

*For any* 用户设置隐私为不在排行榜显示，该用户不应出现在任何公开排行榜中

**Validates: Requirements 9.4, 19.5**

### Property 8: 训练计划完成率计算

*For any* 训练计划，完成率 = (实际完成的训练次数 / 计划要求的训练次数) × 100%，结果应在0-100之间

**Validates: Requirements 17.5**

### Property 9: 数据时间范围过滤

*For any* 指定时间范围的查询，返回的所有记录的日期都应该在该时间范围内

**Validates: Requirements 15.4, 16.4**

### Property 10: 角色权限隔离

*For any* 用户请求，只有具有相应角色权限的用户才能访问对应的API端点

**Validates: Requirements 10.4, 27.4**

### Property 11: 成就解锁条件

*For any* 成就，只有当用户的统计数据达到或超过成就阈值时，该成就才应被解锁

**Validates: Requirements 18.3**

### Property 12: 教练学员关联

*For any* 教练查询学员数据的请求，只能查询到该教练负责的学员数据

**Validates: Requirements 21.5, 22.5**

### Property 13: 数据存储一致性

*For any* 分析结果数据，在MySQL中存储的数据应该与Spark处理后的结果一致

**Validates: Requirements 3.2, 4.5**

### Property 14: 高峰期预警触发

*For any* 时段，当在线人数超过预设阈值时，系统应该触发高峰期拥堵预警

**Validates: Requirements 12.4**

### Property 15: 运动记录完整性

*For any* 新增的运动记录，必须包含用户ID、运动类型、日期、时长等必填字段

**Validates: Requirements 4.5**

### Property 16: 体重变化趋势计算

*For any* 用户的体重记录序列，体重变化量 = 最新体重 - 初始体重

**Validates: Requirements 5.4**

### Property 17: API响应格式统一

*For any* API响应，都应该包含标准的状态码、消息和数据字段

**Validates: Requirements 26.1, 26.4**

### Property 18: 数据清洗幂等性

*For any* 已清洗的数据，再次执行清洗操作应该得到相同的结果

**Validates: Requirements 2.1**

### Property 19: 用户名唯一性

*For any* 新注册用户，用户名不应与系统中已存在的用户名重复

**Validates: Requirements 10.5**

### Property 20: 训练计划日期有效性

*For any* 训练计划，结束日期应该晚于开始日期

**Validates: Requirements 23.2**


## Error Handling

### 1. 数据采集错误处理

**爬虫错误**：
- 网络超时：重试3次，间隔递增(1s, 2s, 4s)
- 403/429错误：记录日志，跳过该数据源
- 数据格式错误：记录错误日志，继续处理其他数据
- robots.txt违规：立即停止爬取，记录警告

**数据生成错误**：
- 模拟数据超过30%：拒绝生成，返回错误
- 数据验证失败：记录详细错误信息，不保存到HDFS

### 2. 数据处理错误处理

**Spark任务错误**：
- 任务失败：自动重试最多3次
- 内存溢出：调整分区数，增加executor内存
- 数据倾斜：使用salting技术重新分区
- HDFS连接失败：等待30秒后重试

**数据验证错误**：
- 缺失必填字段：记录错误，跳过该记录
- 数据类型不匹配：尝试类型转换，失败则跳过
- 数据范围异常：记录警告，标记为异常数据

### 3. API错误处理

**认证错误**：
- 401 Unauthorized：Token无效或过期，返回错误提示
- 403 Forbidden：权限不足，返回权限错误
- Token过期：前端自动跳转登录页

**业务逻辑错误**：
- 400 Bad Request：参数验证失败，返回详细错误信息
- 404 Not Found：资源不存在，返回友好提示
- 409 Conflict：数据冲突（如用户名重复），返回冲突信息

**系统错误**：
- 500 Internal Server Error：记录详细错误日志，返回通用错误信息
- 503 Service Unavailable：服务暂时不可用，建议稍后重试
- 数据库连接失败：使用连接池重试机制

**错误响应格式**：
```json
{
  "success": false,
  "code": "ERROR_CODE",
  "message": "用户友好的错误信息",
  "details": "详细的技术错误信息（仅开发环境）",
  "timestamp": "2024-01-18T10:30:00Z"
}
```

### 4. 前端错误处理

**网络错误**：
- 请求超时：显示超时提示，提供重试按钮
- 网络断开：显示离线提示，缓存用户操作
- 服务器错误：显示友好错误页面

**数据加载错误**：
- 图表数据加载失败：显示占位符和重试按钮
- 图片加载失败：显示默认占位图
- 分页数据加载失败：保留已加载数据，提示加载失败

**用户操作错误**：
- 表单验证失败：实时显示验证错误信息
- 文件上传失败：显示失败原因，允许重新上传
- 操作权限不足：显示权限提示，禁用相关操作

### 5. 数据一致性错误处理

**HDFS与MySQL不一致**：
- 定期运行一致性检查任务
- 发现不一致时记录详细日志
- 提供数据修复工具

**并发冲突**：
- 使用乐观锁处理并发更新
- 版本号不匹配时返回冲突错误
- 提示用户刷新后重试

### 6. 性能问题处理

**响应时间超过阈值**：
- 记录慢查询日志
- 触发性能监控告警
- 自动分析瓶颈并优化

**内存使用过高**：
- 触发GC
- 记录内存快照
- 必要时重启服务

**HDFS存储空间不足**：
- 触发告警通知管理员
- 自动清理过期数据
- 拒绝新的数据写入


## Testing Strategy

本系统采用双重测试策略：单元测试和基于属性的测试(Property-Based Testing)相结合，确保系统的正确性和可靠性。

### 测试框架选择

**后端测试**：
- JUnit 5：单元测试框架
- jqwik：Java的Property-Based Testing库
- Mockito：Mock框架
- Spring Boot Test：集成测试支持
- TestContainers：数据库集成测试

**前端测试**：
- Vitest：Vue 3单元测试框架
- fast-check：JavaScript的Property-Based Testing库
- Vue Test Utils：Vue组件测试工具
- Cypress：E2E测试框架

**大数据测试**：
- Spark Testing Base：Spark单元测试
- 自定义数据生成器：生成测试数据

### 单元测试策略

单元测试专注于：
- 具体示例的验证
- 边界条件测试
- 错误处理测试
- 集成点测试

**示例**：
```java
@Test
void testBMICalculation_normalCase() {
    // 测试正常情况：身高170cm，体重65kg
    double bmi = bodyMetricService.calculateBMI(170, 65);
    assertEquals(22.49, bmi, 0.01);
}

@Test
void testBMICalculation_edgeCase() {
    // 测试边界情况：极低身高
    assertThrows(IllegalArgumentException.class, 
        () -> bodyMetricService.calculateBMI(50, 65));
}
```

### 基于属性的测试策略

基于属性的测试验证通用属性，每个测试运行最少100次迭代，使用随机生成的输入数据。

**配置要求**：
- 每个属性测试最少100次迭代
- 每个测试必须引用设计文档中的属性编号
- 标签格式：`Feature: gym-fitness-analytics, Property {number}: {property_text}`

**Property 1测试示例**：
```java
@Property
@Label("Feature: gym-fitness-analytics, Property 1: 数据采集比例约束")
void simulatedDataRatioShouldNotExceed30Percent(@ForAll @IntRange(min = 1, max = 1000) int totalCount) {
    // 生成随机数量的模拟数据
    int simulatedCount = random.nextInt(totalCount);
    
    // 验证：如果模拟数据超过30%，应该被拒绝
    if (simulatedCount > totalCount * 0.3) {
        assertFalse(dataGeneratorService.validateSimulatedDataRatio(simulatedCount, totalCount));
    } else {
        assertTrue(dataGeneratorService.validateSimulatedDataRatio(simulatedCount, totalCount));
    }
}
```

**Property 2测试示例**：
```java
@Property
@Label("Feature: gym-fitness-analytics, Property 2: 数据去重正确性")
void cleanedDataShouldNotContainDuplicates(@ForAll("exerciseRecords") List<ExerciseRecord> records) {
    // 清洗数据
    Dataset<Row> cleanedData = sparkDataCleanerService.removeDuplicates(toDataset(records));
    
    // 验证：清洗后的数据不应有重复
    List<Row> rows = cleanedData.collectAsList();
    Set<String> uniqueKeys = new HashSet<>();
    
    for (Row row : rows) {
        String key = row.getLong(0) + "-" + row.getDate(1) + "-" + row.getString(2);
        assertTrue(uniqueKeys.add(key), "Found duplicate record: " + key);
    }
}

@Provide
Arbitrary<List<ExerciseRecord>> exerciseRecords() {
    return Arbitraries.list(
        Combinators.combine(
            Arbitraries.longs().between(1, 1000),
            Arbitraries.of(ExerciseType.values()),
            Arbitraries.integers().between(10, 180)
        ).as((userId, type, duration) -> 
            new ExerciseRecord(userId, type, LocalDate.now(), duration)
        )
    ).ofMinSize(10).ofMaxSize(100);
}
```

**Property 5测试示例**：
```java
@Property
@Label("Feature: gym-fitness-analytics, Property 5: BMI计算正确性")
void bmiCalculationShouldBeCorrect(
    @ForAll @DoubleRange(min = 140.0, max = 220.0) double height,
    @ForAll @DoubleRange(min = 40.0, max = 150.0) double weight
) {
    double bmi = bodyMetricService.calculateBMI(height, weight);
    
    // 验证：BMI = 体重 / (身高/100)²
    double expectedBMI = weight / Math.pow(height / 100, 2);
    assertEquals(expectedBMI, bmi, 0.01);
    
    // 验证：BMI应在合理范围内
    assertTrue(bmi >= 10 && bmi <= 50, "BMI out of reasonable range: " + bmi);
}
```

**Property 6测试示例**：
```java
@Property
@Label("Feature: gym-fitness-analytics, Property 6: 排行榜排序正确性")
void leaderboardShouldBeSortedCorrectly(@ForAll("leaderboardData") List<LeaderboardEntry> entries) {
    // 生成排行榜
    List<LeaderboardEntry> leaderboard = analyticsService.generateLeaderboard(entries);
    
    // 验证：排名连续且按值降序
    for (int i = 0; i < leaderboard.size(); i++) {
        assertEquals(i + 1, leaderboard.get(i).getRank());
        
        if (i > 0) {
            assertTrue(leaderboard.get(i - 1).getValue() >= leaderboard.get(i).getValue(),
                "Leaderboard not sorted correctly");
        }
    }
}
```

**Property 7测试示例**：
```java
@Property
@Label("Feature: gym-fitness-analytics, Property 7: 隐私设置生效")
void usersWithPrivacySettingShouldNotAppearInLeaderboard(
    @ForAll("users") List<User> users
) {
    // 设置部分用户隐私为不显示
    List<User> hiddenUsers = users.stream()
        .filter(u -> !u.getShowInLeaderboard())
        .collect(Collectors.toList());
    
    // 获取排行榜
    List<LeaderboardEntry> leaderboard = analyticsService.getPublicLeaderboard();
    
    // 验证：隐私用户不应出现在排行榜中
    Set<Long> leaderboardUserIds = leaderboard.stream()
        .map(LeaderboardEntry::getUserId)
        .collect(Collectors.toSet());
    
    for (User hiddenUser : hiddenUsers) {
        assertFalse(leaderboardUserIds.contains(hiddenUser.getId()),
            "User with privacy setting appeared in leaderboard: " + hiddenUser.getId());
    }
}
```

**Property 10测试示例**：
```java
@Property
@Label("Feature: gym-fitness-analytics, Property 10: 角色权限隔离")
void usersShouldOnlyAccessAuthorizedEndpoints(
    @ForAll("userWithRole") Pair<User, String> userAndEndpoint
) {
    User user = userAndEndpoint.getFirst();
    String endpoint = userAndEndpoint.getSecond();
    
    // 尝试访问端点
    boolean hasAccess = authService.checkAccess(user, endpoint);
    
    // 验证：只有具有相应角色的用户才能访问
    boolean shouldHaveAccess = isAuthorized(user.getRole(), endpoint);
    assertEquals(shouldHaveAccess, hasAccess,
        String.format("User %s with role %s access to %s is incorrect",
            user.getUsername(), user.getRole(), endpoint));
}
```

### 集成测试策略

**数据库集成测试**：
- 使用TestContainers启动MySQL容器
- 测试Mapper层的CRUD操作
- 验证事务管理和数据一致性

**Spark集成测试**：
- 使用本地Spark模式
- 测试数据清洗和分析流程
- 验证HDFS读写操作

**API集成测试**：
- 测试完整的请求-响应流程
- 验证认证和授权
- 测试错误处理

### E2E测试策略

使用Cypress进行端到端测试：
- 用户登录流程
- 数据录入和查询
- 图表渲染和交互
- 跨页面导航

### 性能测试

**负载测试**：
- 使用JMeter模拟并发用户
- 测试API响应时间
- 验证系统在高负载下的稳定性

**Spark性能测试**：
- 测试大数据量处理性能
- 优化Spark任务配置
- 监控资源使用情况

### 测试覆盖率目标

- 单元测试代码覆盖率：≥ 80%
- 属性测试覆盖所有核心业务逻辑
- 集成测试覆盖所有API端点
- E2E测试覆盖主要用户流程

### 持续集成

- 每次提交自动运行单元测试和属性测试
- 每日运行完整的集成测试套件
- 每周运行性能测试
- 测试失败时阻止代码合并


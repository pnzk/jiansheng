# Requirements Document

## Introduction

本系统是一个基于大数据技术的健身房运动行为与健身效果分析系统。通过收集用户的身体指标数据（身高、体重、体脂率等）和健身房运动数据（健身器械使用、心率、卡路里消耗等），利用Hadoop HDFS、Spark等大数据技术进行存储、处理和分析，最终通过Vue.js和ECharts进行数据可视化展示，为健身者提供科学的数据参考，为健身房运营者提供决策支持。

## Glossary

- **System**: 健身房运动行为与健身效果分析系统
- **User**: 使用系统的人员，包括管理员、教练和学员
- **Admin**: 管理员，拥有最高权限
- **Coach**: 教练，负责管理学员和制定训练计划
- **Student**: 学员，健身房的会员用户
- **HDFS**: Hadoop分布式文件系统，用于存储原始数据
- **Spark**: 大数据处理框架，用于数据清洗和分析
- **MySQL**: 关系型数据库，用于存储分析结果
- **Web_Scraper**: 网络爬虫模块，用于爬取公开数据
- **Data_Generator**: 数据生成器，用于生成模拟数据
- **API_Layer**: Spring Boot应用层，提供RESTful API
- **Frontend**: Vue.js前端应用，负责数据可视化
- **Exercise_Record**: 运动记录，包含运动类型、时长、卡路里等
- **Body_Metric**: 身体指标，包含体重、体脂率、BMI等
- **Training_Plan**: 训练计划，由教练为学员制定

## Requirements

### Requirement 1: 数据采集与爬取

**User Story:** 作为系统管理员，我希望能够从多个数据源采集健身数据，以便为分析提供充足的数据基础。

#### Acceptance Criteria

1. WHEN THE System启动数据采集任务 THEN THE Web_Scraper SHALL从Keep、Kaggle等公开平台爬取健身相关数据
2. WHEN爬取数据时 THEN THE Web_Scraper SHALL遵守robots.txt协议和网站使用条款
3. WHEN爬取的数据量不足时 THEN THE Data_Generator SHALL生成模拟数据补充，且模拟数据占比不超过30%
4. WHEN数据采集完成后 THEN THE System SHALL将原始数据存储到指定位置
5. WHEN爬取过程中遇到错误 THEN THE System SHALL记录错误日志并继续处理其他数据源

### Requirement 2: 数据接入与预处理

**User Story:** 作为数据工程师，我希望系统能够自动清洗和统一不同来源的数据格式，以便后续分析使用。

#### Acceptance Criteria

1. WHEN原始数据进入系统时 THEN THE System SHALL使用Spark SQL进行数据清洗
2. WHEN清洗数据时 THEN THE System SHALL去除重复记录
3. WHEN处理不同数据源时 THEN THE System SHALL将数据统一为标准格式
4. WHEN数据包含缺失值时 THEN THE System SHALL根据预定义规则处理缺失值
5. WHEN清洗完成后 THEN THE System SHALL将清洗后的原始数据存储到HDFS中

### Requirement 3: 数据存储

**User Story:** 作为系统架构师，我希望系统能够合理存储不同类型的数据，以便优化查询性能和存储成本。

#### Acceptance Criteria

1. WHEN数据清洗完成后 THEN THE System SHALL将原始数据存储到HDFS
2. WHEN数据分析完成后 THEN THE System SHALL将分析结果存储到MySQL
3. WHEN存储到MySQL时 THEN THE System SHALL使用合适的索引优化查询性能
4. WHEN HDFS存储空间不足时 THEN THE System SHALL发出告警通知
5. WHEN数据存储失败时 THEN THE System SHALL记录错误并重试最多3次

### Requirement 4: 用户行为分析

**User Story:** 作为健身房管理员，我希望了解用户的运动行为模式，以便优化器材配置和课程安排。

#### Acceptance Criteria

1. WHEN执行用户行为分析时 THEN THE System SHALL统计最受欢迎的运动项目和器材
2. WHEN分析用户活跃度时 THEN THE System SHALL计算各时段的活跃用户数量
3. WHEN分析运动时长时 THEN THE System SHALL计算用户的平均运动时长
4. WHEN分析运动频率时 THEN THE System SHALL统计用户的运动频次分布
5. WHEN分析完成后 THEN THE System SHALL将结果存储到MySQL供前端查询

### Requirement 5: 健身效果分析

**User Story:** 作为学员，我希望看到我的健身效果数据分析，以便了解训练成果和调整训练计划。

#### Acceptance Criteria

1. WHEN执行健身效果分析时 THEN THE System SHALL跟踪用户身体指标的变化趋势
2. WHEN生成排行榜时 THEN THE System SHALL基于运动时长、消耗卡路里等维度排序
3. WHEN分析运动效果时 THEN THE System SHALL分析不同运动组合与体脂率或体重的关联
4. WHEN计算减重效果时 THEN THE System SHALL对比用户的初始体重和当前体重
5. WHEN用户查询个人效果时 THEN THE System SHALL返回该用户的完整效果分析数据

### Requirement 6: 数据可视化 - 主页展示

**User Story:** 作为用户，我希望在主页看到系统的整体统计数据，以便快速了解健身房的整体情况。

#### Acceptance Criteria

1. WHEN用户访问主页时 THEN THE Frontend SHALL显示注册用户总数
2. WHEN显示主页数据时 THEN THE Frontend SHALL显示活跃用户数量
3. WHEN显示主页数据时 THEN THE Frontend SHALL显示总运动时长
4. WHEN显示主页数据时 THEN THE Frontend SHALL显示总消耗卡路里
5. WHEN主页数据更新时 THEN THE Frontend SHALL通过API获取最新的JSON数据

### Requirement 7: 数据可视化 - 用户行为页面

**User Story:** 作为健身房管理员，我希望通过可视化图表了解用户的运动行为，以便优化运营策略。

#### Acceptance Criteria

1. WHEN访问用户行为页面时 THEN THE Frontend SHALL使用矩形树图或饼图展示所有运动类型的参与人数和总时长占比
2. WHEN查看时间分布时 THEN THE Frontend SHALL使用柱状图或折线图展示用户健身时间分布
3. WHEN查看运动时长分布时 THEN THE Frontend SHALL使用柱状图展示用户运动时长分布
4. WHEN查看运动频率时 THEN THE Frontend SHALL使用柱状图展示运动频率分布
5. WHEN图表渲染时 THEN THE Frontend SHALL使用ECharts进行可视化

### Requirement 8: 数据可视化 - 效果分析页面

**User Story:** 作为学员，我希望通过可视化图表看到运动与身体变化的关系，以便评估训练效果。

#### Acceptance Criteria

1. WHEN访问效果分析页面时 THEN THE Frontend SHALL使用散点图展示运动时间与体重减少量的关联
2. WHEN查看个体效果趋势时 THEN THE Frontend SHALL在同一时间线上展示体重、体脂率等身体指标和运动时长、卡路里等运动数据
3. WHEN展示趋势图时 THEN THE Frontend SHALL使用双轴图表，左侧Y轴显示身体指标，右侧Y轴显示运动数据
4. WHEN渲染关联图时 THEN THE Frontend SHALL清晰展示运动和效果之间的滞后关系
5. WHEN用户选择不同时间范围时 THEN THE Frontend SHALL动态更新图表数据

### Requirement 9: 数据可视化 - 排行榜

**User Story:** 作为学员，我希望看到各类排行榜，以便激励自己坚持锻炼。

#### Acceptance Criteria

1. WHEN访问排行榜页面时 THEN THE Frontend SHALL显示总运动时长排行榜
2. WHEN显示排行榜时 THEN THE Frontend SHALL显示总消耗卡路里排行榜
3. WHEN显示排行榜时 THEN THE Frontend SHALL显示减重（减脂）最多排行榜
4. WHEN用户设置隐私时 THEN THE System SHALL根据用户隐私设置决定是否在排行榜显示该用户
5. WHEN排行榜更新时 THEN THE System SHALL每日自动更新排行榜数据

### Requirement 10: 用户认证与授权

**User Story:** 作为系统用户，我希望通过安全的方式登录系统，以便保护我的个人数据。

#### Acceptance Criteria

1. WHEN用户访问登录页面时 THEN THE Frontend SHALL显示登录表单
2. WHEN用户提交登录信息时 THEN THE API_Layer SHALL验证用户名和密码
3. WHEN登录成功时 THEN THE System SHALL返回JWT令牌
4. WHEN用户访问受保护资源时 THEN THE System SHALL验证JWT令牌的有效性
5. WHEN用户注册时 THEN THE System SHALL验证用户信息的完整性和唯一性

### Requirement 11: 管理员功能 - 教练和学员管理

**User Story:** 作为管理员，我希望能够管理教练和学员信息，以便维护系统用户数据。

#### Acceptance Criteria

1. WHEN管理员访问教练管理页面时 THEN THE System SHALL显示所有教练信息列表
2. WHEN管理员操作教练信息时 THEN THE System SHALL支持创建、读取、更新、删除（CRUD）操作
3. WHEN管理员访问学员管理页面时 THEN THE System SHALL显示所有学员信息列表
4. WHEN管理员操作学员信息时 THEN THE System SHALL支持CRUD操作
5. WHEN管理员删除用户时 THEN THE System SHALL要求确认操作以防误删

### Requirement 12: 管理员功能 - 健身房全局监控

**User Story:** 作为管理员，我希望实时监控健身房的运营状况，以便及时做出管理决策。

#### Acceptance Criteria

1. WHEN访问全局监控页面时 THEN THE Frontend SHALL显示高峰期拥堵预警
2. WHEN显示在线人数时 THEN THE Frontend SHALL使用热力图展示各时段在线人数
3. WHEN显示器材使用率时 THEN THE Frontend SHALL计算并展示各器材的使用率
4. WHEN在线人数超过阈值时 THEN THE System SHALL触发拥堵预警
5. WHEN监控数据更新时 THEN THE Frontend SHALL每5分钟自动刷新数据

### Requirement 13: 管理员功能 - 用户行为分析（高级）

**User Story:** 作为管理员，我希望看到更深入的用户行为分析，以便制定精准的运营策略。

#### Acceptance Criteria

1. WHEN访问高级分析页面时 THEN THE Frontend SHALL使用3D曲面图展示用户活跃时间段
2. WHEN分析运动偏好时 THEN THE Frontend SHALL使用桑基图展示运动偏好关联分析
3. WHEN查看关联分析时 THEN THE System SHALL展示不同运动类型之间的转换关系
4. WHEN渲染3D图表时 THEN THE Frontend SHALL确保图表可交互旋转和缩放
5. WHEN数据量过大时 THEN THE System SHALL对数据进行采样以保证渲染性能

### Requirement 14: 学员功能 - 个人健身仪表盘

**User Story:** 作为学员，我希望在首页看到我的健身概况，以便快速了解自己的状态。

#### Acceptance Criteria

1. WHEN学员访问仪表盘时 THEN THE Frontend SHALL显示3个数值卡片：当前体重、体脂率、BMI
2. WHEN显示仪表盘时 THEN THE Frontend SHALL显示高峰期拥堵预警
3. WHEN显示本周概况时 THEN THE Frontend SHALL显示本周运动次数、运动时长、消耗卡路里
4. WHEN显示体重变化时 THEN THE Frontend SHALL使用折线图展示体重变化趋势
5. WHEN有训练计划时 THEN THE Frontend SHALL显示训练计划提醒

### Requirement 15: 学员功能 - 运动日历与进度

**User Story:** 作为学员，我希望通过日历查看我的运动记录，以便回顾训练历史。

#### Acceptance Criteria

1. WHEN访问运动日历页面时 THEN THE Frontend SHALL显示交互式日历
2. WHEN点击日历日期时 THEN THE Frontend SHALL显示当日运动详情
3. WHEN显示运动数据时 THEN THE Frontend SHALL使用数据表格展示详细运动数据
4. WHEN显示运动类型分布时 THEN THE Frontend SHALL使用饼图或环形图展示
5. WHEN日历加载时 THEN THE Frontend SHALL标记有运动记录的日期

### Requirement 16: 学员功能 - 健身效果追踪

**User Story:** 作为学员，我希望追踪我的健身效果，以便评估训练计划的有效性。

#### Acceptance Criteria

1. WHEN访问效果追踪页面时 THEN THE Frontend SHALL使用双轴折线图展示体重和体脂率变化趋势
2. WHEN显示关联分析时 THEN THE Frontend SHALL使用散点图矩阵展示运动时长、卡路里、体重变化的关联
3. WHEN显示运动强度时 THEN THE Frontend SHALL使用热力图展示周运动强度分布
4. WHEN用户选择时间范围时 THEN THE System SHALL返回该时间范围内的效果数据
5. WHEN数据不足时 THEN THE Frontend SHALL显示提示信息

### Requirement 17: 学员功能 - 我的训练计划

**User Story:** 作为学员，我希望查看和管理我的训练计划，以便按计划进行锻炼。

#### Acceptance Criteria

1. WHEN访问训练计划页面时 THEN THE Frontend SHALL显示我的教练基本信息
2. WHEN显示计划信息时 THEN THE Frontend SHALL显示计划目标（减脂/增肌）和目标值
3. WHEN显示进度时 THEN THE Frontend SHALL计算并显示计划完成进度
4. WHEN显示周训练安排时 THEN THE Frontend SHALL展示本周的训练安排
5. WHEN显示完成情况时 THEN THE Frontend SHALL使用条形图对比计划vs实际，使用仪表盘显示完成率

### Requirement 18: 学员功能 - 健身成就与排行榜

**User Story:** 作为学员，我希望看到我的健身成就，以便获得成就感和激励。

#### Acceptance Criteria

1. WHEN访问成就页面时 THEN THE Frontend SHALL显示成就勋章墙
2. WHEN显示成就时 THEN THE Frontend SHALL显示成就名称、描述和解锁时间
3. WHEN用户达成新成就时 THEN THE System SHALL自动解锁并通知用户
4. WHEN显示排行榜时 THEN THE Frontend SHALL显示减重排行榜和运动时长排行榜
5. WHEN用户设置隐私时 THEN THE System SHALL根据隐私设置决定是否显示用户在排行榜中

### Requirement 19: 学员功能 - 个人中心与数据管理

**User Story:** 作为学员，我希望管理我的个人信息和隐私设置，以便保护我的数据安全。

#### Acceptance Criteria

1. WHEN访问个人中心时 THEN THE Frontend SHALL显示用户的个人信息
2. WHEN用户修改信息时 THEN THE System SHALL验证信息的有效性
3. WHEN用户修改密码时 THEN THE System SHALL要求输入旧密码进行验证
4. WHEN用户更新身体指标时 THEN THE System SHALL记录更新时间和历史数据
5. WHEN用户调整隐私设置时 THEN THE System SHALL立即应用新的隐私规则

### Requirement 20: 教练功能 - 学员总览仪表盘

**User Story:** 作为教练，我希望看到我负责的学员总体情况，以便合理安排教学工作。

#### Acceptance Criteria

1. WHEN教练访问仪表盘时 THEN THE Frontend SHALL显示学员总数、男生学员数、女生学员数、平均年龄
2. WHEN显示健身目标分析时 THEN THE Frontend SHALL使用饼图展示减重、减脂、增肌等目标的分布
3. WHEN显示待办事项时 THEN THE Frontend SHALL列出需要处理的事项提醒
4. WHEN数据更新时 THEN THE System SHALL实时计算统计数据
5. WHEN教练首次登录时 THEN THE System SHALL显示欢迎信息和使用指南

### Requirement 21: 教练功能 - 学员管理列表

**User Story:** 作为教练，我希望查看和管理我的学员信息，以便了解学员情况。

#### Acceptance Criteria

1. WHEN教练访问学员管理页面时 THEN THE Frontend SHALL显示学员信息列表
2. WHEN显示学员信息时 THEN THE Frontend SHALL包含姓名、年龄、身高、体重、联系方式等基本信息
3. WHEN教练搜索学员时 THEN THE System SHALL支持按姓名、年龄等条件筛选
4. WHEN教练点击学员时 THEN THE System SHALL跳转到该学员的详细分析页面
5. WHEN学员列表加载时 THEN THE System SHALL只显示该教练负责的学员

### Requirement 22: 教练功能 - 学员详细分析

**User Story:** 作为教练，我希望看到学员的详细分析数据，以便制定个性化训练计划。

#### Acceptance Criteria

1. WHEN访问学员详细分析页面时 THEN THE Frontend SHALL显示学员的基本信息
2. WHEN显示身体变化时 THEN THE Frontend SHALL展示体重变化和体脂变化的趋势图
3. WHEN显示运动习惯时 THEN THE Frontend SHALL显示总运动次数、平均时长、经常运动类型
4. WHEN分析运动效果时 THEN THE System SHALL计算该学员的训练效果指标
5. WHEN教练查看数据时 THEN THE System SHALL根据教练权限控制数据访问

### Requirement 23: 教练功能 - 训练计划制定与管理

**User Story:** 作为教练，我希望为学员制定和管理训练计划，以便指导学员科学锻炼。

#### Acceptance Criteria

1. WHEN访问训练计划管理页面时 THEN THE Frontend SHALL显示所有训练计划列表
2. WHEN教练添加新计划时 THEN THE System SHALL提供计划创建表单
3. WHEN教练编辑计划时 THEN THE System SHALL加载现有计划数据供修改
4. WHEN教练删除计划时 THEN THE System SHALL要求确认操作
5. WHEN教练筛选计划时 THEN THE System SHALL支持按进度、目标、状态等条件筛选

### Requirement 24: 教练功能 - 效果与对比报告

**User Story:** 作为教练，我希望看到学员的效果对比报告，以便评估教学质量。

#### Acceptance Criteria

1. WHEN访问报告页面时 THEN THE Frontend SHALL使用条形图展示减重效果排行榜
2. WHEN显示关联分析时 THEN THE Frontend SHALL使用散点图展示运动量与效果的关联
3. WHEN对比学员进度时 THEN THE Frontend SHALL支持选择多个学员进行对比
4. WHEN生成报告时 THEN THE System SHALL计算各项效果指标
5. WHEN导出报告时 THEN THE System SHALL支持导出PDF或Excel格式

### Requirement 25: 教练功能 - 个人中心

**User Story:** 作为教练，我希望管理我的个人信息，以便保持信息准确。

#### Acceptance Criteria

1. WHEN访问个人中心时 THEN THE Frontend SHALL显示教练的个人信息
2. WHEN教练修改信息时 THEN THE System SHALL验证信息的有效性
3. WHEN教练修改密码时 THEN THE System SHALL要求输入旧密码进行验证
4. WHEN教练调整隐私设置时 THEN THE System SHALL立即应用新的隐私规则
5. WHEN教练更新资质信息时 THEN THE System SHALL记录更新历史

### Requirement 26: API层设计

**User Story:** 作为前端开发者，我希望有清晰的RESTful API接口，以便前端调用后端服务。

#### Acceptance Criteria

1. WHEN前端请求API时 THEN THE API_Layer SHALL返回JSON格式的数据
2. WHEN API调用失败时 THEN THE API_Layer SHALL返回标准的错误响应
3. WHEN API需要认证时 THEN THE API_Layer SHALL验证JWT令牌
4. WHEN API响应时 THEN THE API_Layer SHALL包含适当的HTTP状态码
5. WHEN API文档更新时 THEN THE System SHALL自动生成Swagger文档

### Requirement 27: 数据安全与隐私

**User Story:** 作为用户，我希望我的个人数据得到保护，以便安全使用系统。

#### Acceptance Criteria

1. WHEN用户密码存储时 THEN THE System SHALL使用加密算法加密密码
2. WHEN传输敏感数据时 THEN THE System SHALL使用HTTPS协议
3. WHEN用户设置隐私选项时 THEN THE System SHALL严格遵守用户的隐私设置
4. WHEN访问用户数据时 THEN THE System SHALL验证访问权限
5. WHEN发生数据泄露风险时 THEN THE System SHALL记录安全日志并告警

### Requirement 28: 系统性能与可扩展性

**User Story:** 作为系统架构师，我希望系统具有良好的性能和可扩展性，以便支持大规模用户使用。

#### Acceptance Criteria

1. WHEN并发用户数增加时 THEN THE System SHALL保持响应时间在3秒以内
2. WHEN数据量增长时 THEN THE System SHALL通过Spark分布式处理保证处理速度
3. WHEN HDFS存储扩容时 THEN THE System SHALL支持动态添加存储节点
4. WHEN数据库查询缓慢时 THEN THE System SHALL使用缓存机制优化查询
5. WHEN系统负载过高时 THEN THE System SHALL自动触发负载均衡

# Implementation Plan: Gym Fitness Analytics System

## Overview

本实现计划将健身房运动行为与健身效果分析系统分解为可执行的开发任务。系统采用大数据技术栈（Hadoop HDFS + Spark）+ Spring Boot后端 + Vue.js前端的架构。

实现顺序：
1. 基础设施搭建（数据库、HDFS、Spark环境）
2. 数据采集模块（爬虫 + 数据生成器）
3. 数据处理模块（Spark清洗和分析）
4. 后端API服务（Spring Boot）
5. 前端界面（Vue.js + ECharts）
6. 集成测试和优化

## Tasks

- [-] 1. 环境搭建与基础配置
  - [x] 配置MySQL数据库Schema
  - [ ] 配置Hadoop HDFS集群（可选）
  - [ ] 配置Spark环境（已使用本地模式）
  - [ ] 配置Spring Boot项目结构
  - [ ] 配置Vue.js前端项目
  - _Requirements: 3.1, 3.2_

- [-] 2. 数据模型与数据库设计
  - [x] 2.1 创建数据库Schema
    - 创建users表（用户信息）
    - 创建exercise_records表（运动记录）
    - 创建body_metrics表（身体指标）
    - 创建training_plans表（训练计划）
    - 创建achievements表（成就）
    - 创建user_achievements表（用户成就）
    - 创建equipment_usage表（器材使用）
    - 创建user_behavior_analysis表（行为分析结果）
    - 创建leaderboards表（排行榜）
    - 添加索引和外键约束
    - _Requirements: 3.2_

  - [ ] 2.2 编写数据模型单元测试
    - 测试实体类的getter/setter
    - 测试数据验证注解
    - _Requirements: 3.2_

- [-] 3. 数据采集模块实现
  - [ ] 3.1 实现Web Scraper组件
    - 实现ScraperConfig配置类
    - 实现Keep平台数据爬取
    - 实现Kaggle数据集下载
    - 实现robots.txt协议遵守
    - 实现数据验证逻辑
    - 实现错误处理和重试机制
    - _Requirements: 1.1, 1.2_

  - [ ] 3.2 编写Property测试：数据采集比例约束
    - **Property 1: 数据采集比例约束**
    - **Validates: Requirements 1.3**

  - [x] 3.3 实现Data Generator组件
    - 实现用户数据生成器
    - 实现运动记录生成器
    - 实现身体指标生成器
    - 实现数据比例验证（<30%）
    - 使用Java Faker库生成真实感数据
    - _Requirements: 1.3_

  - [ ] 3.4 编写数据生成器单元测试
    - 测试生成数据的格式正确性
    - 测试数据量控制
    - _Requirements: 1.3_

  - [ ] 3.5 实现HDFS存储接口
    - 实现HDFSStorageService
    - 实现数据写入HDFS
    - 实现数据读取HDFS
    - 实现存储空间检查
    - _Requirements: 1.4, 3.1_

- [ ] 4. Checkpoint - 数据采集模块验证
  - 确保所有测试通过，询问用户是否有问题

- [-] 5. 数据处理模块实现（Spark）
  - [x] 5.1 实现Spark数据清洗组件
    - 配置Spark Session
    - 实现数据读取from HDFS
    - 实现数据去重逻辑
    - 实现格式统一转换
    - 实现缺失值处理
    - 保存清洗后数据到HDFS
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ] 5.2 编写Property测试：数据去重正确性
    - **Property 2: 数据去重正确性**
    - **Validates: Requirements 2.2**

  - [ ] 5.3 编写Property测试：数据清洗幂等性
    - **Property 18: 数据清洗幂等性**
    - **Validates: Requirements 2.1**

  - [x] 5.4 实现Spark数据分析组件
    - 实现用户行为分析（最受欢迎运动、活跃时段、平均时长）
    - 实现健身效果分析（身体指标变化趋势）
    - 实现运动关联分析（运动组合与效果关联）
    - 实现排行榜生成逻辑
    - 保存分析结果到MySQL
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ] 5.5 编写Spark分析单元测试
    - 测试统计计算正确性
    - 测试数据聚合逻辑
    - _Requirements: 4.5, 5.5_

- [ ] 6. Checkpoint - 数据处理模块验证
  - 确保所有测试通过，询问用户是否有问题

- [ ] 7. 用户认证与授权模块
  - [ ] 7.1 实现JWT认证服务
    - 实现JwtUtils工具类
    - 实现Token生成和验证
    - 实现Token刷新机制
    - 配置Spring Security
    - _Requirements: 10.3, 10.4_

  - [ ] 7.2 编写Property测试：用户认证令牌有效性
    - **Property 3: 用户认证令牌有效性**
    - **Validates: Requirements 10.3, 10.4**

  - [ ] 7.3 编写Property测试：密码加密不可逆
    - **Property 4: 密码加密不可逆**
    - **Validates: Requirements 27.1**

  - [ ] 7.4 实现AuthService
    - 实现用户登录
    - 实现用户注册
    - 实现密码加密（BCrypt）
    - 实现登出逻辑
    - _Requirements: 10.1, 10.2, 10.5_

  - [ ] 7.5 编写Property测试：用户名唯一性
    - **Property 19: 用户名唯一性**
    - **Validates: Requirements 10.5**

  - [ ] 7.6 实现角色权限控制
    - 配置角色权限映射
    - 实现权限拦截器
    - 实现权限验证注解
    - _Requirements: 10.4_

  - [ ] 7.7 编写Property测试：角色权限隔离
    - **Property 10: 角色权限隔离**
    - **Validates: Requirements 10.4, 27.4**

- [ ] 8. 用户管理API实现
  - [ ] 8.1 实现UserService和UserController
    - 实现获取用户信息
    - 实现更新用户信息
    - 实现修改密码
    - 实现更新隐私设置
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5_

  - [ ]* 8.2 编写用户管理API单元测试
    - 测试各API端点
    - 测试参数验证
    - _Requirements: 19.1-19.5_

- [ ] 9. 运动记录管理API实现
  - [ ] 9.1 实现ExerciseService和ExerciseController
    - 实现添加运动记录
    - 实现查询运动记录
    - 实现运动统计
    - 实现删除运动记录
    - _Requirements: 15.1, 15.2, 15.3, 15.4_

  - [ ]* 9.2 编写Property测试：运动记录完整性
    - **Property 15: 运动记录完整性**
    - **Validates: Requirements 4.5**

  - [ ]* 9.3 编写Property测试：数据时间范围过滤
    - **Property 9: 数据时间范围过滤**
    - **Validates: Requirements 15.4, 16.4**

- [ ] 10. 身体指标管理API实现
  - [ ] 10.1 实现BodyMetricService和BodyMetricController
    - 实现添加身体指标
    - 实现查询身体指标历史
    - 实现获取最新指标
    - 实现BMI计算
    - _Requirements: 14.1, 16.1, 16.2_

  - [ ]* 10.2 编写Property测试：BMI计算正确性
    - **Property 5: BMI计算正确性**
    - **Validates: Requirements 14.1**

  - [ ]* 10.3 编写Property测试：体重变化趋势计算
    - **Property 16: 体重变化趋势计算**
    - **Validates: Requirements 5.4**

- [ ] 11. 训练计划管理API实现
  - [ ] 11.1 实现TrainingPlanService和TrainingPlanController
    - 实现创建训练计划
    - 实现更新训练计划
    - 实现删除训练计划
    - 实现查询训练计划
    - 实现更新计划进度
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 23.1, 23.2, 23.3, 23.4, 23.5_

  - [ ]* 11.2 编写Property测试：训练计划完成率计算
    - **Property 8: 训练计划完成率计算**
    - **Validates: Requirements 17.5**

  - [ ]* 11.3 编写Property测试：训练计划日期有效性
    - **Property 20: 训练计划日期有效性**
    - **Validates: Requirements 23.2**

  - [ ]* 11.4 编写Property测试：教练学员关联
    - **Property 12: 教练学员关联**
    - **Validates: Requirements 21.5, 22.5**

- [ ] 12. 数据分析API实现
  - [ ] 12.1 实现AnalyticsService和AnalyticsController
    - 实现主页统计数据API
    - 实现用户行为分析API
    - 实现健身效果分析API
    - 实现排行榜API
    - 实现高峰期预警API
    - 实现器材使用率API
    - _Requirements: 6.1-6.5, 7.1-7.5, 8.1-8.5, 9.1-9.5, 12.1-12.5, 13.1-13.5_

  - [ ]* 12.2 编写Property测试：排行榜排序正确性
    - **Property 6: 排行榜排序正确性**
    - **Validates: Requirements 9.1, 9.2, 9.3**

  - [ ]* 12.3 编写Property测试：隐私设置生效
    - **Property 7: 隐私设置生效**
    - **Validates: Requirements 9.4, 19.5**

  - [ ]* 12.4 编写Property测试：高峰期预警触发
    - **Property 14: 高峰期预警触发**
    - **Validates: Requirements 12.4**

- [ ] 13. 成就系统API实现
  - [ ] 13.1 实现AchievementService和AchievementController
    - 实现获取用户成就
    - 实现检查并解锁成就
    - 实现获取所有可用成就
    - _Requirements: 18.1, 18.2, 18.3_

  - [ ]* 13.2 编写Property测试：成就解锁条件
    - **Property 11: 成就解锁条件**
    - **Validates: Requirements 18.3**

- [ ] 14. 管理员功能API实现
  - [ ] 14.1 实现AdminService和AdminController
    - 实现教练管理CRUD
    - 实现学员管理CRUD
    - 实现全局监控数据API
    - 实现高级分析API
    - _Requirements: 11.1-11.5, 12.1-12.5, 13.1-13.5_

  - [ ]* 14.2 编写管理员API单元测试
    - 测试CRUD操作
    - 测试权限控制
    - _Requirements: 11.1-11.5_

- [ ] 15. Checkpoint - 后端API验证
  - 确保所有测试通过，询问用户是否有问题

- [ ] 16. 前端项目搭建
  - [ ] 16.1 创建Vue.js项目结构
    - 初始化Vue 3项目
    - 配置Vue Router
    - 配置Pinia状态管理
    - 安装Element Plus UI库
    - 安装ECharts 5
    - 配置Axios API客户端
    - _Requirements: 6.5, 7.5, 8.5, 9.5_

  - [ ] 16.2 实现通用组件
    - 实现Layout组件
    - 实现导航菜单组件
    - 实现面包屑组件
    - 实现加载状态组件
    - 实现错误提示组件
    - _Requirements: 6.5_

- [ ] 17. 认证页面实现
  - [ ] 17.1 实现登录页面
    - 创建登录表单
    - 实现表单验证
    - 实现登录逻辑
    - 实现Token存储
    - _Requirements: 10.1, 10.2_

  - [ ] 17.2 实现注册页面
    - 创建注册表单
    - 实现表单验证
    - 实现注册逻辑
    - _Requirements: 10.5_

  - [ ] 17.3 实现404页面
    - 创建404错误页面
    - 配置路由重定向
    - _Requirements: 无_

- [ ] 18. 学员页面实现
  - [ ] 18.1 实现个人健身仪表盘
    - 创建Dashboard布局
    - 实现数值卡片组件（体重、体脂率、BMI）
    - 实现高峰期预警组件
    - 实现本周运动概况组件
    - 使用ECharts实现体重变化折线图
    - 实现训练计划提醒组件
    - _Requirements: 14.1-14.5_

  - [ ] 18.2 实现运动日历与进度页面
    - 实现交互式日历组件
    - 实现运动详情弹窗
    - 实现运动数据表格
    - 使用ECharts实现运动类型饼图
    - _Requirements: 15.1-15.5_

  - [ ] 18.3 实现健身效果追踪页面
    - 使用ECharts实现双轴折线图（体重+体脂率）
    - 使用ECharts实现散点图矩阵（运动关联）
    - 使用ECharts实现热力图（周运动强度）
    - 实现时间范围选择器
    - _Requirements: 16.1-16.5_

  - [ ] 18.4 实现我的训练计划页面
    - 显示教练信息
    - 显示计划信息和进度
    - 显示周训练安排
    - 使用ECharts实现条形图（计划vs实际）
    - 使用ECharts实现仪表盘（完成率）
    - _Requirements: 17.1-17.5_

  - [ ] 18.5 实现健身成就与排行榜页面
    - 实现成就勋章墙
    - 实现成就时间线
    - 实现排行榜列表
    - _Requirements: 18.1-18.5_

  - [ ] 18.6 实现个人中心页面
    - 实现个人信息展示和编辑
    - 实现修改密码表单
    - 实现隐私设置
    - 实现系统设置
    - _Requirements: 19.1-19.5_

- [ ] 19. 教练页面实现
  - [ ] 19.1 实现学员总览仪表盘
    - 实现数值卡片（学员统计）
    - 使用ECharts实现饼图（健身目标分析）
    - 实现待办事项提醒
    - _Requirements: 20.1-20.5_

  - [ ] 19.2 实现学员管理列表页面
    - 实现学员信息表格
    - 实现搜索和筛选
    - 实现学员详情跳转
    - _Requirements: 21.1-21.5_

  - [ ] 19.3 实现学员详细分析页面
    - 显示学员基本信息
    - 使用ECharts实现体重/体脂变化图
    - 显示运动习惯分析
    - _Requirements: 22.1-22.5_

  - [ ] 19.4 实现训练计划管理页面
    - 实现计划列表表格
    - 实现添加/编辑/删除计划
    - 实现计划筛选
    - _Requirements: 23.1-23.5_

  - [ ] 19.5 实现效果与对比报告页面
    - 使用ECharts实现减重效果条形图
    - 使用ECharts实现运动量与效果散点图
    - 实现学员进度对比
    - _Requirements: 24.1-24.5_

  - [ ] 19.6 实现教练个人中心页面
    - 实现个人信息展示和编辑
    - 实现修改密码
    - 实现隐私设置
    - _Requirements: 25.1-25.5_

- [ ] 20. 管理员页面实现
  - [ ] 20.1 实现教练管理页面
    - 实现教练信息表格
    - 实现CRUD操作
    - _Requirements: 11.1, 11.2_

  - [ ] 20.2 实现学员管理页面
    - 实现学员信息表格
    - 实现CRUD操作
    - _Requirements: 11.3, 11.4_

  - [ ] 20.3 实现健身房全局监控页面
    - 实现高峰期拥堵预警
    - 使用ECharts实现热力图（各时段在线人数）
    - 显示器材使用率
    - _Requirements: 12.1-12.5_

  - [ ] 20.4 实现用户行为分析页面
    - 使用ECharts实现3D曲面图（用户活跃时段）
    - 使用ECharts实现桑基图（运动偏好关联）
    - _Requirements: 13.1-13.5_

- [ ] 21. 数据可视化页面实现
  - [ ] 21.1 实现主页展示
    - 显示注册用户总数
    - 显示活跃用户数量
    - 显示总运动时长
    - 显示总消耗卡路里
    - _Requirements: 6.1-6.5_

  - [ ] 21.2 实现用户行为页面
    - 使用ECharts实现矩形树图/饼图（运动类型分布）
    - 使用ECharts实现柱状图/折线图（时间分布）
    - 使用ECharts实现柱状图（运动时长分布）
    - 使用ECharts实现柱状图（运动频率）
    - _Requirements: 7.1-7.5_

  - [ ] 21.3 实现效果分析页面
    - 使用ECharts实现散点图（运动时间与体重关联）
    - 使用ECharts实现双轴图（个体效果趋势）
    - _Requirements: 8.1-8.5_

  - [ ] 21.4 实现排行榜页面
    - 实现总运动时长排行榜
    - 实现总消耗卡路里排行榜
    - 实现减重/减脂排行榜
    - _Requirements: 9.1-9.5_

- [ ] 22. Checkpoint - 前端功能验证
  - 确保所有页面正常渲染，询问用户是否有问题

- [ ] 23. API响应格式统一
  - [ ] 23.1 实现统一响应包装
    - 创建Result响应类
    - 实现全局异常处理器
    - 统一错误码定义
    - _Requirements: 26.1, 26.2, 26.4_

  - [ ]* 23.2 编写Property测试：API响应格式统一
    - **Property 17: API响应格式统一**
    - **Validates: Requirements 26.1, 26.4**

- [ ] 24. 数据一致性验证
  - [ ] 24.1 实现HDFS与MySQL一致性检查
    - 实现定期一致性检查任务
    - 实现数据修复工具
    - _Requirements: 3.2, 4.5_

  - [ ]* 24.2 编写Property测试：数据存储一致性
    - **Property 13: 数据存储一致性**
    - **Validates: Requirements 3.2, 4.5**

- [ ] 25. 性能优化
  - [ ] 25.1 后端性能优化
    - 添加Redis缓存
    - 优化数据库查询
    - 配置连接池
    - _Requirements: 28.1, 28.4_

  - [ ] 25.2 Spark性能优化
    - 调整Spark配置参数
    - 优化数据分区策略
    - 实现增量处理
    - _Requirements: 28.2_

  - [ ] 25.3 前端性能优化
    - 实现路由懒加载
    - 优化图表渲染
    - 实现数据分页
    - _Requirements: 28.1_

- [ ] 26. 集成测试
  - [ ]* 26.1 编写端到端测试
    - 测试用户登录流程
    - 测试数据录入流程
    - 测试数据查询流程
    - 测试图表渲染
    - _Requirements: 所有_

  - [ ]* 26.2 编写API集成测试
    - 测试完整请求-响应流程
    - 测试认证和授权
    - 测试错误处理
    - _Requirements: 26.1-26.5_

- [ ] 27. 部署配置
  - [ ] 27.1 配置生产环境
    - 配置HDFS集群
    - 配置Spark集群
    - 配置MySQL主从复制
    - 配置Nginx反向代理
    - _Requirements: 28.2, 28.3_

  - [ ] 27.2 编写部署文档
    - 编写安装指南
    - 编写配置说明
    - 编写运维手册
    - _Requirements: 无_

- [ ] 28. Final Checkpoint - 系统验证
  - 运行完整测试套件
  - 验证所有功能正常
  - 询问用户是否有问题

## Notes

- 标记为`*`的任务是可选的测试任务，可以跳过以加快MVP开发
- 每个任务都引用了具体的需求编号，确保可追溯性
- Checkpoint任务确保增量验证
- Property测试验证通用正确性属性
- 单元测试验证具体示例和边界情况

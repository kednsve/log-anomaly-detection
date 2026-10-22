# 基于访问日志的异常检测与可视化分析系统

系统上传 CSV 格式的 Web 访问日志，完成脏数据处理后存入数据库。支持流量记录的分页与条件查询，并提供可视化展示：访问趋势图、IP 访问 TOP、访问目标 TOP、IP 归属地分布、异常统计。

异常检测采用规则 + 无监督学习双层机制。规则层检测高频访问、大量 404、异常耗时等明确异常；模型层由 Python 服务实现，从数据库读取日志，按 IP 聚合构造统计特征（请求频率、404 比例、URL 熵、唯一 URL 比例等），用 Isolation Forest 输出异常判定与分数，通过 FastAPI 暴露接口供 SpringBoot 调用。两层结果合并后生成告警，支持告警管理与处理状态跟踪。

| 技术栈（Java后端) | 用途               |
| :---------------- | ------------------ |
| SpringBoot 3.0.2  | 后端框架           |
| MyBatis-Plus      | 与数据库交互与分页 |
| MySQL             | 数据库系统         |
| EasyExcel         | 数据导入与导出     |
| Redis             | 缓存               |
| JWT               | 登录认证           |

| 技术栈（前端） | 用途        |
| -------------- | ----------- |
| Vue3           | 前端框架    |
| Element Plus   | 组件库      |
| Echarts        | 可视化图表  |
| Axios          | 调用后端API |
| Pinia          | 状态存储    |
| Vue Router     | 路由管理    |

| 模型检测（Python） | 用途                           |
| ------------------ | ------------------------------ |
| FastAPI            | 对外暴露接口                   |
| Pandas             | 读取数据库、数据清洗、特征工程 |
| Numpy              | 数值计算                       |
| scikit-learn       | 机器学习Isolation Forest模型   |
| joblib             | 模型保存与加载                 |
| pymysql            | 连接数据库                     |

# 项目目录结构

## 整体结构

log-anomaly-system/
├── backend/ # SpringBoot 后端
├── frontend/ # Vue3 前端
└── detection-service/ # Python 检测服务

---

## 一、后端 `backend/`

backend/
├── pom.xml
└── src/
└── main/
├── java/
│ └── com/yourname/loganomaly/
│ ├── LogAnomalyApplication.java # 启动类
│ │
│ ├── config/ # 配置
│ │ ├── WebConfig.java # 跨域、拦截器
│ │ ├── RedisConfig.java # Redis 序列化配置
│ │ └── MybatisPlusConfig.java # 分页插件
│ │
│ ├── common/ # 公共模块
│ │ ├── Result.java # 统一返回体
│ │ ├── PageResult.java # 分页返回体
│ │ ├── exception/
│ │ │ ├── BusinessException.java
│ │ │ └── GlobalExceptionHandler.java
│ │ └── utils/
│ │ ├── JwtUtil.java
│ │ ├── IpUtil.java # 获取真实IP、校验IP、判断内网
│ │ └── CsvUtil.java # CSV解析与脏数据处理
│ │
│ ├── controller/ # 接口层
│ │ ├── AuthController.java # 登录
│ │ ├── UploadController.java # CSV上传
│ │ ├── LogController.java # 日志查询/分页/导出
│ │ ├── DetectController.java # 触发检测
│ │ ├── AlertController.java # 告警管理
│ │ └── StatsController.java # 可视化统计
│ │
│ ├── service/ # 业务层
│ │ ├── AuthService.java
│ │ ├── UploadService.java # 解析+脏数据处理+入库
│ │ ├── LogService.java
│ │ ├── DetectService.java # 调Python + 规则检测 + 合并
│ │ ├── AlertService.java
│ │ ├── StatsService.java
│ │ └── IpLocationService.java # IP归属地 + Redis缓存
│ │
│ ├── mapper/ # MyBatis-Plus Mapper
│ │ ├── AccessLogMapper.java
│ │ ├── AlertMapper.java
│ │ ├── UserMapper.java
│ │ └── UploadTaskMapper.java
│ │
│ ├── entity/ # 数据库实体
│ │ ├── AccessLog.java
│ │ ├── Alert.java
│ │ ├── User.java
│ │ └── UploadTask.java
│ │
│ ├── dto/ # 请求/响应对象
│ │ ├── LoginDTO.java
│ │ ├── LogQueryDTO.java
│ │ ├── DetectRequestDTO.java
│ │ └── DetectResultDTO.java
│ │
│ ├── vo/ # 前端展示对象
│ │ ├── LogVO.java
│ │ ├── AlertVO.java
│ │ ├── TrendVO.java
│ │ ├── TopIpVO.java
│ │ └── IpLocationVO.java
│ │
│ ├── rule/ # 规则检测引擎
│ │ ├── RuleEngine.java
│ │ ├── Rule.java
│ │ └── impl/
│ │ ├── HighFrequencyRule.java
│ │ ├── ScanRule.java
│ │ └── SlowResponseRule.java
│ │
│ └── client/ # 调Python的客户端
│ └── PythonDetectClient.java
│
└── resources/
├── application.yml
├── mapper/ # XML（复杂SQL）
│ ├── AccessLogMapper.xml
│ └── StatsMapper.xml
└── sql/
└── init.sql # 建表语句

---

## 二、前端 `frontend/`

frontend/
├── package.json
├── vite.config.js
├── index.html
└── src/
├── main.js
├── App.vue
│
├── api/ # 接口封装
│ ├── request.js # axios 实例 + 拦截器
│ ├── auth.js
│ ├── upload.js
│ ├── log.js
│ ├── detect.js
│ ├── alert.js
│ └── stats.js
│
├── router/
│ └── index.js # 路由 + 登录守卫
│
├── stores/ # Pinia
│ ├── user.js # 用户信息/token
│ └── detect.js # 检测任务状态
│
├── layout/
│ ├── index.vue # 整体布局（侧边栏+顶栏）
│ ├── Sidebar.vue
│ └── Header.vue
│
├── views/ # 页面
│ ├── login/
│ │ └── index.vue
│ ├── dashboard/
│ │ └── index.vue # 可视化大屏（重点）
│ ├── upload/
│ │ └── index.vue # CSV上传
│ ├── log/
│ │ └── index.vue # 日志列表+筛选+分页
│ ├── alert/
│ │ └── index.vue # 告警管理
│ └── report/
│ └── index.vue # 统计报表+导出
│
├── components/ # 可复用组件
│ ├── charts/
│ │ ├── TrendChart.vue # 访问趋势折线图
│ │ ├── TopIpChart.vue # IP TOP 柱状图
│ │ ├── TopUrlChart.vue # URL TOP 柱状图
│ │ ├── LocationMap.vue # 归属地分布
│ │ └── AlertPieChart.vue # 异常统计饼图
│ ├── UploadFile.vue
│ └── LogTable.vue
│
└── utils/
├── auth.js # token 存取
└── format.js # 时间格式化等

---

## 三、Python 检测服务 `detection-service/`

detection-service/
├── requirements.txt
├── main.py # FastAPI 入口
├── config.py # 数据库连接配置
│
├── api/
│ └── detect.py # /detect 路由
│
├── core/
│ ├── features.py # 特征工程（按IP聚合）
│ ├── detector.py # Isolation Forest 加载+预测
│ └── rules.py # 规则检测（Python侧备份）
│
├── db/
│ └── connection.py # pymysql/SQLAlchemy 连接
│
├── models/ # 训练好的模型文件
│ ├── iforest_model.pkl
│ ├── scaler.pkl
│ └── feature_cols.pkl
│
├── train/
│ └── train.py # 离线训练脚本（跑一次）
│
└── tests/
└── test_detect.py # 简单测试

---

## 四、数据库表（`init.sql` 概要）

| 表名          | 说明                                             |
| ------------- | ------------------------------------------------ |
| `user`        | 管理员账号                                       |
| `access_log`  | 访问日志（核心表，含规则/模型/最终三层异常标记） |
| `alert`       | 异常告警记录                                     |
| `upload_task` | 上传任务记录与脏数据统计                         |

---

## 五、开发顺序建议

| 阶段 | 做什么                                   | 涉及目录                                          |
| ---- | ---------------------------------------- | ------------------------------------------------- |
| 1    | Python 读 CSV 跑通检测                   | `detection-service/train/`                        |
| 2    | Python 包成 FastAPI                      | `detection-service/main.py`                       |
| 3    | SpringBoot 建项目、建表、上传解析入库    | `backend/`                                        |
| 4    | SpringBoot 调 Python、规则检测、合并结果 | `backend/client/` + `rule/`                       |
| 5    | 前端登录 + 上传 + 日志列表               | `frontend/views/upload` + `log`                   |
| 6    | 前端可视化大屏                           | `frontend/views/dashboard` + `components/charts/` |
| 7    | 告警管理 + 报表导出                      | `frontend/views/alert` + `report`                 |
| 8    | IP归属地 + Redis 缓存                    | `backend/service/IpLocationService`               |
| 9    | 论文实验：规则 vs 模型 vs 结合           | `detection-service/tests/`                        |

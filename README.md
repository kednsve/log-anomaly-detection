# 基于访问日志的异常检测与可视化分析系统

## 一、项目目标

本系统用于 Web 访问日志的异常检测与分析：

1. 上传 CSV 格式的访问日志，完成校验与脏数据处理后存入 MySQL；
2. 按 IP、状态码、时间范围等条件分页查询访问日志与异常告警；
3. 对一次上传任务执行「规则 + 模型」两层异常检测，合并生成告警；
4. （规划）提供访问趋势、IP/URL TOP、归属地分布、异常统计等可视化页面。

数据集：CSE-CIC-IDS 2018。

## 二、当前实现状态

| 模块 | 状态 | 说明 |
| --- | --- | --- |
| JWT 登录 | 已实现 | `POST /login`，`PUT /login` 可重置 admin 密码 |
| CSV 异步上传 | 已实现 | 上传后创建任务，后台解析入库，支持轮询与分页查询 |
| 访问日志查询/更新 | 已实现 | `GET /AccessLog/query`、`PUT /AccessLog/update` |
| 告警查询/更新 | 已实现 | `GET /Alert/query`、`PUT /Alert/update` |
| 规则引擎 | 已实现 | 三条规则：高频访问、大量 404、认证失败；生成告警、回写标记并记录 `rule_detect` 状态 |
| Python 模型检测 | 已实现 | FastAPI 推理服务（`/detect`、`/online`）+ Java 客户端；Isolation Forest 输出每 IP 异常分数，与规则层合并告警并支持幂等重跑 |
| 异步检测 | 已实现 | 三个检测入口均为「提交即返回」，前端轮询 `/uploadTask/polling` 获取 `rule_detect` / `model_detect` 与检测摘要 |
| 统计接口 | 未实现 | — |
| Vue 前端 | 未实现 | 仅有结构规划（见设计文档 06） |
| Redis | 未启用 | 依赖与配置均处于注释状态 |

## 三、异常检测机制

系统按「规则 + 模型」两层设计：

| 层次 | 手段 | 定位 | 状态 |
| --- | --- | --- | --- |
| 规则层 | 高频访问、大量 404、认证失败 | 高精度识别行为特征明显的攻击 | 已实现 |
| 模型层 | Isolation Forest（FastAPI 推理服务） | 识别低频、伪装流量 | 已实现 |

规则层把请求级日志按 IP 聚合为行为特征，用 `application.yml` 中的阈值判定，将结果写入 `access_log.rule_abnormal` / `rule_type` / `final_abnormal`，并为每个异常 IP 生成一条 `alert`。模型层由独立 FastAPI 服务提供：`db` 按任务读取 `access_log`，`core/features.py` 聚合 25 个行为特征，加载 `models/isolation_forest.joblib` 输出每个 IP 的异常分数，Java 再回写 `access_log.model_score` / `model_abnormal` / `final_abnormal`。`final_abnormal = rule_abnormal OR model_abnormal`，告警来源通过 `alert_source` 区分 `rule` / `model` / `both`；两层检测均支持幂等重跑。

三个检测入口（`/Detect/runRule`、`/Detect/runModel`、`/Detect/runAll`）都是异步提交：接口立即返回，客户端轮询 `/uploadTask/polling` 的 `rule_detect` / `model_detect` 状态，完成后可从 `rule_detail` / `model_detail` 读取耗时与异常数摘要。

原设计中的「慢响应」规则已移除：当前合成数据没有真实慢速攻击样本，开启只会产生误报；待数据生成器修正后再恢复。

## 四、数据集

本地 `data/` 目录存放 6 个 CIC-IDS2018 日文件：

| 文件 | 日期 | 攻击内容 |
| --- | --- | --- |
| `Wednesday1.csv` | 2018-02-14 | FTP-BruteForce、SSH-Bruteforce |
| `Friday1.csv` | 2018-02-16 | DoS-Hulk、DoS-SlowHTTPTest |
| `Wednesday2.csv` | 2018-02-21 | DDoS-HOIC、DDoS-LOIC-UDP |
| `Friday2.csv` | 2018-02-23 | Brute Force-Web/XSS、SQL Injection（约 566 行攻击） |
| `Thursday1.csv` | 2018-03-01 | Infiltration |
| `Friday3.csv` | 2018-03-02 | Bot |

`data/cleaned/` 由 `clean_data.py` 生成，每个原始文件对应 `-train.csv`、`-demo.csv`、`-truth.csv` 三份数据。

## 五、技术栈

### 5.1 Java 后端

| 技术 | 版本/说明 |
| --- | --- |
| Spring Boot | 3.3.5 |
| Java | 17 |
| Maven | 构建工具（无 wrapper） |
| MyBatis-Plus | 3.5.10.1（含分页插件） |
| MySQL | 业务数据库 |
| EasyExcel | 3.3.4，CSV 解析 |
| JJWT | 0.13.0，JWT 签发与校验 |
| spring-security-crypto | BCrypt 密码加密 |
| Lombok | 实体、构造器、日志 |
| Jackson | JSON 与日期格式化 |
| fastjson2 | 2.0.66，失败明细 JSON |
| SLF4J + Logback | 日志 |

Redis 依赖已注释，当前未使用。

### 5.2 Python

| 技术 | 说明 |
| --- | --- |
| Python | 3.12.10（`.venv`） |
| pandas / numpy | 数据清洗与采样 |
| FastAPI | 推理服务：`POST /detect`（模型打分）、`GET /online`（探活） |
| ip2region | 离线 IP 归属地查询（`ipdb/ip2region_v4.xdb`） |
| scikit-learn / joblib | Isolation Forest 训练、离线评估与推理加载 |
| pymysql / SQLAlchemy / uvicorn | 推理服务读取 MySQL 与 HTTP 服务 |

### 5.3 前端

Vue3、Element Plus、ECharts、Axios、Pinia、Vue Router、Vite。当前仓库中没有 `frontend/` 目录。

## 六、快速开始

```bash
# 1. 初始化数据库（会重建表并写入 admin/123456）
mysql -u root -p < sql/init.sql

# 2. 启动后端（在 backend/log-anomaly-detection/ 下）
mvn spring-boot:run

# 3. 生成清洗数据（在 backend/detection-service/ 下）
.venv/Scripts/python.exe clean_data.py

# 4. 训练模型（生成 models/isolation_forest.joblib）
.venv/Scripts/python.exe -m train.train --contamination 0.12

# 5. 启动推理服务（Java 模型检测依赖，端口 8000）
.venv/Scripts/python.exe -m uvicorn main:app --reload --port 8000
```

接口默认监听 `http://localhost:8080`，除 `/login` 外需携带 `Authorization: Bearer <token>`。规则检测不依赖 Python；模型检测与「全部检测」需要 8000 端口的推理服务在线。检测接口为异步提交，前端/测试通过 `GET /uploadTask/polling?taskId=` 观察 `rule_detect` / `model_detect` 状态与 `rule_detail` / `model_detail` 摘要。

## 七、设计文档

| 编号 | 文档 | 内容 |
| --- | --- | --- |
| 01 | [项目结构总览](doc/design/01-项目结构总览.md) | 顶层目录与两个后端模块 |
| 02 | [数据库设计](doc/design/02-数据库设计.md) | 4 张表的字段、类型、默认值、索引 |
| 03 | [后端设计](doc/design/03-后端设计.md) | SpringBoot 目录结构、分层、接口与配置 |
| 04 | [规则引擎设计](doc/design/04-规则引擎设计.md) | 特征聚合、三条规则、写回幂等、检测接口 |
| 05 | [Python 数据生成与检测服务](doc/design/05-Python数据生成与检测服务.md) | 数据生成、特征工程、Isolation Forest 训练与评估、FastAPI 推理服务 |
| 06 | [前端设计（规划）](doc/design/06-前端设计（规划）.md) | Vue3 前端结构规划与接口对应关系 |

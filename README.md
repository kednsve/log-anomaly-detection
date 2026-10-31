# 基于访问日志的异常检测与可视化分析系统

系统上传 CSV 格式的 Web 访问日志，完成脏数据处理后存入数据库。支持流量记录的分页与条件查询，并提供可视化展示：访问趋势图、IP 访问 TOP、访问目标 TOP、IP 归属地分布、异常统计。

异常检测采用规则 + 无监督学习双层机制。规则层检测高频访问、大量 404、异常耗时等明确异常；模型层由 Python 服务实现，从数据库读取日志，按 IP 聚合构造统计特征（请求频率、404 比例、URL 熵、唯一 URL 比例等），用 Isolation Forest 输出异常判定与分数，通过 FastAPI 暴露接口供 SpringBoot 调用。两层结果合并后生成告警，支持告警管理与处理状态跟踪。

数据集：CSE-CIC-IDS 2018

| 技术栈（Java 后端）        | 用途                               |
| -------------------------- | ---------------------------------- |
| SpringBoot 3.3.5           | 后端框架                           |
| Java 17                    | 开发语言与运行时                   |
| Maven                      | 项目构建与依赖管理                 |
| MyBatis-Plus               | 与数据库交互与分页                 |
| MySQL                      | 数据库系统                         |
| EasyExcel                  | CSV 解析、数据导入与导出           |
| Redis                      | 缓存                               |
| JWT（jjwt）                | 登录认证                           |
| Spring Security Crypto     | BCrypt 密码加密                    |
| Lombok                     | 简化实体类、构造器与日志代码       |
| Jackson                    | JSON 序列化与日期格式化            |
| fastjson2                  | 失败明细与拦截器响应的 JSON 序列化 |
| JUnit 5 / Spring Boot Test | 单元测试与上下文测试               |
| SLF4J + Logback            | 日志记录                           |

| 技术栈（前端） | 用途                 |
| -------------- | -------------------- |
| Vue3           | 前端框架             |
| Element Plus   | 组件库               |
| Echarts        | 可视化图表           |
| Axios          | 调用后端API          |
| Pinia          | 状态存储             |
| Vue Router     | 路由管理             |
| Vite           | 前端构建与开发服务器 |

| 模型检测（Python） | 用途                           |
| ------------------ | ------------------------------ |
| Python 3.12        | 运行环境                       |
| FastAPI            | 对外暴露接口                   |
| Uvicorn            | ASGI 服务器                    |
| Pandas             | 读取数据库、数据清洗、特征工程 |
| Numpy              | 数值计算                       |
| scikit-learn       | 机器学习Isolation Forest模型   |
| joblib             | 模型保存与加载                 |
| pymysql            | 连接数据库                     |
| ip2region          | 离线 IP 归属地查询             |

## 设计文档

设计文档已按章节拆分到 [`doc/design/`](doc/design/)：

| 序号 | 文档 | 内容 |
| --- | --- | --- |
| 01 | [系统概述](doc/design/01-系统概述.md) | 系统目标、检测机制、数据集、技术栈 |
| 02 | [项目整体结构](doc/design/02-项目结构总览.md) | 顶层目录划分 |
| 03 | [后端设计](doc/design/03-后端设计.md) | SpringBoot 后端目录结构与各包职责 |
| 04 | [前端设计](doc/design/04-前端设计.md) | Vue3 前端目录结构与页面/组件划分 |
| 05 | [Python 检测服务](doc/design/05-Python检测服务.md) | FastAPI 检测服务目录结构 |
| 06 | [数据库设计](doc/design/06-数据库设计.md) | 4 张表的字段、类型、默认值、索引 |


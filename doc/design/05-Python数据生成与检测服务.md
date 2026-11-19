# 05 - Python 数据生成与检测服务

> 版本：2026-11-18

## 一、当前状态

`backend/detection-service/` 目前包含离线数据生成脚本和一个 FastAPI 桩，**没有检测接口、特征工程、模型或数据库客户端**。

```text
backend/detection-service/
├── clean_data.py              # 数据清洗、采样、生成训练/演示/真值文件
├── personas.py                # 合成 IP 画像与访问行为
├── main.py                    # FastAPI Hello World 桩
├── test_main.http             # 接口测试文件（端口 8000）
├── requirements.txt           # 依赖版本（Python 3.12）
├── ipdb/
│   └── ip2region_v4.xdb       # 离线 IP 归属地库（已跟踪）
└── .venv/                     # 本地虚拟环境（gitignore，Windows 布局）
```

- Python 版本：3.12.10；
- 已安装：fastapi、uvicorn、pandas、numpy、scikit-learn、joblib、pymysql、sqlalchemy、ip2region；
- 依赖版本锁定在 `requirements.txt`。

## 二、数据生成脚本

### 2.1 clean_data.py

读取 `data/*.csv`，为每个原始文件生成三份数据到 `data/cleaned/`：

- `<stem>-train.csv`：按时间排序的前 70%；
- `<stem>-demo.csv`：后 30%；
- `<stem>-truth.csv`：IP 级真值（`ip, is_attack, label, attack_type, request_count, location, user_agent`）。

输入/输出目录在脚本顶部硬编码为 `F:/MyProjects/log-anomaly-detection/...`。

处理流程：

1. 只读取 7 列：`Dst Port, Protocol, Timestamp, Flow Duration, TotLen Fwd Pkts, TotLen Bwd Pkts, Label`，其余列忽略；
2. 丢弃标签为空、NaN 以及标签为表头值 `Label` 的行；
3. `Timestamp` 按 `%d/%m/%Y %H:%M:%S` 解析，去重；
4. 过滤 HTTP 端口 `{80, 443, 8000, 8080, 8888}`（`ONLY_HTTP_PORTS` 已定义但未使用，过滤实际无条件执行）；若过滤后不足 1 万行或没有攻击行，回退到全部端口；
5. 每个文件目标 1 万行，攻击行最多占 30%（`ATTACK_RATIO`）；攻击不足时全部保留，其余用良性行补足。

采样与分块：

- 良性流量按 5 分钟间隔切分会话，每块最多 20 行；
- 攻击流量按 10 分钟切分攻击活动，每块最多 20 行；
- 会话/块分配给 IP 画像：良性按权重分配，攻击保证每个攻击 IP 至少拿到一个块。

输出列与后端 `CsvUtil` 完全一致（13 列，顺序一致）。

### 2.2 personas.py

提供合成行为层：

- 200 个良性 IP、20 个攻击 IP，其中包含固定 C2 `18.219.211.138`；
- 每个画像固定归属地、User-Agent、URL 子集、权重和慢响应标记；
- 前两个非 C2 攻击 IP 被设置为低频画像；
- 攻击画像：Botnet、BruteForce、DDoS、WebAttack、Infiltration、Other；
- 标签映射：`Infilteration`（原始拼写错误）按 infiltration 处理；DoS/DDoS 映射为 DDoS 画像；`Brute Force -XSS` 映射为 BruteForce；`SQL Injection` 映射为 WebAttack；
- URL、状态码、方法、响应时间合成时加入 10%–15% 的「看起来正常」的攻击请求，以及少量良性 404/500/POST 噪声，使两类数据不完全可分。

### 2.3 已知数据问题：慢速攻击不真实

`personas.py` 把所有 DoS/DDoS 标签（包括 `DoS-SlowHTTPTest`）统一映射为 DDoS 画像，响应时间合成得很快。因此数据中不存在真实的慢速攻击样本，Java 规则层的「慢响应」规则已移除。

恢复慢响应检测的前提：

1. 修改 `personas.py`，为 SlowHTTPTest 生成真实的秒级响应时间；
2. 在聚合 SQL、`IpFeature` 中补回慢请求特征；
3. 重新加入慢响应规则与配置。

## 三、FastAPI 桩

`main.py` 目前只有一个 Hello World 接口，`test_main.http` 指向 `http://localhost:8000`：

```
uvicorn main:app --reload --port 8000
```

当前 `.venv` 已安装所需依赖，版本见 `requirements.txt`；在其他环境运行时先执行 `pip install -r requirements.txt`。

## 四、规划中的检测服务（未实现）

后续接入模型层时，计划补充以下结构：

```text
detection-service/
├── requirements.txt
├── main.py                  # FastAPI 入口
├── config.py                # 数据库连接配置
├── api/
│   └── detect.py            # /detect 路由
├── core/
│   ├── features.py          # 按 IP 聚合特征
│   ├── detector.py          # Isolation Forest 加载与预测
├── db/
│   └── connection.py        # pymysql / SQLAlchemy
├── models/                  # 训练好的模型、scaler、特征列
├── train/
│   └── train.py             # 离线训练脚本
└── tests/
    └── test_detect.py
```

规划中的分工：

- Java 后端在规则检测完成后调用 Python 检测接口；
- Python 从 `access_log` 读取日志，按 IP 聚合特征，输出 `model_abnormal` / `model_score`；
- Java 将模型结果与规则结果合并为 `final_abnormal`，并把同一 `(task_id, ip)` 的告警来源更新为 `both`；
- Java 在调用前把 `upload_task.model_detect` 置 1，成功后置 2、失败置 3；Python 服务不直接修改 `upload_task`。

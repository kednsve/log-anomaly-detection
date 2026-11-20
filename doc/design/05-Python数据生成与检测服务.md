# 05 - Python 数据生成与检测服务

## 一、当前状态

`backend/detection-service/` 已包含数据生成、特征工程、离线训练与评估、训练好的 Isolation Forest 模型，以及 FastAPI 推理服务。Java 后端通过 `ModelDetectClient` 调用推理服务，并负责全部数据库写回。

```text
backend/detection-service/
├── config.py                  # 路径、采样、训练常量与数据库连接
├── clean_data.py              # 数据清洗、采样、生成训练/演示/真值文件
├── personas.py                # 合成 IP 画像与访问行为
├── core/features.py           # IP 级特征聚合（build_features）
├── core/inference.py          # 模型加载与打分（score_logs）
├── train/train.py             # 离线训练与跨文件评估
├── db/database.py             # MySQL 只读客户端（load_logs）
├── api/schema.py              # 请求/响应模型
├── api/detect.py              # POST /detect 推理路由
├── models/                    # 训练产物（isolation_forest.joblib）
├── main.py                    # FastAPI 入口（/detect + /online）
├── requirements.txt           # 依赖版本（Python 3.12）
├── ipdb/
│   └── ip2region_v4.xdb       # 离线 IP 归属地库（已跟踪）
└── .venv/                     # 本地虚拟环境（gitignore，Windows 布局）
```

- Python 版本：3.12.10；
- 依赖：fastapi、uvicorn、pandas、numpy、scikit-learn、joblib、pymysql、sqlalchemy、ip2region，版本锁定在 `requirements.txt`；
- 模型文件：`models/isolation_forest.joblib`；
- 当前训练使用 `contamination=0.12`；
- 推理服务只读 `access_log`，不写任何表。

## 二、数据生成脚本

### 2.1 clean_data.py

读取 `data/*.csv`，为每个原始文件生成三份数据到 `data/cleaned/`：

- `<stem>-train.csv`：按时间排序的前 70%；
- `<stem>-demo.csv`：后 30%；
- `<stem>-truth.csv`：IP 级真值（`ip, is_attack, label, attack_type, request_count, location, user_agent`）。

输入/输出目录和 `SAMPLE_SIZE` 来自 `config.py`（`DATA_DIR`、`CLEANED_DIR`、`SAMPLE_SIZE=30000`），不再硬编码本机路径。

处理流程：

1. 只读取 7 列：`Dst Port, Protocol, Timestamp, Flow Duration, TotLen Fwd Pkts, TotLen Bwd Pkts, Label`，其余列忽略；
2. 丢弃标签为空、NaN 以及标签为表头值 `Label` 的行；
3. `Timestamp` 按 `%d/%m/%Y %H:%M:%S` 解析，并按上述列去重；
4. 正常流和攻击流**分开做端口过滤**：
   - 攻击流：HTTP 端口行数达到 `need_attack` 时保留 HTTP 端口，否则回退到全部端口；
   - 正常流：HTTP 端口行数达到 `need_benign` 时保留 HTTP 端口，否则回退到全部端口，只保留 TCP/UDP 行，并将输出端口统一映射为 80；
   - `need_attack = min(攻击行数, SAMPLE_SIZE × ATTACK_RATIO)`，`need_benign = min(正常行数, SAMPLE_SIZE − need_attack)`；
5. 采样目标：
   - `attack_target = min(攻击行数, SAMPLE_SIZE × ATTACK_RATIO)`，当前为最多 9000 行；
   - `benign_target = min(正常行数, SAMPLE_SIZE − attack_target)`；
   - 正常行不足时宁可少输出，不再用攻击行补足。

采样与分块：

- 正常流量按 5 分钟间隔切会话，每块最多 20 行；
- 攻击流量按 10 分钟切 campaign，每块最多 20 行；
- 会话/块分配给 IP 画像：正常流按权重随机分配；攻击流先保证每个攻击画像拿到一个块（选中块不少于 20 个时），其余按权重分配；
- 攻击流的权重在画像 shuffle **之后**计算，避免权重和画像错位。

输出与真值：

- 输出 13 列，顺序与后端 `CsvUtil` 完全一致；
- truth 只写实际生成过请求的 IP，因此正常 IP 通常少于 200 个；
- train/demo 是按时间 7:3 切分，**同一个 IP 可能同时出现在两边**，模型评估必须跨文件留出；
- `ATTACK_RATIO` 是攻击行数上限，不是严格的输出占比上限；当前 30k 数据正常行充足，攻击量足够的文件实际接近 30%，Friday2 因攻击行少约为 1.9%。

### 2.2 personas.py

提供合成行为层：

- 200 个正常 IP、20 个攻击 IP，其中包含固定 C2 `18.219.211.138`；
- 每个画像固定归属地、User-Agent、URL 子集、权重和慢响应标记；
- 前两个非 C2 攻击 IP 被设置为低频画像（权重 0.2）；
- 攻击画像：Botnet、BruteForce、DDoS、WebAttack、Infiltration、Other；
- 标签映射：`Infilteration`（原始拼写错误）按 infiltration 处理；DoS/DDoS 映射为 DDoS 画像；`Brute Force -XSS` 映射为 BruteForce；`SQL Injection` 映射为 WebAttack；
- URL、状态码、方法、响应时间合成时加入 10%–15% 的「看起来正常」的攻击请求，以及少量正常 404/500/POST 噪声，使两类数据不完全可分；
- ip2region 数据库路径来自 `config.py` 的 `IPDB_PATH`。

### 2.3 已知数据问题

**慢速攻击不真实**

`personas.py` 把所有 DoS/DDoS 标签（包括 `DoS-SlowHTTPTest`）统一映射为 DDoS 画像，响应时间合成得很快。因此数据中不存在真实的慢速攻击样本，Java 规则层的「慢响应」规则已移除。恢复慢响应检测需要先修改 `personas.py`，再补回特征和规则。

**Friday1 原始数据攻击占比高**

Friday1 的攻击流绝大多数在 80 端口，正常流大多不在 HTTP 端口，所以旧版统一端口过滤会几乎删光正常行。当前版本改为正常流/攻击流分别过滤，正常流不足时回退到全部端口，已能生成约 140 个正常 IP。

**正常 IP 数量少于 200**

正常块按权重随机分配，不是每个正常画像都保证有数据；truth 只写有请求的 IP，因此每个文件的正常 IP 通常约 120–150 个。

**train/demo 共享 IP**

按时间切分导致同一 IP 同时出现在 train 和 demo，不能直接用同文件 demo 评估；当前训练采用跨文件留出。

## 三、特征工程

`core/features.py` 的入口函数是 `build_features`（训练与推理共用），输入是一行一条访问日志的 DataFrame，输出一行一个 IP 的特征表：`ip` + 25 个特征。

| 类别 | 特征 |
| --- | --- |
| 频率 | 总请求数、活跃分钟数、平均每分钟请求、单分钟峰值 |
| 状态码 | 404、401/403、5xx、200 占比 |
| URL 多样性 | 唯一路径数、唯一路径占比、URL 熵（按去掉查询参数的路径计算） |
| 方法 | GET/POST/PUT/HEAD 占比 |
| 响应时间 | 平均、最大、P95、慢请求（>1s）占比 |
| 流量 | 平均发送/接收字节、平均连接时长 |
| 时间 | 夜间（0–6 点）请求占比、请求间隔均值/标准差 |

设计点：

- 不使用 IP、归属地、UA 本身作为特征，避免模型记住身份；
- URL 先去查询参数，避免正常用户的 `/search?q=xxx` 被算成大量唯一 URL；
- 不使用标准化，Isolation Forest 是树模型，量纲不影响切分；
- `FEATURE_COLUMNS` 固定特征顺序，是训练和推理的共同契约。

## 四、训练与评估

`train/train.py` 的流程：

1. 扫描 `data/cleaned/*-train.csv`，配对 train/demo/truth；
2. 每个文件的 train/demo 只聚合一次特征；
3. **留一文件交叉评估**：用其余 5 个文件的 train 训练，在剩余文件的 demo 上测试，并对照 truth 计算指标；
4. 指标：Precision、Recall、F1、AUC、PR-AUC、混淆矩阵；
5. 按 `attack_type` 统计每类攻击的检出率（模型不预测类型，仅用于评估）；
6. 用全部 6 个文件的 train 数据重新训练最终模型，保存到 `models/isolation_forest.joblib`。

模型与参数：

```text
IsolationForest(
    n_estimators=300,
    contamination=0.12,
    random_state=42,
    n_jobs=-1,
)
```

训练时**不使用 truth 标签**，truth 只用于评估，保持无监督异常检测的定位。

当前 30k 数据、`contamination=0.12` 的一次交叉评估结果：

| 指标 | 值 |
| --- | --- |
| 平均 Precision | 0.742 |
| 平均 Recall | 0.916 |
| 平均 F1 | 0.802 |
| 平均 AUC | 0.995 |
| 平均 PR-AUC | 0.961 |

分类型召回：

| 类型 | 检出 |
| --- | --- |
| Botnet | 20/20 |
| BruteForce | 25/25 |
| Infiltration | 20/20 |
| WebAttack | 3/3 |
| DDoS | 29/39（74.4%） |

模型文件包含：

```text
model            # IsolationForest 实例
features         # FEATURE_COLUMNS
contamination    # 0.12
random_state     # 42
n_estimators     # 300
train_stems      # 参与训练的 6 个文件
train_samples    # 训练 IP 数
n_features       # 25
offset           # score_samples 的判定阈值（decision_function = score_samples - offset）
```

尚未记录 `trained_at`、`sklearn_version`、`python_version`，可选补充。

注意：

- `contamination` 是在 0.08–0.15 的交叉评估结果上选的，严格说属于测试集调参；论文中应把整组结果作为阈值敏感性分析报告；
- 模型只输出异常分数和 0/1 判定，不预测攻击类型；分类型结果来自 truth；
- 最终保存的模型是用全部 train 数据重训的部署模型，前面表格中的指标来自 6 次留出评估，不是这个最终模型自身的评估。

## 五、推理服务（已实现）

FastAPI 服务由 `main.py` 启动，提供两个接口：

| 接口 | 说明 |
| --- | --- |
| `GET /online` | 探活，返回 `{"status": "ok"}`，供 Java 调用前检查 |
| `POST /detect` | 按任务 ID 读取访问日志、聚合特征、加载模型打分 |

代码结构：

```text
db/database.py      # SQLAlchemy engine + load_logs(task_id)
core/inference.py   # score_logs(logs)：加载 joblib、build_features、打分
api/schema.py       # DetectRequest / IpScore / DetectResponse
api/detect.py       # POST /detect 路由
main.py             # FastAPI 应用 + GET /online
```

`POST /detect` 请求：

```json
{"taskId": 6}
```

响应：

```json
{
  "taskId": 6,
  "ipCount": 145,
  "abnormalIpCount": 17,
  "scores": [
    {"ip": "103.45.152.188", "score": 0.0022, "isAbnormal": true,
     "lastAccessTime": "2018-02-16 01:48:43"}
  ]
}
```

说明：

- `scores` 包含**全部 IP**；`isAbnormal=false` 的条目 Java 不使用，只写异常 IP 的分数；
- `score = -decision_function`，越大越异常；`isAbnormal` 对应 `predict() == -1`，即 `score > 0`（等价于 `score_samples < bundle["offset"]`）；
- `lastAccessTime` 是该 IP 最后一条日志的时间，Java 用它作为模型告警的 `trigger_time`；
- `taskId <= 0` 返回 400，任务没有日志返回 404；
- 模型懒加载，进程内只读一次 joblib；读取日志用 SQLAlchemy `text()` + 命名参数（SQLAlchemy 2.x 要求）。

职责划分：

- Python 只读 `access_log`，不修改 `upload_task`、不写 `alert`；
- Java 负责调用 Python、写 `model_score` / `model_abnormal` / `final_abnormal`、合并告警，并维护 `model_detect` 状态与 `model_detail` 摘要；
- Java 调用前先请求 `GET /online`（1s 连接 / 2s 读超时）；离线时该次模型检测失败（`model_detect=3`），日志提示「模型检测服务离线」。

## 六、运行方式

在 `backend/detection-service/` 下：

```bash
# 生成数据
.venv/Scripts/python.exe clean_data.py

# 训练与评估，保存模型
.venv/Scripts/python.exe -m train.train --contamination 0.12

# 启动推理服务（端口 8000）
.venv/Scripts/python.exe -m uvicorn main:app --reload --port 8000
# 也可以直接运行：.venv/Scripts/python.exe main.py
```

Java 侧的模型检测（`/Detect/runModel`、`/Detect/runAll`）依赖该服务在线；规则检测不受影响。

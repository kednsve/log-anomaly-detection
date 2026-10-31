# Python 检测服务（detection-service/）

```text
detection-service/
├── requirements.txt
├── main.py                      # FastAPI 入口
├── config.py                    # 数据库连接配置
│
├── api/
│   └── detect.py                # /detect 路由
│
├── core/
│   ├── features.py              # 特征工程（按IP聚合）
│   ├── detector.py              # Isolation Forest 加载+预测
│   └── rules.py                 # 规则检测（Python侧备份）
│
├── db/
│   └── connection.py            # pymysql/SQLAlchemy 连接
│
├── models/                      # 训练好的模型文件
│   ├── iforest_model.pkl
│   ├── scaler.pkl
│   └── feature_cols.pkl
│
├── train/
│   └── train.py                 # 离线训练脚本（跑一次）
│
└── tests/
    └── test_detect.py           # 简单测试
```

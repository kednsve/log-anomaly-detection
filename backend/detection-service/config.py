"""检测服务通用配置：路径与训练常量。"""

from pathlib import Path

# 服务根目录：backend/detection-service
SERVICE_DIR = Path(__file__).resolve().parent

# 仓库根目录：log-anomaly-detection
PROJECT_ROOT = SERVICE_DIR.parents[1]

# 数据目录
DATA_DIR = PROJECT_ROOT / "data"
CLEANED_DIR = DATA_DIR / "cleaned"

# 模型与 IP 库
MODEL_DIR = SERVICE_DIR / "models"
MODEL_PATH = MODEL_DIR / "isolation_forest.joblib"
IPDB_PATH = SERVICE_DIR / "ipdb" / "ip2region_v4.xdb"

# 数据生成
SAMPLE_SIZE = 30000

# 训练参数
RANDOM_STATE = 42
N_ESTIMATORS = 300
DEFAULT_CONTAMINATION = 0.12

# 数据库
DB_HOST = "localhost"
DB_PORT = 3306
DB_USER = "root"
DB_PASSWORD = "123456"
DB_NAME = "log_anomaly"
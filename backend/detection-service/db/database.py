from sqlalchemy import create_engine, text
from config import DB_HOST, DB_NAME, DB_PORT, DB_USER, DB_PASSWORD
import pandas as pd

engine = create_engine(
    f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4",
    pool_pre_ping=True,
    pool_recycle=3600,
    pool_size=5,
    max_overflow=10,
)
_SQL = text("""
            select ip,
                   access_time,
                   url,
                   status_code,
                   method,
                   response_time,
                   duration,
                   src_bytes,
                   dst_bytes
            from access_log
            where upload_task_id = :task_id
            """)


def load_logs(task_id: int) -> pd.DataFrame:
    return pd.read_sql(_SQL, engine, params={"task_id": task_id})

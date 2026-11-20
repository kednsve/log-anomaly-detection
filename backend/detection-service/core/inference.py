import joblib
import pandas as pd
from sklearn.ensemble import IsolationForest

from config import MODEL_PATH
from core.features import build_features

_bundle = None


def get_bundle():
    """懒加载模型"""
    global _bundle
    if _bundle is None:
        _bundle = joblib.load(MODEL_PATH)
    return _bundle


def score_logs(logs: pd.DataFrame) -> pd.DataFrame:
    bundle = get_bundle()
    features = build_features(logs)

    model: IsolationForest = bundle["model"]
    # 修改列顺序
    X = features[bundle["features"]]
    # -1为异常
    pred = model.predict(X)
    # 分数越高越异常
    score = -model.decision_function(X)

    result = features[["ip"]].copy()
    result["score"] = score
    result["is_abnormal"] = pred == -1
    # 填 alert.trigger_time 用
    result["last_access_time"] = (
        logs.groupby("ip")["access_time"].max().reindex(result["ip"]).values
    )

    return result
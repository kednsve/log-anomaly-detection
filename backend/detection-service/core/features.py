"""
  按 IP 聚合访问日志特征。
"""
import numpy as np
import pandas as pd

FEATURE_COLUMNS = [
    "total_requests",
    "active_minutes",
    "req_per_minute",
    "peak_per_minute",
    "ratio_404",
    "ratio_401_403",
    "ratio_5xx",
    "ratio_200",
    "unique_urls",
    "unique_url_ratio",
    "url_entropy",
    "ratio_get",
    "ratio_post",
    "ratio_put",
    "ratio_head",
    "avg_response_time",
    "max_response_time",
    "p95_response_time",
    "slow_ratio",
    "avg_src_bytes",
    "avg_dst_bytes",
    "avg_duration",
    "night_ratio",
    "avg_interval_sec",
    "std_interval_sec",
]


def _entropy(counts: pd.Series) -> float:
    """香农熵：URL 分布越集中越小，越分散越大。"""
    p = counts / counts.sum()
    return float(-(p * np.log(p + 1e-12)).sum())


def build_features(df: pd.DataFrame) -> pd.DataFrame:
    """
    输入：ip, access_time, url, status_code, method,
            response_time, duration, src_bytes, dst_bytes
    返回列：ip + FEATURE_COLUMNS
    """
    df = df.copy()
    df["access_time"] = pd.to_datetime(df["access_time"])
    df["minute"] = df["access_time"].dt.floor("min")
    df["hour"] = df["access_time"].dt.hour

    # URL 去掉查询参数，避免正常用户的 /search?q=xxx 也被算成大量唯一 URL
    df["url_path"] = df["url"].fillna("").str.split("?", n=1).str[0]

    # 基础计数
    count = df.groupby("ip").agg(
        total_requests=("ip", "size"),
        active_minutes=("minute", "nunique"),
        avg_response_time=("response_time", "mean"),
        max_response_time=("response_time", "max"),
        p95_response_time=("response_time", lambda s: s.quantile(0.95)),
        avg_src_bytes=("src_bytes", "mean"),
        avg_dst_bytes=("dst_bytes", "mean"),
        avg_duration=("duration", "mean"),
        night_ratio=("hour", lambda s: float((s < 6).mean())),
    )

    # 单分钟峰值
    peak = (
        df.groupby(["ip", "minute"])
        .size()
        .groupby("ip")
        .max()
        .rename("peak_per_minute")
    )
    count = count.join(peak)

    # 状态码 / 方法 / 慢请求比例
    flags = pd.DataFrame({
        "ip": df["ip"],
        "is_404": df["status_code"].eq(404),
        "is_auth": df["status_code"].isin([401, 403]),
        "is_5xx": df["status_code"].between(500, 599),
        "is_200": df["status_code"].eq(200),
        "is_get": df["method"].eq("GET"),
        "is_post": df["method"].eq("POST"),
        "is_put": df["method"].eq("PUT"),
        "is_head": df["method"].eq("HEAD"),
        "is_slow": df["response_time"].gt(1000),
    })
    ratios = flags.groupby("ip").mean().rename(columns={
        "is_404": "ratio_404",
        "is_auth": "ratio_401_403",
        "is_5xx": "ratio_5xx",
        "is_200": "ratio_200",
        "is_get": "ratio_get",
        "is_post": "ratio_post",
        "is_put": "ratio_put",
        "is_head": "ratio_head",
        "is_slow": "slow_ratio",
    })
    count = count.join(ratios)

    # URL 多样性
    url_counts = df.groupby(["ip", "url_path"]).size().reset_index(name="cnt")
    url_stats = url_counts.groupby("ip").agg(
        unique_urls=("url_path", "nunique"),
        url_entropy=("cnt", _entropy),
    )
    count = count.join(url_stats)

    # 请求间隔
    ordered = df.sort_values(["ip", "access_time"])
    intervals = ordered.groupby("ip")["access_time"].diff().dt.total_seconds()
    interval_stats = (
        pd.DataFrame({"ip": ordered["ip"], "interval": intervals})
        .groupby("ip")["interval"]
        .agg(["mean", "std"])
        .rename(columns={"mean": "avg_interval_sec", "std": "std_interval_sec"})
    )
    count = count.join(interval_stats)

    # 派生特征 平均每分钟请求量 url多样性比率
    count["req_per_minute"] = count["total_requests"] / count["active_minutes"].clip(lower=1)
    count["unique_url_ratio"] = count["unique_urls"] / count["total_requests"]

    count = count.fillna(0.0).reset_index()
    return count[["ip", *FEATURE_COLUMNS]]
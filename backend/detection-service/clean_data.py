"""
将 data/ 目录下所有 CIC-IDS2018 CSV 合成为 Web 访问日志。

每个输入文件独立处理，输出到 data/cleaned/，按输入文件名命名：
    <输入文件名>-train.csv   按时间前 70%，供离线训练
    <输入文件名>-demo.csv    按时间后 30%，供检测演示
    <输入文件名>-truth.csv   IP 级真值，供评估

IP 画像、URL/状态码/UA 行为和响应时间生成在 personas.py 中。本脚本只负责：
读取与清洗 -> 计算目标行数 -> 切会话/战役 -> 分配给 IP -> 写出 CSV。

运行方式（在 detection-service 目录下）：
    .venv/Scripts/python.exe clean_data.py
"""

import glob
import os
import random
import traceback
from collections import Counter

import pandas as pd

from personas import (
    build_personas,
    make_attack_behavior,
    make_normal_behavior,
    make_response_time,
)

# ================= 配置区 =================
INPUT_DIR = 'F:/MyProjects/log-anomaly-detection/data/'
OUTPUT_DIR = 'F:/MyProjects/log-anomaly-detection/data/cleaned/'

SAMPLE_SIZE = 10000
RANDOM_SEED = 42
ATTACK_RATIO = 0.30          # 目标攻击行占比，实际按 Label 分布自动调整
TRAIN_RATIO = 0.70

BENIGN_GAP_MINUTES = 5       # 正常流量切会话的时间间隔
ATTACK_GAP_MINUTES = 10      # 攻击流量切 campaign 的时间间隔
BLOCK_MAX_ROWS = 20          # 会话 / campaign 的最大块行数

HTTP_PORTS = {80, 443, 8000, 8080, 8888}

COLUMNS_ORDER = [
    'ip', 'ip_location', 'access_time', 'url', 'status_code', 'method',
    'response_time', 'user_agent', 'dest_port', 'protocol',
    'duration', 'src_bytes', 'dst_bytes',
]


def to_int(value, default=0):
    """把可能为 NaN / inf / 字符串的数值安全转成 int。"""
    try:
        if pd.isna(value):
            return default
        return int(float(value))
    except (TypeError, ValueError, OverflowError):
        return default


# ================= 记录生成 =================
def build_record(row, persona, behavior_kind, url, status, method, rng):
    """把一条 CIC 流转换为一条访问日志。"""
    duration = max(0, min(to_int(row.get("Flow Duration"), 0), 60_000_000))
    dest_port = to_int(row.get("Dst Port"), 80)

    proto_num = row.get("Protocol", 6)
    proto_num = 6 if pd.isna(proto_num) else to_int(proto_num, 6)
    protocol = "TCP" if proto_num == 6 else ("UDP" if proto_num == 17 else "OTHER")

    src_bytes = max(0, to_int(row.get("TotLen Fwd Pkts"), 0))
    dst_bytes = max(0, to_int(row.get("TotLen Bwd Pkts"), 0))
    response_time = make_response_time(rng, persona, url, behavior_kind, duration)

    # 兜底：OTHER 协议的脏行，网络流字段全部清零
    if protocol == "OTHER":
        dest_port = 0
        duration = 0
        response_time = 0
        src_bytes = 0
        dst_bytes = 0

    return {
        "ip": persona.ip,
        "ip_location": persona.location,
        "access_time": row["__ts"].strftime("%Y-%m-%d %H:%M:%S"),
        "url": url,
        "status_code": int(status),
        "method": method,
        "response_time": int(response_time),
        "user_agent": persona.user_agent,
        "dest_port": int(dest_port),
        "protocol": protocol,
        "duration": int(duration),
        "src_bytes": int(src_bytes),
        "dst_bytes": int(dst_bytes),
    }


def generate_rows(blocks, personas, rng, attack):
    """把时间块分配给 IP，再逐条生成记录。"""
    if not blocks or not personas:
        return []

    weights = [persona.weight for persona in personas]
    if attack:
        rng.shuffle(personas)

    records = []
    for index, block in enumerate(blocks):
        # 攻击流量先保证每个 IP 至少拿到一个块，其余按权重随机分配
        if attack and index < len(personas):
            persona = personas[index]
        else:
            persona = rng.choices(personas, weights=weights, k=1)[0]

        behavior = make_attack_behavior if attack else make_normal_behavior
        for _, row in block.iterrows():
            url, status, method, kind = behavior(rng, persona)
            records.append(build_record(row, persona, kind, url, status, method, rng))
    return records


# ================= 时间切块与分配 =================
def split_into_blocks(df, gap_minutes, max_rows):
    """按时间间隔切块，并把过大的块按行数切成连续小块。"""
    if df.empty:
        return []
    df = df.sort_values("__ts").reset_index(drop=True).copy()
    gaps = df["__ts"].diff().dt.total_seconds().fillna(0) / 60.0
    df["__block"] = (gaps > gap_minutes).cumsum()

    blocks = []
    for _, part in df.groupby("__block", sort=False):
        if len(part) <= max_rows:
            blocks.append(part)
        else:
            for start in range(0, len(part), max_rows):
                blocks.append(part.iloc[start:start + max_rows])
    return blocks


def select_blocks(blocks, target_rows, rng):
    """随机挑选时间块，凑够目标行数；块过大时在块内随机取一段连续行。"""
    if not blocks or target_rows <= 0:
        return []
    order = list(range(len(blocks)))
    rng.shuffle(order)

    selected = []
    total = 0
    for index in order:
        if total >= target_rows:
            break
        block = blocks[index]
        remaining = target_rows - total
        if len(block) > remaining:
            start = rng.randint(0, len(block) - remaining)
            block = block.iloc[start:start + remaining]
        selected.append(block)
        total += len(block)
    return selected


# ================= 输出 =================
def write_outputs(output_data, out_df, benign_personas, attack_personas,
                  train_file, demo_file, truth_file):
    """写出 IP 真值和按时间切分的训练/演示文件。"""
    counts = Counter(record["ip"] for record in output_data)
    rows = []
    for persona in benign_personas + attack_personas:
        count = counts.get(persona.ip, 0)
        if count == 0:
            continue
        is_attack = 1 if persona.kind == "attack" else 0
        rows.append({
            "ip": persona.ip,
            "is_attack": is_attack,
            "label": "Attack" if is_attack else "Benign",
            "attack_type": persona.label if is_attack else "Benign",
            "request_count": count,
            "location": persona.location,
            "user_agent": persona.user_agent,
        })
    if rows:
        pd.DataFrame(rows).sort_values(
            ["is_attack", "request_count"], ascending=[False, False]
        ).to_csv(truth_file, index=False, encoding="utf-8")
        print(f"生成 {os.path.basename(truth_file)}：{len(rows)} 个 IP")

    sorted_df = out_df.sort_values("access_time").reset_index(drop=True)
    cut = int(len(sorted_df) * TRAIN_RATIO)
    train_df = sorted_df.iloc[:cut][COLUMNS_ORDER]
    demo_df = sorted_df.iloc[cut:][COLUMNS_ORDER]
    train_df.to_csv(train_file, index=False, encoding="utf-8")
    demo_df.to_csv(demo_file, index=False, encoding="utf-8")
    print(
        f"生成 {os.path.basename(train_file)}（{len(train_df)} 行）、"
        f"{os.path.basename(demo_file)}（{len(demo_df)} 行）"
    )


# ================= 单文件处理 =================
def process_file(input_file, seed):
    """处理一个 CIC-IDS2018 CSV，输出 <文件名>-train / -demo / -truth。"""
    stem = os.path.splitext(os.path.basename(input_file))[0]
    train_file = os.path.join(OUTPUT_DIR, f"{stem}-train.csv")
    demo_file = os.path.join(OUTPUT_DIR, f"{stem}-demo.csv")
    truth_file = os.path.join(OUTPUT_DIR, f"{stem}-truth.csv")

    rng = random.Random(seed)

    print(f"正在读取: {input_file} ...")
    usecols = {
        "Dst Port", "Protocol", "Timestamp", "Flow Duration",
        "TotLen Fwd Pkts", "TotLen Bwd Pkts", "Label",
    }
    try:
        df = pd.read_csv(
            input_file,
            low_memory=False,
            encoding="utf-8-sig",
            usecols=lambda column: column.strip() in usecols,
            on_bad_lines="skip",
        )
    except FileNotFoundError:
        print("找不到文件！请检查文件名是否正确。")
        return
    except ValueError as error:
        print(f"读取失败（可能是列名不匹配）：{error}")
        return

    df.columns = df.columns.str.strip()
    missing_columns = usecols - set(df.columns)
    if missing_columns:
        print(f"缺少必要列：{sorted(missing_columns)}")
        return

    # 清洗无穷大值，避免后续 int() 溢出
    df = df.replace([float('inf'), float('-inf')], 0)

    # 关键数值列统一转成数值类型，坏值变成 NaN，后续按默认值处理
    for column in (
        "Dst Port", "Protocol", "Flow Duration",
        "TotLen Fwd Pkts", "TotLen Bwd Pkts",
    ):
        df[column] = pd.to_numeric(df[column], errors="coerce")

    total_raw = len(df)

    # Label 为空的行直接丢弃（旧脚本会把 NaN 当成攻击）
    df = df[df["Label"].notna()].copy()
    df["Label"] = df["Label"].astype(str).str.strip()
    df = df[df["Label"] != ""]

    # 丢弃重复表头等脏行（Label 列值就是 "Label"）
    df = df[df["Label"].str.lower() != "label"]

    # 时间解析失败的行丢弃
    df["__ts"] = pd.to_datetime(
        df["Timestamp"].astype(str).str.strip(),
        format="%d/%m/%Y %H:%M:%S",
        errors="coerce",
    )
    df = df[df["__ts"].notna()].copy()

    # CIC-IDS2018 重复流很多，先去重再抽样
    before_dedup = len(df)
    df = df.drop_duplicates(
        subset=[
            "Dst Port", "Protocol", "Timestamp", "Flow Duration",
            "TotLen Fwd Pkts", "TotLen Bwd Pkts", "Label",
        ]
    )
    print(
        f"原始 {total_raw} 行，有效 {len(df)} 行"
        f"（去重 {before_dedup - len(df)} 行，时间/标签无效 {total_raw - before_dedup} 行）"
    )

    http_df = df[df["Dst Port"].isin(HTTP_PORTS)]
    http_attack = int((http_df["Label"].str.lower() != "benign").sum())
    if len(http_df) >= SAMPLE_SIZE and http_attack > 0:
        print(f"仅保留 HTTP(S) 端口流量：{len(http_df)} 行（攻击 {http_attack} 行）")
        df = http_df
    else:
        print(
            f"HTTP 端口流量不足（{len(http_df)} 行，攻击 {http_attack} 行），"
            f"保留全部端口"
        )

    benign_df = df[df["Label"].str.lower() == "benign"].copy()
    attack_df = df[df["Label"].str.lower() != "benign"].copy()

    print(f"正常 {len(benign_df)} 行，攻击 {len(attack_df)} 行")

    if benign_df.empty:
        print("没有正常样本，跳过该文件。")
        return
    if attack_df.empty:
        print("没有攻击样本，跳过该文件。")
        return

    # 目标行数：攻击占比按实际数据自动收缩
    if len(df) <= SAMPLE_SIZE:
        benign_target, attack_target = len(benign_df), len(attack_df)
    else:
        attack_target = min(len(attack_df), int(round(SAMPLE_SIZE * ATTACK_RATIO)))
        benign_target = min(len(benign_df), SAMPLE_SIZE - attack_target)
        if benign_target + attack_target < SAMPLE_SIZE:
            attack_target = min(len(attack_df), SAMPLE_SIZE - benign_target)

    # 构造 IP 画像池
    benign_personas, attack_personas = build_personas(attack_df, rng)

    # 正常流量：按会话整段分配给同一个 IP
    benign_blocks = split_into_blocks(
        benign_df, BENIGN_GAP_MINUTES, BLOCK_MAX_ROWS
    )
    benign_sessions = select_blocks(benign_blocks, benign_target, rng)
    benign_records = generate_rows(benign_sessions, benign_personas, rng, attack=False)

    # 攻击流量：按 campaign 整段分配给同一个 IP
    attack_blocks = split_into_blocks(
        attack_df, ATTACK_GAP_MINUTES, BLOCK_MAX_ROWS
    )
    selected_attack_blocks = select_blocks(attack_blocks, attack_target, rng)
    attack_records = generate_rows(
        selected_attack_blocks, attack_personas, rng, attack=True
    )

    output_data = benign_records + attack_records
    if not output_data:
        print("没有生成任何数据，跳过该文件。")
        return

    out_df = pd.DataFrame(output_data)
    out_df = out_df.sort_values("access_time").reset_index(drop=True)

    write_outputs(
        output_data, out_df, benign_personas, attack_personas,
        train_file, demo_file, truth_file,
    )


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    input_files = sorted(glob.glob(os.path.join(INPUT_DIR, "*.csv")))
    if not input_files:
        print(f"{INPUT_DIR} 下没有找到 CSV 文件")
        return

    print(f"共找到 {len(input_files)} 个 CSV 文件")
    for index, input_file in enumerate(input_files):
        print("\n" + "=" * 80)
        print(f"[{index + 1}/{len(input_files)}] {os.path.basename(input_file)}")
        try:
            process_file(input_file, RANDOM_SEED + index)
        except Exception as error:
            print(f"处理失败：{error}")
            traceback.print_exc()


if __name__ == '__main__':
    main()

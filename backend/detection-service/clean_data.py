import pandas as pd
import random

# ================= 配置区 =================
INPUT_PATH = 'F:/MyProjects/log-anomaly-detection/data/'
FILE_NAME = 'Friday-02-03-2018_TrafficForML_CICFlowMeter.csv'
INPUT_FILE = INPUT_PATH + FILE_NAME
OUTPUT_PATH = 'F:/MyProjects/log-anomaly-detection/data/cleaned/'
OUTPUT_FILE = OUTPUT_PATH + 'data_set_dev.csv'
SAMPLE_SIZE = 10000
# ==========================================


def gen_ip(prefix):
    """根据前缀补足到 4 段 IPv4，prefix 形如 '114.114.114.' 或 '45.12.'"""
    need = 4 - prefix.count('.')  # 已有段数 = 点的个数
    parts = [str(random.randint(2, 254)) for _ in range(need)]
    return prefix + ".".join(parts)


def clean_cic_2018():
    print(f"正在读取: {INPUT_FILE} ...")
    try:
        df = pd.read_csv(INPUT_FILE, low_memory=False, encoding='utf-8')
    except FileNotFoundError:
        print("找不到文件！请检查文件名是否正确。")
        return

    # 去除列名可能存在的空格
    df.columns = df.columns.str.strip()

    # 清洗无穷大值
    df = df.replace([float('inf'), float('-inf')], 0)

    # 随机抽样
    if len(df) > SAMPLE_SIZE:
        df = df.sample(n=SAMPLE_SIZE, random_state=42).reset_index(drop=True)

    output_data = []

    # 真实的 C2 控制端 IP
    ATTACKER_IP = "18.219.211.138"

    # 正常的国内网民 IP 段
    normal_ip_prefixes = ["114.114.114.", "223.5.5.", "119.29.29.", "180.76.76."]
    # 随机生成的受控肉鸡（黑客） IP 段
    botnet_prefixes = ["45.12.", "103.45.", "185.22.", "198.51."]

    print("正在处理数据...")

    for index, row in df.iterrows():
        label = str(row.get('Label', 'Benign')).strip()

        # ====== IP 分配逻辑 ======
        if label == 'Benign':
            # 正常流量：随机分配国内 IP（4 段）
            ip = gen_ip(random.choice(normal_ip_prefixes))
            status_code = 200
            method = 'GET'
            url = '/index.html'
        else:
            # 攻击流量：20% 给真实的 C2 服务器，80% 给随机肉鸡
            if random.random() < 0.2:
                ip = ATTACKER_IP
            else:
                ip = gen_ip(random.choice(botnet_prefixes))

            status_code = 403
            method = 'POST'
            url = '/api/botnet/c2'

        # ====== 时间解析 ======
        raw_time = str(row.get('Timestamp', ''))
        try:
            # 按照原始格式 02/03/2018 08:47:38 解析
            access_time = pd.to_datetime(raw_time, format='%d/%m/%Y %H:%M:%S').strftime('%Y-%m-%d %H:%M:%S')
        except Exception:
            access_time = "2018-03-02 10:00:00"

        # ====== 特征映射 ======
        dest_port = int(row.get('Dst Port', 80)) if pd.notna(row.get('Dst Port')) else 80

        flow_duration = row.get('Flow Duration', 0)
        duration = int(flow_duration) if pd.notna(flow_duration) else 0
        duration = min(duration, 60_000_000)  # 60s 封顶，避免 9100 万级别的脏值
        response_time = int(duration / 1000)  # 微秒转毫秒

        src_bytes = int(row.get('TotLen Fwd Pkts', 0)) if pd.notna(row.get('TotLen Fwd Pkts')) else 0
        dst_bytes = int(row.get('TotLen Bwd Pkts', 0)) if pd.notna(row.get('TotLen Bwd Pkts')) else 0

        # 协议翻译 (6->TCP, 17->UDP)
        proto_num = row.get('Protocol', 6)
        protocol = "TCP" if proto_num == 6 else ("UDP" if proto_num == 17 else "OTHER")

        # 兜底：OTHER 协议的脏行，端口/时长/字节全部清零
        if protocol == "OTHER":
            dest_port = 0
            duration = 0
            response_time = 0
            src_bytes = 0
            dst_bytes = 0

        # ====== 组装数据 ======
        output_data.append({
            'ip': ip,
            'access_time': access_time,
            'url': url,
            'status_code': status_code,
            'method': method,
            'response_time': response_time,
            'user_agent': "Mozilla/5.0",
            'dest_port': dest_port,
            'protocol': protocol,
            'duration': duration,
            'src_bytes': src_bytes,
            'dst_bytes': dst_bytes
        })

    # ====== 导出 ======
    out_df = pd.DataFrame(output_data)

    # 确保列的顺序完全符合你的 CsvUtil 索引要求
    columns_order = [
        'ip', 'access_time', 'url', 'status_code', 'method',
        'response_time', 'user_agent', 'dest_port', 'protocol',
        'duration', 'src_bytes', 'dst_bytes'
    ]
    out_df = out_df[columns_order]

    out_df.to_csv(OUTPUT_FILE, index=False)
    print(f"✅ 成功生成 {OUTPUT_FILE}，共 {len(out_df)} 条数据！")
    print(f"提示：攻击 IP 池中包含真实 C2 ({ATTACKER_IP}) 和随机肉鸡 IP。")


if __name__ == '__main__':
    clean_cic_2018()
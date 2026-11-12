"""
IP 画像与请求行为生成。

CIC-IDS2018 的 CSV 没有 IP、URL、状态码、User-Agent，这些字段需要合成。
本模块负责：

1. 生成正常用户 / 攻击者的 IP 画像池（归属地、UA、常用 URL、权重固定）；
2. 定义不同攻击类型的 URL / 状态码 / 方法画像；
3. 生成单条请求的行为（URL、状态码、方法、响应时间）。

clean_data.py 负责读取原始流、切会话/战役、把流分配给 IP 并写出 CSV。
"""

import os
from collections import Counter
from dataclasses import dataclass, field

import ip2region.util as util
import ip2region.searcher as xdb

# ================= 配置区 =================
BENIGN_IP_COUNT = 200        # 正常用户 IP 数量
ATTACK_IP_COUNT = 20         # 攻击者 IP 数量（含 C2）
C2_IP = "18.219.211.138"     # 真实的 C2 控制端 IP

# 正常用户 IP 段（国内 ISP，每段补两段随机数）
BENIGN_IP_PREFIXES = [
    "1.180.", "27.184.", "36.110.", "58.247.", "101.226.",
    "106.11.", "113.87.", "116.24.", "119.137.", "120.229.",
    "121.35.", "183.14.", "223.5.", "119.29.",
]

# 攻击者 IP 段（每段补两段随机数）
ATTACK_IP_PREFIXES = [
    "45.12.", "103.45.", "185.22.", "194.26.", "212.83.",
    "91.219.", "5.188.", "89.248.", "141.98.", "193.32.",
]

# ================= IP 归属地 =================
_DB_PATH = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "ipdb", "ip2region_v4.xdb"
)
searcher = xdb.new_with_vector_index(
    util.IPv4, _DB_PATH, util.load_vector_index_from_file(_DB_PATH)
)

_location_cache = {}


def get_ip_location(ip):
    """查询 IP 归属地，返回 '中国 广东省 深圳市' 格式。同一 IP 只查一次。"""
    if ip in _location_cache:
        return _location_cache[ip]
    try:
        # 查询返回格式：国家|区域|省份|城市|ISP
        result = searcher.search(ip)
        if not result or '|' not in result:
            location = "未知"
        else:
            parts = result.split('|')
            country = parts[0] if parts[0] != '0' else ''
            province = parts[2] if parts[2] != '0' else ''
            city = parts[3] if parts[3] != '0' else ''
            # 拼接并去重
            location_parts = list(dict.fromkeys([p for p in [country, province, city] if p]))
            location = " ".join(location_parts) if location_parts else "未知"
    except Exception:
        location = "未知"
    _location_cache[ip] = location
    return location


# ================= IP 画像 =================
@dataclass
class IpPersona:
    ip: str
    location: str
    user_agent: str
    kind: str                      # benign / attack
    label: str                     # Benign 或具体攻击类型
    weight: float                  # 请求量权重，越大分到的会话越多
    slow: bool = False             # 是否为慢响应画像
    urls: list[str] = field(default_factory=list)   # 正常用户常用 URL


def gen_ip(prefix, rng):
    """根据前缀补足到 4 段 IPv4，prefix 形如 '114.114.114.' 或 '45.12.'"""
    need = 4 - prefix.count('.')  # 已有段数 = 点的个数
    return prefix + ".".join(str(rng.randint(2, 254)) for _ in range(need))


def generate_unique_ips(prefixes, count, rng):
    """生成指定数量的不重复 IP。"""
    ips = set()
    attempts = 0
    while len(ips) < count and attempts < count * 1000:
        ips.add(gen_ip(rng.choice(prefixes), rng))
        attempts += 1
    if len(ips) < count:
        raise RuntimeError(f"IP 池空间不足，只生成了 {len(ips)} 个 IP")
    return sorted(ips)


def pick_choice(rng, entries):
    """从 [(值, 权重), ...] 中按权重取一个值。"""
    values = [entry[0] for entry in entries]
    weights = [entry[1] for entry in entries]
    return rng.choices(values, weights=weights, k=1)[0]


# ================= 行为池 =================
NORMAL_URLS = [
    ("/", 18),
    ("/index.html", 14),
    ("/login", 6),
    ("/register", 2),
    ("/product/list", 9),
    ("/product/detail", 8),
    ("/search", 5),
    ("/api/user/info", 6),
    ("/api/order/list", 5),
    ("/api/order/create", 2),
    ("/about", 2),
    ("/contact", 1),
    ("/static/css/app.css", 5),
    ("/static/js/app.js", 5),
    ("/static/img/logo.png", 4),
    ("/favicon.ico", 4),
]

STATIC_URLS = [
    url for url, _ in NORMAL_URLS
    if url.startswith("/static/") or url == "/favicon.ico"
]

MISSING_URLS = [
    "/wp-admin", "/wp-login.php", "/.env", "/phpmyadmin/", "/admin/",
    "/backup.zip", "/config.php", "/.git/config", "/server-status",
    "/actuator/env", "/old/index.html", "/api/../../etc/passwd",
]

SEARCH_KEYWORDS = [
    "phone", "laptop", "headphone", "monitor",
    "keyboard", "router", "camera", "printer",
]

NORMAL_USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
]

SUSPICIOUS_USER_AGENTS = [
    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)",
    "python-requests/2.25.1",
    "curl/7.68.0",
    "Go-http-client/1.1",
    "Mozilla/5.0 (compatible; Nmap Scripting Engine; https://nmap.org/book/nse.html)",
    "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0",
    "",
]

# WebAttack 的查询串载荷
ATTACK_PAYLOADS = [
    "q=<script>alert(1)</script>",
    "id=1%27%20OR%20%271%27%3D%271",
    "q=../../../../etc/passwd",
    "name=<img%20src=x%20onerror=alert(1)>",
    "file=../../../../etc/passwd",
]

# 不同攻击类型的行为画像。normal_overlap 是“看起来像正常请求”的比例，
# missing_ratio 是命中不存在路径（404）的比例。
ATTACK_PROFILES = {
    "Botnet": {
        "urls": [("/gate.php", 30), ("/api/beacon", 25), ("/task", 20), ("/report", 10), ("/ping", 8)],
        "methods": [("POST", 60), ("GET", 35), ("PUT", 5)],
        "statuses": [(200, 45), (403, 25), (404, 20), (500, 10)],
        "missing_ratio": 0.15,
        "normal_overlap": 0.12,
        "response_base": (20, 250),
    },
    "BruteForce": {
        "urls": [("/login", 30), ("/admin/login", 20), ("/wp-login.php", 20), ("/api/auth", 15), ("/user/login", 10)],
        "methods": [("POST", 90), ("GET", 10)],
        "statuses": [(401, 40), (403, 25), (200, 25), (500, 10)],
        "missing_ratio": 0.05,
        "normal_overlap": 0.10,
        "response_base": (80, 500),
    },
    "DDoS": {
        "urls": [("/", 30), ("/index.html", 25), ("/api/order/list", 15), ("/search", 10), ("/product/list", 10)],
        "methods": [("GET", 70), ("POST", 30)],
        "statuses": [(200, 40), (503, 30), (502, 10), (429, 10), (500, 10)],
        "missing_ratio": 0.02,
        "normal_overlap": 0.15,
        "response_base": (1, 80),
    },
    "WebAttack": {
        "urls": [("/search", 25), ("/product/detail", 20), ("/login", 15), ("/api/comment", 15), ("/upload", 10)],
        "methods": [("GET", 60), ("POST", 40)],
        "statuses": [(200, 35), (403, 30), (500, 20), (404, 10), (400, 5)],
        "missing_ratio": 0.10,
        "normal_overlap": 0.10,
        "response_base": (100, 800),
    },
    "Infiltration": {
        "urls": [("/upload", 25), ("/admin", 20), ("/api/internal", 20), ("/api/user/info", 15), ("/backup.zip", 10)],
        "methods": [("POST", 50), ("GET", 50)],
        "statuses": [(200, 40), (403, 30), (404, 20), (500, 10)],
        "missing_ratio": 0.15,
        "normal_overlap": 0.10,
        "response_base": (50, 400),
    },
    "Other": {
        "urls": [("/api/beacon", 20), ("/admin/", 15), ("/login", 15), ("/upload", 10), ("/search", 10)],
        "methods": [("POST", 45), ("GET", 45), ("PUT", 10)],
        "statuses": [(200, 40), (403, 30), (404, 20), (500, 10)],
        "missing_ratio": 0.10,
        "normal_overlap": 0.12,
        "response_base": (30, 400),
    },
}


def attack_category(label):
    """把数据集里的攻击 Label 归到脚本支持的画像类别。"""
    text = str(label).strip().lower()
    if 'bot' in text:
        return "Botnet"
    if any(key in text for key in ('ddos', 'dos', 'hulk', 'goldeneye', 'slowloris', 'slowhttptest')):
        return "DDoS"
    if any(key in text for key in ('brute', 'ftp', 'ssh')):
        return "BruteForce"
    if any(key in text for key in ('web', 'xss', 'sql', 'injection')):
        return "WebAttack"
    if 'infilt' in text:  # 原始数据里常拼成 Infilteration
        return "Infiltration"
    return "Other"


# ================= URL / 状态码 / 方法 =================
def enrich_normal_url(url, rng):
    """给正常 URL 补上常见的查询参数。"""
    if url == "/search":
        return f"/search?q={rng.choice(SEARCH_KEYWORDS)}"
    if url == "/product/detail":
        return f"/product/detail?id={rng.randint(1, 500)}"
    if url == "/api/order/list":
        return f"/api/order/list?page={rng.randint(1, 20)}"
    if url == "/api/user/info":
        return f"/api/user/info?uid={rng.randint(1, 5000)}"
    return url


def enrich_attack_url(url, profile_name, rng):
    """给攻击 URL 补上载荷或随机参数。"""
    if profile_name == "WebAttack":
        payload = rng.choice(ATTACK_PAYLOADS)
        sep = "&" if "?" in url else "?"
        return url + sep + payload
    if profile_name == "BruteForce" and rng.random() < 0.7:
        password = rng.choice(["123456", "admin", "password", "root", "qwerty"])
        return f"{url}?username=admin&password={password}"
    if profile_name == "Botnet" and rng.random() < 0.5:
        return f"{url}?id={rng.randint(1000, 9999)}&v={rng.randint(1, 9)}"
    if profile_name == "DDoS" and rng.random() < 0.3:
        return f"{url}?_={rng.randint(1, 999999)}"
    return url


def make_normal_behavior(rng, persona):
    """生成一条正常请求的 URL / 状态码 / 方法。"""
    r = rng.random()
    if r < 0.03:
        # 少量正常用户也会访问不存在的路径（旧书签、失效链接）
        url = rng.choice(MISSING_URLS)
        status = pick_choice(rng, [(404, 75), (403, 15), (500, 10)])
        method = "GET" if rng.random() < 0.85 else "POST"
        return url, status, method, "missing"
    if r < 0.15:
        url = rng.choice(STATIC_URLS)
        status = 200 if rng.random() < 0.95 else 304
        return url, status, "GET", "static"

    base_url = rng.choice(persona.urls) if persona.urls else pick_choice(rng, NORMAL_URLS)
    url = enrich_normal_url(base_url, rng)
    method = pick_choice(rng, [("GET", 83), ("POST", 13), ("HEAD", 3), ("PUT", 1)])
    status = pick_choice(rng, [(200, 86), (304, 6), (404, 4), (500, 2), (403, 1.5), (401, 0.5)])
    if method != "GET" and status == 304:
        status = 200
    return url, status, method, "page"


def make_attack_behavior(rng, persona):
    """生成一条攻击请求的 URL / 状态码 / 方法。"""
    profile_name = persona.label
    profile = ATTACK_PROFILES.get(profile_name, ATTACK_PROFILES["Other"])

    # 少量攻击流量伪装成正常请求，避免两类数据完全可分
    if rng.random() < profile["normal_overlap"]:
        url, status, method, _ = make_normal_behavior(rng, persona)
        return url, status, method, "overlap"

    # 扫描器式的不存在路径
    if rng.random() < profile["missing_ratio"]:
        url = rng.choice(MISSING_URLS)
        status = pick_choice(rng, [(404, 60), (403, 30), (500, 10)])
        method = pick_choice(rng, [("GET", 80), ("POST", 20)])
        return url, status, method, "missing"

    url = enrich_attack_url(pick_choice(rng, profile["urls"]), profile_name, rng)
    method = pick_choice(rng, profile["methods"])
    status = pick_choice(rng, profile["statuses"])
    return url, status, method, "attack"


def make_response_time(rng, persona, url, behavior_kind, duration):
    """生成 Web 层响应耗时（毫秒）。duration 是原始流持续时间（微秒）。"""
    if behavior_kind == "attack":
        profile = ATTACK_PROFILES.get(persona.label, ATTACK_PROFILES["Other"])
        low, high = profile["response_base"]
    elif behavior_kind == "missing":
        low, high = 5, 100
    elif url.startswith("/static/") or url.endswith((".css", ".js", ".png", ".ico")):
        low, high = 5, 60
    elif url.startswith("/api/"):
        low, high = 50, 500
    elif url.startswith("/login") or url.startswith("/register"):
        low, high = 100, 800
    elif url.startswith("/search"):
        low, high = 80, 600
    else:
        low, high = 30, 250

    value = rng.uniform(low, high)
    if persona.slow:
        value *= rng.uniform(3, 8)
    if rng.random() < 0.02:
        value = rng.uniform(2000, 5000)

    # 保留一点原始流时长的影响，但只占很小权重
    flow_ms = duration / 1000.0
    value += min(flow_ms * 0.02, 300) + rng.uniform(0, 30)
    return max(1, min(int(value), 60_000))


def build_personas(attack_df, rng):
    """构造正常用户和攻击者 IP 画像池。"""
    benign_personas = []
    for ip in generate_unique_ips(BENIGN_IP_PREFIXES, BENIGN_IP_COUNT, rng):
        # 每个正常用户固定一组常用 URL
        pool = list(NORMAL_URLS)
        urls = []
        for _ in range(rng.randint(4, min(8, len(pool)))):
            url = pick_choice(rng, pool)
            urls.append(url)
            pool = [entry for entry in pool if entry[0] != url]

        benign_personas.append(IpPersona(
            ip=ip,
            location=get_ip_location(ip),
            user_agent=rng.choice(NORMAL_USER_AGENTS),
            kind="benign",
            label="Benign",
            weight=rng.lognormvariate(0.0, 0.9),
            slow=rng.random() < 0.05,
            urls=urls,
        ))

    attack_personas = []
    if attack_df.empty:
        return benign_personas, attack_personas

    category_counts = Counter(attack_category(label) for label in attack_df["Label"])
    categories = list(category_counts.keys())
    category_weights = [category_counts[category] for category in categories]
    main_category = category_counts.most_common(1)[0][0]

    # 固定 C2 作为一个强信号 IP
    attack_personas.append(IpPersona(
        ip=C2_IP,
        location=get_ip_location(C2_IP),
        user_agent=rng.choice(SUSPICIOUS_USER_AGENTS),
        kind="attack",
        label=main_category,
        weight=4.0,
        slow=rng.random() < 0.10,
        urls=[],
    ))

    for ip in generate_unique_ips(ATTACK_IP_PREFIXES, max(0, ATTACK_IP_COUNT - 1), rng):
        category = rng.choices(categories, weights=category_weights, k=1)[0]
        # 前两个普通攻击 IP 设成低频画像，作为难以检出的样本
        weight = 0.2 if len(attack_personas) <= 2 else rng.uniform(0.6, 1.8)
        user_agent = (
            rng.choice(NORMAL_USER_AGENTS)
            if rng.random() < 0.15
            else rng.choice(SUSPICIOUS_USER_AGENTS)
        )
        attack_personas.append(IpPersona(
            ip=ip,
            location=get_ip_location(ip),
            user_agent=user_agent,
            kind="attack",
            label=category,
            weight=weight,
            slow=rng.random() < 0.10,
            urls=[],
        ))

    return benign_personas, attack_personas

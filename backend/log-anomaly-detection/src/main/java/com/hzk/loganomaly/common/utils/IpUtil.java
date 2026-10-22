package com.hzk.loganomaly.common.utils;

public class IpUtil {

    /**
     * 校验 IPv4 格式是否合法
     */
    public static boolean isValidIpv4(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String regex = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
        return ip.matches(regex);
    }

    /**
     * 判断是否为内网 IP（用于跳过外部归属地 API 调用）
     */
    public static boolean isInternalIp(String ip) {
        if (!isValidIpv4(ip)) {
            return false;
        }
        String[] parts = ip.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);

        // 10.0.0.0/8
        if (first == 10) return true;
        // 172.16.0.0/12
        if (first == 172 && second >= 16 && second <= 31) return true;
        // 192.168.0.0/16
        if (first == 192 && second == 168) return true;
        // 127.0.0.0/8 (本机回环地址)
        if (first == 127) return true;

        return false;
    }
}

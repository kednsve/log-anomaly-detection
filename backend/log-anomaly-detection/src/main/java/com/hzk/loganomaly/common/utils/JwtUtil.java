package com.hzk.loganomaly.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret-key}")
    private String secretKeyStr;

    @Value("${jwt.expire-time}")
    private Long expireTime;

    private SecretKey secretKey;

    /**
     * 初始化密钥
     */
    @PostConstruct
    public void initKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyStr);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成token，存入userId作为负载
     *
     * @param userId 用户id
     * @return jwt token字符串
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireTime);

        return Jwts.builder()
                .subject(userId.toString()) // 主题，这里放userId
                .issuedAt(now)              // 签发时间
                .expiration(expireDate)     // 过期时间
                .signWith(secretKey)        // 签名密钥
                .compact();
    }

    /**
     * 从token中获取userId
     */
    public Long getUserIdByToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 校验token是否合法、没过期
     *
     * @return true合法；false无效/过期/篡改
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

}

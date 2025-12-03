package com.collab.collab_editor_backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    // 直接用原始密钥字符串（无需Base64）
    @Value("${jwt.secret}")
    private String rawSecret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.header}")
    private String header;

    // 直接用UTF-8编码生成密钥（无Base64解码环节）
    private SecretKey getSigningKey() {
        log.info("当前使用的原始密钥：{}", rawSecret);
        byte[] keyBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        log.info("密钥UTF-8编码后长度：{}字节", keyBytes.length);
        // 强制验证密钥长度（HS256必须≥32字节）
        if (keyBytes.length < 32) {
            throw new RuntimeException("密钥长度不足32字节！当前：" + keyBytes.length);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 生成Token（逻辑不变，密钥来源简化）
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        String token = Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        log.info("生成的完整Token：{}", token); // 打印完整Token，让用户直接复制
        return token;
    }

    // 解析Token（逻辑不变，密钥来源简化）
    public Claims parseToken(String token) {
        log.info("待解析的Token：{}", token);
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // 和生成时用同一个密钥
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 解析用户ID（保持不变）
    public Long getUserIdFromToken(String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "").trim();
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    public String getHeader() {
        return header;
    }
}
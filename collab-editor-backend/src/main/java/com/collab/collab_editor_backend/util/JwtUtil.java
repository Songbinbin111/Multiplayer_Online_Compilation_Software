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
        log.info("当前使用的密钥类型：HS256");
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
        log.info("生成Token成功：userId={}, username={}", userId, username); // 仅打印用户信息，不打印完整Token
        return token;
    }

    // 解析Token（增加允许的时钟偏差，解决服务器时间与客户端时间不一致的问题）
    public Claims parseToken(String token) {
        log.info("开始解析Token");
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // 和生成时用同一个密钥
                .setAllowedClockSkewSeconds(30) // 允许30秒的时钟偏差（解决服务器时间与客户端时间不一致的问题）
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 解析用户ID，添加对null和空字符串的处理
    public Long getUserIdFromToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            throw new RuntimeException("Authorization header is missing");
        }
        String token = authorizationHeader.replace("Bearer ", "").trim();
        if (token.isEmpty()) {
            throw new RuntimeException("Token is missing");
        }
        try {
            Claims claims = parseToken(token);
            return claims.get("userId", Long.class);
        } catch (Exception e) {
            log.error("Token parsing failed: {}", e.getMessage());
            throw new RuntimeException("Invalid or expired token");
        }
    }
    
    // 从过期的令牌中获取用户ID（用于刷新令牌接口）
    public Long getUserIdFromExpiredToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            throw new RuntimeException("Authorization header is missing");
        }
        String token = authorizationHeader.replace("Bearer ", "").trim();
        if (token.isEmpty()) {
            throw new RuntimeException("Token is missing");
        }
        try {
            // 解析过期的令牌，设置允许的时钟偏差
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .setAllowedClockSkewSeconds(30) // 允许30秒的时钟偏差
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("userId", Long.class);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // 如果是过期异常，我们仍然可以获取claims
            log.warn("Token is expired, but we can still get claims: {}", e.getMessage());
            return e.getClaims().get("userId", Long.class);
        } catch (Exception e) {
            log.error("Expired token parsing failed: {}", e.getMessage());
            throw new RuntimeException("Invalid token");
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    public String getHeader() {
        return header;
    }
}
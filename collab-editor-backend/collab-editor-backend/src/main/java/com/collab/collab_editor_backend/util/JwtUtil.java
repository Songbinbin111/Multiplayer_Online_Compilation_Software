package com.collab.collab_editor_backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String base64Secret; // Base64编码的密钥（配置文件中需填写）

    @Value("${jwt.expiration}")
    private long expiration; // 有效期（毫秒，配置文件中需填写）

    @Value("${jwt.header}")
    private String header; // 请求头名称（如"Authorization"，配置文件中需填写）

    // 解析Base64密钥为加密密钥（核心）
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getHeader() {
        return header;
    }

    // 生成Token（需要userId和username，与调用处匹配）
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId); // 存入用户ID
        claims.put("username", username); // 存入用户名
        return Jwts.builder()
                .setClaims(claims) // 设置自定义数据
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 签名（使用合规密钥）
                .compact();
    }

    // 解析Token，获取全部数据
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // 验证密钥
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 补充：从Token中提取用户ID（后续拦截器需要）
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    // 补充：从Token中提取用户名（可选）
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }
}
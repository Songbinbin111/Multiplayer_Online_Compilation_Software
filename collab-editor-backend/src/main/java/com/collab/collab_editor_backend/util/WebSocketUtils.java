package com.collab.collab_editor_backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class WebSocketUtils {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketUtils.class);

    /**
     * 从WebSocket会话中获取用户ID
     * 支持从 query string 中提取 userId (不安全，仅限调试) 或 token (推荐)
     */
    public static Long getUserIdFromSession(WebSocketSession session, JwtUtil jwtUtil) {
        try {
            String uri = session.getUri().toString();
            int queryIndex = uri.indexOf("?");
            if (queryIndex == -1) {
                return null;
            }

            String queryString = uri.substring(queryIndex + 1);
            String[] params = queryString.split("&");
            
            // 1. 优先尝试从 token 获取
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    if ("token".equals(key)) {
                        String token = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        try {
                            Long uid = jwtUtil.getUserIdFromToken(token);
                            if (uid != null) {
                                return uid;
                            }
                        } catch (Exception e) {
                            logger.warn("Token validation failed: {}", e.getMessage());
                        }
                    }
                }
            }

            // 2. 如果没有token或token无效，尝试直接获取 userId (为了兼容旧代码，但建议逐步废弃)
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    if ("userId".equals(key)) {
                        try {
                            Long uid = Long.parseLong(URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                            return uid;
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid userId format: {}", keyValue[1]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing WebSocket session params", e);
        }

        return null;
    }
}
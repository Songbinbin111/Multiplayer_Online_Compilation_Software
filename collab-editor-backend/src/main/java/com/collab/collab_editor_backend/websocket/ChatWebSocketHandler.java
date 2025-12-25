package com.collab.collab_editor_backend.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.collab.collab_editor_backend.util.JwtUtil;
import com.collab.collab_editor_backend.util.WebSocketUtils;
import com.collab.collab_editor_backend.mapper.UserMapper;
import com.collab.collab_editor_backend.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 聊天WebSocket处理器
 * 处理聊天消息的实时推送
 */
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserMapper userMapper;

    /**
     * 保存用户ID与WebSocketSession的映射
     */
    private static final Map<Long, CopyOnWriteArraySet<WebSocketSession>> userSessionMap = new ConcurrentHashMap<>();
    private static final Map<Long, String> userInfoMap = new ConcurrentHashMap<>();

    /**
     * 连接建立时的处理
     * @param session WebSocketSession对象
     * @throws Exception 异常
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("ChatWebSocket尝试建立连接, URI: {}", session.getUri());
        try {
            // 使用工具类获取并验证用户ID
            Long userId = WebSocketUtils.getUserIdFromSession(session, jwtUtil);
            
            if (userId != null) {
                // 保存用户ID与WebSocketSession的映射
                userSessionMap.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
                
                String username = null;
                try {
                    String query = session.getUri().getQuery();
                    if (query != null) {
                        String[] params = query.split("&");
                        for (String p : params) {
                            String[] kv = p.split("=", 2);
                            if (kv.length == 2) {
                                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                                if ("token".equals(key)) {
                                    String token = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                                    username = jwtUtil.getUsernameFromToken(token);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("解析聊天用户名失败: {}", e.getMessage());
                }
                if (username == null) {
                    try {
                        User u = userMapper.selectById(userId);
                        if (u != null && u.getUsername() != null && !u.getUsername().isEmpty()) {
                            username = u.getUsername();
                        }
                    } catch (Exception ignored) {}
                }
                if (username == null) {
                    username = "用户" + userId;
                }
                userInfoMap.put(userId, username);
                broadcastOnlineUsers();
                logger.info("用户 {} 建立了WebSocket连接", userId);
                return;
            }
            
            // 如果没有找到有效的userId参数或token验证失败
            String query = session.getUri().getQuery();
            logger.error("WebSocket连接失败：认证失败或无效参数, query: {}", query);
            session.close(new CloseStatus(4000, "认证失败"));
        } catch (Exception e) {
            logger.error("WebSocket连接失败：{}", e.getMessage(), e);
            session.close(new CloseStatus(5000, "服务器内部错误"));
        }
    }

    /**
     * 连接关闭时的处理
     * @param session WebSocketSession对象
     * @param status 关闭状态
     * @throws Exception 异常
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 移除用户ID与WebSocketSession的映射
        for (Map.Entry<Long, CopyOnWriteArraySet<WebSocketSession>> entry : userSessionMap.entrySet()) {
            if (entry.getValue().remove(session)) {
                Long userId = entry.getKey();
                if (entry.getValue().isEmpty()) {
                    userSessionMap.remove(userId);
                    userInfoMap.remove(userId);
                }
                logger.info("用户 {} 关闭了WebSocket连接, status: {}", userId, status);
                broadcastOnlineUsers();
                break;
            }
        }
    }

    /**
     * 接收消息时的处理
     * @param session WebSocketSession对象
     * @param message 消息对象
     * @throws Exception 异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 目前不需要处理客户端发送的消息
    }

    /**
     * 发送消息给指定用户
     * @param userId 用户ID
     * @param message 消息内容
     */
    public static void sendMessageToUser(Long userId, String message) {
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessionMap.get(userId);
        if (sessions != null) {
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 发送消息给所有用户
     * @param message 消息内容
     */
    public static void sendMessageToUsers(String message) {
        TextMessage textMessage = new TextMessage(message);
        for (CopyOnWriteArraySet<WebSocketSession> sessions : userSessionMap.values()) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    private static void broadcastOnlineUsers() {
        try {
            List<Map<String, Object>> users = new ArrayList<>();
            for (Map.Entry<Long, CopyOnWriteArraySet<WebSocketSession>> entry : userSessionMap.entrySet()) {
                Long uid = entry.getKey();
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    Map<String, Object> u = new HashMap<>();
                    u.put("userId", uid);
                    u.put("username", userInfoMap.getOrDefault(uid, "用户" + uid));
                    users.add(u);
                }
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "online_users");
            payload.put("users", users);
            String json = objectMapper.writeValueAsString(payload);
            sendMessageToUsers(json);
        } catch (Exception ignored) {}
    }
}

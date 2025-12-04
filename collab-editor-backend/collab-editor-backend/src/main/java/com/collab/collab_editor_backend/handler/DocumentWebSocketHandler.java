package com.collab.collab_editor_backend.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档协作WebSocket处理器
 * 处理文档内容同步、用户加入/离开等事件
 */
public class DocumentWebSocketHandler extends TextWebSocketHandler {

    // 文档ID到会话列表的映射，使用并发安全的集合
    private static final Map<Integer, Set<WebSocketSession>> DOCUMENT_SESSIONS = new ConcurrentHashMap<>();
    // 用户会话到文档ID的映射
    private static final Map<WebSocketSession, Integer> SESSION_DOCUMENTS = new ConcurrentHashMap<>();
    // 文档ID到在线用户列表的映射
    private static final Map<Integer, Set<Map<String, Object>>> DOCUMENT_USERS = new ConcurrentHashMap<>();
    // JSON序列化/反序列化工具
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 获取文档ID
        Integer docId = getDocIdFromSession(session);
        if (docId == null) {
            session.close(new CloseStatus(4000, "无效的文档ID"));
            return;
        }

        // 将会话添加到文档会话列表
        DOCUMENT_SESSIONS.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(session);
        SESSION_DOCUMENTS.put(session, docId);

        System.out.println("用户连接到文档: " + docId + ", 当前连接数: " + DOCUMENT_SESSIONS.get(docId).size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 解析接收到的消息
        Map<String, Object> messageMap = objectMapper.readValue(message.getPayload(), Map.class);
        String type = (String) messageMap.get("type");
        Integer docId = messageMap.get("docId") instanceof Number ? ((Number) messageMap.get("docId")).intValue() : null;

        if (docId == null) {
            return;
        }

        switch (type) {
            case "join":
                handleUserJoin(session, messageMap, docId);
                break;
            case "content_update":
                handleContentUpdate(session, messageMap, docId);
                break;
            // 可以添加更多消息类型处理
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Integer docId = SESSION_DOCUMENTS.remove(session);
        if (docId == null) {
            return;
        }

        // 从文档会话列表中移除会话
        Set<WebSocketSession> sessions = DOCUMENT_SESSIONS.get(docId);
        if (sessions != null) {
            sessions.remove(session);
            
            // 如果文档没有会话了，清理资源
            if (sessions.isEmpty()) {
                DOCUMENT_SESSIONS.remove(docId);
                DOCUMENT_USERS.remove(docId);
            } else {
                // 从用户列表中移除用户并通知其他用户
                removeUserFromDocument(session, docId);
            }
        }

        System.out.println("用户断开文档连接: " + docId + ", 当前连接数: " + (sessions != null ? sessions.size() : 0));
    }

    /**
     * 处理用户加入事件
     */
    private void handleUserJoin(WebSocketSession session, Map<String, Object> messageMap, Integer docId) throws IOException {
        Integer userId = messageMap.get("userId") instanceof Number ? ((Number) messageMap.get("userId")).intValue() : null;
        String username = (String) messageMap.get("username");

        if (userId == null || username == null) {
            return;
        }

        // 创建用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("username", username);
        
        // 将用户添加到文档用户列表（检查是否已存在）
        Set<Map<String, Object>> users = DOCUMENT_USERS.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet());
        // 检查用户是否已存在
        boolean userExists = false;
        for (Map<String, Object> existingUser : users) {
            if (existingUser.get("userId").equals(userId)) {
                userExists = true;
                break;
            }
        }
        // 如果用户不存在，则添加
        if (!userExists) {
            users.add(userInfo);
        }
        
        // 将用户会话和用户信息关联起来
        session.getAttributes().put("user", userInfo);

        // 发送更新后的用户列表给所有用户
        sendUserList(docId);
        
        // 通知其他用户有新用户加入
        notifyUserJoin(docId, userInfo);
        
        System.out.println("用户加入文档: " + docId + ", 用户名: " + username);
    }

    /**
     * 处理内容更新事件
     */
    private void handleContentUpdate(WebSocketSession session, Map<String, Object> messageMap, Integer docId) throws IOException {
        String content = (String) messageMap.get("content");
        if (content == null) {
            return;
        }

        // 广播内容更新给文档的所有其他用户
        Set<WebSocketSession> sessions = DOCUMENT_SESSIONS.get(docId);
        if (sessions != null) {
            for (WebSocketSession otherSession : sessions) {
                if (!otherSession.equals(session)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "content_update");
                    response.put("docId", docId);
                    response.put("content", content);
                    response.put("userId", messageMap.get("userId"));
                    response.put("username", messageMap.get("username"));

                    otherSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                }
            }
        }
    }

    /**
     * 从会话中获取文档ID
     */
    private Integer getDocIdFromSession(WebSocketSession session) {
        // 从URI中获取文档ID参数
        String uri = session.getUri().toString();
        String[] parts = uri.split("/");
        if (parts.length < 4) {
            return null;
        }
        try {
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从文档中移除用户
     */
    private void removeUserFromDocument(WebSocketSession session, Integer docId) throws IOException {
        Map<String, Object> userInfo = (Map<String, Object>) session.getAttributes().remove("user");
        if (userInfo == null) {
            return;
        }

        Set<Map<String, Object>> users = DOCUMENT_USERS.get(docId);
        if (users != null) {
            users.removeIf(user -> user.get("userId").equals(userInfo.get("userId")));
            
            // 发送更新后的用户列表给所有用户
            sendUserList(docId);
            
            // 通知其他用户有用户离开
            notifyUserLeave(docId, userInfo);
        }
    }

    /**
     * 发送用户列表给文档的所有用户
     */
    private void sendUserList(Integer docId) throws IOException {
        Set<WebSocketSession> sessions = DOCUMENT_SESSIONS.get(docId);
        Set<Map<String, Object>> users = DOCUMENT_USERS.get(docId);
        
        if (sessions == null || users == null) {
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "user_list");
        response.put("docId", docId);
        response.put("users", new ArrayList<>(users));

        String message = objectMapper.writeValueAsString(response);
        for (WebSocketSession session : sessions) {
            session.sendMessage(new TextMessage(message));
        }
    }

    /**
     * 通知用户加入
     */
    private void notifyUserJoin(Integer docId, Map<String, Object> userInfo) throws IOException {
        Set<WebSocketSession> sessions = DOCUMENT_SESSIONS.get(docId);
        if (sessions == null) {
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "user_join");
        response.put("docId", docId);
        response.put("user", userInfo);

        String message = objectMapper.writeValueAsString(response);
        for (WebSocketSession session : sessions) {
            session.sendMessage(new TextMessage(message));
        }
    }

    /**
     * 通知用户离开
     */
    private void notifyUserLeave(Integer docId, Map<String, Object> userInfo) throws IOException {
        Set<WebSocketSession> sessions = DOCUMENT_SESSIONS.get(docId);
        if (sessions == null) {
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "user_leave");
        response.put("docId", docId);
        response.put("userId", userInfo.get("userId"));
        response.put("username", userInfo.get("username"));

        String message = objectMapper.writeValueAsString(response);
        for (WebSocketSession session : sessions) {
            session.sendMessage(new TextMessage(message));
        }
    }
}

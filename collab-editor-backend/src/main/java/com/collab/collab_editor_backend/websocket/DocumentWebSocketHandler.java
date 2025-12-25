package com.collab.collab_editor_backend.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.collab.collab_editor_backend.service.DocPermissionService;
import com.collab.collab_editor_backend.util.JwtUtil;
import com.collab.collab_editor_backend.util.WebSocketUtils;
import com.collab.collab_editor_backend.handler.OTAlgorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文档协作WebSocket处理器
 * 处理文档内容同步、用户加入/离开等事件
 */
public class DocumentWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(DocumentWebSocketHandler.class);

    // 依赖注入
    @Autowired
    private DocPermissionService docPermissionService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    // 文档ID到会话列表的映射，使用并发安全的集合
    private static final Map<Long, Set<WebSocketSession>> DOCUMENT_SESSIONS = new ConcurrentHashMap<>();
    // 用户会话到文档ID的映射
    private static final Map<WebSocketSession, Long> SESSION_DOCUMENTS = new ConcurrentHashMap<>();
    // 文档ID到在线用户列表的映射
    private static final Map<Long, Set<Map<String, Object>>> DOCUMENT_USERS = new ConcurrentHashMap<>();
    // 文档ID到当前版本的映射
    private static final Map<Long, AtomicInteger> DOCUMENT_VERSIONS = new ConcurrentHashMap<>();
    // 文档ID到操作历史的映射
    private static final Map<Long, List<OTAlgorithm.Operation>> DOCUMENT_OPERATIONS = new ConcurrentHashMap<>();
    // 文档ID到当前内容的映射
    private static final Map<Long, String> DOCUMENT_CONTENTS = new ConcurrentHashMap<>();
    // JSON序列化/反序列化工具
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 获取文档ID
        Long docId = getDocIdFromSession(session);
        if (docId == null) {
            logger.error("WebSocket连接失败：无效的文档ID, URI: {}", session.getUri());
            session.close(new CloseStatus(4000, "无效的文档ID"));
            return;
        }
        
        // 获取用户ID
        Long userId = WebSocketUtils.getUserIdFromSession(session, jwtUtil);
        if (userId == null) {
            logger.error("WebSocket连接失败：无法识别用户身份, URI: {}", session.getUri());
            session.close(new CloseStatus(4001, "无法识别用户身份"));
            return;
        }
        
        // 共享模式下，所有用户都有权限访问
        // 检查用户是否有文档的查看权限
        // if (!docPermissionService.hasViewPermission(docId, userId)) {
        //     logger.warn("用户没有权限访问文档, docId: {}, userId: {}", docId, userId);
        //     session.close(new CloseStatus(4003, "您没有权限访问此文档"));
        //     return;
        // }

        // 将会话添加到文档会话列表
        DOCUMENT_SESSIONS.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(session);
        SESSION_DOCUMENTS.put(session, docId);
        
        // 初始化文档版本和操作历史
        DOCUMENT_VERSIONS.computeIfAbsent(docId, k -> new AtomicInteger(0));
        DOCUMENT_OPERATIONS.computeIfAbsent(docId, k -> Collections.synchronizedList(new ArrayList<>()));

        // 自动加入在线用户列表并广播
        try {
            String uri = session.getUri().toString();
            String token = null;
            int qIdx = uri.indexOf("?");
            if (qIdx != -1) {
                String qs = uri.substring(qIdx + 1);
                String[] params = qs.split("&");
                for (String p : params) {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2 && "token".equals(java.net.URLDecoder.decode(kv[0], java.nio.charset.StandardCharsets.UTF_8))) {
                        token = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                        break;
                    }
                }
            }
            String username = null;
            if (token != null && !token.isEmpty()) {
                try {
                    username = jwtUtil.getUsernameFromToken(token);
                } catch (Exception e) {
                    logger.warn("解析用户名失败: {}", e.getMessage());
                }
            }
            if (username == null) {
                username = "用户" + userId;
            }
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", userId.intValue());
            userInfo.put("username", username);
            Set<Map<String, Object>> users = DOCUMENT_USERS.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet());
            boolean exists = false;
            for (Map<String, Object> u : users) {
                if (u.get("userId").equals(userId.intValue())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                users.add(userInfo);
            }
            session.getAttributes().put("user", userInfo);
            sendUserList(docId);
            notifyUserJoin(docId, userInfo);
        } catch (Exception e) {
            logger.warn("自动加入在线用户失败: {}", e.getMessage());
        }

        logger.info("用户连接到文档: {}, 用户ID: {}, 当前连接数: {}", docId, userId, DOCUMENT_SESSIONS.get(docId).size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 解析接收到的消息
        Map<String, Object> messageMap = objectMapper.readValue(message.getPayload(), Map.class);
        String type = (String) messageMap.get("type");
        Long docId = messageMap.get("docId") instanceof Number ? ((Number) messageMap.get("docId")).longValue() : null;

        if (docId == null) {
            return;
        }

        switch (type) {
            case "join":
            case "user_join":  // 同时支持两种消息类型，兼容旧版和新版前端
                handleUserJoin(session, messageMap, docId);
                break;
            case "content_update":
                handleContentUpdate(session, messageMap, docId);
                break;
            case "operation":
                handleOperation(session, messageMap, docId);
                break;
            case "get_document":
                handleGetDocument(session, docId);
                break;
            case "cursor_position":
                handleCursorPosition(session, messageMap, docId);
                break;
            // 可以添加更多消息类型处理
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long docId = SESSION_DOCUMENTS.remove(session);
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
                DOCUMENT_VERSIONS.remove(docId);
                DOCUMENT_OPERATIONS.remove(docId);
                DOCUMENT_CONTENTS.remove(docId);
            } else {
                // 从用户列表中移除用户并通知其他用户
                removeUserFromDocument(session, docId);
            }
        }

        logger.info("用户断开文档连接: {}, 当前连接数: {}", docId, (sessions != null ? sessions.size() : 0));
    }

    /**
     * 处理用户加入事件
     */
    private void handleUserJoin(WebSocketSession session, Map<String, Object> messageMap, Long docId) throws IOException {
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
        
        logger.info("用户加入文档: {}, 用户名: {}", docId, username);
    }

    /**
     * 处理内容更新事件
     */
    private void handleContentUpdate(WebSocketSession session, Map<String, Object> messageMap, Long docId) throws IOException {
        String content = (String) messageMap.get("content");
        if (content == null) {
            return;
        }

        // 更新文档内容
        DOCUMENT_CONTENTS.put(docId, content);
        
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

                    synchronized (otherSession) {
                        otherSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                    }
                }
            }
        }
    }

    /**
     * 处理光标位置更新事件
     */
    private void handleCursorPosition(WebSocketSession session, Map<String, Object> messageMap, Long docId) throws IOException {
        Integer userId = messageMap.get("userId") instanceof Number ? ((Number) messageMap.get("userId")).intValue() : null;
        String username = (String) messageMap.get("username");
        Integer cursorPosition = messageMap.get("cursorPosition") instanceof Number ? ((Number) messageMap.get("cursorPosition")).intValue() : null;

        if (userId == null || username == null || cursorPosition == null) {
            return;
        }

        // 广播光标位置更新给其他用户
        Set<WebSocketSession> sessions = DOCUMENT_SESSIONS.get(docId);
        if (sessions != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "cursor_position_update");
            response.put("docId", docId);
            response.put("userId", userId);
            response.put("username", username);
            response.put("cursorPosition", cursorPosition);

            String responseJson = objectMapper.writeValueAsString(response);
            for (WebSocketSession otherSession : sessions) {
                if (!otherSession.equals(session)) {
                    if (otherSession.isOpen()) {
                        synchronized (otherSession) {
                            otherSession.sendMessage(new TextMessage(responseJson));
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理操作事件
     */
    private void handleOperation(WebSocketSession session, Map<String, Object> messageMap, Long docId) throws IOException {
        String opType = (String) messageMap.get("operationType");
        Integer position = messageMap.get("position") instanceof Number ? ((Number) messageMap.get("position")).intValue() : null;
        String content = (String) messageMap.get("content");
        Integer version = messageMap.get("version") instanceof Number ? ((Number) messageMap.get("version")).intValue() : null;

        if (opType == null || position == null || version == null) {
            return;
        }

        // 创建操作对象
        OTAlgorithm.OperationType type = "insert".equals(opType) ? OTAlgorithm.OperationType.INSERT : OTAlgorithm.OperationType.DELETE;
        OTAlgorithm.Operation operation = new OTAlgorithm.Operation(type, position, content, version);

        // 获取文档的当前版本和操作历史
        AtomicInteger currentVersion = DOCUMENT_VERSIONS.get(docId);
        List<OTAlgorithm.Operation> operations = DOCUMENT_OPERATIONS.get(docId);
        String currentContent = DOCUMENT_CONTENTS.getOrDefault(docId, "");

        // 如果操作版本不是当前版本，需要进行操作转换
        if (version < currentVersion.get()) {
            // 获取需要转换的操作列表
            List<OTAlgorithm.Operation> pendingOperations = operations.subList(version, currentVersion.get());
            // 转换操作
            for (OTAlgorithm.Operation pendingOp : pendingOperations) {
                operation = OTAlgorithm.transform(operation, pendingOp);
            }
        }

        // 应用操作到文档内容
        String newContent = OTAlgorithm.applyOperation(currentContent, operation);
        DOCUMENT_CONTENTS.put(docId, newContent);

        // 将操作添加到历史记录
        operations.add(operation);
        currentVersion.incrementAndGet();

        // 广播操作给所有其他用户
        Set<WebSocketSession> sessions = DOCUMENT_SESSIONS.get(docId);
        if (sessions != null) {
            for (WebSocketSession otherSession : sessions) {
                if (!otherSession.equals(session)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "operation");
                    response.put("docId", docId);
                    response.put("operationType", opType);
                    response.put("position", operation.getPosition());
                    response.put("content", operation.getContent());
                    response.put("version", currentVersion.get());
                    response.put("userId", messageMap.get("userId"));
                    response.put("username", messageMap.get("username"));

                    synchronized (otherSession) {
                        otherSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                    }
                }
            }
        }
    }

    /**
     * 处理获取文档内容请求
     */
    private void handleGetDocument(WebSocketSession session, Long docId) throws IOException {
        String content = DOCUMENT_CONTENTS.getOrDefault(docId, "");
        int version = DOCUMENT_VERSIONS.get(docId).get();

        Map<String, Object> response = new HashMap<>();
        response.put("type", "document_content");
        response.put("docId", docId);
        response.put("content", content);
        response.put("version", version);

        synchronized (session) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }

    /**
     * 从会话中获取文档ID
     */
    private Long getDocIdFromSession(WebSocketSession session) {
        // 从URI中获取文档ID参数
        String uri = session.getUri().toString();
        
        // 移除查询参数
        int queryIndex = uri.indexOf("?");
        if (queryIndex != -1) {
            uri = uri.substring(0, queryIndex);
        }
        
        String[] parts = uri.split("/");
        if (parts.length < 4) {
            return null;
        }
        try {
            return Long.parseLong(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从文档中移除用户
     */
    private void removeUserFromDocument(WebSocketSession session, Long docId) throws IOException {
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
    private void sendUserList(Long docId) throws IOException {
        Set<WebSocketSession> sessions = DOCUMENT_SESSIONS.get(docId);
        Set<Map<String, Object>> users = DOCUMENT_USERS.get(docId);
        
        if (sessions == null || users == null) {
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "online_users");
        response.put("docId", docId);
        response.put("users", new ArrayList<>(users));

        String message = objectMapper.writeValueAsString(response);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        }
    }

    /**
     * 通知用户加入
     */
    private void notifyUserJoin(Long docId, Map<String, Object> userInfo) throws IOException {
        Set<WebSocketSession> sessions = DOCUMENT_SESSIONS.get(docId);
        if (sessions == null) {
            return;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "user_join");
        response.put("docId", docId);
        response.put("userId", userInfo.get("userId"));
        response.put("username", userInfo.get("username"));

        String message = objectMapper.writeValueAsString(response);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        }
    }

    /**
     * 通知用户离开
     */
    private void notifyUserLeave(Long docId, Map<String, Object> userInfo) throws IOException {
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
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        }
    }
}

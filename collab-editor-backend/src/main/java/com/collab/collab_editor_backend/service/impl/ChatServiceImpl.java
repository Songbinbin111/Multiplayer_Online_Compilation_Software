package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.collab_editor_backend.entity.ChatMessage;
import com.collab.collab_editor_backend.entity.User;
import com.collab.collab_editor_backend.mapper.ChatMessageMapper;
import com.collab.collab_editor_backend.mapper.UserMapper;
import com.collab.collab_editor_backend.service.ChatService;
import com.collab.collab_editor_backend.util.MinIOUtil;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.collab.collab_editor_backend.websocket.ChatWebSocketHandler;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MinIOUtil minIOUtil;

    /**
     * 发送消息
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param content 消息内容
     * @return 发送结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> sendMessage(Long senderId, Long receiverId, String content) {
        // 1. 验证发送者和接收者是否存在
        User sender = userMapper.selectById(senderId);
        User receiver = userMapper.selectById(receiverId);
        if (sender == null || receiver == null) {
            return Result.error("用户不存在");
        }

        // 2. 创建消息实体
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSenderId(senderId);
        chatMessage.setReceiverId(receiverId);
        chatMessage.setContent(content);
        chatMessage.setSendTime(LocalDateTime.now());
        chatMessage.setIsRead(0); // 初始化为未读

        // 3. 保存消息
        chatMessageMapper.insert(chatMessage);

        // 4. 通过WebSocket向接收者推送新消息
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("type", "new_message");
        messageData.put("senderId", senderId);
        messageData.put("receiverId", receiverId);
        messageData.put("content", content);
        messageData.put("sendTime", chatMessage.getSendTime());
        messageData.put("isRead", chatMessage.getIsRead());
        
        // 将消息数据转换为JSON字符串
        String jsonMessage = "{\"type\":\"new_message\",\"senderId\":\"" + senderId + "\",\"receiverId\":\"" + receiverId + "\",\"content\":\"" + content.replace("\"", "\\\"") + "\",\"sendTime\":\"" + chatMessage.getSendTime() + "\",\"isRead\":\"" + chatMessage.getIsRead() + "\"}";
        
        ChatWebSocketHandler.sendMessageToUser(receiverId, jsonMessage);

        return Result.success("消息发送成功");
    }

    /**
     * 获取聊天历史记录
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @return 聊天记录列表
     */
    @Override
    public Result<List<ChatMessage>> getChatHistory(Long userId1, Long userId2) {
        // 1. 验证用户是否存在
        User user1 = userMapper.selectById(userId1);
        User user2 = userMapper.selectById(userId2);
        if (user1 == null || user2 == null) {
            return Result.error("用户不存在");
        }

        // 2. 获取聊天历史记录
        List<ChatMessage> chatHistory = chatMessageMapper.getChatHistory(userId1, userId2);

        // 3. 标记对方发送的消息为已读
        chatMessageMapper.markMessagesAsRead(userId2, userId1);

        return Result.success(chatHistory);
    }

    /**
     * 标记消息为已读
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @return 操作结果
     */
    @Override
    public Result<?> markAsRead(Long senderId, Long receiverId) {
        // 1. 验证用户是否存在
        User sender = userMapper.selectById(senderId);
        User receiver = userMapper.selectById(receiverId);
        if (sender == null || receiver == null) {
            return Result.error("用户不存在");
        }

        // 2. 标记消息为已读
        chatMessageMapper.markMessagesAsRead(senderId, receiverId);

        return Result.success("消息已标记为已读");
    }

    /**
     * 获取用户的未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    @Override
    public Result<Integer> getUnreadCount(Long userId) {
        // 1. 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 2. 获取未读消息数量
        int unreadCount = chatMessageMapper.getUnreadCount(userId);

        return Result.success(unreadCount);
    }

    /**
     * 获取用户的聊天列表（包含最近一条消息和未读数量）
     * @param userId 用户ID
     * @return 聊天列表
     */
    @Override
    public Result<?> getChatList(Long userId) {
        // 1. 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 2. 获取所有相关的聊天消息
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSenderId, userId)
                .or()
                .eq(ChatMessage::getReceiverId, userId);
        queryWrapper.orderByDesc(ChatMessage::getSendTime);
        List<ChatMessage> allMessages = chatMessageMapper.selectList(queryWrapper);

        // 3. 分组并获取每个聊天对象的最近一条消息
        Map<Long, ChatMessage> chatMap = new LinkedHashMap<>();
        for (ChatMessage message : allMessages) {
            Long otherUserId = message.getSenderId().equals(userId) ? message.getReceiverId() : message.getSenderId();
            if (!chatMap.containsKey(otherUserId)) {
                chatMap.put(otherUserId, message);
            }
        }

        // 4. 构建聊天列表响应
        List<Map<String, Object>> chatList = new ArrayList<>();
        for (Map.Entry<Long, ChatMessage> entry : chatMap.entrySet()) {
            Long otherUserId = entry.getKey();
            ChatMessage lastMessage = entry.getValue();
            User otherUser = userMapper.selectById(otherUserId);
            
            // 获取与该用户的未读消息数量
            LambdaQueryWrapper<ChatMessage> unreadQuery = new LambdaQueryWrapper<>();
            unreadQuery.eq(ChatMessage::getSenderId, otherUserId)
                    .eq(ChatMessage::getReceiverId, userId)
                    .eq(ChatMessage::getIsRead, 0);
            Long unreadCount = chatMessageMapper.selectCount(unreadQuery);

            Map<String, Object> chatInfo = new HashMap<>();
            chatInfo.put("userId", otherUserId);
            chatInfo.put("username", otherUser.getUsername());
            chatInfo.put("lastMessage", lastMessage.getContent());
            chatInfo.put("lastMessageTime", lastMessage.getSendTime());
            chatInfo.put("unreadCount", unreadCount);
            chatList.add(chatInfo);
        }

        return Result.success(chatList);
    }

    /**
     * 发送文件消息
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param file 上传的文件
     * @return 发送结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> sendFile(Long senderId, Long receiverId, MultipartFile file) {
        try {
            // 1. 验证发送者和接收者是否存在
            User sender = userMapper.selectById(senderId);
            User receiver = userMapper.selectById(receiverId);
            if (sender == null || receiver == null) {
                return Result.error("用户不存在");
            }

            // 2. 上传文件到MinIO或本地存储
            String fileUrl = minIOUtil.uploadFile(file);

            // 3. 创建文件消息实体
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSenderId(senderId);
            chatMessage.setReceiverId(receiverId);
            chatMessage.setContent(file.getOriginalFilename()); // 内容设置为文件名
            chatMessage.setFileName(file.getOriginalFilename());
            chatMessage.setFileUrl(fileUrl);
            chatMessage.setFileType(file.getContentType());
            chatMessage.setFileSize(file.getSize());
            chatMessage.setMessageType(1); // 文件消息
            chatMessage.setSendTime(LocalDateTime.now());
            chatMessage.setIsRead(0); // 初始化为未读

            // 4. 保存消息
            chatMessageMapper.insert(chatMessage);

            // 5. 通过WebSocket向接收者推送新文件消息
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "new_message");
            messageData.put("senderId", senderId);
            messageData.put("receiverId", receiverId);
            messageData.put("content", file.getOriginalFilename());
            messageData.put("fileName", file.getOriginalFilename());
            messageData.put("fileUrl", fileUrl);
            messageData.put("fileType", file.getContentType());
            messageData.put("fileSize", file.getSize());
            messageData.put("messageType", 1);
            messageData.put("sendTime", chatMessage.getSendTime());
            messageData.put("isRead", chatMessage.getIsRead());
            
            // 将消息数据转换为JSON字符串
            String jsonMessage = String.format("{\"type\":\"new_message\",\"senderId\":\"%d\",\"receiverId\":\"%d\",\"content\":\"%s\",\"fileName\":\"%s\",\"fileUrl\":\"%s\",\"fileType\":\"%s\",\"fileSize\":\"%d\",\"messageType\":\"%d\",\"sendTime\":\"%s\",\"isRead\":\"%d\"}",
                    senderId, receiverId, file.getOriginalFilename().replace("\"", "\\\""), 
                    file.getOriginalFilename().replace("\"", "\\\""), fileUrl.replace("\"", "\\\""), 
                    file.getContentType() != null ? file.getContentType().replace("\"", "\\\"") : "", 
                    file.getSize(), 1, chatMessage.getSendTime(), chatMessage.getIsRead());
            
            ChatWebSocketHandler.sendMessageToUser(receiverId, jsonMessage);

            return Result.success("文件发送成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发送文件失败：" + e.getMessage());
        }
    }
}

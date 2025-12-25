package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.entity.ChatMessage;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 聊天服务接口
 */
public interface ChatService {
    
    /**
     * 发送消息
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param content 消息内容
     * @return 发送结果
     */
    Result<?> sendMessage(Long senderId, Long receiverId, String content);
    
    /**
     * 获取聊天历史记录
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @return 聊天记录列表
     */
    Result<List<ChatMessage>> getChatHistory(Long userId1, Long userId2);
    
    /**
     * 标记消息为已读
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @return 操作结果
     */
    Result<?> markAsRead(Long senderId, Long receiverId);
    
    /**
     * 获取用户的未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    Result<Integer> getUnreadCount(Long userId);
    
    /**
     * 获取用户的聊天列表（包含最近一条消息和未读数量）
     * @param userId 用户ID
     * @return 聊天列表
     */
    Result<?> getChatList(Long userId);

    /**
     * 发送文件消息
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param file 上传的文件
     * @return 发送结果
     */
    Result<?> sendFile(Long senderId, Long receiverId, MultipartFile file);
}

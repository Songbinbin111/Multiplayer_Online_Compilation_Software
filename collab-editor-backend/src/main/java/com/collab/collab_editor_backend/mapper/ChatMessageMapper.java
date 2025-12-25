package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.ChatMessage;
import java.util.List;

/**
 * 聊天消息Mapper接口
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    
    /**
     * 获取两个用户之间的聊天记录
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @return 聊天记录列表
     */
    List<ChatMessage> getChatHistory(Long userId1, Long userId2);
    
    /**
     * 更新消息为已读
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @return 更新成功的记录数
     */
    int markMessagesAsRead(Long senderId, Long receiverId);
    
    /**
     * 获取用户的未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    int getUnreadCount(Long userId);
}

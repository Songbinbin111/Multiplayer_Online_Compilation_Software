package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.entity.Notification;
import java.util.List;

/**
 * 通知模块Service接口
 */
public interface NotificationService {
    
    /**
     * 创建通知
     * @param notification 通知对象
     * @return 创建成功的通知
     */
    Notification create(Notification notification);
    
    /**
     * 根据用户ID获取通知列表
     * @param userId 用户ID
     * @param isRead 是否已读（可选，null表示所有通知）
     * @return 通知列表
     */
    List<Notification> getByUserId(Long userId, Boolean isRead);
    
    /**
     * 将通知标记为已读
     * @param notificationId 通知ID
     * @return 是否成功
     */
    Boolean markAsRead(Long notificationId);
    
    /**
     * 将用户的所有通知标记为已读
     * @param userId 用户ID
     * @return 成功标记的通知数
     */
    Integer markAllAsRead(Long userId);
    
    /**
     * 获取用户未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    Integer countUnreadByUserId(Long userId);
    
    /**
     * 删除通知
     * @param notificationId 通知ID
     * @return 是否成功
     */
    Boolean delete(Long notificationId);
    
    /**
     * 发送@提及通知
     * @param docId 文档ID
     * @param content 通知内容
     * @param userId 提及的用户ID
     * @param creatorId 创建者ID
     * @return 通知对象
     */
    Notification sendMentionNotification(Long docId, String content, Long userId, Long creatorId);
    
    /**
     * 发送任务分配通知
     * @param taskId 任务ID
     * @param title 任务标题
     * @param docId 文档ID
     * @param assigneeId 负责人ID
     * @param creatorId 创建者ID
     * @return 通知对象
     */
    Notification sendTaskAssignNotification(Long taskId, String title, Long docId, Long assigneeId, Long creatorId);
}

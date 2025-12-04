package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.collab_editor_backend.entity.Notification;
import com.collab.collab_editor_backend.mapper.NotificationMapper;
import com.collab.collab_editor_backend.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知模块Service实现类
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    
    @Autowired
    private NotificationMapper notificationMapper;
    
    /**
     * 创建通知
     * @param notification 通知对象
     * @return 创建成功的通知
     */
    @Override
    public Notification create(Notification notification) {
        try {
            notification.setCreateTime(LocalDateTime.now());
            notification.setIsRead(false);
            notificationMapper.insert(notification);
            logger.info("创建通知成功: {}", notification);
            return notification;
        } catch (Exception e) {
            logger.error("创建通知失败: {}", notification, e);
            throw e;
        }
    }
    
    /**
     * 根据用户ID获取通知列表
     * @param userId 用户ID
     * @param isRead 是否已读（可选，null表示所有通知）
     * @return 通知列表
     */
    @Override
    public List<Notification> getByUserId(Long userId, Boolean isRead) {
        try {
            List<Notification> notifications = notificationMapper.getNotificationsByUserId(userId, isRead);
            logger.info("获取用户{}的通知列表，isRead={}，共{}条", userId, isRead, notifications.size());
            return notifications;
        } catch (Exception e) {
            logger.error("获取用户{}的通知列表失败，isRead={}", userId, isRead, e);
            throw e;
        }
    }
    
    /**
     * 将通知标记为已读
     * @param notificationId 通知ID
     * @return 是否成功
     */
    @Override
    public Boolean markAsRead(Long notificationId) {
        try {
            Notification notification = notificationMapper.selectById(notificationId);
            if (notification == null) {
                logger.warn("通知ID不存在: {}", notificationId);
                return false;
            }
            notification.setIsRead(true);
            notificationMapper.updateById(notification);
            logger.info("通知ID {} 标记为已读", notificationId);
            return true;
        } catch (Exception e) {
            logger.error("将通知ID {} 标记为已读失败", notificationId, e);
            throw e;
        }
    }
    
    /**
     * 将用户的所有通知标记为已读
     * @param userId 用户ID
     * @return 成功标记的通知数
     */
    @Override
    public Integer markAllAsRead(Long userId) {
        try {
            int count = notificationMapper.markAllAsRead(userId);
            logger.info("用户{}的所有通知标记为已读，共{}条", userId, count);
            return count;
        } catch (Exception e) {
            logger.error("将用户{}的所有通知标记为已读失败", userId, e);
            throw e;
        }
    }
    
    /**
     * 获取用户未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    @Override
    public Integer countUnreadByUserId(Long userId) {
        try {
            int count = notificationMapper.countUnreadByUserId(userId);
            logger.info("用户{}的未读通知数量: {}", userId, count);
            return count;
        } catch (Exception e) {
            logger.error("获取用户{}的未读通知数量失败", userId, e);
            throw e;
        }
    }
    
    /**
     * 删除通知
     * @param notificationId 通知ID
     * @return 是否成功
     */
    @Override
    public Boolean delete(Long notificationId) {
        try {
            int count = notificationMapper.deleteById(notificationId);
            logger.info("删除通知ID {}，结果: {}", notificationId, count > 0);
            return count > 0;
        } catch (Exception e) {
            logger.error("删除通知ID {} 失败", notificationId, e);
            throw e;
        }
    }
    
    /**
     * 发送@提及通知
     * @param docId 文档ID
     * @param content 通知内容
     * @param userId 提及的用户ID
     * @param creatorId 创建者ID
     * @return 通知对象
     */
    @Override
    public Notification sendMentionNotification(Long docId, String content, Long userId, Long creatorId) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType("mention");
            notification.setContent(content);
            notification.setDocId(docId);
            notification.setRelatedId(creatorId);
            notification.setIsRead(false);
            notification.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(notification);
            logger.info("发送@提及通知成功: {}", notification);
            return notification;
        } catch (Exception e) {
            logger.error("发送@提及通知失败，docId={}, content={}, userId={}, creatorId={}", docId, content, userId, creatorId, e);
            throw e;
        }
    }
    
    /**
     * 发送任务分配通知
     * @param taskId 任务ID
     * @param title 任务标题
     * @param docId 文档ID
     * @param assigneeId 负责人ID
     * @param creatorId 创建者ID
     * @return 通知对象
     */
    @Override
    public Notification sendTaskAssignNotification(Long taskId, String title, Long docId, Long assigneeId, Long creatorId) {
        try {
            Notification notification = new Notification();
            notification.setUserId(assigneeId);
            notification.setType("task_assign");
            notification.setContent("您被分配了任务: " + title);
            notification.setDocId(docId);
            notification.setRelatedId(taskId);
            notification.setIsRead(false);
            notification.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(notification);
            logger.info("发送任务分配通知成功: {}", notification);
            return notification;
        } catch (Exception e) {
            logger.error("发送任务分配通知失败，taskId={}, title={}, docId={}, assigneeId={}, creatorId={}", taskId, title, docId, assigneeId, creatorId, e);
            throw e;
        }
    }
}

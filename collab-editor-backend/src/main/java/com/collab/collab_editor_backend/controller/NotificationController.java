package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.entity.Notification;
import com.collab.collab_editor_backend.service.NotificationService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知模块Controller
 */
@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 获取用户的通知列表
     * @param userId 用户ID
     * @param isRead 是否已读（可选，null表示所有通知）
     * @param type 通知类型（可选，null表示所有类型）
     * @return 通知列表
     */
    @GetMapping("/list")
    public Result<List<Notification>> getNotificationList(@RequestParam Long userId, 
                                                          @RequestParam(required = false) Boolean isRead,
                                                          @RequestParam(required = false) String type) {
        try {
            List<Notification> notifications = notificationService.getByUserId(userId, isRead, type);
            return Result.success(notifications);
        } catch (Exception e) {
            return Result.error("获取通知列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 将通知标记为已读
     * @param notificationId 通知ID
     * @return 操作结果
     */
    @PutMapping("/read/{notificationId}")
    public Result<Boolean> markAsRead(@PathVariable Long notificationId) {
        try {
            Boolean result = notificationService.markAsRead(notificationId);
            if (result) {
                return Result.successWithMessage("通知已标记为已读", true);
            } else {
                return Result.error("通知不存在");
            }
        } catch (Exception e) {
            return Result.error("标记通知为已读失败: " + e.getMessage());
        }
    }
    
    /**
     * 将用户的所有通知标记为已读
     * @param userId 用户ID
     * @return 操作结果
     */
    @PutMapping("/read/all")
    public Result<Integer> markAllAsRead(@RequestParam Long userId) {
        try {
            Integer count = notificationService.markAllAsRead(userId);
            return Result.successWithMessage("所有通知已标记为已读", count);
        } catch (Exception e) {
            return Result.error("标记所有通知为已读失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    @GetMapping("/unread/count")
    public Result<Integer> getUnreadCount(@RequestParam Long userId) {
        try {
            Integer count = notificationService.countUnreadByUserId(userId);
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("获取未读通知数量失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除通知
     * @param notificationId 通知ID
     * @return 操作结果
     */
    @DeleteMapping("/{notificationId}")
    public Result<Boolean> deleteNotification(@PathVariable Long notificationId) {
        try {
            Boolean result = notificationService.delete(notificationId);
            if (result) {
                return Result.successWithMessage("通知已删除", true);
            } else {
                return Result.error("通知不存在");
            }
        } catch (Exception e) {
            return Result.error("删除通知失败: " + e.getMessage());
        }
    }
}

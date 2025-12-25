package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知设置实体类
 */
@Data
@TableName(value = "t_notification_setting")
public class NotificationSetting {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId; // 用户ID
    private Boolean mentionEnabled; // 是否接收@提及通知
    private Boolean taskAssignEnabled; // 是否接收任务分配通知
    private Boolean taskStatusEnabled; // 是否接收任务状态变更通知
    private Boolean emailEnabled; // 是否接收邮件通知
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
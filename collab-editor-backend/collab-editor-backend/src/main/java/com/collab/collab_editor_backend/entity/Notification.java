package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_notification") // 对应数据库表名 t_notification
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId; // 接收通知的用户ID
    private String type; // 通知类型：mention（@提及）、task_assign（任务分配）
    private String content; // 通知内容
    private Long docId; // 关联的文档ID（可选）
    private Long relatedId; // 关联的ID（评论ID或任务ID，可选）
    private Boolean isRead; // 是否已读，默认为false
    private LocalDateTime createTime; // 创建时间
}

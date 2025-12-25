package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 * 对应数据库表：t_chat_message
 */
@Data
@TableName(value = "t_chat_message") // 对应数据库表名 t_chat_message
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId; // 发送者ID，对应表中 sender_id 字段
    private Long receiverId; // 接收者ID，对应表中 receiver_id 字段
    private String content; // 消息内容，对应表中 content 字段
    private LocalDateTime sendTime; // 发送时间，对应表中 send_time 字段
    private Integer isRead; // 是否已读：0-未读，1-已读，对应表中 is_read 字段
    
    // 文件相关字段，用于文件共享功能
    private String fileName; // 文件名
    private String fileUrl; // 文件访问URL
    private String fileType; // 文件类型
    private Long fileSize; // 文件大小（字节）
    private Integer messageType; // 消息类型：0-文本消息，1-文件消息
}

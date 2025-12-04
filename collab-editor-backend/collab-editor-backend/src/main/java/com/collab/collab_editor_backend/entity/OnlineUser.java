package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_online_user") // 对应数据库表名 t_online_user
public class OnlineUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId; // 用户ID，对应表中 user_id 字段
    private Long docId; // 文档ID，对应表中 doc_id 字段
    private String sessionId; // WebSocket会话ID，对应表中 session_id 字段
    private Integer onlineStatus; // 在线状态：0-离线，1-在线，2-编辑中，3-离开
    private LocalDateTime lastActiveTime; // 最后活跃时间，对应表中 last_active_time 字段
}
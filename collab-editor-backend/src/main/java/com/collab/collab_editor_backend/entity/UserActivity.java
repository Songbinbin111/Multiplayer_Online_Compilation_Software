package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户行为实体类
 * 记录用户的各种操作行为
 */
@Data
@TableName("t_user_activity")
public class UserActivity {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 行为类型
     * 如：login, logout, create_document, edit_document, view_document, share_document等
     */
    private String activityType;
    
    /**
     * 相关对象ID
     * 如：文档ID、评论ID等
     */
    private Long objectId;
    
    /**
     * 相关对象类型
     * 如：document, comment等
     */
    private String objectType;
    
    /**
     * 行为详情
     * JSON格式，存储详细的行为信息
     */
    private String details;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理信息
     */
    private String userAgent;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
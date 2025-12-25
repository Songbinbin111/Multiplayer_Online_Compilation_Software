package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "t_user") // 对应数据库表名 t_user
public class User {
    @TableId(type = IdType.AUTO) // 主键自增，对应表中 id 字段
    private Long id;
    private String username; // 对应表中 username 字段
    private String password; // 对应表中 password 字段
    private String nickname; // 对应表中 nickname 字段
    private String avatarUrl; // 对应表中 avatar_url 字段（下划线转驼峰）
    private String email; // 对应表中 email 字段
    private String phone; // 对应表中 phone 字段
    private String role; // 对应表中 role 字段（admin, editor, viewer）
    private LocalDateTime createTime; // 对应表中 create_time 字段
    private LocalDateTime updateTime; // 对应表中 update_time 字段
    private String resetToken; // 对应表中 reset_token 字段，密码重置令牌
    private LocalDateTime resetTokenExpiry; // 对应表中 reset_token_expiry 字段，重置令牌过期时间
}
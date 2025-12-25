package com.collab.collab_editor_backend.dto;

import lombok.Data;

/**
 * 用户个人资料DTO，用于接收用户修改个人资料的请求参数
 */
@Data
public class UserProfileDTO {
    private Long id; // 用户ID
    private String nickname; // 昵称
    private String email; // 邮箱
    private String phone; // 手机号
    private String avatarUrl; // 头像URL
}

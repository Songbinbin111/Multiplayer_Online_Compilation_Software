package com.collab.collab_editor_backend.dto;

import jakarta.validation.constraints.NotBlank; // 关键修改：Jakarta 包
import lombok.Data;

/**
 * 用户登录参数DTO
 */
@Data
public class UserLoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username; // 用户名

    @NotBlank(message = "密码不能为空")
    private String password; // 密码
}
package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色权限关联实体类
 */
@Data
@TableName(value = "t_role_permission")
public class RolePermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleName; // 角色名称（admin, editor, viewer）
    private Long permissionId; // 权限ID
}

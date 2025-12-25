package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权限实体类
 */
@Data
@TableName(value = "t_permission")
public class Permission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String permissionCode; // 权限编码
    private String permissionName; // 权限名称
    private String description; // 权限描述
}

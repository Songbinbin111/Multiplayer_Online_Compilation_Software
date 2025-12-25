package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "t_doc_permission") // 对应数据库表名 t_doc_permission
public class DocPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId; // 文档ID
    private Long userId; // 用户ID
    private Integer permissionType; // 权限类型：0-查看者，1-编辑者，2-管理员
    private LocalDateTime createTime; // 创建时间
}

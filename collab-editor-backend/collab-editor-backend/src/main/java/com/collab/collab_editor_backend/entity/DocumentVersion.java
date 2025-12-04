package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档版本实体类
 * 用于存储文档的历史版本信息
 */
@Data
@TableName("t_document_version") // 对应数据库表名
public class DocumentVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId; // 所属文档ID
    private String content; // 文档版本内容
    private String versionName; // 版本名称
    private String description; // 版本描述
    private Long createdBy; // 创建者ID
    private LocalDateTime createdTime; // 创建时间
    private Integer versionNumber; // 版本号
}

package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "t_document") // 对应数据库表名
public class Document {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title; // 文档标题
    @TableField("owner_id") // 映射到数据库表的owner_id字段
    private Long ownerId; // 所有者ID
    @TableField("content") // 明确指定数据库字段名
    private String content; // 文档内容
    @TableField("minio_key") // 明确指定数据库字段名
    private String minioKey; // MinIO 存储路径（对应数据库的minio_key字段）
    private String category; // 文档分类
    private String tags; // 文档标签，用逗号分隔
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_document") // 对应数据库表名
public class Document {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title; // 文档标题
    private Long ownerId; // 所有者ID
    private String content; // MinIO 存储路径
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
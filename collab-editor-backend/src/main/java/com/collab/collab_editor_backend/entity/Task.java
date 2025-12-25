package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "t_task") // 对应数据库表名 t_task
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId; // 关联的文档ID
    private String title; // 任务标题
    private String content; // 任务内容
    private Long creatorId; // 创建者ID
    private Long assigneeId; // 负责人ID
    private Integer status; // 任务状态：0-待处理，1-进行中，2-已完成

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline; // 截止日期

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 更新时间
}

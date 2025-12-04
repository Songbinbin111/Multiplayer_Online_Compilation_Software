package com.collab.collab_editor_backend.dto;

import lombok.Data;

@Data
public class TaskCreateDTO {
    // 文档ID
    private Long docId;
    // 任务标题
    private String title;
    // 任务内容
    private String content;
    // 负责人ID
    private Long assigneeId;
}

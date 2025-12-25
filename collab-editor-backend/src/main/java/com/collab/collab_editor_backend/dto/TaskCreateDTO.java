package com.collab.collab_editor_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

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
    // 截止日期
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime deadline;
}

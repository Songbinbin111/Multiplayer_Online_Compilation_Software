package com.collab.collab_editor_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TaskUpdateDTO {
    // 任务ID
    private Long taskId;
    // 任务状态：0-待处理，1-进行中，2-已完成
    private Integer status;
    // 截止日期
    private LocalDateTime deadline;
}

package com.collab.collab_editor_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class TaskUpdateDTO {
    // 任务ID
    private Long taskId;
    // 任务状态：0-待处理，1-进行中，2-已完成
    private Integer status;
    // 截止日期
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;
}

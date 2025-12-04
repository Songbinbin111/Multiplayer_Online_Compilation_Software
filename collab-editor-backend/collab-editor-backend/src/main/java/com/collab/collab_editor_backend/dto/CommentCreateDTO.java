package com.collab.collab_editor_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCreateDTO {
    @NotNull(message = "文档ID不能为空")
    private Long docId;
    @NotBlank(message = "评论内容不能为空")
    private String content;
    private Long parentId; // 父评论ID，null表示顶级评论
}

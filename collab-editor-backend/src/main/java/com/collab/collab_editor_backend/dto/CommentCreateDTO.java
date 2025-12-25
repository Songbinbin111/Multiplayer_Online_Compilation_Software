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
    private Integer startPos; // 批注开始位置（可选，用于文本批注功能）
    private Integer endPos; // 批注结束位置（可选，用于文本批注功能）
    private String selectedText; // 批注选中的文本内容（可选，用于文本批注功能）
}

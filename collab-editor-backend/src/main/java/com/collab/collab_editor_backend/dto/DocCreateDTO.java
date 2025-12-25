package com.collab.collab_editor_backend.dto;

import jakarta.validation.constraints.NotBlank; // 关键修改：Jakarta 包
import lombok.Data;

/**
 * 文档创建参数DTO
 */
@Data
public class DocCreateDTO {

    @NotBlank(message = "文档标题不能为空")
    private String title; // 文档标题
    
    private String content; // 文档内容
    
    private String category; // 文档分类
    
    private String tags; // 文档标签，用逗号分隔
}
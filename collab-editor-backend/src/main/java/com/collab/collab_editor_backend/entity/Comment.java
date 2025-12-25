package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "t_comment") // 对应数据库表名 t_comment
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId; // 文档ID
    private Long userId; // 评论者ID
    private String content; // 评论内容
    private Long parentId; // 父评论ID（用于回复功能，null表示顶级评论）
    private Integer startPos; // 批注开始位置（用于文本批注功能）
    private Integer endPos; // 批注结束位置（用于文本批注功能）
    private String selectedText; // 批注选中的文本内容（用于文本批注功能）
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}

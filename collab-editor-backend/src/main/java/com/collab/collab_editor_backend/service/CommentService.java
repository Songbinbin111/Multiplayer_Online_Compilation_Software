package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.entity.Comment;
import com.collab.collab_editor_backend.dto.CommentCreateDTO;
import java.util.List;

/**
 * 评论模块服务层接口
 */
public interface CommentService {
    
    /**
     * 创建评论
     * @param userId 用户ID
     * @param commentCreateDTO 评论创建DTO
     * @return 创建的评论
     */
    Comment createComment(Long userId, CommentCreateDTO commentCreateDTO);
    
    /**
     * 根据文档ID获取所有评论
     * @param docId 文档ID
     * @return 评论列表
     */
    List<Comment> getCommentsByDocId(Long docId);
    
    /**
     * 根据父评论ID获取所有回复
     * @param parentId 父评论ID
     * @return 回复列表
     */
    List<Comment> getRepliesByParentId(Long parentId);
    
    /**
     * 删除评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteComment(Long commentId, Long userId);
    
    /**
     * 解析评论内容中的@提及用户
     * @param content 评论内容
     * @return 被提及的用户名列表
     */
    List<String> parseMentionedUsers(String content);
}

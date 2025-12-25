package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.Comment;
import java.util.List;

/**
 * 评论模块Mapper接口
 */
public interface CommentMapper extends BaseMapper<Comment> {
    
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
}

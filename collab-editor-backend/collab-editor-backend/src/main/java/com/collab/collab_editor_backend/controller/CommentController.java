package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.dto.CommentCreateDTO;
import com.collab.collab_editor_backend.entity.Comment;
import com.collab.collab_editor_backend.service.CommentService;
import com.collab.collab_editor_backend.util.JwtUtil;
import com.collab.collab_editor_backend.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建评论接口
     * @param dto 评论创建DTO
     * @param request 请求对象
     * @return 创建结果
     */
    @PostMapping("/create")
    public Result<?> createComment(@RequestBody CommentCreateDTO dto, HttpServletRequest request) {
        try {
            String authorization = request.getHeader("Authorization");
            Long userId = jwtUtil.getUserIdFromToken(authorization);
            Comment comment = commentService.createComment(userId, dto);
            return Result.successWithMessage("评论创建成功", comment.getId());
        } catch (Exception e) {
            return Result.error("评论创建失败：" + e.getMessage());
        }
    }

    /**
     * 获取文档的所有评论接口
     * @param docId 文档ID
     * @return 评论列表
     */
    @GetMapping("/list/{docId}")
    public Result<?> getCommentsByDocId(@PathVariable Long docId) {
        try {
            List<Comment> comments = commentService.getCommentsByDocId(docId);
            return Result.success(comments);
        } catch (Exception e) {
            return Result.error("获取评论失败：" + e.getMessage());
        }
    }

    /**
     * 获取评论的所有回复接口
     * @param parentId 父评论ID
     * @return 回复列表
     */
    @GetMapping("/replies/{parentId}")
    public Result<?> getRepliesByParentId(@PathVariable Long parentId) {
        try {
            List<Comment> replies = commentService.getRepliesByParentId(parentId);
            return Result.success(replies);
        } catch (Exception e) {
            return Result.error("获取回复失败：" + e.getMessage());
        }
    }

    /**
     * 删除评论接口
     * @param commentId 评论ID
     * @param request 请求对象
     * @return 删除结果
     */
    @DeleteMapping("/delete/{commentId}")
    public Result<?> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        try {
            String authorization = request.getHeader("Authorization");
            Long userId = jwtUtil.getUserIdFromToken(authorization);
            boolean success = commentService.deleteComment(commentId, userId);
            if (success) {
                return Result.successWithMessage("评论删除成功");
            } else {
                return Result.error("评论删除失败，无权限或评论不存在");
            }
        } catch (Exception e) {
            return Result.error("评论删除失败：" + e.getMessage());
        }
    }
}
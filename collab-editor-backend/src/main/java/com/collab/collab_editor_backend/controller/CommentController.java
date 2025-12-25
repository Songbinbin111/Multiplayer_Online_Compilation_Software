package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.dto.CommentCreateDTO;
import com.collab.collab_editor_backend.entity.Comment;
import com.collab.collab_editor_backend.entity.User;
import com.collab.collab_editor_backend.service.CommentService;
import com.collab.collab_editor_backend.service.UserService;
import com.collab.collab_editor_backend.util.JwtUtil;
import com.collab.collab_editor_backend.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

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
            
            // 获取用户名信息
            List<Long> userIds = List.of(userId);
            List<User> users = userService.getUsersByIdList(userIds);
            String username = users.isEmpty() ? "未知用户" : users.get(0).getUsername();
            
            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("id", comment.getId());
            commentMap.put("docId", comment.getDocId());
            commentMap.put("userId", comment.getUserId());
            commentMap.put("username", username);
            commentMap.put("content", comment.getContent());
            commentMap.put("parentId", comment.getParentId());
            commentMap.put("startPos", comment.getStartPos());
            commentMap.put("endPos", comment.getEndPos());
            commentMap.put("selectedText", comment.getSelectedText());
            commentMap.put("createTime", comment.getCreateTime());
            commentMap.put("updateTime", comment.getUpdateTime());
            
            return Result.successWithMessage("评论创建成功", commentMap);
        } catch (Exception e) {
            return Result.error("评论创建失败：" + e.getMessage());
        }
    }

    /**
     * 获取文档的评论列表
     * @param docId 文档ID
     * @return 评论列表（包含用户名信息）
     */
    @GetMapping("/list/{docId}")
    public Result<?> getCommentsByDocId(@PathVariable Long docId) {
        try {
            List<Comment> comments = commentService.getCommentsByDocId(docId);
            
            // 将评论转换为包含用户名的Map列表
            List<Map<String, Object>> commentList = new ArrayList<>();
            if (!comments.isEmpty()) {
                // 获取所有评论的用户ID
                List<Long> userIds = comments.stream()
                        .map(Comment::getUserId)
                        .distinct()
                        .toList();
                
                // 获取用户信息
                List<User> users = userService.getUsersByIdList(userIds);
                Map<Long, String> userIdToUsername = new HashMap<>();
                for (User user : users) {
                    userIdToUsername.put(user.getId(), user.getUsername());
                }
                
                // 转换评论数据
                for (Comment comment : comments) {
                    Map<String, Object> commentMap = new HashMap<>();
                    commentMap.put("id", comment.getId());
                    commentMap.put("docId", comment.getDocId());
                    commentMap.put("userId", comment.getUserId());
                    commentMap.put("username", userIdToUsername.getOrDefault(comment.getUserId(), "未知用户"));
                    commentMap.put("content", comment.getContent());
                    commentMap.put("parentId", comment.getParentId());
                    commentMap.put("startPos", comment.getStartPos());
                    commentMap.put("endPos", comment.getEndPos());
                    commentMap.put("selectedText", comment.getSelectedText());
                    commentMap.put("createTime", comment.getCreateTime());
                    commentMap.put("updateTime", comment.getUpdateTime());
                    commentList.add(commentMap);
                }
            }
            
            return Result.success(commentList);
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
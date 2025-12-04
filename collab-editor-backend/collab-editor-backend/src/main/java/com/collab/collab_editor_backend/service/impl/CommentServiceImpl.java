package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.collab_editor_backend.dto.CommentCreateDTO;
import com.collab.collab_editor_backend.entity.Comment;
import com.collab.collab_editor_backend.entity.Notification;
import com.collab.collab_editor_backend.entity.User;
import com.collab.collab_editor_backend.mapper.CommentMapper;
import com.collab.collab_editor_backend.mapper.NotificationMapper;
import com.collab.collab_editor_backend.service.CommentService;
import com.collab.collab_editor_backend.service.UserService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationMapper notificationMapper;

    // 匹配@用户名的正则表达式
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    /**
     * 创建评论
     * @param userId 用户ID
     * @param dto 评论创建DTO
     * @return 创建的评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Comment createComment(Long userId, CommentCreateDTO dto) {
        // 1. 创建评论实体
        Comment comment = new Comment();
        comment.setDocId(dto.getDocId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId());
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());

        // 2. 保存评论
        commentMapper.insert(comment);

        // 3. 解析@提及的用户
        List<String> mentionedUsernames = parseMentionedUsers(dto.getContent());
        if (!mentionedUsernames.isEmpty()) {
            // 4. 获取被提及的用户信息
            List<User> mentionedUsers = userService.getUsersByUsernameList(mentionedUsernames);
            
            // 5. 创建@提及通知
            for (User user : mentionedUsers) {
                if (!user.getId().equals(userId)) { // 不通知自己
                    Notification notification = new Notification();
                    notification.setUserId(user.getId());
                    notification.setType("mention");
                    notification.setContent("你在文档中被@提及");
                    notification.setDocId(dto.getDocId());
                    notification.setRelatedId(comment.getId());
                    notification.setIsRead(false);
                    notification.setCreateTime(LocalDateTime.now());
                    notificationMapper.insert(notification);
                }
            }
        }

        return comment;
    }

    /**
     * 获取文档的所有评论
     * @param docId 文档ID
     * @return 评论列表
     */
    @Override
    public List<Comment> getCommentsByDocId(Long docId) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getDocId, docId)
                .isNull(Comment::getParentId) // 获取顶层评论
                .orderByAsc(Comment::getCreateTime);
        return commentMapper.selectList(queryWrapper);
    }

    /**
     * 获取评论的所有回复
     * @param parentId 父评论ID
     * @return 回复列表
     */
    @Override
    public List<Comment> getRepliesByParentId(Long parentId) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getParentId, parentId)
                .orderByAsc(Comment::getCreateTime);
        return commentMapper.selectList(queryWrapper);
    }

    /**
     * 删除评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 删除结果
     */
    @Override
    public boolean deleteComment(Long commentId, Long userId) {
        // 1. 查询评论
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return false;
        }

        // 2. 验证用户权限（只有评论作者可以删除）
        if (!comment.getUserId().equals(userId)) {
            return false;
        }

        // 3. 删除评论（同时会删除所有回复，需要在数据库中设置外键级联删除）
        commentMapper.deleteById(commentId);
        return true;
    }

    /**
     * 解析评论内容中的@提及用户
     * @param content 评论内容
     * @return 用户名列表
     */
    @Override
    public List<String> parseMentionedUsers(String content) {
        List<String> usernames = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return usernames;
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String username = matcher.group(1);
            if (!usernames.contains(username)) {
                usernames.add(username);
            }
        }

        return usernames;
    }
}
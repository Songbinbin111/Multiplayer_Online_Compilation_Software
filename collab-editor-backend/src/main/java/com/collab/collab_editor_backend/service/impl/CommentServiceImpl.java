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

    @Autowired
    private com.collab.collab_editor_backend.service.NotificationService notificationService;

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
        System.out.println("创建评论，userId: " + userId + ", docId: " + dto.getDocId());
        
        // 1. 创建评论实体
        Comment comment = new Comment();
        comment.setDocId(dto.getDocId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId());
        // 设置批注相关字段（如果有）
        comment.setStartPos(dto.getStartPos());
        comment.setEndPos(dto.getEndPos());
        comment.setSelectedText(dto.getSelectedText());
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());

        // 2. 保存评论
        int result = commentMapper.insert(comment);
        System.out.println("保存评论结果: " + result + ", 评论ID: " + comment.getId());

        // 3. 解析@提及的用户
        List<String> mentionedUsernames = parseMentionedUsers(dto.getContent());
        if (!mentionedUsernames.isEmpty()) {
            // 4. 获取被提及的用户信息
            List<User> mentionedUsers = userService.getUsersByUsernameList(mentionedUsernames);
            
            // 5. 创建@提及通知
            for (User user : mentionedUsers) {
                // if (!user.getId().equals(userId)) { // 不通知自己
                if (true) { // 临时修改：允许通知自己，方便测试
                    Notification notification = new Notification();
                    notification.setUserId(user.getId());
                    notification.setType("mention");
                    notification.setContent("你在文档中被@提及");
                    notification.setDocId(dto.getDocId());
                    notification.setRelatedId(comment.getId());
                    notification.setIsRead(false);
                    notification.setCreateTime(LocalDateTime.now());
                    notificationService.create(notification);
                }
            }
        }

        // 6. 如果是回复，通知原评论作者
        if (dto.getParentId() != null) {
            Comment parentComment = commentMapper.selectById(dto.getParentId());
            if (parentComment != null && !parentComment.getUserId().equals(userId)) {
                // 获取父评论作者信息
                Result<User> parentUserResult = userService.getUserInfo(parentComment.getUserId());
                if (parentUserResult.getCode() == 200 && parentUserResult.getData() != null) {
                    User parentAuthor = parentUserResult.getData();
                    // 避免重复通知（如果@提及已经通知了该用户）
                    if (!mentionedUsernames.contains(parentAuthor.getUsername())) {
                        Notification notification = new Notification();
                        notification.setUserId(parentAuthor.getId());
                        notification.setType("reply");
                        notification.setContent("有人回复了你的评论");
                        notification.setDocId(dto.getDocId());
                        notification.setRelatedId(comment.getId());
                        notification.setIsRead(false);
                        notification.setCreateTime(LocalDateTime.now());
                        notificationService.create(notification);
                    }
                }
            }
        }

        return comment;
    }

    /**
     * 获取文档的所有评论
     * @param docId 文档ID
     * @return 评论列表（包括所有评论和回复）
     */
    @Override
    public List<Comment> getCommentsByDocId(Long docId) {
        System.out.println("=================== 开始获取评论列表 ===================");
        System.out.println("根据文档ID获取评论，docId: " + docId);
        
        // 检查docId是否有效
        if (docId == null) {
            System.out.println("获取评论失败：docId为null");
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getDocId, docId);
        wrapper.orderByAsc(Comment::getCreateTime);
        
        System.out.println("执行SQL查询...");
        List<Comment> comments = commentMapper.selectList(wrapper);
        
        System.out.println("获取到的评论数量: " + comments.size());
        
        // 打印每条评论的详细信息
        for (Comment comment : comments) {
            System.out.println("评论ID: " + comment.getId() + ", 内容: " + comment.getContent() + ", parentId: " + comment.getParentId());
        }
        
        System.out.println("=================== 获取评论列表结束 ===================");
        return comments;
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
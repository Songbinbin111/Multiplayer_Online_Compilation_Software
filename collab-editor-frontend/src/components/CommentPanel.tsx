import React, { useState, useEffect, useMemo } from 'react';
import { commentApi } from '../api';
import { SecurityUtil } from '../utils/security';
import {
  Box,
  Typography,
  Button,
  Avatar,
  TextField,
  IconButton,
  Paper
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import ReplyIcon from '@mui/icons-material/Reply';
import AddCommentIcon from '@mui/icons-material/AddComment';

// 评论接口定义
export interface Comment {
  id: number;
  docId: number;
  userId: number;
  username: string;
  content: string;
  parentId: number | null;
  createTime: string;
  startPos?: number; // 批注开始位置
  endPos?: number; // 批注结束位置
  selectedText?: string; // 批注选中的文本内容
}

interface CommentPanelProps {
  docId: number;
  currentUserId: number;
  selection?: {
    index: number;
    length: number;
    text: string;
  } | null;
  onCommentsLoaded?: (comments: Comment[]) => void;
}

const CommentPanel: React.FC<CommentPanelProps> = ({ docId, currentUserId, selection, onCommentsLoaded }) => {
  const [comments, setComments] = useState<Comment[]>([]);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [replyingTo, setReplyingTo] = useState<number | null>(null);
  const [replyContent, setReplyContent] = useState('');

  // 当评论列表更新时，通知父组件
  useEffect(() => {
    if (onCommentsLoaded) {
      onCommentsLoaded(comments);
    }
  }, [comments]);

  // 保持选择仅用于显示，不自动弹出创建表单

  // 获取文档的所有评论
  const fetchComments = async () => {
    const numericDocId = Number(docId);
    if (isNaN(numericDocId)) {
      setComments([]);
      return;
    }

    try {
      const response = await commentApi.getByDocId(numericDocId);
      if (response.data && response.data.code === 200) {
        const data = response.data.data;
        if (Array.isArray(data)) {
          setComments(data);
        } else {
          setComments([]);
        }
      } else {
        setComments([]);
      }
    } catch (error) {
      console.error('获取评论列表失败：', error);
      setComments([]);
    }
  };

  // 创建评论
  const handleCreateComment = async () => {
    if (!newComment.trim()) return;

    try {
      // 净化评论内容
      const sanitizedContent = SecurityUtil.sanitizeText(newComment);
      const numericDocId = Number(docId);

      if (isNaN(numericDocId)) {
        alert('创建评论失败：文档ID无效');
        return;
      }

      const commentData: any = {
        docId: numericDocId,
        content: sanitizedContent
      };

      // 如果有选中文本，添加批注信息
      if (selection && selection.length > 0) {
        commentData.startPos = selection.index;
        commentData.endPos = selection.index + selection.length;
        commentData.selectedText = selection.text;
      }

      const response = await commentApi.create(commentData);

      if (response.data && response.data.code === 200) {
        setNewComment('');
        setShowCreateForm(false);
        await fetchComments(); // 强制刷新评论列表
      } else {
        alert('创建评论失败: ' + (response.data && response.data.message ? response.data.message : '未知错误'));
      }
    } catch (error) {
      console.error('创建评论失败:', error);
      alert('创建评论失败: 网络错误或服务器问题');
    }
  };

  // 创建回复
  const handleCreateReply = async (parentId: number) => {
    if (!replyContent.trim()) return;

    try {
      // 净化回复内容
      const sanitizedContent = SecurityUtil.sanitizeText(replyContent);
      const numericDocId = Number(docId);

      if (isNaN(numericDocId)) {
        alert('创建回复失败：文档ID无效');
        return;
      }

      const response = await commentApi.create({
        docId: numericDocId,
        content: sanitizedContent,
        parentId
      });

      if (response.data && response.data.code === 200) {
        setReplyContent('');
        setReplyingTo(null);
        fetchComments(); // 刷新评论列表
      } else {
        alert('创建回复失败: ' + (response.data?.message || '未知错误'));
      }
    } catch (error) {
      console.error('创建回复失败:', error);
      alert('创建回复失败: 网络错误或服务器问题');
    }
  };

  // 删除评论
  const handleDeleteComment = async (commentId: number) => {
    if (window.confirm('确定要删除这个评论吗？')) {
      try {
        await commentApi.delete(commentId);
        fetchComments(); // 刷新评论列表
      } catch (error) {
        console.error('删除评论失败:', error);
      }
    }
  };

  // 解析@提及用户
  const parseMentions = (content: string) => {
    const sanitizedContent = SecurityUtil.sanitizeText(content);
    const mentionRegex = /@(\w+)/g;
    const parts = [];
    let lastIndex = 0;
    let match;
    let keyCounter = 0;

    while ((match = mentionRegex.exec(sanitizedContent)) !== null) {
      if (match.index > lastIndex) {
        parts.push(
          <span key={`text-${keyCounter++}`}>{sanitizedContent.slice(lastIndex, match.index)}</span>
        );
      }

      parts.push(
        <span key={`mention-${keyCounter++}`} style={{ color: '#1976d2', fontWeight: 'bold' }}>
          {match[0]}
        </span>
      );

      lastIndex = match.index + match[0].length;
    }

    if (lastIndex < sanitizedContent.length) {
      parts.push(
        <span key={`text-${keyCounter++}`}>{sanitizedContent.slice(lastIndex)}</span>
      );
    }

    return parts;
  };

  // 获取根评论
  const getRootComments = useMemo(() => {
    return () => {
      if (!Array.isArray(comments)) return [];
      return comments.filter(comment => {
        const parentId = comment.parentId;
        return parentId === null || parentId === undefined || parentId === 0;
      });
    };
  }, [comments]);

  // 获取评论的回复
  const getReplies = useMemo(() => {
    return (parentId: number) => {
      if (!Array.isArray(comments)) return [];
      return comments.filter(comment => comment.parentId === parentId);
    };
  }, [comments]);

  // 组件挂载时获取评论列表
  useEffect(() => {
    const numericDocId = Number(docId);
    if (!isNaN(numericDocId)) {
      fetchComments();
    } else {
      setComments([]);
    }
  }, [docId]);

  const renderCommentItem = (comment: Comment, isReply = false) => {
    const replies = !isReply ? getReplies(comment.id) : [];

    return (
      <Box key={comment.id} sx={{ mb: 2, pl: isReply ? 4 : 0 }}>
        <Paper elevation={isReply ? 0 : 1} sx={{ p: 2, bgcolor: isReply ? 'grey.50' : 'background.paper' }}>
          <Box sx={{ display: 'flex', alignItems: 'flex-start', mb: 1 }}>
            <Avatar sx={{ width: 32, height: 32, mr: 1, bgcolor: 'primary.light', fontSize: '0.875rem' }}>
              {comment.username.charAt(0).toUpperCase()}
            </Avatar>
            <Box sx={{ flexGrow: 1 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="subtitle2" component="span">
                  {comment.username}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {new Date(comment.createTime).toLocaleString()}
                </Typography>
              </Box>

              {comment.selectedText && (
                <Box sx={{
                  bgcolor: 'action.hover',
                  p: 1,
                  my: 1,
                  borderLeft: 3,
                  borderColor: 'primary.main',
                  borderRadius: 1
                }}>
                  <Typography variant="caption" color="text.secondary" display="block">
                    选中文本:
                  </Typography>
                  <Typography variant="body2" sx={{ fontStyle: 'italic' }}>
                    "{comment.selectedText}"
                  </Typography>
                </Box>
              )}

              <Typography variant="body2" sx={{ mt: 0.5 }}>
                {parseMentions(comment.content)}
              </Typography>
            </Box>
          </Box>

          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
            {!isReply && (
              <Button
                size="small"
                startIcon={<ReplyIcon />}
                onClick={() => setReplyingTo(replyingTo === comment.id ? null : comment.id)}
              >
                回复
              </Button>
            )}
            {comment.userId === currentUserId && (
              <IconButton
                size="small"
                color="error"
                onClick={() => handleDeleteComment(comment.id)}
              >
                <DeleteIcon fontSize="small" />
              </IconButton>
            )}
          </Box>

          {/* 回复表单 */}
          {replyingTo === comment.id && (
            <Box sx={{ mt: 2, pl: 2, borderLeft: 2, borderColor: 'divider' }}>
              <TextField
                fullWidth
                size="small"
                multiline
                rows={2}
                placeholder="回复评论..."
                value={replyContent}
                onChange={(e) => setReplyContent(e.target.value)}
                sx={{
                  mb: 1,
                  '& .MuiInputBase-input': { color: '#000', fontWeight: 700 },
                  '& .MuiInputBase-input::placeholder': { color: '#444', opacity: 1, fontWeight: 600 }
                }}
              />
              <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                <Button size="small" onClick={() => { setReplyingTo(null); setReplyContent(''); }}>
                  取消
                </Button>
                <Button size="small" variant="contained" onClick={() => handleCreateReply(comment.id)}>
                  发送
                </Button>
              </Box>
            </Box>
          )}
        </Paper>

        {/* 渲染子回复 */}
        {replies.length > 0 && (
          <Box sx={{ mt: 1 }}>
            {replies.map(reply => renderCommentItem(reply, true))}
          </Box>
        )}
      </Box>
    );
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h6">评论</Typography>
        <Button
          startIcon={<AddCommentIcon />}
          variant="contained"
          size="small"
          onClick={() => setShowCreateForm(true)}
        >
          添加评论
        </Button>
      </Box>

      {/* 创建评论对话框/区域 */}
      {showCreateForm && (
        <Paper sx={{ p: 2, m: 2, bgcolor: 'grey.50' }}>
          {selection && selection.length > 0 && (
            <Box sx={{ mb: 1, p: 1, bgcolor: 'background.paper', border: 1, borderColor: 'divider', borderRadius: 1 }}>
              <Typography variant="caption" color="text.secondary">针对选中文本:</Typography>
              <Typography variant="body2" noWrap>"{selection.text}"</Typography>
            </Box>
          )}
          <TextField
            fullWidth
            multiline
            rows={3}
            placeholder="输入评论内容..."
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            sx={{
              mb: 1,
              '& .MuiInputBase-input': { color: '#000', fontWeight: 700 },
              '& .MuiInputBase-input::placeholder': { color: '#444', opacity: 1, fontWeight: 600 }
            }}
          />
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
            <Button onClick={() => setShowCreateForm(false)}>取消</Button>
            <Button variant="contained" onClick={handleCreateComment}>发表</Button>
          </Box>
        </Paper>
      )}

      <Box sx={{ flexGrow: 1, overflow: 'auto', p: 2 }}>
        {comments.length === 0 ? (
          <Box sx={{ textAlign: 'center', color: 'text.secondary', mt: 4 }}>
            <Typography variant="body2">暂无评论</Typography>
          </Box>
        ) : (
          getRootComments().map(comment => renderCommentItem(comment))
        )}
      </Box>
    </Box>
  );
};

export default CommentPanel;

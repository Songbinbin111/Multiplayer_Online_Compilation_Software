import React, { useState, useEffect } from 'react';
import { commentApi } from '../api';

// 评论接口定义
interface Comment {
  id: number;
  docId: number;
  userId: number;
  username: string;
  content: string;
  parentId: number | null;
  createTime: string;
}

interface CommentPanelProps {
  docId: number;
  currentUserId: number;
}

const CommentPanel: React.FC<CommentPanelProps> = ({ docId, currentUserId }) => {
  const [comments, setComments] = useState<Comment[]>([]);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [replyingTo, setReplyingTo] = useState<number | null>(null);
  const [replyContent, setReplyContent] = useState('');

  // 获取文档的所有评论
  const fetchComments = async () => {
    try {
      const response = await commentApi.getByDocId(docId);
      const data = response.data;
      setComments(data);
    } catch (error) {
      console.error('获取评论列表失败:', error);
    }
  };

  // 创建评论
  const handleCreateComment = async () => {
    if (!newComment.trim()) return;

    try {
      await commentApi.create({
        docId,
        content: newComment
      });

      setNewComment('');
      setShowCreateForm(false);
      fetchComments(); // 刷新评论列表
    } catch (error) {
      console.error('创建评论失败:', error);
    }
  };

  // 创建回复
  const handleCreateReply = async (parentId: number) => {
    if (!replyContent.trim()) return;

    try {
      await commentApi.create({
        docId,
        content: replyContent,
        parentId
      });

      setReplyContent('');
      setReplyingTo(null);
      fetchComments(); // 刷新评论列表
    } catch (error) {
      console.error('创建回复失败:', error);
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
    const mentionRegex = /@(\w+)/g;
    const parts = [];
    let lastIndex = 0;
    let match;

    while ((match = mentionRegex.exec(content)) !== null) {
      if (match.index > lastIndex) {
        parts.push(
          <span key={lastIndex}>{content.slice(lastIndex, match.index)}</span>
        );
      }

      parts.push(
        <span key={match.index} className="mention">
          {match[0]}
        </span>
      );

      lastIndex = match.index + match[0].length;
    }

    if (lastIndex < content.length) {
      parts.push(
        <span key={lastIndex}>{content.slice(lastIndex)}</span>
      );
    }

    return parts;
  };



  // 获取根评论
  const getRootComments = () => {
    return comments.filter(comment => comment.parentId === null);
  };

  // 获取评论的回复
  const getReplies = (parentId: number) => {
    return comments.filter(comment => comment.parentId === parentId);
  };

  // 组件挂载时获取评论列表
  useEffect(() => {
    fetchComments();
  }, [docId]);

  return (
    <div className="comment-panel">
      <div className="comment-panel-header">
        <h3>评论</h3>
        <button
          className="create-comment-btn"
          onClick={() => setShowCreateForm(!showCreateForm)}
        >
          {showCreateForm ? '取消' : '添加评论'}
        </button>
      </div>

      {/* 创建评论表单 */}
      {showCreateForm && (
        <div className="create-comment-form">
          <textarea
            placeholder="添加评论..."
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            className="comment-content-textarea"
          />
          <div className="comment-form-actions">
            <button onClick={handleCreateComment} className="save-comment-btn">
              发表评论
            </button>
          </div>
        </div>
      )}

      {/* 评论列表 */}
      <div className="comment-list">
        {comments.length === 0 ? (
          <p className="no-comments">暂无评论</p>
        ) : (
          getRootComments().map(comment => (
            <div key={comment.id} className="comment-item">
              <div className="comment-header">
                <div className="user-info">
                  <div className="user-avatar">
                    {comment.username.charAt(0).toUpperCase()}
                  </div>
                  <span className="user-name">{comment.username}</span>
                  <span className="comment-time">{comment.createTime}</span>
                </div>
                {comment.userId === currentUserId && (
                  <button
                    onClick={() => handleDeleteComment(comment.id)}
                    className="delete-comment-btn"
                  >
                    删除
                  </button>
                )}
              </div>
              <div className="comment-content">
                {parseMentions(comment.content)}
              </div>
              <div className="comment-actions">
                <button
                  onClick={() => setReplyingTo(replyingTo === comment.id ? null : comment.id)}
                  className="reply-btn"
                >
                  回复
                </button>
              </div>

              {/* 回复表单 */}
              {replyingTo === comment.id && (
                <div className="create-reply-form">
                  <textarea
                    placeholder="回复评论..."
                    value={replyContent}
                    onChange={(e) => setReplyContent(e.target.value)}
                    className="reply-content-textarea"
                  />
                  <div className="reply-form-actions">
                    <button
                      onClick={() => handleCreateReply(comment.id)}
                      className="save-reply-btn"
                    >
                      发表回复
                    </button>
                    <button
                      onClick={() => { setReplyingTo(null); setReplyContent(''); }}
                      className="cancel-reply-btn"
                    >
                      取消
                    </button>
                  </div>
                </div>
              )}

              {/* 回复列表 */}
              {getReplies(comment.id).length > 0 && (
                <div className="replies-list">
                  {getReplies(comment.id).map(reply => (
                    <div key={reply.id} className="reply-item">
                      <div className="comment-header">
                        <div className="user-info">
                          <div className="user-avatar">
                            {reply.username.charAt(0).toUpperCase()}
                          </div>
                          <span className="user-name">{reply.username}</span>
                          <span className="comment-time">{reply.createTime}</span>
                        </div>
                        {reply.userId === currentUserId && (
                          <button
                            onClick={() => handleDeleteComment(reply.id)}
                            className="delete-comment-btn"
                          >
                            删除
                          </button>
                        )}
                      </div>
                      <div className="comment-content">
                        {parseMentions(reply.content)}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default CommentPanel;

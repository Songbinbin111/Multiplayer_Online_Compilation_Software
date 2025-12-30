import React, { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { useParams, useNavigate } from 'react-router-dom';
import TaskPanel from './TaskPanel';
import CommentPanel from './CommentPanel';
import type { Comment } from './CommentPanel';
import PermissionPanel from './PermissionPanel';
import VersionControl from './VersionControl';
import NotificationPanel from './NotificationPanel';
import VideoMeetingModal from './VideoMeetingModal';
import ChatPanel from './ChatPanel';
import { ReactQuillWrapper } from './ReactQuillWrapper';

// 引入自定义 Hook，处理认证、文档逻辑和协同编辑
import { useAuth } from '../hooks/useAuth';
import { useDocument } from '../hooks/useDocument';
import { useCollaboration } from '../hooks/useCollaboration';

// UI 组件库导入
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  Paper,
  Avatar,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Tooltip,
  CircularProgress,
  Chip
} from '@mui/material';
import {
  Save as SaveIcon,
  History as HistoryIcon,
  Chat as ChatIcon,
  VideoCall as VideoCallIcon,
  Person as PersonIcon,
  Logout as LogoutIcon,
  Add as AddIcon,
  Group as GroupIcon
} from '@mui/icons-material';

/**
 * 编辑器主组件
 * 负责整合文档编辑、协同功能、侧边栏和各类模态框
 */
const Editor: React.FC = () => {
  // 获取路由参数 docId
  const { docId } = useParams<{ docId: string }>();
  const navigate = useNavigate();
  // Quill 编辑器引用
  const quillRef = useRef<any>(null);
  // Quill 编辑器 DOM 容器，用于渲染协同光标
  const [quillContainer, setQuillContainer] = useState<HTMLElement | null>(null);

  // UI 状态控制
  const [showVideoMeetingModal, setShowVideoMeetingModal] = useState<boolean>(false);
  const [showChatPanel, setShowChatPanel] = useState<boolean>(false);
  const [currentSelection, setCurrentSelection] = useState<{ index: number; length: number; text: string } | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);

  // 1. 认证 Hook：检查登录状态和处理登出
  const { checkLogin, handleLogout } = useAuth();

  // 2. 文档 Hook：处理文档内容的加载、保存、导出和版本控制
  const {
    content,
    setContent,
    loading,
    lastSaved,
    saveDocumentContent,
    createVersion,
    exportToPdf,
    exportToMarkdown,
    showVersionControl,
    setShowVersionControl,
    showVersionContent,
    setShowVersionContent,
    versionContent,
    setVersionContent,
    versionInfo,
    setVersionInfo,
    saveTimeoutRef
  } = useDocument(docId, checkLogin);

  // 3. 协同 Hook：处理 WebSocket 连接、在线用户和光标同步
  const {
    onlineUsers,
    cursorPositions,
    handleCursorChange,
    broadcastContent,
    getUserColor
  } = useCollaboration(docId, content, setContent, quillRef);

  useEffect(() => {
    const el = document.querySelector('.quill-container .ql-editor') as HTMLElement | null;
    if (el) {
      setQuillContainer(el);
    }
  }, []);

  /**
   * 处理自动保存逻辑
   * @param newContent 编辑器的新内容
   * @param delta 变更内容
   * @param source 变更来源
   * @param editor 编辑器实例
   */
  const handleAutoSave = (newContent: string, _delta: any, source: string, _editor: any) => {
    // 只有用户手动修改的内容才需要处理保存和广播
    // 忽略来自 API 的更新（即 WebSocket 同步过来的内容）
    if (source !== 'user') {
      return;
    }

    setContent(newContent);

    // 防抖机制：延迟 1 秒后保存到数据库
    if (saveTimeoutRef.current) {
      clearTimeout(saveTimeoutRef.current);
    }

    saveTimeoutRef.current = window.setTimeout(() => {
      saveDocumentContent(newContent);
    }, 1000);

    // 实时广播内容更新给其他协作者
    broadcastContent(newContent);
  };

  /**
   * 处理选区变化
   * 用于捕获用户选中的文本，供评论功能使用
   * 同时触发光标位置的广播
   */
  const handleSelectionChange = (range: any) => {
    if (range) {
      // 广播当前光标位置
      const selection = handleCursorChange();

      // 更新本地选区状态
      if (quillRef.current && selection) {
        const quill = quillRef.current.getEditor ? quillRef.current.getEditor() : quillRef.current;
        if (selection.length > 0) {
          const text = quill.getText(selection.index, selection.length);
          setCurrentSelection({
            index: selection.index,
            length: selection.length,
            text: text
          });
        } else {
          setCurrentSelection(null);
        }
      }
    }
  };

  /**
   * 当评论列表更新时，在编辑器中高亮显示批注
   */
  useEffect(() => {
    if (quillRef.current && comments.length > 0) {
      try {
        const quill = quillRef.current.getEditor ? quillRef.current.getEditor() : quillRef.current;
        comments.forEach(comment => {
          if (comment.startPos !== undefined && comment.endPos !== undefined && comment.endPos > comment.startPos) {
            // 使用淡黄色背景高亮
            quill.formatText(comment.startPos, comment.endPos - comment.startPos, 'background', '#fff9c4');
          }
        });
      } catch (e) {
        console.error('Error applying comment highlights:', e);
      }
    }
  }, [comments, loading]);

  /**
   * 获取 Quill 编辑器的 DOM 容器
   * 只有在容器准备好后，才能通过 Portal 渲染其他用户的光标
   */
  useEffect(() => {
    if (!loading && quillRef.current) {
      const timer = setTimeout(() => {
        try {
          const quill = quillRef.current.getEditor ? quillRef.current.getEditor() : quillRef.current;
          if (quill) {
            setQuillContainer(quill.container);
          }
        } catch (e) {
          console.error('Error getting quill container:', e);
        }
      }, 100);
      return () => clearTimeout(timer);
    }
  }, [loading]);

  // 加载状态显示
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>
      {/* 顶部导航栏 */}
      <AppBar position="static" color="transparent" elevation={0}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1, color: 'text.primary', fontWeight: 'bold' }}>
            文档编辑器
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mr: 3 }}>
            上次保存: {lastSaved || '未保存'}
          </Typography>

          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            {/* 通知面板 */}
            <NotificationPanel currentUserId={parseInt(localStorage.getItem('userId') || '0')} />

            <Tooltip title="手动保存">
              <IconButton onClick={() => saveDocumentContent(content)} color="primary">
                <SaveIcon />
              </IconButton>
            </Tooltip>

            <Tooltip title="导出 PDF">
              <Button size="small" onClick={exportToPdf}>PDF</Button>
            </Tooltip>

            <Tooltip title="导出 Markdown">
              <Button size="small" onClick={exportToMarkdown}>MD</Button>
            </Tooltip>

            <Tooltip title="视频会议">
              <IconButton onClick={() => setShowVideoMeetingModal(true)} color="primary">
                <VideoCallIcon />
              </IconButton>
            </Tooltip>

            <Tooltip title="聊天">
              <IconButton onClick={() => setShowChatPanel(!showChatPanel)} color={showChatPanel ? "primary" : "default"}>
                <ChatIcon />
              </IconButton>
            </Tooltip>

            <Tooltip title="版本历史">
              <IconButton onClick={() => setShowVersionControl(true)}>
                <HistoryIcon />
              </IconButton>
            </Tooltip>

            <Tooltip title="创建新版本">
              <IconButton onClick={createVersion}>
                <AddIcon />
              </IconButton>
            </Tooltip>

            <Tooltip title="个人资料">
              <IconButton onClick={() => navigate('/profile')}>
                <PersonIcon />
              </IconButton>
            </Tooltip>

            <Tooltip title="退出">
              <IconButton onClick={handleLogout} color="error">
                <LogoutIcon />
              </IconButton>
            </Tooltip>
          </Box>
        </Toolbar>
      </AppBar>

      {/* 主内容区域 */}
      <Box sx={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* 左侧编辑器区域 */}
        <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', p: 2 }}>
          <Paper sx={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', position: 'relative' }} elevation={0} variant="outlined">
            <ReactQuillWrapper
              ref={quillRef}
              value={content}
              onChange={handleAutoSave}
              theme="snow"
              placeholder="开始编辑文档..."
              modules={{ toolbar: true }}
              formats={["bold", "italic", "underline", "strike", "list", "bullet", "link"]}
              onChangeSelection={handleSelectionChange}
            />
            <style>{`
              @keyframes collabBlink { 0% { opacity: 1; } 50% { opacity: 0; } 100% { opacity: 1; } }
            `}</style>
            {/* 渲染其他用户的光标：使用 Portal 渲染到 Quill 容器内 */}
            {cursorPositions.map((cursor) => {
              let style: React.CSSProperties = { display: 'none' };
              const color = getUserColor(cursor.userId);

              if (quillRef.current) {
                const quill = quillRef.current.getEditor ? quillRef.current.getEditor() : quillRef.current;

                if (quill && typeof quill.getBounds === 'function') {
                  try {
                    const contentLength = quill.getLength();
                    const pos = Math.min(cursor.position, contentLength - 1);
                    const bounds = quill.getBounds(pos, cursor.length || 0);
                    if (bounds) {
                      style = {
                        left: `${bounds.left}px`,
                        top: `${bounds.top}px`,
                        height: `${bounds.height}px`,
                        width: typeof bounds.width === 'number' ? `${bounds.width}px` : undefined,
                        display: 'block',
                        position: 'absolute',
                        pointerEvents: 'none',
                        zIndex: 100
                      };
                    }
                  } catch (e) {
                  }
                }
              }

              return quillContainer ? createPortal(
                <div
                  key={cursor.userId}
                  style={style}
                >
                  {cursor.length && cursor.length > 0 ? (
                    <div
                      style={{
                        position: 'absolute',
                        left: 0,
                        top: 0,
                        width: (style as any).width,
                        height: (style as any).height,
                        backgroundColor: `${color}22`,
                        borderLeft: `2px solid ${color}`,
                        borderRight: `2px solid ${color}`,
                        borderRadius: 2
                      }}
                    />
                  ) : null}
                  <div style={{
                    position: 'absolute',
                    width: '2px',
                    backgroundColor: color,
                    height: '100%',
                    animation: 'collabBlink 1s step-start infinite'
                  }}>
                    <div style={{
                      position: 'absolute',
                      top: '-20px',
                      left: 0,
                      backgroundColor: color,
                      color: 'white',
                      padding: '2px 4px',
                      borderRadius: '4px',
                      fontSize: '10px',
                      whiteSpace: 'nowrap'
                    }}>
                      <span style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 4
                      }}>
                        <span style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          width: 14,
                          height: 14,
                          borderRadius: '50%',
                          backgroundColor: '#ffffff55',
                          color: 'white',
                          fontSize: 10,
                          fontWeight: 700
                        }}>{cursor.username?.charAt(0).toUpperCase()}</span>
                        {cursor.username}
                      </span>
                    </div>
                  </div>
                </div>,
                quillContainer
              ) : null;
            })}
          </Paper>
        </Box>

        {/* 右侧面板区域 */}
        <Paper sx={{ width: 400, display: 'flex', flexDirection: 'column', borderLeft: 1, borderColor: 'divider', overflowY: 'auto' }} elevation={0}>
          {/* 在线用户列表 */}
          <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
              <GroupIcon sx={{ mr: 1, color: 'text.secondary' }} />
              <Typography variant="subtitle1" fontWeight="bold">在线用户</Typography>
            </Box>

            {onlineUsers.length === 0 ? (
              <Typography variant="body2" color="text.secondary">暂无其他用户在线</Typography>
            ) : (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {onlineUsers.map((user) => (
                  <Chip
                    key={user.userId}
                    avatar={<Avatar sx={{ bgcolor: 'primary.main', color: 'white' }}>{user.username.charAt(0).toUpperCase()}</Avatar>}
                    label={user.username}
                    variant="outlined"
                    size="small"
                  />
                ))}
              </Box>
            )}
          </Box>

          {/* 任务面板 */}
          <TaskPanel
            docId={parseInt(docId!)}
            onlineUsers={onlineUsers.map(user => ({ id: user.userId, username: user.username }))}
            currentUserId={parseInt(localStorage.getItem('userId') || '0')}
          />
          {/* 评论面板 */}
          <CommentPanel
            docId={parseInt(docId!)}
            currentUserId={parseInt(localStorage.getItem('userId') || '0')}
            selection={currentSelection}
            onCommentsLoaded={setComments}
          />
          {/* 权限面板 */}
          <PermissionPanel
            docId={parseInt(docId!)}
            currentUserId={parseInt(localStorage.getItem('userId') || '0')}
            onlineUsers={onlineUsers}
          />
        </Paper>
      </Box>

      {/* 版本控制面板 */}
      {showVersionControl && (
        <VersionControl
          docId={parseInt(docId!)}
          onClose={() => setShowVersionControl(false)}
          onVersionSelect={(version) => {
            setVersionContent(version.content);
            setVersionInfo({ id: version.id, versionName: version.versionName });
            setShowVersionContent(true);
          }}
        />
      )}

      {/* 版本内容预览对话框 */}
      <Dialog
        open={showVersionContent}
        onClose={() => setShowVersionContent(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          {versionInfo?.versionName || `版本 ${versionInfo?.id}`} 内容预览
        </DialogTitle>
        <DialogContent dividers>
          <Box
            sx={{
              p: 2,
              bgcolor: 'background.paper',
              minHeight: '200px',
              '& img': { maxWidth: '100%' }
            }}
            dangerouslySetInnerHTML={{ __html: versionContent }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowVersionContent(false)}>关闭</Button>
          <Button
            variant="contained"
            onClick={() => {
              if (window.confirm('确定要恢复到此版本吗？当前未保存的内容将丢失。')) {
                setContent(versionContent);
                setShowVersionContent(false);
                setShowVersionControl(false);
                saveDocumentContent(versionContent);
              }
            }}
          >
            恢复此版本
          </Button>
        </DialogActions>
      </Dialog>

      {/* 视频会议模态框 */}
      {showVideoMeetingModal && (
        <VideoMeetingModal
          isVisible={true}
          docId={parseInt(docId!)}
          currentUserId={parseInt(localStorage.getItem('userId') || '0')}
          onlineUsers={onlineUsers}
          onClose={() => setShowVideoMeetingModal(false)}
        />
      )}

      {/* 聊天面板 - 侧边抽屉 */}
      {showChatPanel && (
        <Paper sx={{
          position: 'fixed',
          top: 0,
          right: 0,
          bottom: 0,
          zIndex: 1300,
          width: 400,
          boxShadow: '0 20px 40px rgba(0,0,0,0.4)',
          display: 'flex',
          flexDirection: 'column',
          borderRadius: 0,
          borderLeft: '1px solid rgba(255,255,255,0.08)',
          bgcolor: 'rgba(19, 20, 22, 0.95)',
          backdropFilter: 'blur(16px)'
        }}>
          <ChatPanel
            onClose={() => setShowChatPanel(false)}
          />
        </Paper>
      )}
    </Box>
  );
};

export default Editor;

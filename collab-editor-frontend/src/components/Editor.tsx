import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';
import MarkdownIt from 'markdown-it';
import MdEditor from 'react-markdown-editor-lite';
import 'react-markdown-editor-lite/lib/index.css';
import { userApi } from '../api';
import { documentApi, versionApi } from '../api/documentApi';
import TaskPanel from './TaskPanel';
import CommentPanel from './CommentPanel';
import PermissionPanel from './PermissionPanel';
import VersionControl from './VersionControl';
import NotificationPanel from './NotificationPanel';

import './Editor.css';

interface OnlineUser {
  userId: number;
  username: string;
}

const Editor: React.FC = () => {
  const { docId } = useParams<{ docId: string }>();
  const navigate = useNavigate();
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [onlineUsers, setOnlineUsers] = useState<OnlineUser[]>([]);
  const [lastSaved, setLastSaved] = useState<string>('');
  const [showVersionControl, setShowVersionControl] = useState<boolean>(false);
  const [showVersionContent, setShowVersionContent] = useState<boolean>(false);
  const [versionContent, setVersionContent] = useState<string>('');
  const [versionInfo, setVersionInfo] = useState<{ id: number; versionName?: string } | null>(null);
  const [editorType, setEditorType] = useState<'rich' | 'markdown'>('rich');
  const quillRef = useRef<ReactQuill>(null);
  const wsRef = useRef<WebSocket | null>(null);
  const saveTimeoutRef = useRef<number | null>(null);
  const mdParser = new MarkdownIt();

  // HTML转Markdown的简单实现
  const htmlToMarkdown = (html: string): string => {
    // 简单的HTML转Markdown转换
    let markdown = html;
    // 转换标题
    markdown = markdown.replace(/<h1>(.*?)<\/h1>/g, '# $1\n');
    markdown = markdown.replace(/<h2>(.*?)<\/h2>/g, '## $1\n');
    markdown = markdown.replace(/<h3>(.*?)<\/h3>/g, '### $1\n');
    // 转换段落
    markdown = markdown.replace(/<p>(.*?)<\/p>/g, '$1\n\n');
    // 转换粗体和斜体
    markdown = markdown.replace(/<strong>(.*?)<\/strong>/g, '**$1**');
    markdown = markdown.replace(/<em>(.*?)<\/em>/g, '*$1*');
    // 转换列表
    markdown = markdown.replace(/<li>(.*?)<\/li>/g, '- $1\n');
    // 移除其他HTML标签
    markdown = markdown.replace(/<[^>]*>/g, '');
    return markdown.trim();
  };

  // 检查是否登录
  const checkLogin = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return false;
    }
    return true;
  };

  // 获取文档内容
  const fetchDocumentContent = async () => {
    if (!checkLogin() || !docId) return;

    try {
      setLoading(true);
      const response = await documentApi.getContent(parseInt(docId));
      const data = response.data;
      // 如果当前使用Markdown编辑器，转换HTML为Markdown
      setContent(editorType === 'markdown' ? htmlToMarkdown(data) : data);
    } catch (error) {
      console.error('获取文档内容失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 保存文档内容
  const saveDocumentContent = async (newContent: string) => {
    if (!docId) return;

    try {
      // 如果使用Markdown编辑器，保存前转换为HTML
      const contentToSave = editorType === 'markdown' ? mdParser.render(newContent) : newContent;
      await documentApi.saveContent(parseInt(docId), contentToSave);
      setLastSaved(new Date().toLocaleString());
    } catch (error) {
      console.error('保存文档内容失败:', error);
    }
  };

  // 手动保存
  const handleManualSave = async () => {
    await saveDocumentContent(content);
  };

  // 自动保存
  const handleAutoSave = (newContent: string) => {
    setContent(newContent);

    // 防抖保存，避免频繁请求
    if (saveTimeoutRef.current) {
      clearTimeout(saveTimeoutRef.current);
    }

    saveTimeoutRef.current = setTimeout(() => {
      saveDocumentContent(newContent);
    }, 1000);

    // 通过WebSocket发送给其他用户
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      // 始终发送HTML格式给其他用户
      const contentToSend = editorType === 'markdown' ? mdParser.render(newContent) : newContent;
      wsRef.current.send(JSON.stringify({
        type: 'content_update',
        docId: parseInt(docId!),
        content: contentToSend,
        userId: parseInt(localStorage.getItem('userId')!),
        username: localStorage.getItem('username')
      }));
    }
  };

  // 处理格式切换
  const handleEditorTypeChange = (type: 'rich' | 'markdown') => {
    if (editorType === type) return;

    if (type === 'markdown') {
      // 从富文本切换到Markdown
      setContent(htmlToMarkdown(content));
    } else {
      // 从Markdown切换到富文本
      setContent(mdParser.render(content));
    }

    setEditorType(type);
  };

  // 初始化WebSocket连接
  const initWebSocket = () => {
    if (!docId) return;

    try {
      const ws = new WebSocket(`ws://localhost:8080/ws/document/${docId}`);
      wsRef.current = ws;

      ws.onopen = () => {
        console.log('WebSocket连接已建立');
        // 发送用户加入信息
        ws.send(JSON.stringify({
          type: 'user_join',
          docId: parseInt(docId),
          userId: parseInt(localStorage.getItem('userId')!),
          username: localStorage.getItem('username')
        }));
      };

      ws.onmessage = (event) => {
        const message = JSON.parse(event.data);

        switch (message.type) {
          case 'content_update':
            // 只有当内容变化时才更新，避免循环更新
            if (message.content !== content) {
              // 如果当前使用Markdown编辑器，转换收到的HTML为Markdown
              const contentToSet = editorType === 'markdown' ? htmlToMarkdown(message.content) : message.content;
              setContent(contentToSet);
            }
            break;
          case 'user_join':
            setOnlineUsers((prev) => [...prev, { userId: message.userId, username: message.username }]);
            break;
          case 'user_leave':
            setOnlineUsers((prev) => prev.filter(user => user.userId !== message.userId));
            break;
          case 'online_users':
            setOnlineUsers(message.users);
            break;
          default:
            break;
        }
      };

      ws.onclose = () => {
        console.log('WebSocket连接已关闭');
      };

      ws.onerror = (error) => {
        console.error('WebSocket连接错误:', error);
      };
    } catch (error) {
      console.error('初始化WebSocket连接失败:', error);
    }
  };

  // 创建版本
  const createVersion = async () => {
    if (!docId) return;

    try {
      const versionName = `V${new Date().getTime()}`;
      const description = `自动版本 ${new Date().toLocaleString()}`;
      await versionApi.createVersion(parseInt(docId), content, versionName, description);
      alert('版本创建成功');
    } catch (error) {
      console.error('创建版本失败:', error);
      alert('创建版本失败，请稍后重试');
    }
  };

  // 退出登录
  const handleLogout = async () => {
    try {
      await userApi.logout();
      localStorage.clear();
      navigate('/login');
    } catch (error) {
      console.error('退出登录失败:', error);
    }
  };

  // 文件导入导出功能
  const handleImportWord = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    documentApi.importWord(file)
      .then((response: any) => {
        // 更新文档内容
        setContent(editorType === 'markdown' ? htmlToMarkdown(response.content) : response.content);
        alert('Word文件导入成功');
      })
      .catch((error: any) => {
        console.error('Word文件导入失败:', error);
        alert('Word文件导入失败，请稍后重试');
      });
  };

  const handleImportPdf = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    documentApi.importPdf(file)
      .then((response: any) => {
        // 更新文档内容
        setContent(editorType === 'markdown' ? htmlToMarkdown(response.content) : response.content);
        alert('PDF文件导入成功');
      })
      .catch((error: any) => {
        console.error('PDF文件导入失败:', error);
        alert('PDF文件导入失败，请稍后重试');
      });
  };

  const handleExportWord = () => {
    documentApi.exportWord(parseInt(docId!))
      .then((response: any) => {
        const url = window.URL.createObjectURL(new Blob([response]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `document_${docId}.docx`);
        document.body.appendChild(link);
        link.click();
        link.remove();
      })
      .catch((error: any) => {
        console.error('Word文件导出失败:', error);
        alert('Word文件导出失败，请稍后重试');
      });
  };

  const handleExportPdf = () => {
    documentApi.exportPdf(parseInt(docId!), content)
      .then((response: any) => {
        const url = window.URL.createObjectURL(new Blob([response]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `document_${docId}.pdf`);
        document.body.appendChild(link);
        link.click();
        link.remove();
      })
      .catch((error: any) => {
        console.error('PDF文件导出失败:', error);
        alert('PDF文件导出失败，请稍后重试');
      });
  };

  // 组件挂载时初始化
  useEffect(() => {
    if (!checkLogin()) return;

    // 获取文档内容
    fetchDocumentContent();

    // 初始化WebSocket连接
    initWebSocket();

    // 组件卸载时清理WebSocket连接
    return () => {
      if (wsRef.current) {
        // 发送用户离开信息
        wsRef.current.send(JSON.stringify({
          type: 'user_leave',
          docId: parseInt(docId!),
          userId: parseInt(localStorage.getItem('userId')!)
        }));
        wsRef.current.close();
      }

      // 清理保存定时器
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }
    };
  }, [docId, editorType]);

  if (loading) {
    return <div className="loading">加载文档中...</div>;
  }

  return (
    <div className="editor-container">
      <header className="editor-header">
        <div className="editor-title">
          <h1>文档编辑器</h1>
          <span className="save-status">
            上次保存: {lastSaved || '未保存'}
          </span>
        </div>
        <div className="editor-header-actions">
          <NotificationPanel currentUserId={parseInt(localStorage.getItem('userId') || '0')} />
          <div className="editor-type-switch">
            <button
              className={`editor-type-btn ${editorType === 'rich' ? 'active' : ''}`}
              onClick={() => handleEditorTypeChange('rich')}
            >
              富文本
            </button>
            <button
              className={`editor-type-btn ${editorType === 'markdown' ? 'active' : ''}`}
              onClick={() => handleEditorTypeChange('markdown')}
            >
              Markdown
            </button>
          </div>
          <button onClick={handleManualSave} className="save-button">
            手动保存
          </button>
          {/* 导入导出功能按钮 */}
          <div className="import-export-buttons">
            <input type="file" id="word-import" accept=".docx,.doc" style={{ display: 'none' }} onChange={handleImportWord} />
            <input type="file" id="pdf-import" accept=".pdf" style={{ display: 'none' }} onChange={handleImportPdf} />
            <button className="import-button" onClick={() => document.getElementById('word-import')?.click()}>导入Word</button>
            <button className="import-button" onClick={() => document.getElementById('pdf-import')?.click()}>导入PDF</button>
            <button className="export-button" onClick={handleExportWord}>导出Word</button>
            <button className="export-button" onClick={handleExportPdf}>导出PDF</button>
          </div>
          <button className="version-button" onClick={() => setShowVersionControl(true)}>
            版本历史
          </button>
          <button className="version-create-button" onClick={createVersion}>
            创建版本
          </button>
          <button onClick={handleLogout} className="logout-button">
            退出
          </button>
        </div>
      </header>

      <div className="editor-content">
        <div className="editor-wrapper">
          {editorType === 'rich' ? (
            <div className="quill-editor">
              <ReactQuill
                ref={quillRef}
                value={content}
                onChange={handleAutoSave}
                theme="snow"
                placeholder="开始编辑文档..."
              />
            </div>
          ) : (
            <div className="markdown-editor">
              <MdEditor
                value={content}
                onChange={(value) => handleAutoSave(value.text)}
                renderHTML={(text) => mdParser.render(text)}
                config={{
                  view: {
                    menu: true,
                    md: true,
                    html: false
                  },
                  htmlClass: 'markdown-body'
                }}
              />
            </div>
          )}
        </div>

        <div className="editor-sidebar">
          <div className="online-users-panel">
            <h3>在线用户</h3>
            {onlineUsers.length === 0 ? (
              <p>暂无其他用户在线</p>
            ) : (
              <ul className="user-list">
                {onlineUsers.map((user) => (
                  <li key={user.userId} className="user-item">
                    <div className="user-avatar">
                      {user.username.charAt(0).toUpperCase()}
                    </div>
                    <span className="user-name">{user.username}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <TaskPanel
            docId={parseInt(docId!)}
            onlineUsers={onlineUsers.map(user => ({ id: user.userId, username: user.username }))}
            currentUserId={parseInt(localStorage.getItem('userId') || '0')}
          />
          <CommentPanel
            docId={parseInt(docId!)}
            currentUserId={parseInt(localStorage.getItem('userId') || '0')}
          />
          <PermissionPanel
            docId={parseInt(docId!)}
            currentUserId={parseInt(localStorage.getItem('userId') || '0')}
            onlineUsers={onlineUsers}
          />
        </div>
      </div>

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

      {/* 版本内容预览 */}
      {showVersionContent && (
        <div className="version-content-modal">
          <div className="version-content-modal-content">
            <div className="version-content-modal-header">
              <h2>版本内容预览</h2>
              <button onClick={() => setShowVersionContent(false)}>关闭</button>
            </div>
            <div className="version-content-modal-body">
              <div className="version-content-info">
                <h3>{versionInfo?.versionName || '版本'}</h3>
              </div>
              <div className="version-content">
                <div dangerouslySetInnerHTML={{ __html: versionContent }} />
              </div>
              <div className="version-content-actions">
                <button
                  onClick={async () => {
                    if (versionInfo) {
                      await versionApi.rollbackToVersion(parseInt(docId!), versionInfo.id);
                      alert('版本回滚成功');
                      fetchDocumentContent();
                      setShowVersionContent(false);
                      setShowVersionControl(false);
                    }
                  }}
                >
                  应用到当前文档
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Editor;
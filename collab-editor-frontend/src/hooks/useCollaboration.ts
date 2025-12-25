import { useState, useRef, useEffect } from 'react';

export interface OnlineUser {
  userId: number;
  username: string;
}

export interface CursorPosition {
  userId: number;
  username: string;
  position: number;
}

/**
 * 协同编辑 Hook
 * 处理 WebSocket 连接、用户状态同步、光标同步和内容广播
 */
export const useCollaboration = (
  docId: string | undefined,
  content: string,
  setContent: (content: string) => void,
  quillRef: React.MutableRefObject<any>
) => {
  const [onlineUsers, setOnlineUsers] = useState<OnlineUser[]>([]);
  const [cursorPositions, setCursorPositions] = useState<CursorPosition[]>([]);
  const wsRef = useRef<WebSocket | null>(null);
  const retryCountRef = useRef(0);

  /**
   * 为用户生成唯一颜色
   */
  const getUserColor = (userId: number) => {
    const colors = ['#f44336', '#e91e63', '#9c27b0', '#673ab7', '#3f51b5', '#2196f3', '#03a9f4', '#00bcd4', '#009688', '#4caf50'];
    return colors[userId % colors.length];
  };

  /**
   * 初始化 WebSocket 连接
   * 建立与服务器的长连接，并处理各种消息类型
   */
  const initWebSocket = () => {
    if (!docId) return;

    try {
      // 构建 WebSocket 连接地址
      const apiUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
      const apiUrlStr = String(apiUrl);
      const wsProtocol = apiUrlStr.startsWith('https') ? 'wss' : 'ws';
      const baseUrl = apiUrlStr.replace(/^https?:\/\//, '');
      const token = localStorage.getItem('token');
      const wsUrl = `${wsProtocol}://${baseUrl}/ws/document/${docId}?token=${encodeURIComponent(token || '')}`;

      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      // 连接建立成功后的处理
      ws.onopen = () => {
        const userIdStr = localStorage.getItem('userId');
        const username = localStorage.getItem('username');

        if (!userIdStr || !username) {
          console.error('用户信息不存在，无法发送加入消息');
          return;
        }

        // 发送用户加入消息
        ws.send(JSON.stringify({
          type: 'user_join',
          docId: parseInt(docId),
          userId: parseInt(userIdStr),
          username: username
        }));
      };

      // 接收消息处理
      ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        const currentUserId = parseInt(localStorage.getItem('userId') || '0');

        switch (message.type) {
          case 'content_update':
            // 收到内容更新消息
            // 忽略自己的更新，只处理其他用户的修改
            if (message.userId !== currentUserId) {
              if (message.content !== content) {
                // 更新内容并保留光标位置
                if (quillRef.current) {
                  const quill = quillRef.current.getEditor ? quillRef.current.getEditor() : quillRef.current;
                  const range = quill.getSelection();
                  
                  setContent(message.content);
                  
                  // 恢复光标位置
                  if (range) {
                    setTimeout(() => {
                      quill.setSelection(range.index, range.length);
                    }, 0);
                  }
                } else {
                  setContent(message.content);
                }
              }
            }
            break;
            
          case 'user_join':
            // 用户加入消息处理
            if (message && message.userId && message.username) {
              setOnlineUsers((prev) => {
                // 避免重复添加
                if (prev.some(user => user.userId === message.userId)) return prev;
                return [...prev, { userId: message.userId, username: message.username }];
              });
            }
            break;
            
          case 'user_leave':
            // 用户离开消息处理
            if (message && message.userId) {
              setOnlineUsers((prev) => prev.filter(user => user.userId !== message.userId));
              setCursorPositions((prev) => prev.filter(cursor => cursor.userId !== message.userId));
            }
            break;
            
          case 'online_users':
            // 在线用户列表全量同步
            if (message && message.users && Array.isArray(message.users)) {
              setOnlineUsers(message.users
                .filter((user: any) => user && user.userId !== undefined)
                .filter((user: any, index: number, self: any) =>
                  // 去重
                  index === self.findIndex((t: any) => t.userId === user.userId)
                )
              );
            }
            break;
            
          case 'cursor_position_update':
          case 'cursor_position': // 兼容不同消息类型
            // 光标位置更新
            if (message.userId !== currentUserId) {
              setCursorPositions((prev) => {
                const updated = prev.filter(cursor => cursor.userId !== message.userId);
                return [...updated, {
                  userId: message.userId,
                  username: message.username,
                  position: message.cursorPosition
                }];
              });
            }
            break;
        }
      };

      // 连接关闭处理，支持自动重连
      ws.onclose = () => {
        if (retryCountRef.current < 5) {
          retryCountRef.current += 1;
          setTimeout(initWebSocket, 2000 * retryCountRef.current);
        }
      };

      ws.onerror = (error) => {
        console.error('WebSocket连接错误:', error);
      };
    } catch (error) {
      console.error('WebSocket初始化失败:', error);
    }
  };

  /**
   * 广播内容更新
   */
  const broadcastContent = (newContent: string) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN && docId) {
      const userIdStr = localStorage.getItem('userId');
      const username = localStorage.getItem('username');

      if (!userIdStr || !username) return;

      wsRef.current.send(JSON.stringify({
        type: 'content_update',
        docId: parseInt(docId),
        content: newContent,
        userId: parseInt(userIdStr),
        username: username
      }));
    }
  };

  /**
   * 处理光标位置变化
   */
  const handleCursorChange = () => {
    if (quillRef.current) {
      const quill = quillRef.current.getEditor ? quillRef.current.getEditor() : quillRef.current;
      const selection = quill.getSelection();

      if (selection) {
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN && docId) {
          const userIdStr = localStorage.getItem('userId');
          const username = localStorage.getItem('username');

          if (userIdStr && username) {
            wsRef.current.send(JSON.stringify({
              type: 'cursor_position',
              docId: parseInt(docId),
              cursorPosition: selection.index,
              userId: parseInt(userIdStr),
              username: username
            }));
          }
        }
        return selection;
      }
    }
    return null;
  };

  useEffect(() => {
    initWebSocket();
    return () => {
      if (wsRef.current) {
        if (wsRef.current.readyState === WebSocket.OPEN && docId) {
          wsRef.current.send(JSON.stringify({
            type: 'user_leave',
            docId: parseInt(docId),
            userId: parseInt(localStorage.getItem('userId') || '0')
          }));
        }
        wsRef.current.close();
      }
    };
  }, [docId]);

  return {
    onlineUsers,
    cursorPositions,
    handleCursorChange,
    broadcastContent,
    getUserColor
  };
};

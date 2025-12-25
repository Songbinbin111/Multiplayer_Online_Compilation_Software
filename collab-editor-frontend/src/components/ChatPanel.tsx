import React, { useState, useEffect, useRef } from 'react';
import { chatApi } from '../api';
import type { ChatListItem, ChatMessage } from '../api';
import {
  Box,
  Typography,
  IconButton,
  List,
  ListItemAvatar,
  ListItemText,
  Avatar,
  TextField,
  InputAdornment,
  ListItemButton,
  Paper
} from '@mui/material';
import {
  Close as CloseIcon,
  Search as SearchIcon,
  Send as SendIcon,
  AttachFile as AttachFileIcon,
  InsertDriveFile as FileIcon,
  ArrowBack as ArrowBackIcon
} from '@mui/icons-material';

interface ChatPanelProps {
  onClose: () => void;
}

// 旧的用户列表接口已移除

const ChatPanel: React.FC<ChatPanelProps> = ({ onClose }) => {
  const [chatList, setChatList] = useState<ChatListItem[]>([]);
  const [selectedChat, setSelectedChat] = useState<ChatListItem | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [messageContent, setMessageContent] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [onlineUsers, setOnlineUsers] = useState<Array<{ userId: number; username: string }>>([]);
  const [isNewChatView, setIsNewChatView] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Ref for selectedChat to access in WebSocket callback without reconnection
  const selectedChatRef = useRef<ChatListItem | null>(null);

  useEffect(() => {
    selectedChatRef.current = selectedChat;
  }, [selectedChat]);

  // 获取聊天列表
  const fetchChatList = async () => {
    try {
      const response = await chatApi.getChatList();
      if (response.status === 200) {
        const data = response.data && response.data.data ? response.data.data : response.data;
        setChatList(Array.isArray(data) ? data : []);
      }
    } catch (error) {
      console.error('获取聊天列表失败:', error);
      setChatList([]);
    }
  };

  // 用户列表功能已废弃，统一依赖在线用户列表

  // 获取聊天历史
  const fetchChatHistory = async (otherUserId: number) => {
    try {
      const response = await chatApi.getChatHistory(otherUserId);
      if (response.status === 200) {
        const data = response.data && response.data.data ? response.data.data : response.data;
        setMessages(Array.isArray(data) ? data : []);
        // 标记消息为已读
        await chatApi.markAsRead(otherUserId);
        // 更新聊天列表
        await fetchChatList();
      }
    } catch (error) {
      console.error('获取聊天历史失败:', error);
      setMessages([]);
    }
  };

  // 发送消息
  const handleSendMessage = async () => {
    if (!selectedChat || !messageContent.trim()) return;

    try {
      await chatApi.sendMessage(selectedChat.userId, messageContent);
      setMessageContent('');
      // 重新获取聊天历史
      await fetchChatHistory(selectedChat.userId);
      // 更新聊天列表
      await fetchChatList();
    } catch (error) {
      console.error('发送消息失败:', error);
    }
  };

  // 处理文件上传
  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0 || !selectedChat) return;

    const file = e.target.files[0];
    try {
      await chatApi.sendFile(selectedChat.userId, file);
      // 重新获取聊天历史
      await fetchChatHistory(selectedChat.userId);
      // 更新聊天列表
      await fetchChatList();
      // 重置文件输入
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    } catch (error) {
      console.error('发送文件失败:', error);
    }
  };

  // 触发文件选择
  const handleFileButtonClick = () => {
    fileInputRef.current?.click();
  };

  // 旧的聊天列表入口已移除，统一从在线用户进入

  const handleSelectOnlineUser = (user: { userId: number; username: string }) => {
    const existingChat = chatList.find(item => item.userId === user.userId);
    if (existingChat) {
      setSelectedChat(existingChat);
      fetchChatHistory(user.userId);
    } else {
      const newChat: ChatListItem = {
        userId: user.userId,
        username: user.username,
        lastMessage: '',
        lastMessageTime: new Date().toISOString(),
        unreadCount: 0
      };
      setSelectedChat(newChat);
      // 即使是新会话，也尝试获取历史消息，因为可能之前有过聊天记录
      fetchChatHistory(user.userId);
    }
    setIsNewChatView(false);
  };

  // 旧的新聊天入口已废弃，统一通过在线用户列表选择进入

  useEffect(() => {
    fetchChatList();

    // 初始化WebSocket连接
    let reconnectTimeout: ReturnType<typeof setTimeout>;

    const connectWebSocket = () => {
      const token = localStorage.getItem('token');
      if (!token) return;

      const apiUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
      const apiUrlStr = String(apiUrl);
      const wsProtocol = apiUrlStr.startsWith('https') ? 'wss' : 'ws';
      const baseUrl = apiUrlStr.replace(/^https?:\/\//, '');
      const wsUrl = `${wsProtocol}://${baseUrl}/ws/chat?token=${encodeURIComponent(token)}`;

      if (wsRef.current) {
        // Prevent onclose triggering reconnect when manually closing
        wsRef.current.onclose = null;
        wsRef.current.close();
      }

      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        console.log('聊天WebSocket连接已建立');
      };

      ws.onmessage = (event) => {
        try {
          const messageData = JSON.parse(event.data);
          if (messageData.type === 'online_users' && Array.isArray(messageData.users)) {
            const currentUserId = parseInt(localStorage.getItem('userId') || '0');
            const normalized = messageData.users
              .map((u: any) => ({
                userId: typeof u.userId === 'number' ? u.userId : parseInt(u.userId, 10),
                username: typeof u.username === 'string' && u.username.trim().length > 0
                  ? u.username
                  : `用户${typeof u.userId === 'number' ? u.userId : parseInt(u.userId, 10)}`
              }))
              .filter((u: any) => Number.isFinite(u.userId) && u.userId !== currentUserId)
              .filter((u: any, i: number, self: any[]) => i === self.findIndex(t => t.userId === u.userId));
            setOnlineUsers(normalized);
            return;
          }
          if (messageData.type === 'new_message') {
            // 更新聊天列表
            fetchChatList();

            // 如果当前正在与发送者聊天，更新消息列表
            const currentSelectedChat = selectedChatRef.current;
            const senderIdNum = typeof messageData.senderId === 'string' ? parseInt(messageData.senderId, 10) : messageData.senderId;
            if (currentSelectedChat && currentSelectedChat.userId === senderIdNum) {
              fetchChatHistory(currentSelectedChat.userId);
            }
          }
        } catch (error) {
          console.error('解析WebSocket消息失败:', error);
        }
      };

      ws.onclose = () => {
        console.log('聊天WebSocket连接已关闭');
        // 尝试重新连接
        reconnectTimeout = setTimeout(() => {
          console.log('尝试重新连接聊天WebSocket...');
          connectWebSocket();
        }, 3000);
      };

      ws.onerror = (error) => {
        console.error('聊天WebSocket连接错误:', error);
      };
    };

    connectWebSocket();

    // 定期更新聊天列表作为备份机制
    const interval = setInterval(() => {
      fetchChatList();
      // 如果有选中的聊天，更新消息
      const currentSelectedChat = selectedChatRef.current;
      if (currentSelectedChat) {
        fetchChatHistory(currentSelectedChat.userId);
      }
    }, 30000); // 降低刷新频率到30秒

    return () => {
      clearTimeout(reconnectTimeout);
      clearInterval(interval);
      // 关闭WebSocket连接
      if (wsRef.current) {
        wsRef.current.onclose = null; // Prevent reconnect on unmount
        wsRef.current.close();
      }
    };
  }, []);

  const filteredOnlineUsers = onlineUsers.filter(user =>
    user.username.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', bgcolor: 'background.paper' }}>
      {/* Header */}
      <Box sx={{
        p: 2,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderBottom: 1,
        borderColor: 'divider',
        bgcolor: 'primary.main',
        color: 'primary.contrastText'
      }}>
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          {selectedChat || isNewChatView ? (
            <IconButton
              color="inherit"
              edge="start"
              onClick={() => { setSelectedChat(null); setIsNewChatView(false); }}
              sx={{ mr: 1 }}
            >
              <ArrowBackIcon />
            </IconButton>
          ) : null}
          <Typography variant="h6" component="div">
            {selectedChat ? selectedChat.username : '在线用户'}
          </Typography>
        </Box>
        <IconButton color="inherit" onClick={onClose} edge="end">
          <CloseIcon />
        </IconButton>
      </Box>

      {/* Content */}
      <Box sx={{ flexGrow: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        {!selectedChat ? (
          <>
            <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
              <TextField
                fullWidth
                size="small"
                placeholder="搜索在线用户..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon color="action" />
                    </InputAdornment>
                  ),
                }}
              />
            </Box>
            <List sx={{ flexGrow: 1, overflow: 'auto' }}>
              {filteredOnlineUsers.length === 0 ? (
                <Box sx={{ p: 3, textAlign: 'center', color: 'text.secondary' }}>
                  <Typography variant="body2">暂无在线用户</Typography>
                </Box>
              ) : (
                filteredOnlineUsers.map((user) => (
                  <ListItemButton
                    key={user.userId}
                    onClick={() => handleSelectOnlineUser(user)}
                    divider
                  >
                    <ListItemAvatar>
                      <Avatar sx={{ bgcolor: 'primary.light' }}>{user.username.charAt(0).toUpperCase()}</Avatar>
                    </ListItemAvatar>
                    <ListItemText primary={<Typography variant="subtitle2" noWrap>{user.username}</Typography>} />
                  </ListItemButton>
                ))
              )}
            </List>
          </>
        ) : (
          // Chat Window
          <>
            <Box sx={{ flexGrow: 1, overflow: 'auto', p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
              {messages.map((message) => {
                const isReceived = message.senderId === selectedChat?.userId;

                return (
                  <Box
                    key={message.id}
                    sx={{
                      display: 'flex',
                      justifyContent: isReceived ? 'flex-start' : 'flex-end',
                      mb: 1
                    }}
                  >
                    <Paper
                      elevation={1}
                      sx={{
                        p: 1.5,
                        maxWidth: '80%',
                        bgcolor: isReceived ? 'grey.100' : 'primary.light',
                        color: isReceived ? 'text.primary' : 'primary.contrastText',
                        borderRadius: 2,
                        borderTopLeftRadius: isReceived ? 0 : 2,
                        borderTopRightRadius: isReceived ? 2 : 0
                      }}
                    >
                      {message.fileUrl ? (
                        <Box
                          component="a"
                          href={message.fileUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          sx={{ display: 'flex', alignItems: 'center', color: 'inherit', textDecoration: 'none' }}
                        >
                          <FileIcon sx={{ mr: 1 }} />
                          <Box>
                            <Typography variant="body2" sx={{ fontWeight: 'bold' }}>{message.fileName}</Typography>
                            <Typography variant="caption">{Math.round((message.fileSize || 0) / 1024)} KB</Typography>
                          </Box>
                        </Box>
                      ) : (
                        <Typography variant="body2">{message.content}</Typography>
                      )}
                      <Typography
                        variant="caption"
                        sx={{
                          display: 'block',
                          textAlign: 'right',
                          mt: 0.5,
                          opacity: 0.8,
                          fontSize: '0.7rem'
                        }}
                      >
                        {new Date(message.sendTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </Typography>
                    </Paper>
                  </Box>
                );
              })}
            </Box>

            <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider', display: 'flex', alignItems: 'center', gap: 1 }}>
              <input
                type="file"
                ref={fileInputRef}
                style={{ display: 'none' }}
                onChange={handleFileUpload}
              />
              <IconButton onClick={handleFileButtonClick} color="primary">
                <AttachFileIcon />
              </IconButton>
              <TextField
                fullWidth
                size="small"
                placeholder="输入消息..."
                value={messageContent}
                onChange={(e) => setMessageContent(e.target.value)}
                onKeyPress={(e) => {
                  if (e.key === 'Enter') handleSendMessage();
                }}
              />
              <IconButton onClick={handleSendMessage} color="primary" disabled={!messageContent.trim()}>
                <SendIcon />
              </IconButton>
            </Box>
          </>
        )}
      </Box>
    </Box>
  );
};

export default ChatPanel;

import { api } from './request';

// 聊天消息接口定义
export interface ChatMessage {
  id: number;
  senderId: number;
  receiverId: number;
  content: string;
  sendTime: string;
  isRead: number;
  fileName?: string;
  fileUrl?: string;
  fileType?: string;
  fileSize?: number;
  messageType?: number; // 0-文本消息，1-文件消息
}

// 发送文件消息接口
export interface SendFileMessageRequest {
  receiverId: number;
  file: File;
}

// 聊天列表项接口定义
export interface ChatListItem {
  userId: number;
  username: string;
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
}

// 聊天相关API
export const chatApi = {
  // 发送消息
  sendMessage: (receiverId: number, content: string) => {
    return api.post('/api/chat/send', { receiverId, content });
  },

  // 发送文件消息
  sendFile: (receiverId: number, file: File) => {
    const formData = new FormData();
    formData.append('receiverId', receiverId.toString());
    formData.append('file', file);
    return api.post('/api/chat/send-file', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  // 获取聊天历史记录
  getChatHistory: (otherUserId: number) => {
    return api.get(`/api/chat/history/${otherUserId}`);
  },

  // 标记消息为已读
  markAsRead: (senderId: number) => {
    return api.put(`/api/chat/read/${senderId}`);
  },

  // 获取未读消息数量
  getUnreadCount: () => {
    return api.get('/api/chat/unread/count');
  },

  // 获取聊天列表
  getChatList: () => {
    return api.get('/api/chat/list');
  }
};

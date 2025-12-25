// API统一入口
import { chatApi } from './chatApi';
import type { ChatListItem, ChatMessage } from './chatApi';
import { commentApi } from './commentApi';
import { documentApi, versionApi } from './documentApi';
import { monitorApi } from './monitorApi';
import { notificationApi } from './notificationApi';
import { notificationSettingApi } from './notificationSettingApi';
import type { NotificationSetting } from './notificationSettingApi';
import { permissionApi } from './permissionApi';
import { taskApi } from './taskApi';
import { userApi } from './userApi';
import { videoMeetingApi } from './VideoMeetingApi';

// 导出所有API
export {
  chatApi,
  commentApi,
  documentApi,
  versionApi,
  monitorApi,
  notificationApi,
  notificationSettingApi,
  permissionApi,
  taskApi,
  userApi,
  videoMeetingApi
};

// 导出所有类型
export type {
  ChatListItem,
  ChatMessage,
  NotificationSetting
};

// 导出VideoMeetingApi类型
export type { CreateMeetingRequest, JoinMeetingRequest, MeetingResponse, MeetingInfo } from './VideoMeetingApi';

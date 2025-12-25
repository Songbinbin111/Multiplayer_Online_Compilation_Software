import request from './request';

/**
 * 通知设置API
 */
export interface NotificationSetting {
  id: number;
  userId: number;
  mentionEnabled: boolean;
  taskAssignEnabled: boolean;
  taskStatusEnabled: boolean;
  emailEnabled: boolean;
  createTime: string;
  updateTime: string;
}

export const notificationSettingApi = {
  /**
   * 获取用户的通知设置
   * @param userId 用户ID
   */
  getSetting: (userId: number) => {
    return request.get('/api/notification/setting', {
      params: { userId }
    });
  },

  /**
   * 更新通知设置
   * @param setting 通知设置
   */
  updateSetting: (setting: NotificationSetting) => {
    return request.put('/api/notification/setting', setting);
  }
};

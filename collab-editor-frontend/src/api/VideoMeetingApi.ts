import { api } from './request';

// 创建视频会议请求体接口
export interface CreateMeetingRequest {
  docId: number;
  title: string;
}

// 加入视频会议请求体接口
export interface JoinMeetingRequest {
  meetingId: string;
}

// 视频会议响应接口
export interface MeetingResponse {
  meetingId: string;
  channelName: string;
  token: string;
  title: string;
  startTime: string;
}

// 视频会议信息接口
export interface MeetingInfo {
  id: number;
  meetingId: string;
  channelName: string;
  token: string;
  creatorId: number;
  docId: number;
  title: string;
  status: number;
  startTime: string;
  endTime?: string;
  createTime: string;
  updateTime: string;
}

// 视频会议API
export const videoMeetingApi = {
  /**
   * 创建视频会议
   * @param data 创建会议的参数
   * @returns 会议信息
   */
  createMeeting: async (data: CreateMeetingRequest): Promise<MeetingResponse> => {
    // 后端使用@RequestParam接收参数，需要通过params传递
    const response = await api.post(`/api/video-meeting/create`, null, { params: data });
    return response.data;
  },

  /**
   * 加入视频会议
   * @param meetingId 会议ID
   * @returns 会议信息
   */
  joinMeeting: async (meetingId: string): Promise<MeetingResponse> => {
    const response = await api.post(`/api/video-meeting/join`, null, { 
      params: { meetingId } 
    });
    return response.data;
  },

  /**
   * 结束视频会议
   * @param meetingId 会议ID
   * @returns 是否成功结束会议
   */
  endMeeting: async (meetingId: string): Promise<{ success: boolean }> => {
    const response = await api.post(`/api/video-meeting/end`, null, { 
      params: { meetingId } 
    });
    return response.data;
  },

  /**
   * 获取会议信息
   * @param meetingId 会议ID
   * @returns 会议信息
   */
  getMeetingInfo: async (meetingId: string): Promise<MeetingInfo> => {
    const response = await api.get(`/api/video-meeting/info`, {
      params: { meetingId },
    });
    return response.data;
  },

  /**
   * 获取文档的活跃会议
   * @param docId 文档ID
   * @returns 会议信息
   */
  getActiveMeeting: async (docId: number): Promise<MeetingInfo | null> => {
    const response = await api.get(`/api/video-meeting/active`, {
      params: { docId },
    });
    return response.data;
  },
};

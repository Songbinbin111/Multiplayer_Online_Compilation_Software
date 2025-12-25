package com.collab.collab_editor_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.collab.collab_editor_backend.entity.VideoMeeting;

import java.util.Map;

/**
 * 视频会议服务接口
 */
public interface VideoMeetingService extends IService<VideoMeeting> {
    /**
     * 创建视频会议
     * @param creatorId 创建者ID
     * @param docId 关联的文档ID
     * @param title 会议标题
     * @return 会议信息，包含meetingId、channelName和token
     */
    Map<String, Object> createMeeting(Long creatorId, Long docId, String title);
    
    /**
     * 加入视频会议
     * @param meetingId 会议ID
     * @param userId 用户ID
     * @return 会议信息，包含channelName和token
     */
    Map<String, Object> joinMeeting(String meetingId, Long userId);
    
    /**
     * 结束视频会议
     * @param meetingId 会议ID
     * @param userId 用户ID
     * @return 是否成功结束会议
     */
    boolean endMeeting(String meetingId, Long userId);
    
    /**
     * 获取会议信息
     * @param meetingId 会议ID
     * @return 会议信息
     */
    VideoMeeting getMeetingInfo(String meetingId);
    
    /**
     * 获取文档的活跃会议
     * @param docId 文档ID
     * @return 会议信息
     */
    VideoMeeting getActiveMeetingByDocId(Long docId);
}

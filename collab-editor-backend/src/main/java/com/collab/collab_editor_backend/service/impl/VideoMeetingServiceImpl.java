package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.collab_editor_backend.entity.VideoMeeting;
import com.collab.collab_editor_backend.mapper.VideoMeetingMapper;
import com.collab.collab_editor_backend.service.VideoMeetingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 视频会议服务实现类
 */
@Service
public class VideoMeetingServiceImpl extends ServiceImpl<VideoMeetingMapper, VideoMeeting> implements VideoMeetingService {

    private static final Logger log = LoggerFactory.getLogger(VideoMeetingServiceImpl.class);

    @Value("${agora.app-id}")
    private String appId;

    @Value("${agora.certificate}")
    private String appCertificate;

    @Value("${agora.token-expiration:3600}")
    private int tokenExpiration;
    
    @Override
    public Map<String, Object> createMeeting(Long creatorId, Long docId, String title) {
        // 生成唯一的会议ID和频道名称
        String meetingId = UUID.randomUUID().toString().replace("-", "");
        String channelName = "meeting_" + meetingId;
        
        // 生成Agora Token
        // 如果没有配置Certificate (App ID only模式)，Token应为null
        // 如果配置了Certificate，需要生成真实的Token
        String token = null;
        if (appCertificate != null && !appCertificate.trim().isEmpty()) {
            // TODO: 集成Agora Token生成算法
            // 目前如果配置了证书但没有Token生成逻辑，暂时返回null或抛出警告
            // 为了避免"invalid vendor key"错误，只有在确实能生成有效Token时才返回非空值
            // token = AgoraTokenUtil.buildToken(appId, appCertificate, channelName, creatorId);
            log.warn("配置了Agora Certificate但未实现Token生成逻辑，视频会议可能无法连接");
        }
        
        // 创建会议记录
        VideoMeeting meeting = new VideoMeeting();
        meeting.setMeetingId(meetingId);
        meeting.setChannelName(channelName);
        meeting.setToken(token); 
        meeting.setCreatorId(creatorId);
        meeting.setDocId(docId);
        meeting.setTitle(title);
        meeting.setStatus(1); // 设置会议状态为进行中
        meeting.setStartTime(LocalDateTime.now());
        meeting.setCreateTime(LocalDateTime.now());
        meeting.setUpdateTime(LocalDateTime.now());
        
        this.save(meeting);
        
        // 返回会议信息
        Map<String, Object> result = new HashMap<>();
        result.put("meetingId", meetingId);
        result.put("channelName", channelName);
        result.put("token", token);
        result.put("title", title);
        result.put("startTime", meeting.getStartTime());
        result.put("appId", appId); // 返回AppId给前端
        
        return result;
    }
    
    @Override
    public Map<String, Object> joinMeeting(String meetingId, Long userId) {
        // 查询会议信息
        VideoMeeting meeting = this.baseMapper.getByMeetingId(meetingId);
        if (meeting == null || meeting.getStatus() != 1) {
            throw new RuntimeException("会议不存在或已结束");
        }
        
        // 为当前用户生成Token
        String token = null;
        if (appCertificate != null && !appCertificate.trim().isEmpty()) {
             // TODO: 集成Agora Token生成算法
             // token = AgoraTokenUtil.buildToken(appId, appCertificate, meeting.getChannelName(), userId);
        }
        
        // 返回会议信息
        Map<String, Object> result = new HashMap<>();
        result.put("channelName", meeting.getChannelName());
        result.put("token", token);
        result.put("title", meeting.getTitle());
        result.put("startTime", meeting.getStartTime());
        result.put("appId", appId); // 返回AppId给前端
        
        return result;
    }
    
    @Override
    public boolean endMeeting(String meetingId, Long userId) {
        // 查询会议信息
        VideoMeeting meeting = this.baseMapper.getByMeetingId(meetingId);
        if (meeting == null) {
            throw new RuntimeException("会议不存在");
        }
        
        // 只有会议创建者才能结束会议
        if (!meeting.getCreatorId().equals(userId)) {
            throw new RuntimeException("只有会议创建者才能结束会议");
        }
        
        // 更新会议状态
        meeting.setStatus(2); // 设置会议状态为已结束
        meeting.setEndTime(LocalDateTime.now());
        meeting.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(meeting);
    }
    
    @Override
    public VideoMeeting getMeetingInfo(String meetingId) {
        return this.baseMapper.getByMeetingId(meetingId);
    }
    
    @Override
    public VideoMeeting getActiveMeetingByDocId(Long docId) {
        return this.baseMapper.getActiveMeetingByDocId(docId);
    }
}

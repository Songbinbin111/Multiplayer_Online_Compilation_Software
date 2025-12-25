package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.entity.VideoMeeting;
import com.collab.collab_editor_backend.service.VideoMeetingService;
import com.collab.collab_editor_backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 视频会议控制器
 */
@RestController
@RequestMapping("/api/video-meeting")
public class VideoMeetingController {
    
    @Autowired
    private VideoMeetingService videoMeetingService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 创建视频会议
     * @param docId 关联的文档ID
     * @param title 会议标题
     * @param token JWT令牌
     * @return 会议信息
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createMeeting(
            @RequestParam Long docId,
            @RequestParam String title,
            @RequestHeader("Authorization") String token) {
        
        // 从JWT令牌中获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        // 创建会议
        Map<String, Object> result = videoMeetingService.createMeeting(userId, docId, title);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 加入视频会议
     * @param meetingId 会议ID
     * @param token JWT令牌
     * @return 会议信息
     */
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinMeeting(
            @RequestParam String meetingId,
            @RequestHeader("Authorization") String token) {
        
        // 从JWT令牌中获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        // 加入会议
        Map<String, Object> result = videoMeetingService.joinMeeting(meetingId, userId);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 结束视频会议
     * @param meetingId 会议ID
     * @param token JWT令牌
     * @return 是否成功结束会议
     */
    @PostMapping("/end")
    public ResponseEntity<Map<String, Boolean>> endMeeting(
            @RequestParam String meetingId,
            @RequestHeader("Authorization") String token) {
        
        // 从JWT令牌中获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        // 结束会议
        boolean success = videoMeetingService.endMeeting(meetingId, userId);
        
        Map<String, Boolean> result = Map.of("success", success);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取会议信息
     * @param meetingId 会议ID
     * @return 会议信息
     */
    @GetMapping("/info")
    public ResponseEntity<VideoMeeting> getMeetingInfo(
            @RequestParam String meetingId) {
        
        VideoMeeting meeting = videoMeetingService.getMeetingInfo(meetingId);
        return ResponseEntity.ok(meeting);
    }
    
    /**
     * 获取文档的活跃会议
     * @param docId 文档ID
     * @return 会议信息
     */
    @GetMapping("/active")
    public ResponseEntity<VideoMeeting> getActiveMeeting(
            @RequestParam Long docId) {
        
        VideoMeeting meeting = videoMeetingService.getActiveMeetingByDocId(docId);
        return ResponseEntity.ok(meeting);
    }
}

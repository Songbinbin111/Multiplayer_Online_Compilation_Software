package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 视频会议实体类
 */
@Data
@TableName(value = "t_video_meeting") // 对应数据库表名 t_video_meeting
public class VideoMeeting {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String meetingId; // 会议ID
    
    private String channelName; // 频道名称
    
    private String token; // 会议令牌
    
    private Long creatorId; // 创建者ID
    
    private Long docId; // 关联的文档ID
    
    private String title; // 会议标题
    
    private Integer status; // 会议状态：0-未开始，1-进行中，2-已结束
    
    private LocalDateTime startTime; // 开始时间
    
    private LocalDateTime endTime; // 结束时间
    
    private LocalDateTime createTime; // 创建时间
    
    private LocalDateTime updateTime; // 更新时间
}

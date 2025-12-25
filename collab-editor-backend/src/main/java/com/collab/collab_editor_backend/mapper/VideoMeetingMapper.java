package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.VideoMeeting;
import org.apache.ibatis.annotations.Select;

/**
 * 视频会议Mapper接口
 */
public interface VideoMeetingMapper extends BaseMapper<VideoMeeting> {
    /**
     * 根据会议ID查询会议信息
     * @param meetingId 会议ID
     * @return 视频会议信息
     */
    @Select("SELECT * FROM t_video_meeting WHERE meeting_id = #{meetingId}")
    VideoMeeting getByMeetingId(String meetingId);
    
    /**
     * 根据文档ID查询进行中的会议
     * @param docId 文档ID
     * @return 视频会议信息
     */
    @Select("SELECT * FROM t_video_meeting WHERE doc_id = #{docId} AND status = 1 LIMIT 1")
    VideoMeeting getActiveMeetingByDocId(Long docId);
}

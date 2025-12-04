package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.Task;
import java.util.List;

/**
 * 任务模块Mapper接口
 */
public interface TaskMapper extends BaseMapper<Task> {
    
    /**
     * 根据文档ID获取所有任务
     * @param docId 文档ID
     * @return 任务列表
     */
    List<Task> getTasksByDocId(Long docId);
    
    /**
     * 根据负责人ID获取所有任务
     * @param assigneeId 负责人ID
     * @return 任务列表
     */
    List<Task> getTasksByAssigneeId(Long assigneeId);
    
    /**
     * 根据创建者ID获取所有任务
     * @param creatorId 创建者ID
     * @return 任务列表
     */
    List<Task> getTasksByCreatorId(Long creatorId);
}

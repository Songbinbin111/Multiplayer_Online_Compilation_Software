package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.dto.TaskCreateDTO;
import com.collab.collab_editor_backend.dto.TaskUpdateDTO;
import com.collab.collab_editor_backend.entity.Task;

import java.util.List;

public interface TaskService {
    /**
     * 创建任务
     * @param creatorId 创建者ID
     * @param dto 任务创建DTO
     * @return 任务ID
     */
    Long createTask(Long creatorId, TaskCreateDTO dto);

    /**
     * 获取文档的所有任务
     * @param docId 文档ID
     * @return 任务列表
     */
    List<Task> getTasksByDocId(Long docId);

    /**
     * 获取用户负责的所有任务
     * @param assigneeId 负责人ID
     * @return 任务列表
     */
    List<Task> getTasksByAssigneeId(Long assigneeId);

    /**
     * 获取用户创建的所有任务
     * @param creatorId 创建者ID
     * @return 任务列表
     */
    List<Task> getTasksByCreatorId(Long creatorId);

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param dto 任务更新DTO
     * @return 更新结果
     */
    boolean updateTaskStatus(Long taskId, Long userId, TaskUpdateDTO dto);

    /**
     * 删除任务
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 删除结果
     */
    boolean deleteTask(Long taskId, Long userId);
}

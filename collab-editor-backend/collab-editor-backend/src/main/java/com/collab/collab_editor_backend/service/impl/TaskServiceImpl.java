package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.collab_editor_backend.dto.TaskCreateDTO;
import com.collab.collab_editor_backend.dto.TaskUpdateDTO;
import com.collab.collab_editor_backend.entity.Notification;
import com.collab.collab_editor_backend.entity.Task;
import com.collab.collab_editor_backend.mapper.NotificationMapper;
import com.collab.collab_editor_backend.mapper.TaskMapper;
import com.collab.collab_editor_backend.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    /**
     * 创建任务
     * @param creatorId 创建者ID
     * @param dto 任务创建DTO
     * @return 任务ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(Long creatorId, TaskCreateDTO dto) {
        // 1. 创建任务实体
        Task task = new Task();
        task.setDocId(dto.getDocId());
        task.setTitle(dto.getTitle());
        task.setContent(dto.getContent());
        task.setCreatorId(creatorId);
        task.setAssigneeId(dto.getAssigneeId());
        task.setStatus(0); // 初始状态：待处理
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        // 2. 保存任务
        taskMapper.insert(task);

        // 3. 创建任务分配通知
        if (!dto.getAssigneeId().equals(creatorId)) { // 不通知自己
            Notification notification = new Notification();
            notification.setUserId(dto.getAssigneeId());
            notification.setType("task_assign");
            notification.setContent("你被分配了新任务：" + dto.getTitle());
            notification.setDocId(dto.getDocId());
            notification.setRelatedId(task.getId());
            notification.setIsRead(false);
            notification.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(notification);
        }

        return task.getId();
    }

    /**
     * 获取文档的所有任务
     * @param docId 文档ID
     * @return 任务列表
     */
    @Override
    public List<Task> getTasksByDocId(Long docId) {
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Task::getDocId, docId)
                .orderByAsc(Task::getCreateTime);
        return taskMapper.selectList(queryWrapper);
    }

    /**
     * 获取用户负责的所有任务
     * @param assigneeId 负责人ID
     * @return 任务列表
     */
    @Override
    public List<Task> getTasksByAssigneeId(Long assigneeId) {
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Task::getAssigneeId, assigneeId)
                .orderByAsc(Task::getUpdateTime);
        return taskMapper.selectList(queryWrapper);
    }

    /**
     * 获取用户创建的所有任务
     * @param creatorId 创建者ID
     * @return 任务列表
     */
    @Override
    public List<Task> getTasksByCreatorId(Long creatorId) {
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Task::getCreatorId, creatorId)
                .orderByAsc(Task::getUpdateTime);
        return taskMapper.selectList(queryWrapper);
    }

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param dto 任务更新DTO
     * @return 更新结果
     */
    @Override
    public boolean updateTaskStatus(Long taskId, Long userId, TaskUpdateDTO dto) {
        // 1. 查询任务
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }

        // 2. 验证权限（只有任务负责人或创建者可以更新状态）
        if (!task.getAssigneeId().equals(userId) && !task.getCreatorId().equals(userId)) {
            return false;
        }

        // 3. 更新任务状态
        task.setStatus(dto.getStatus());
        task.setUpdateTime(LocalDateTime.now());
        int updateCount = taskMapper.updateById(task);

        return updateCount > 0;
    }

    /**
     * 删除任务
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 删除结果
     */
    @Override
    public boolean deleteTask(Long taskId, Long userId) {
        // 1. 查询任务
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }

        // 2. 验证权限（只有任务创建者可以删除）
        if (!task.getCreatorId().equals(userId)) {
            return false;
        }

        // 3. 删除任务
        int deleteCount = taskMapper.deleteById(taskId);

        return deleteCount > 0;
    }
}

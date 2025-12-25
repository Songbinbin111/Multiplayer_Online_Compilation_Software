package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.collab_editor_backend.dto.TaskCreateDTO;
import com.collab.collab_editor_backend.dto.TaskUpdateDTO;
import com.collab.collab_editor_backend.entity.Notification;
import com.collab.collab_editor_backend.entity.Task;
import com.collab.collab_editor_backend.mapper.NotificationMapper;
import com.collab.collab_editor_backend.mapper.TaskMapper;
import com.collab.collab_editor_backend.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private com.collab.collab_editor_backend.service.NotificationService notificationService;

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
        task.setDeadline(dto.getDeadline()); // 设置截止日期
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        // 2. 保存任务
        taskMapper.insert(task);

        // 3. 创建任务分配通知
        // if (!dto.getAssigneeId().equals(creatorId)) { // 不通知自己
        if (true) { // 临时修改：允许通知自己，方便测试
            notificationService.sendTaskAssignNotification(task.getId(), dto.getTitle(), dto.getDocId(), dto.getAssigneeId(), creatorId);
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

        // 3. 更新任务状态和截止日期
        Integer oldStatus = task.getStatus();
        LocalDateTime oldDeadline = task.getDeadline();
        
        // 更新状态
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }
        
        // 更新截止日期
        if (dto.getDeadline() != null) {
            task.setDeadline(dto.getDeadline());
        }
        
        task.setUpdateTime(LocalDateTime.now());
        int updateCount = taskMapper.updateById(task);

        // 4. 如果状态或截止日期发生变化，发送通知
        boolean statusChanged = dto.getStatus() != null && !oldStatus.equals(dto.getStatus());
        boolean deadlineChanged = dto.getDeadline() != null && (oldDeadline == null || !oldDeadline.equals(dto.getDeadline()));
        
        if (updateCount > 0 && (statusChanged || deadlineChanged)) {
            try {
                String statusText = "";
                switch (dto.getStatus()) {
                    case 0: statusText = "待处理";
                        break;
                    case 1: statusText = "进行中";
                        break;
                    case 2: statusText = "已完成";
                        break;
                }
                
                // 准备通知内容
                StringBuilder notificationContent = new StringBuilder();
                if (statusChanged) {
                    switch (dto.getStatus()) {
                        case 0: statusText = "待处理";
                            break;
                        case 1: statusText = "进行中";
                            break;
                        case 2: statusText = "已完成";
                            break;
                        default: statusText = "未知状态";
                            break;
                    }
                    notificationContent.append("任务状态已更新为").append(statusText);
                }
                
                if (deadlineChanged) {
                    if (notificationContent.length() > 0) {
                        notificationContent.append("，");
                    }
                    notificationContent.append("截止日期已更新为").append(dto.getDeadline().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                }
                
                // 通知创建者
                // if (!task.getCreatorId().equals(userId)) {
                if (true) { // 临时修改：允许通知自己，方便测试
                    Notification notification = new Notification();
                    notification.setUserId(task.getCreatorId());
                    notification.setType("task_status");
                    notification.setContent("您创建的任务\"" + task.getTitle() + "\"" + notificationContent);
                    notification.setDocId(task.getDocId()); // 补充DocId
                    notification.setRelatedId(task.getId());
                    notification.setIsRead(false);
                    notification.setCreateTime(LocalDateTime.now());
                    notificationService.create(notification);
                }
                
                // 通知负责人
                // if (!task.getAssigneeId().equals(userId)) {
                if (true) { // 临时修改：允许通知自己，方便测试
                    Notification notification = new Notification();
                    notification.setUserId(task.getAssigneeId());
                    notification.setType("task_status");
                    notification.setContent("您负责的任务\"" + task.getTitle() + "\"" + notificationContent);
                    notification.setDocId(task.getDocId()); // 补充DocId
                    notification.setRelatedId(task.getId());
                    notification.setIsRead(false);
                    notification.setCreateTime(LocalDateTime.now());
                    notificationService.create(notification);
                }
            } catch (Exception e) {
                logger.error("发送任务状态更新通知失败，taskId={}", taskId, e);
                // 通知发送失败不影响任务状态更新
            }
        }

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

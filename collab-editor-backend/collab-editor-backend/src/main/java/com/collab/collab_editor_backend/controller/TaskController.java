package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.dto.TaskCreateDTO;
import com.collab.collab_editor_backend.dto.TaskUpdateDTO;
import com.collab.collab_editor_backend.entity.Task;
import com.collab.collab_editor_backend.service.TaskService;
import com.collab.collab_editor_backend.util.JwtUtil;
import com.collab.collab_editor_backend.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建任务接口
     * @param dto 任务创建DTO
     * @param request 请求对象
     * @return 创建结果
     */
    @PostMapping("/create")
    public Result<?> createTask(@RequestBody TaskCreateDTO dto, HttpServletRequest request) {
        try {
            String authorization = request.getHeader("Authorization");
            Long creatorId = jwtUtil.getUserIdFromToken(authorization);
            Long taskId = taskService.createTask(creatorId, dto);
            return Result.successWithMessage("任务创建成功", taskId);
        } catch (Exception e) {
            return Result.error("任务创建失败：" + e.getMessage());
        }
    }

    /**
     * 获取文档的所有任务接口
     * @param docId 文档ID
     * @return 任务列表
     */
    @GetMapping("/list/{docId}")
    public Result<?> getTasksByDocId(@PathVariable Long docId) {
        try {
            List<Task> tasks = taskService.getTasksByDocId(docId);
            return Result.success(tasks);
        } catch (Exception e) {
            return Result.error("获取任务失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户负责的所有任务接口
     * @param request 请求对象
     * @return 任务列表
     */
    @GetMapping("/my/assigned")
    public Result<?> getMyAssignedTasks(HttpServletRequest request) {
        try {
            String authorization = request.getHeader("Authorization");
            Long userId = jwtUtil.getUserIdFromToken(authorization);
            List<Task> tasks = taskService.getTasksByAssigneeId(userId);
            return Result.success(tasks);
        } catch (Exception e) {
            return Result.error("获取任务失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户创建的所有任务接口
     * @param request 请求对象
     * @return 任务列表
     */
    @GetMapping("/my/created")
    public Result<?> getMyCreatedTasks(HttpServletRequest request) {
        try {
            String authorization = request.getHeader("Authorization");
            Long userId = jwtUtil.getUserIdFromToken(authorization);
            List<Task> tasks = taskService.getTasksByCreatorId(userId);
            return Result.success(tasks);
        } catch (Exception e) {
            return Result.error("获取任务失败：" + e.getMessage());
        }
    }

    /**
     * 更新任务状态接口
     * @param dto 任务更新DTO
     * @param request 请求对象
     * @return 更新结果
     */
    @PostMapping("/update/status")
    public Result<?> updateTaskStatus(@RequestBody TaskUpdateDTO dto, HttpServletRequest request) {
        try {
            String authorization = request.getHeader("Authorization");
            Long userId = jwtUtil.getUserIdFromToken(authorization);
            boolean success = taskService.updateTaskStatus(dto.getTaskId(), userId, dto);
            if (success) {
                return Result.successWithMessage("任务状态更新成功");
            } else {
                return Result.error("任务状态更新失败，无权限或任务不存在");
            }
        } catch (Exception e) {
            return Result.error("任务状态更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除任务接口
     * @param taskId 任务ID
     * @param request 请求对象
     * @return 删除结果
     */
    @DeleteMapping("/delete/{taskId}")
    public Result<?> deleteTask(@PathVariable Long taskId, HttpServletRequest request) {
        try {
            String authorization = request.getHeader("Authorization");
            Long userId = jwtUtil.getUserIdFromToken(authorization);
            boolean success = taskService.deleteTask(taskId, userId);
            if (success) {
                return Result.successWithMessage("任务删除成功");
            } else {
                return Result.error("任务删除失败，无权限或任务不存在");
            }
        } catch (Exception e) {
            return Result.error("任务删除失败：" + e.getMessage());
        }
    }
}
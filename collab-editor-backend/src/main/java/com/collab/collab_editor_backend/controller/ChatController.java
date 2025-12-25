package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.service.ChatService;
import com.collab.collab_editor_backend.util.JwtUtil;
import com.collab.collab_editor_backend.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 发送消息接口
     * @param request 请求对象
     * @param params 消息参数
     * @return 发送结果
     */
    @PostMapping("/send")
    public Result<?> sendMessage(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        try {
            // 从Token获取当前用户ID
            String authorization = request.getHeader("Authorization");
            Long userId = jwtUtil.getUserIdFromToken(authorization);
            
            // 获取参数
            Long receiverId = Long.parseLong(params.get("receiverId").toString());
            String content = params.get("content").toString();
            
            return chatService.sendMessage(userId, receiverId, content);
        } catch (Exception e) {
            return Result.error("发送消息失败：" + e.getMessage());
        }
    }

    /**
     * 获取聊天历史记录接口
     * @param request 请求对象
     * @param otherUserId 对方用户ID
     * @return 聊天记录列表
     */
    @GetMapping("/history/{otherUserId}")
    public Result<?> getChatHistory(HttpServletRequest request, @PathVariable Long otherUserId) {
        try {
            // 从Token获取当前用户ID
            String authorization = request.getHeader("Authorization");
            Long userId = jwtUtil.getUserIdFromToken(authorization);
            
            return chatService.getChatHistory(userId, otherUserId);
        } catch (Exception e) {
            return Result.error("获取聊天记录失败：" + e.getMessage());
        }
    }

    /**
     * 标记消息为已读接口
     * @param request 请求对象
     * @param senderId 发送者ID
     * @return 操作结果
     */
    @PutMapping("/read/{senderId}")
    public Result<?> markAsRead(HttpServletRequest request, @PathVariable Long senderId) {
        try {
            // 从Token获取当前用户ID
            String authorization = request.getHeader("Authorization");
            Long userId = jwtUtil.getUserIdFromToken(authorization);
            
            return chatService.markAsRead(senderId, userId);
        } catch (Exception e) {
            return Result.error("标记消息为已读失败：" + e.getMessage());
        }
    }

    /**
     * 获取未读消息数量接口
     * @param request 请求对象
     * @return 未读消息数量
     */
    @GetMapping("/unread/count")
    public Result<?> getUnreadCount(HttpServletRequest request) {
        try {
            // 从Token获取当前用户ID
            String authorization = request.getHeader("Authorization");
            Long userId = jwtUtil.getUserIdFromToken(authorization);
            
            return chatService.getUnreadCount(userId);
        } catch (Exception e) {
            return Result.error("获取未读消息数量失败：" + e.getMessage());
        }
    }

    /**
     * 获取聊天列表接口
     * @param request 请求对象
     * @return 聊天列表
     */
    @GetMapping("/list")
    public Result<?> getChatList(HttpServletRequest request) {
        try {
            // 从Token获取当前用户ID
            String authorization = request.getHeader("Authorization");
            Long userId = jwtUtil.getUserIdFromToken(authorization);
            
            return chatService.getChatList(userId);
        } catch (Exception e) {
            return Result.error("获取聊天列表失败：" + e.getMessage());
        }
    }

    /**
     * 发送文件消息接口
     * @param request 请求对象
     * @param receiverId 接收者ID
     * @param file 上传的文件
     * @return 发送结果
     */
    @PostMapping("/send-file")
    public Result<?> sendFile(HttpServletRequest request, 
                             @RequestParam("receiverId") Long receiverId, 
                             @RequestParam("file") MultipartFile file) {
        try {
            // 从Token获取当前用户ID
            String authorization = request.getHeader("Authorization");
            Long userId = jwtUtil.getUserIdFromToken(authorization);
            
            return chatService.sendFile(userId, receiverId, file);
        } catch (Exception e) {
            return Result.error("发送文件失败：" + e.getMessage());
        }
    }
}

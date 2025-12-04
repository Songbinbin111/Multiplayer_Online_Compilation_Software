package com.collab.collab_editor_backend.config; // 这行是自动生成的，若不一致需手动修改

import com.collab.collab_editor_backend.handler.DocumentWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 基础配置类：仅启用功能，暂不写复杂业务
 */
@Configuration // 告诉 Spring 这是一个配置类
@EnableWebSocket // 关键注解：启用 WebSocket 功能（少了这行 WebSocket 不生效）
public class WebSocketConfig implements WebSocketConfigurer {

    // 注册 WebSocket 处理器（暂时用 Spring 自带的空处理器，后续再替换成自定义逻辑）
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 1. 配置协作文档的 WebSocket 路径：/ws/collab/{docId}（按文档ID区分不同协作会话）
        // 2. 使用自定义的 DocumentWebSocketHandler 处理协作逻辑
        // 3. setAllowedOrigins：允许前端 React 项目（默认端口 5173）跨域连接
        registry.addHandler(new DocumentWebSocketHandler(), "/ws/collab/{docId}")
                .setAllowedOrigins("http://localhost:5173");
    }
}
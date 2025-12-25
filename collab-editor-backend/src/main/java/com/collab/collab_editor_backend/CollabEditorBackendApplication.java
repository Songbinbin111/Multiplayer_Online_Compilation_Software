package com.collab.collab_editor_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 只保留核心注解，删除多余配置
@SpringBootApplication
@MapperScan("com.collab.collab_editor_backend.mapper") // 仅扫描 Mapper 包
public class CollabEditorBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(CollabEditorBackendApplication.class, args);
    }
}
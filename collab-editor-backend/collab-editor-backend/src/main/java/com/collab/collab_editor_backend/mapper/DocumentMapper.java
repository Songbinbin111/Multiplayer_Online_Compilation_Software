package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.Document;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 仅保留 @Mapper 注解
public interface DocumentMapper extends BaseMapper<Document> {
    // 无需任何额外方法
}
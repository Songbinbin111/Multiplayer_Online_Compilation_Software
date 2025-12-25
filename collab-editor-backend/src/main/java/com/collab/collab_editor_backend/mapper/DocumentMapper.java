package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.Document;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface DocumentMapper extends BaseMapper<Document> {
    // 按所有者ID查询文档列表（适配你的实体类字段）
    List<Document> selectByOwnerId(@Param("ownerId") Long ownerId);
}
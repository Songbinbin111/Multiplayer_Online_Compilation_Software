package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collab.collab_editor_backend.entity.DocumentVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档版本Mapper接口
 */
@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {
    
    /**
     * 根据文档ID获取版本列表
     */
    List<DocumentVersion> getVersionsByDocId(@Param("docId") Long docId);
    
    /**
     * 获取文档最新版本
     */
    DocumentVersion getLatestVersionByDocId(@Param("docId") Long docId);
    
    /**
     * 分页获取文档版本列表
     */
    IPage<DocumentVersion> getVersionsByDocIdWithPage(Page<DocumentVersion> page, @Param("docId") Long docId);
}

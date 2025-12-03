package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.OnlineUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OnlineUserMapper extends BaseMapper<OnlineUser> {
    // 继承 BaseMapper，已包含 CRUD 基础方法，无需额外自定义
}
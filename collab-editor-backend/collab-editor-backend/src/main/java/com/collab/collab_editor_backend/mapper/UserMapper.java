package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper// 标记为 MyBatis Mapper 接口
public interface UserMapper extends BaseMapper<User> {
    // 自定义查询：根据用户名查询用户（登录/注册时用）
    User selectByUsername(@Param("username") String username);
}
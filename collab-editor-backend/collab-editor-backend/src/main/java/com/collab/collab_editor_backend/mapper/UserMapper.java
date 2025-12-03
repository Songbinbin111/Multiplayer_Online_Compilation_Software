package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {

    // 按用户名查询用户（注解方式，直接执行SQL）
    @Select("SELECT id, username, password, nickname, create_time, update_time FROM t_user WHERE username = #{username}")
    User selectByUsername(@Param("username") String username);
}
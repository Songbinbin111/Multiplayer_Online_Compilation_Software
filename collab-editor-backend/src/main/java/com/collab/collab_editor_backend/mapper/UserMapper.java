package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    // 按用户名查询用户（注解方式，直接执行SQL）
    @Select("SELECT id, username, password, nickname, create_time, update_time FROM t_user WHERE username = #{username}")
    User selectByUsername(@Param("username") String username);
    
    // 按用户名列表查询用户（用于@提及功能）
    @Select("<script>SELECT id, username, nickname FROM t_user WHERE username IN <foreach collection='usernameList' item='username' open='(' separator=',' close=')'>#{username}</foreach></script>")
    List<User> selectByUsernameList(@Param("usernameList") List<String> usernameList);
    
    // 按邮箱查询用户（用于密码找回）
    @Select("SELECT * FROM t_user WHERE email = #{email}")
    User selectByEmail(@Param("email") String email);
    
    // 按手机号查询用户（用于手机号注册和密码找回）
    @Select("SELECT * FROM t_user WHERE phone = #{phone}")
    User selectByPhone(@Param("phone") String phone);
    
    // 按重置令牌查询用户（用于密码重置）
    @Select("SELECT * FROM t_user WHERE reset_token = #{resetToken} AND reset_token_expiry > NOW()")
    User selectByResetToken(@Param("resetToken") String resetToken);
    
    // 更新重置令牌和过期时间
    @Update("UPDATE t_user SET reset_token = #{resetToken}, reset_token_expiry = #{resetTokenExpiry}, update_time = NOW() WHERE id = #{userId}")
    int updateResetToken(@Param("userId") Long userId, @Param("resetToken") String resetToken, @Param("resetTokenExpiry") LocalDateTime resetTokenExpiry);
    
    // 清除重置令牌（密码重置后）
    @Update("UPDATE t_user SET reset_token = NULL, reset_token_expiry = NULL, update_time = NOW() WHERE id = #{userId}")
    int clearResetToken(@Param("userId") Long userId);
}
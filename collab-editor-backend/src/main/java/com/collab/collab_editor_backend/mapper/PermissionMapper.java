package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 权限Mapper接口
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
    /**
     * 根据角色名称获取权限列表
     */
    List<Permission> selectPermissionsByRoleName(String roleName);
}

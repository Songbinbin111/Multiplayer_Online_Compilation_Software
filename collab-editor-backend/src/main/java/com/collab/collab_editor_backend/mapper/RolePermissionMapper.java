package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 角色权限关联Mapper接口
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
    /**
     * 根据角色名称删除所有关联的权限
     */
    int deleteByRoleName(String roleName);
    
    /**
     * 根据角色名称获取关联的权限ID列表
     */
    List<Long> selectPermissionIdsByRoleName(String roleName);
}

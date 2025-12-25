package com.collab.collab_editor_backend.mapper;

import com.collab.collab_editor_backend.entity.ErrorLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 错误日志Mapper接口
 */
@Mapper
public interface ErrorLogMapper {
    
    /**
     * 插入错误日志
     * @param errorLog 错误日志实体
     * @return 插入结果
     */
    @Insert("INSERT INTO error_log (`timestamp`, `type`, `message`, `stack`, `url`, `line`, `column`, `user_agent`, `user_id`, `doc_id`, `additional_info`, `create_time`) " +
            "VALUES (#{timestamp}, #{type}, #{message}, #{stack}, #{url}, #{line}, #{column}, #{userAgent}, #{userId}, #{docId}, #{additionalInfo}, #{createTime})")
    int insertErrorLog(ErrorLog errorLog);
    
    /**
     * 批量插入错误日志
     * @param errorLogs 错误日志列表
     * @return 插入结果
     */
    int batchInsertErrorLogs(List<ErrorLog> errorLogs);
    
    /**
     * 查询所有错误日志
     * @return 错误日志列表
     */
    @Select("SELECT * FROM error_log ORDER BY create_time DESC")
    List<ErrorLog> selectAllErrorLogs();
    
    /**
     * 根据时间范围查询错误日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 错误日志列表
     */
    @Select("SELECT * FROM error_log WHERE create_time BETWEEN #{startTime} AND #{endTime} ORDER BY create_time DESC")
    List<ErrorLog> selectErrorLogsByTimeRange(String startTime, String endTime);
    
    /**
     * 根据错误类型查询错误日志
     * @param type 错误类型
     * @return 错误日志列表
     */
    @Select("SELECT * FROM error_log WHERE type = #{type} ORDER BY create_time DESC")
    List<ErrorLog> selectErrorLogsByType(String type);
    
    /**
     * 根据用户ID查询错误日志
     * @param userId 用户ID
     * @return 错误日志列表
     */
    @Select("SELECT * FROM error_log WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<ErrorLog> selectErrorLogsByUserId(Long userId);
    
    /**
     * 根据文档ID查询错误日志
     * @param docId 文档ID
     * @return 错误日志列表
     */
    @Select("SELECT * FROM error_log WHERE doc_id = #{docId} ORDER BY create_time DESC")
    List<ErrorLog> selectErrorLogsByDocId(Long docId);
}

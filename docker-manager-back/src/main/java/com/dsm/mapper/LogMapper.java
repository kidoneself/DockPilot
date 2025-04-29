package com.dsm.mapper;

import com.dsm.pojo.entity.Log;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LogMapper {
    
    @Insert("INSERT INTO logs (type, level, content, create_time) VALUES (#{type}, #{level}, #{content}, datetime('now'))")
    void insert(Log log);

    @Select("SELECT * FROM logs ORDER BY create_time DESC LIMIT #{limit}")
    List<Log> findRecentLogs(int limit);

    @Select("SELECT * FROM logs WHERE type = #{type} ORDER BY create_time DESC LIMIT #{limit}")
    List<Log> findRecentLogsByType(@Param("type") String type, @Param("limit") int limit);

    @Select("SELECT * FROM logs WHERE level = #{level} ORDER BY create_time DESC LIMIT #{limit}")
    List<Log> findRecentLogsByLevel(@Param("level") String level, @Param("limit") int limit);

    @Delete("DELETE FROM logs WHERE create_time < datetime('now', '-' || #{days} || ' days')")
    void deleteByCreateTimeBefore(@Param("days") int days);
} 
package com.dockpilot.mapper;

import com.dockpilot.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户数据访问接口
 * 提供用户数据的数据库操作
 */
@Mapper
public interface UserMapper {
    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息，如果不存在返回null
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);

    /**
     * 更新用户密码
     *
     * @param user 用户信息（包含新密码）
     * @return 影响的行数
     */
    @Update("UPDATE users SET password = #{password}, updated_at = CURRENT_TIMESTAMP WHERE username = #{username}")
    int updatePassword(User user);
} 
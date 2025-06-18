package com.dockpilot.service.http;

import com.dockpilot.model.User;
import com.dockpilot.model.dto.ChangePasswordRequest;
import com.dockpilot.model.dto.ChangeUsernameRequest;
import com.dockpilot.model.dto.LoginRequest;
import com.dockpilot.utils.ApiResponse;

/**
 * 用户服务接口
 * 提供用户登录和信息查询等功能
 */
public interface UserService {
    /**
     * 用户登录
     * 验证用户名和密码，生成JWT token
     *
     * @param loginRequest 登录请求参数（用户名和密码）
     * @return 登录结果，成功返回JWT token，失败返回错误信息
     */
    ApiResponse<String> login(LoginRequest loginRequest);

    /**
     * 获取用户信息
     * 从JWT token中获取当前登录用户的信息
     *
     * @return 用户信息，包含用户等级等
     */
    ApiResponse<User> getUserInfo();

    /**
     * 修改密码
     * 验证旧密码并更新为新密码
     *
     * @param request 修改密码请求（包含旧密码和新密码）
     * @return 修改结果
     */
    ApiResponse<Void> changePassword(ChangePasswordRequest request);

    /**
     * 修改用户名
     * 更新当前用户的用户名
     *
     * @param request 修改用户名请求（包含新用户名）
     * @return 修改结果
     */
    ApiResponse<Void> changeUsername(ChangeUsernameRequest request);

    /**
     * 根据用户名获取用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);
} 
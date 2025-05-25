package com.dsm.service.http.impl;

import com.dsm.mapper.UserMapper;
import com.dsm.model.User;
import com.dsm.model.dto.ChangePasswordRequest;
import com.dsm.model.dto.LoginRequest;
import com.dsm.service.http.UserService;
import com.dsm.utils.ApiResponse;
import com.dsm.utils.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 * 实现用户登录和信息查询等功能
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户登录
     * 验证用户名和密码，生成JWT token
     *
     * @param loginRequest 登录请求参数（用户名和密码）
     * @return 登录结果，成功返回JWT token，失败返回错误信息
     */
    @Override
    public ApiResponse<String> login(LoginRequest loginRequest) {
        // 根据用户名查询用户
        User dbUser = userMapper.findByUsername(loginRequest.getUsername());
        if (dbUser == null) {
            return ApiResponse.error("用户不存在");
        }
        // 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), dbUser.getPassword())) {
            return ApiResponse.error("密码错误");
        }
        // 生成JWT token
        String token = jwtUtil.generateToken(dbUser);
        return ApiResponse.success(token);
    }

    /**
     * 获取用户信息
     * 从JWT token中获取当前登录用户的信息
     *
     * @return 用户信息，包含用户等级等
     */
    @Override
    public ApiResponse<User> getUserInfo() {
        // 从token中获取用户信息
        User user = jwtUtil.getCurrentUser();
        if (user == null) {
            return ApiResponse.error("未登录");
        }
        return ApiResponse.success(user);
    }

    /**
     * 修改密码
     * 验证旧密码并更新为新密码
     *
     * @param request 修改密码请求（包含旧密码和新密码）
     * @return 修改结果
     */
    @Override
    public ApiResponse<Void> changePassword(ChangePasswordRequest request) {
        // 获取当前登录用户
        User currentUser = jwtUtil.getCurrentUser();
        if (currentUser == null) {
            return ApiResponse.error("未登录");
        }

        // 从数据库获取最新用户信息
        User dbUser = userMapper.findByUsername(currentUser.getUsername());
        if (dbUser == null) {
            return ApiResponse.error("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), dbUser.getPassword())) {
            return ApiResponse.error("旧密码错误");
        }

        // 加密新密码
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());

        // 更新密码
        dbUser.setPassword(encodedNewPassword);
        userMapper.updatePassword(dbUser);

        return ApiResponse.success();
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.findByUsername(username);
    }
} 
package com.dsm.controller;

import com.dsm.common.annotation.Anonymous;
import com.dsm.model.User;
import com.dsm.model.dto.ChangePasswordRequest;
import com.dsm.model.dto.LoginRequest;
import com.dsm.service.http.UserService;
import com.dsm.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 用户管理接口
 * 提供用户登录、获取用户信息等功能
 */
@Tag(name = "用户管理", description = "用户登录、信息查询等功能")
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * 验证用户名和密码，返回JWT token
     *
     * @param loginRequest 登录请求参数（用户名和密码）
     * @return JWT token
     */
    @Operation(summary = "用户登录", description = "验证用户名和密码，返回JWT token")
    @PostMapping("/login")
    @Anonymous  // 允许匿名访问
    public ApiResponse<String> login(@RequestBody LoginRequest loginRequest) {
        return userService.login(loginRequest);
    }

    /**
     * 获取用户信息
     * 从JWT token中获取当前登录用户的信息
     *
     * @return 用户信息（包含用户等级）
     */
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的信息，包含用户等级")
    @GetMapping("/info")
    public ApiResponse<User> getUserInfo() {
        return userService.getUserInfo();
    }

    /**
     * 修改密码
     * 验证旧密码并更新为新密码
     *
     * @param request 修改密码请求（包含旧密码和新密码）
     * @return 修改结果
     */
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        return userService.changePassword(request);
    }

    /**
     * 专业版功能示例
     * 仅专业版用户可访问
     */
    @Operation(summary = "专业版功能", description = "仅专业版用户可访问")
    @GetMapping("/pro-features")
    @PreAuthorize("hasRole('PRO')")  // 需要PRO角色
    public ApiResponse<List<String>> getProFeatures() {
        return ApiResponse.success(Arrays.asList("高级功能1", "高级功能2"));
    }

    /**
     * 免费版功能示例
     */
    @Operation(summary = "免费版功能", description = "免费版用户可访问")
    @GetMapping("/free-features")
    public ApiResponse<List<String>> getFreeFeatures() {
        return ApiResponse.success(Arrays.asList("基础功能1", "基础功能2"));
    }
} 
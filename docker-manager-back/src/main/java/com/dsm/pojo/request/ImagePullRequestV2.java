package com.dsm.pojo.request;

import lombok.Data;

/**
 * 镜像拉取请求V2
 */
@Data
public class ImagePullRequestV2 {
    /**
     * 镜像名称
     */
    private String image;

    /**
     * 镜像标签
     */
    private String tag;

    /**
     * 仓库地址
     */
    private String registry;

    /**
     * 仓库用户名
     */
    private String username;

    /**
     * 仓库密码
     */
    private String password;

    /**
     * 是否使用代理
     */
    private boolean useProxy;

    /**
     * 代理地址
     */
    private String proxyAddress;

    /**
     * 代理端口
     */
    private Integer proxyPort;

    /**
     * 代理用户名
     */
    private String proxyUsername;

    /**
     * 代理密码
     */
    private String proxyPassword;

    /**
     * 是否跳过TLS验证
     */
    private boolean skipTlsVerify;

    /**
     * 拉取超时时间（秒）
     */
    private Integer timeout;

    /**
     * 是否显示进度
     */
    private boolean showProgress;

    /**
     * 是否强制拉取
     */
    private boolean forcePull;

    /**
     * 平台架构
     */
    private String platform;
} 
package com.dsm.websocket.dispatcher;

import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.sender.DockerWebSocketMessageSender;
import com.dsm.websocket.service.DockerImageService;
import com.dsm.websocket.service.DockerInstallService;
import com.dsm.websocket.service.DockerValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
public class DockerMessageDispatcher {

    @Autowired
    private DockerImageService imageService;

    @Autowired
    private DockerInstallService installService;

    @Autowired
    private DockerValidationService validationService;

    @Autowired
    private DockerWebSocketMessageSender messageSender;

    public void dispatch(WebSocketSession session, DockerWebSocketMessage message) {
        try {
            switch (message.getType()) {
                case "PULL_IMAGE"://拉取镜像
                    imageService.handlePullImage(session, message);
                    break;
                case "INSTALL_CHECK_IMAGES"://商店安装校验镜像是否存在
                    imageService.handleInstallCheckImages(session, message);
                    break;
                case "INSTALL_PULL_IMAGE"://弃用
                    imageService.handleInstallPullImage(session, message);
                    break;
                case "INSTALL_VALIDATE"://商店安装校验参数是否合理
                    validationService.handleInstallValidate(session, message);
                    break;
                case "INSTALL_START"://应用商店的开始安装
                    installService.handleInstallStart(session, message);
                    break;
                case "UPDATE_IMAGE"://更新镜像
                    imageService.handleUpdateImage(session, message);
                    break;
                case "CHECK_IMAGE_UPDATES"://检查镜像更新
                    imageService.handleCheckImageUpdates(session, message);
                    break;
                default:
                    log.warn("未知的消息类型: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("处理消息时发生错误", e);
            messageSender.sendErrorMessage(session, "处理消息时发生错误: " + e.getMessage());
        }
    }
} 
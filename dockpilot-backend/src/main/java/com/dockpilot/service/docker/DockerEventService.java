package com.dockpilot.service.docker;

import com.dockpilot.api.DockerService;
import com.dockpilot.common.config.DockerEventsConfig;
import com.dockpilot.mapper.ContainerInfoMapper;
import com.dockpilot.model.ContainerInfo;
import com.dockpilot.utils.LogUtil;
import com.dockpilot.websocket.sender.WebSocketMessageSender;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.EventsCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.core.command.EventsResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Docker事件监听服务
 * 实时监听Docker容器事件，同步容器状态到数据库
 */
@Slf4j
@Service
public class DockerEventService implements ApplicationRunner {

    @Autowired
    private DockerClient dockerClient;
    
    @Autowired
    private DockerService dockerService;
    
    @Autowired
    private ContainerInfoMapper containerInfoMapper;
    
    @Autowired
    private DockerEventsConfig eventsConfig;
    
    @Autowired
    private WebSocketMessageSender messageSender;

    private EventsResultCallback eventsCallback;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private CompletableFuture<Void> eventListenerFuture;
    
    // 🔥 新增：容器重启统计
    private final Map<String, AtomicInteger> containerRestartCount = new ConcurrentHashMap<>();
    private final Map<String, Long> containerLastRestartTime = new ConcurrentHashMap<>();
    
    // 🔥 新增：重启频率阈值配置
    private static final int RESTART_WARNING_THRESHOLD = 3;  // 3次重启告警
    private static final long RESTART_TIME_WINDOW = 5 * 60 * 1000L; // 5分钟时间窗口

    @Override
    public void run(ApplicationArguments args) {
        if (eventsConfig.isEnabled()) {
            log.info("🔧 Docker Events监听已启用，正在启动...");
            startEventListener();
        } else {
            log.info("🔧 Docker Events监听已禁用");
        }
    }

    /**
     * 启动Docker事件监听
     */
    public void startEventListener() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("🔄 启动Docker Events监听服务...");
            
            eventListenerFuture = CompletableFuture.runAsync(() -> {
                try {
                    eventsCallback = new EventsResultCallback() {
                        @Override
                        public void onNext(Event event) {
                            handleDockerEvent(event);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            log.error("Docker Events监听出错", throwable);
                            // 重新启动监听
                            if (isRunning.get() && eventsConfig.isAutoRestart()) {
                                log.info("尝试重新启动Docker Events监听...");
                                restartEventListener();
                            }
                        }

                        @Override
                        public void onComplete() {
                            log.info("Docker Events监听已完成");
                        }
                    };

                    // 只监听容器事件
                    EventsCmd eventsCmd = dockerClient.eventsCmd()
                            .withEventTypeFilter("container");
                    
                    eventsCmd.exec(eventsCallback);
                    
                    log.info("✅ Docker Events监听服务已启动");
                    
                } catch (Exception e) {
                    log.error("启动Docker Events监听失败", e);
                    isRunning.set(false);
                }
            });
        }
    }

    /**
     * 处理Docker事件
     */
    private void handleDockerEvent(Event event) {
        try {
            String eventType = event.getAction();
            String containerId = event.getId();
            
            log.debug("🔔 Docker Event: {} - {}", eventType, containerId);
            
            switch (eventType) {
                case "create":
                    handleContainerCreate(containerId);
                    break;
                case "start":
                    handleContainerStart(containerId);
                    break;
                case "stop":
                    handleContainerStop(containerId, true);
                    break;
                case "kill":
                    handleContainerStop(containerId, false);
                    break;
                case "die":
                    // 🔥 新增：处理die事件，分析退出码
                    handleContainerDie(containerId, event);
                    break;
                case "destroy":
                    handleContainerDestroy(containerId);
                    break;
                case "rename":
                    handleContainerRename(containerId);
                    break;
                case "restart":
                    // 🔥 新增：处理restart事件，检测频繁重启
                    handleContainerRestart(containerId);
                    break;
                case "oom":
                    // 🔥 新增：处理OOM事件
                    handleContainerOOM(containerId);
                    break;
                case "health_status":
                    // 🔥 新增：处理健康检查状态变化
                    handleHealthStatus(containerId, event);
                    break;
                default:
                    // 其他事件暂不处理
                    break;
            }
            
            // 推送容器列表更新通知
            notifyContainerListUpdate();
            
        } catch (Exception e) {
            log.error("处理Docker事件失败: {}", event, e);
        }
    }

    /**
     * 🔥 新增：处理容器die事件，分析退出码
     */
    private void handleContainerDie(String containerId, Event event) {
        try {
            String containerName = getContainerName(containerId);
            
            // 从Event中获取退出码
            Integer exitCode = getExitCodeFromEvent(event);
            
            updateContainerStatus(containerId, "exited");
            
            // 分析退出码，判断是正常停止还是异常崩溃
            String exitAnalysis = analyzeExitCode(exitCode);
            boolean isAbnormal = isAbnormalExit(exitCode);
            
            log.info("💀 容器死亡: {} - 退出码: {} ({})", containerName, exitCode, exitAnalysis);
            
            // 🔥 根据退出码选择不同的通知级别和图标
            String icon;
            String message;
            if (isAbnormal) {
                icon = "💥";
                message = String.format("💥 容器 %s 异常退出 (退出码: %d - %s)", 
                    containerName, exitCode, exitAnalysis);
            } else {
                icon = "✅";
                message = String.format("✅ 容器 %s 正常退出 (退出码: %d)", 
                    containerName, exitCode);
            }
            
            // 发送WebSocket通知
            messageSender.sendDockerEventNotification(
                "die", 
                containerId, 
                containerName, 
                message
            );
            
        } catch (Exception e) {
            log.error("处理容器die事件失败: {}", containerId, e);
        }
    }

    /**
     * 🔥 新增：处理容器restart事件，检测频繁重启
     */
    private void handleContainerRestart(String containerId) {
        try {
            String containerName = getContainerName(containerId);
            long currentTime = System.currentTimeMillis();
            
            // 更新重启统计
            AtomicInteger restartCount = containerRestartCount.computeIfAbsent(
                containerId, k -> new AtomicInteger(0));
            Long lastRestartTime = containerLastRestartTime.get(containerId);
            
            // 如果距离上次重启超过时间窗口，重置计数
            if (lastRestartTime != null && 
                (currentTime - lastRestartTime) > RESTART_TIME_WINDOW) {
                restartCount.set(0);
            }
            
            int currentRestartCount = restartCount.incrementAndGet();
            containerLastRestartTime.put(containerId, currentTime);
            
            updateContainerStatus(containerId, "restarting");
            log.info("🔄 容器重启: {} (第{}次)", containerName, currentRestartCount);
            
            // 🔥 检测频繁重启
            String message;
            boolean isFrequentRestart = currentRestartCount >= RESTART_WARNING_THRESHOLD;
            
            if (isFrequentRestart) {
                message = String.format("⚠️ 容器 %s 频繁重启 (5分钟内第%d次重启，可能存在问题)", 
                    containerName, currentRestartCount);
                log.warn("⚠️ 检测到容器频繁重启: {} - {}次", containerName, currentRestartCount);
            } else {
                message = String.format("🔄 容器 %s 重启 (第%d次)", 
                    containerName, currentRestartCount);
            }
            
            // 发送WebSocket通知
            messageSender.sendDockerEventNotification(
                "restart", 
                containerId, 
                containerName, 
                message
            );
            
        } catch (Exception e) {
            log.error("处理容器restart事件失败: {}", containerId, e);
        }
    }

    /**
     * 🔥 新增：处理容器OOM事件
     */
    private void handleContainerOOM(String containerId) {
        try {
            String containerName = getContainerName(containerId);
            
            log.warn("💥 容器OOM: {}", containerName);
            
            // 发送WebSocket通知
            messageSender.sendDockerEventNotification(
                "oom", 
                containerId, 
                containerName, 
                "💥 容器 " + containerName + " 内存不足被系统杀死 (OOM)"
            );
            
        } catch (Exception e) {
            log.error("处理容器OOM事件失败: {}", containerId, e);
        }
    }

    /**
     * 🔥 新增：处理健康检查状态变化
     */
    private void handleHealthStatus(String containerId, Event event) {
        try {
            String containerName = getContainerName(containerId);
            String healthStatus = getHealthStatusFromEvent(event);
            
            if ("unhealthy".equals(healthStatus)) {
                log.warn("🚨 容器健康检查失败: {}", containerName);
                
                // 发送WebSocket通知
                messageSender.sendDockerEventNotification(
                    "health_status", 
                    containerId, 
                    containerName, 
                    "🚨 容器 " + containerName + " 健康检查失败"
                );
            } else if ("healthy".equals(healthStatus)) {
                log.info("💚 容器健康检查恢复: {}", containerName);
                
                // 发送WebSocket通知
                messageSender.sendDockerEventNotification(
                    "health_status", 
                    containerId, 
                    containerName, 
                    "💚 容器 " + containerName + " 健康检查恢复正常"
                );
            }
            
        } catch (Exception e) {
            log.error("处理容器健康检查事件失败: {}", containerId, e);
        }
    }

    /**
     * 🔥 新增：从Event中获取退出码
     */
    private Integer getExitCodeFromEvent(Event event) {
        try {
            if (event.getActor() != null && event.getActor().getAttributes() != null) {
                Map<String, String> attributes = event.getActor().getAttributes();
                String exitCodeStr = attributes.get("exitCode");
                if (exitCodeStr != null && !exitCodeStr.isEmpty()) {
                    return Integer.parseInt(exitCodeStr);
                }
            }
        } catch (Exception e) {
            log.debug("获取退出码失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 🔥 新增：从Event中获取健康状态
     */
    private String getHealthStatusFromEvent(Event event) {
        try {
            if (event.getActor() != null && event.getActor().getAttributes() != null) {
                Map<String, String> attributes = event.getActor().getAttributes();
                return attributes.get("health_status");
            }
        } catch (Exception e) {
            log.debug("获取健康状态失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 🔥 新增：分析退出码含义
     */
    private String analyzeExitCode(Integer exitCode) {
        if (exitCode == null) {
            return "未知";
        }
        
        switch (exitCode) {
            case 0:
                return "正常退出";
            case 1:
                return "一般错误";
            case 125:
                return "Docker run命令失败";
            case 126:
                return "命令无法执行";
            case 127:
                return "命令无法找到";
            case 130:
                return "容器被Ctrl+C终止";
            case 137:
                return "容器被SIGKILL信号杀死";
            case 143:
                return "容器被SIGTERM信号终止";
            default:
                if (exitCode >= 128) {
                    int signal = exitCode - 128;
                    return String.format("致命错误信号 %d", signal);
                } else {
                    return "应用程序错误";
                }
        }
    }

    /**
     * 🔥 新增：判断是否为异常退出
     */
    private boolean isAbnormalExit(Integer exitCode) {
        if (exitCode == null) {
            return true; // 无法获取退出码视为异常
        }
        
        // 0为正常退出，143(SIGTERM)为正常终止
        return exitCode != 0 && exitCode != 143;
    }

    /**
     * 处理容器创建事件
     */
    private void handleContainerCreate(String containerId) {
        try {
            // 获取容器详细信息
            List<Container> containers = dockerService.listContainers();
            Container container = containers.stream()
                    .filter(c -> c.getId().equals(containerId))
                    .findFirst()
                    .orElse(null);

            if (container != null) {
                String containerName = container.getNames()[0].replaceFirst("/", "");
                
                // 检查数据库中是否已存在
                ContainerInfo existing = containerInfoMapper.selectAll().stream()
                        .filter(info -> info.getContainerId().equals(containerId))
                        .findFirst()
                        .orElse(null);

                if (existing == null) {
                    // 创建新记录
                    ContainerInfo containerInfo = new ContainerInfo();
                    containerInfo.setContainerId(container.getId());
                    containerInfo.setName(containerName);
                    containerInfo.setImage(container.getImage());
                    containerInfo.setStatus(container.getState());
                    containerInfo.setOperationStatus("success");
                    containerInfo.setNeedUpdate(0);
                    containerInfo.setCreatedAt(new java.util.Date());
                    containerInfo.setUpdatedAt(new java.util.Date());
                    
                    containerInfoMapper.insert(containerInfo);
                    log.info("📦 容器创建: {} ({})", containerName, containerId);
                    
                    // 🔥 发送WebSocket通知
                    messageSender.sendDockerEventNotification(
                        "create", 
                        containerId, 
                        containerName, 
                        "📦 容器 " + containerName + " 已创建"
                    );
                }
            }
        } catch (Exception e) {
            log.error("处理容器创建事件失败: {}", containerId, e);
        }
    }

    /**
     * 处理容器启动事件
     */
    private void handleContainerStart(String containerId) {
        try {
            // 获取容器名称
            String containerName = getContainerName(containerId);
            
            updateContainerStatus(containerId, "running");
            log.info("▶️ 容器启动: {}", containerId);
            
            // 🔥 发送WebSocket通知
            messageSender.sendDockerEventNotification(
                "start", 
                containerId, 
                containerName, 
                "▶️ 容器 " + containerName + " 已启动"
            );
        } catch (Exception e) {
            log.error("处理容器启动事件失败: {}", containerId, e);
        }
    }

    /**
     * 处理容器停止事件
     */
    private void handleContainerStop(String containerId, boolean sendNotification) {
        try {
            // 获取容器名称
            String containerName = getContainerName(containerId);
            
            updateContainerStatus(containerId, "exited");
            log.info("⏹️ 容器停止: {}", containerId);
            
            if (sendNotification) {
                // 🔥 发送WebSocket通知
                messageSender.sendDockerEventNotification(
                    "stop", 
                    containerId, 
                    containerName, 
                    "⏹️ 容器 " + containerName + " 已停止"
                );
            }
        } catch (Exception e) {
            log.error("处理容器停止事件失败: {}", containerId, e);
        }
    }

    /**
     * 处理容器删除事件
     */
    private void handleContainerDestroy(String containerId) {
        try {
            // 先获取容器名称（在删除前）
            String containerName = getContainerName(containerId);
            
            // 🔥 清理重启统计
            containerRestartCount.remove(containerId);
            containerLastRestartTime.remove(containerId);
            
            // 从数据库删除容器记录
            List<ContainerInfo> allContainers = containerInfoMapper.selectAll();
            List<ContainerInfo> toDelete = allContainers.stream()
                    .filter(info -> info.getContainerId().equals(containerId))
                    .collect(Collectors.toList());

            for (ContainerInfo container : toDelete) {
                containerInfoMapper.deleteById(container.getId());
                log.info("🗑️ 容器删除: {} ({})", container.getName(), containerId);
            }
            
            // 🔥 发送WebSocket通知
            messageSender.sendDockerEventNotification(
                "destroy", 
                containerId, 
                containerName, 
                "🗑️ 容器 " + containerName + " 已删除"
            );
        } catch (Exception e) {
            log.error("处理容器删除事件失败: {}", containerId, e);
        }
    }

    /**
     * 处理容器重命名事件
     */
    private void handleContainerRename(String containerId) {
        try {
            // 获取最新容器信息并更新名称
            List<Container> containers = dockerService.listContainers();
            Container container = containers.stream()
                    .filter(c -> c.getId().equals(containerId))
                    .findFirst()
                    .orElse(null);

            if (container != null) {
                ContainerInfo dbContainer = containerInfoMapper.selectAll().stream()
                        .filter(info -> info.getContainerId().equals(containerId))
                        .findFirst()
                        .orElse(null);

                if (dbContainer != null) {
                    String oldName = dbContainer.getName();
                    String newName = container.getNames()[0].replaceFirst("/", "");
                    
                    dbContainer.setName(newName);
                    dbContainer.setUpdatedAt(new java.util.Date());
                    containerInfoMapper.update(dbContainer);
                    log.info("📝 容器重命名: {} -> {}", containerId, newName);
                    
                    // 🔥 发送WebSocket通知
                    messageSender.sendDockerEventNotification(
                        "rename", 
                        containerId, 
                        newName, 
                        "📝 容器 " + oldName + " 重命名为 " + newName
                    );
                }
            }
        } catch (Exception e) {
            log.error("处理容器重命名事件失败: {}", containerId, e);
        }
    }

    /**
     * 更新容器状态
     */
    private void updateContainerStatus(String containerId, String status) {
        try {
            List<ContainerInfo> allContainers = containerInfoMapper.selectAll();
            List<ContainerInfo> matchingContainers = allContainers.stream()
                    .filter(info -> info.getContainerId().equals(containerId))
                    .collect(Collectors.toList());

            for (ContainerInfo container : matchingContainers) {
                container.setStatus(status);
                container.setUpdatedAt(new java.util.Date());
                containerInfoMapper.update(container);
            }
        } catch (Exception e) {
            log.error("更新容器状态失败: {} -> {}", containerId, status, e);
        }
    }

    /**
     * 通知前端容器列表更新
     */
    private void notifyContainerListUpdate() {
        try {
            // TODO: 实现WebSocket广播容器列表更新事件
            // 可以通过现有的WebSocket消息机制推送更新通知
            log.debug("容器列表已更新，可通过WebSocket通知前端");
            
            // 示例：如果ApplicationWebSocketService有广播方法，可以这样调用
            // webSocketService.broadcastMessage("CONTAINER_LIST_UPDATED", "容器列表已更新");
        } catch (Exception e) {
            log.error("推送容器列表更新通知失败", e);
        }
    }

    /**
     * 获取容器名称
     */
    private String getContainerName(String containerId) {
        try {
            // 先从数据库获取
            ContainerInfo dbContainer = containerInfoMapper.selectAll().stream()
                    .filter(info -> info.getContainerId().equals(containerId))
                    .findFirst()
                    .orElse(null);
            
            if (dbContainer != null && dbContainer.getName() != null) {
                return dbContainer.getName();
            }
            
            // 如果数据库没有，从Docker API获取
            List<Container> containers = dockerService.listContainers();
            Container container = containers.stream()
                    .filter(c -> c.getId().equals(containerId))
                    .findFirst()
                    .orElse(null);
            
            if (container != null && container.getNames() != null && container.getNames().length > 0) {
                return container.getNames()[0].replaceFirst("/", "");
            }
            
            // 如果都获取不到，返回容器ID的短格式
            return containerId.length() > 12 ? containerId.substring(0, 12) : containerId;
            
        } catch (Exception e) {
            log.warn("获取容器名称失败: {}", containerId, e);
            return containerId.length() > 12 ? containerId.substring(0, 12) : containerId;
        }
    }

    /**
     * 重启事件监听
     */
    private void restartEventListener() {
        stopEventListener();
        try {
            Thread.sleep(eventsConfig.getRestartDelay()); // 使用配置的延迟时间
            startEventListener();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 停止事件监听
     */
    public void stopEventListener() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("⏹️ 停止Docker Events监听服务...");
            
            if (eventsCallback != null) {
                try {
                    eventsCallback.close();
                } catch (Exception e) {
                    log.error("关闭Docker Events回调失败", e);
                }
            }
            
            if (eventListenerFuture != null) {
                eventListenerFuture.cancel(true);
            }
            
            log.info("✅ Docker Events监听服务已停止");
        }
    }

    /**
     * 获取监听状态
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * 🔥 新增：获取容器重启统计信息
     */
    public Map<String, Integer> getContainerRestartStats() {
        return containerRestartCount.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().get()
                ));
    }

    @PreDestroy
    public void destroy() {
        stopEventListener();
    }
} 
package com.dsm.websocket.service;

import com.dsm.websocket.model.DockerWebSocketMessage;
import com.dsm.websocket.sender.DockerWebSocketMessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Slf4j
@Service
public class DockerValidationService {
    
    @Autowired
    private DockerWebSocketMessageSender messageSender;
    
    public void handleInstallValidate(WebSocketSession session, DockerWebSocketMessage message) {
        try {
            @SuppressWarnings("unchecked") Map<String, Object> params = (Map<String, Object>) message.getData();
            List<Map<String, Object>> results = new ArrayList<>();

            StringBuilder checkCommand = new StringBuilder();

            if (params.containsKey("ports")) {
                @SuppressWarnings("unchecked") List<Map<String, Object>> ports = (List<Map<String, Object>>) params.get("ports");
                for (Map<String, Object> port : ports) {
                    String portStr = port.get("hostPort").toString().trim();
                    int hostPort = Integer.parseInt(portStr);
                    checkCommand.append("nc -z 127.0.0.1 ").append(hostPort).append(" >/dev/null 2>&1; ").append("code=$?; ").append("if [ $code -eq 0 ]; then ").append("echo '::type=port::port=").append(hostPort).append("::status=1::message=Host port is in use'; ").append("else ").append("echo '::type=port::port=").append(hostPort).append("::status=0::message=Host port is available'; ").append("fi; ");
                }
            }

            if (params.containsKey("paths")) {
                @SuppressWarnings("unchecked") List<Map<String, Object>> paths = (List<Map<String, Object>>) params.get("paths");
                for (Map<String, Object> path : paths) {
                    String hostPath = path.get("hostPath").toString().trim();
                    String target = "/host" + hostPath;
                    checkCommand.append("[ -d ").append(target).append(" ] && dir=0 || dir=1; ").append("[ -r ").append(target).append(" ] && read=0 || read=1; ").append("[ -w ").append(target).append(" ] && write=0 || write=1; ").append("if [ $dir -eq 0 ] && [ $read -eq 0 ] && [ $write -eq 0 ]; then ").append("echo '::type=path::path=").append(hostPath).append("::status=0::message=Path is valid and accessible'; ").append("else ").append("msg=\"\"; ").append("if [ $dir -ne 0 ]; then msg=\"$msg Path does not exist;\"; fi; ").append("if [ $read -ne 0 ]; then msg=\"$msg Not readable;\"; fi; ").append("if [ $write -ne 0 ]; then msg=\"$msg Not writable;\"; fi; ").append("echo '::type=path::path=").append(hostPath).append("::status=1::message='$msg''; ").append("fi; ");
                }
            }

            ProcessBuilder pb = new ProcessBuilder("docker", "run", "--rm", "--network", "host", "-v", "/:/host", "docker-manager:latest", "sh", "-c", checkCommand.toString());

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Check output: {}", line);
                    if (!line.startsWith("::")) continue;

                    String[] parts = line.substring(2).split("::");
                    Map<String, String> map = new HashMap<>();
                    for (String part : parts) {
                        int idx = part.indexOf('=');
                        if (idx > 0) {
                            map.put(part.substring(0, idx), part.substring(idx + 1));
                        }
                    }

                    Map<String, Object> result = new HashMap<>();
                    String type = map.get("type");
                    result.put("type", type);
                    result.put("message", map.get("message"));
                    result.put("valid", "0".equals(map.get("status")));

                    if ("port".equals(type)) {
                        result.put("port", Integer.parseInt(map.get("port")));
                    } else if ("path".equals(type)) {
                        result.put("path", map.get("path"));
                    }

                    results.add(result);
                }
            }

            messageSender.sendMessage(session, new DockerWebSocketMessage("INSTALL_VALIDATE_RESULT", message.getTaskId(), results));
        } catch (Exception e) {
            log.error("Parameter validation failed", e);
            messageSender.sendErrorMessage(session, "Parameter validation failed: " + e.getMessage());
        }
    }
} 
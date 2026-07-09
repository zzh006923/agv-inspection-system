package com.example.agv.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * AGV 车辆移动控制服务。
 *
 * 对外接口保持老师文档要求：
 * GET  /agv/movement/heartbeat
 * POST /agv/movement/forward
 * POST /agv/movement/stop
 * POST /agv/movement/backward
 *
 * 底层支持三种模式：
 * 1. mock：课堂无车演示，使用本地状态模拟车辆位置；
 * 2. http：连接车载 WiFi 后，将控制请求转发到真实车载 web 服务，例如 http://192.168.2.57/prod-api；
 * 3. tcp：连接车载 WiFi 后，向配置的车辆 IP + 行驶端口发送 TCP 控制指令。
 *
 * 你们拿到真实小车协议后，只需要在 application.yml 中切换 agv.control-mode，
 * 并填入真实 base-url 或 TCP 指令，不需要修改前端和 Controller 接口。
 */
@Service
public class AgvMovementStateService {

    @Resource
    private AgvConfigReader agvConfigReader;

    @Value("${agv.control-mode:mock}")
    private String controlMode;

    @Value("${agv.vehicle-api-base-url:http://192.168.2.57/prod-api}")
    private String vehicleApiBaseUrl;

    @Value("${agv.control-timeout-ms:3000}")
    private int timeoutMs;

    @Value("${agv.tcp-command.heartbeat:HEARTBEAT}")
    private String heartbeatCommand;

    @Value("${agv.tcp-command.forward:FORWARD}")
    private String forwardCommand;

    @Value("${agv.tcp-command.stop:STOP}")
    private String stopCommand;

    @Value("${agv.tcp-command.backward:BACKWARD}")
    private String backwardCommand;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean running = false;
    private String direction = "stop";
    private double currentPosition = 0.0;
    private LocalDateTime lastUpdateTime = LocalDateTime.now();

    private static final double SPEED_METER_PER_SECOND = 0.5;

    public synchronized Map<String, Object> heartbeat() {
        if (isHttpMode()) {
            return callVehicleHttp("heartbeat", HttpMethod.GET);
        }
        if (isTcpMode()) {
            String raw = sendTcpCommand(heartbeatCommand);
            Map<String, Object> parsed = parsePossibleJson(raw);
            if (!parsed.isEmpty()) {
                parsed.put("connectionMode", "tcp");
                parsed.put("rawResponse", raw);
                return parsed;
            }
            Map<String, Object> data = mockHeartbeat();
            data.put("connectionMode", "tcp");
            data.put("rawResponse", raw);
            data.put("message", "已向真实车辆发送心跳指令，车辆返回非 JSON 数据");
            return data;
        }
        Map<String, Object> data = mockHeartbeat();
        data.put("connectionMode", "mock");
        return data;
    }

    public synchronized Map<String, Object> forward() {
        if (isHttpMode()) {
            return callVehicleHttp("forward", HttpMethod.POST);
        }
        if (isTcpMode()) {
            String raw = sendTcpCommand(forwardCommand);
            refreshPosition();
            this.running = true;
            this.direction = "forward";
            this.lastUpdateTime = LocalDateTime.now();
            Map<String, Object> data = mockHeartbeat();
            data.put("connectionMode", "tcp");
            data.put("rawResponse", raw);
            data.put("message", "已向真实车辆发送前进指令");
            return data;
        }
        refreshPosition();
        this.running = true;
        this.direction = "forward";
        this.lastUpdateTime = LocalDateTime.now();
        Map<String, Object> data = mockHeartbeat();
        data.put("connectionMode", "mock");
        return data;
    }

    public synchronized Map<String, Object> backward() {
        if (isHttpMode()) {
            return callVehicleHttp("backward", HttpMethod.POST);
        }
        if (isTcpMode()) {
            String raw = sendTcpCommand(backwardCommand);
            refreshPosition();
            this.running = true;
            this.direction = "backward";
            this.lastUpdateTime = LocalDateTime.now();
            Map<String, Object> data = mockHeartbeat();
            data.put("connectionMode", "tcp");
            data.put("rawResponse", raw);
            data.put("message", "已向真实车辆发送后退指令");
            return data;
        }
        refreshPosition();
        this.running = true;
        this.direction = "backward";
        this.lastUpdateTime = LocalDateTime.now();
        Map<String, Object> data = mockHeartbeat();
        data.put("connectionMode", "mock");
        return data;
    }

    public synchronized Map<String, Object> stop() {
        if (isHttpMode()) {
            return callVehicleHttp("stop", HttpMethod.POST);
        }
        if (isTcpMode()) {
            String raw = sendTcpCommand(stopCommand);
            refreshPosition();
            this.running = false;
            this.direction = "stop";
            this.lastUpdateTime = LocalDateTime.now();
            Map<String, Object> data = mockHeartbeat();
            data.put("connectionMode", "tcp");
            data.put("rawResponse", raw);
            data.put("message", "已向真实车辆发送停止指令");
            return data;
        }
        refreshPosition();
        this.running = false;
        this.direction = "stop";
        this.lastUpdateTime = LocalDateTime.now();
        Map<String, Object> data = mockHeartbeat();
        data.put("connectionMode", "mock");
        return data;
    }

    /**
     * 真实 AGV 连接检查。供 /system/check/agv 调用。
     */
    public Map<String, Object> checkRealConnection() {
        if (isHttpMode()) {
            return callVehicleHttp("heartbeat", HttpMethod.GET);
        }
        if (isTcpMode()) {
            String host = agvConfigReader.getHost();
            Integer port = agvConfigReader.getDrivePort();
            if (isBlank(host) || port == null) {
                throw new IllegalStateException("系统配置不完整：未设置车辆 IP 或行驶端口");
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeoutMs);
                Map<String, Object> data = new HashMap<>();
                data.put("connectionMode", "tcp");
                data.put("host", host);
                data.put("drivePort", port);
                data.put("connected", true);
                return data;
            } catch (Exception e) {
                throw new IllegalStateException("AGV TCP 连接失败：" + host + ":" + port + " - " + e.getMessage(), e);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("connectionMode", "mock");
        data.put("connected", true);
        data.put("message", "当前为 mock 模式，未连接真实车辆");
        return data;
    }

    private Map<String, Object> callVehicleHttp(String action, HttpMethod method) {
        String url = trimRightSlash(vehicleApiBaseUrl) + "/agv/movement/" + action;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    method,
                    HttpEntity.EMPTY,
                    String.class
            );
            Map<String, Object> parsed = parsePossibleJson(response.getBody());
            Map<String, Object> data = new HashMap<>();
            data.put("connectionMode", "http");
            data.put("target", url);
            data.put("httpStatus", response.getStatusCodeValue());
            if (!parsed.isEmpty()) {
                data.put("upstream", parsed);
                Object upstreamData = parsed.get("data");
                if (upstreamData instanceof Map) {
                    Map<?, ?> upstreamMap = (Map<?, ?>) upstreamData;
                    for (Map.Entry<?, ?> entry : upstreamMap.entrySet()) {
                        if (entry.getKey() != null) {
                            data.put(String.valueOf(entry.getKey()), entry.getValue());
                        }
                    }
                }
            } else {
                data.put("rawResponse", response.getBody());
            }
            return data;
        } catch (Exception e) {
            throw new IllegalStateException("真实车辆 HTTP 接口调用失败：" + url + " - " + e.getMessage(), e);
        }
    }

    private String sendTcpCommand(String command) {
        String host = agvConfigReader.getHost();
        Integer port = agvConfigReader.getDrivePort();
        if (isBlank(host) || port == null) {
            throw new IllegalStateException("系统配置不完整：未设置车辆 IP 或行驶端口");
        }
        String normalizedCommand = normalizeCommand(command);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            OutputStream os = socket.getOutputStream();
            os.write(normalizedCommand.getBytes(StandardCharsets.UTF_8));
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String response = reader.readLine();
            return response == null ? "" : response;
        } catch (Exception e) {
            throw new IllegalStateException("AGV TCP 指令发送失败：" + host + ":" + port + " command=" + command + " - " + e.getMessage(), e);
        }
    }

    private Map<String, Object> mockHeartbeat() {
        refreshPosition();
        Map<String, Object> data = new HashMap<>();
        data.put("sysTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("isRunning", running);
        data.put("direction", direction);
        data.put("currentPosition", round(currentPosition));
        return data;
    }

    private void refreshPosition() {
        LocalDateTime now = LocalDateTime.now();
        long seconds = Duration.between(lastUpdateTime, now).getSeconds();

        if (running && seconds > 0) {
            double delta = seconds * SPEED_METER_PER_SECOND;
            if ("forward".equals(direction)) {
                currentPosition += delta;
            } else if ("backward".equals(direction)) {
                currentPosition -= delta;
                if (currentPosition < 0) {
                    currentPosition = 0;
                }
            }
            lastUpdateTime = now;
        }
    }

    private Map<String, Object> parsePossibleJson(String text) {
        if (isBlank(text)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return new HashMap<>();
        }
    }

    private boolean isHttpMode() {
        return "http".equalsIgnoreCase(controlMode);
    }

    private boolean isTcpMode() {
        return "tcp".equalsIgnoreCase(controlMode);
    }

    private String normalizeCommand(String command) {
        String value = command == null ? "" : command;
        value = value.replace("\\r", "\r").replace("\\n", "\n");
        if (!value.endsWith("\n")) {
            value += "\n";
        }
        return value;
    }

    private String trimRightSlash(String value) {
        if (isBlank(value)) {
            return "http://192.168.2.57/prod-api";
        }
        String text = value.trim();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

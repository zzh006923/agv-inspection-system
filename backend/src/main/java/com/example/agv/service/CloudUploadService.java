package com.example.agv.service;

import com.example.agv.entity.AgvFlaw;
import com.example.agv.entity.AgvIotActionRecord;
import com.example.agv.entity.AgvSensorRecord;
import com.example.agv.entity.AgvTask;
import com.example.agv.entity.AgvUploadRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 云端上传服务。
 *
 * 老师需求中要求“任务完成后上传巡检记录并展示上传进度”。
 * 本服务提供真实 HTTP 上传能力：当 agv.cloud-upload-enabled=true 时，
 * 会把任务、故障、传感器记录、联动记录和上传清单一起 POST 到 agv_config.cloud_url。
 *
 * 没有云端接口或课堂离线演示时，可保持 agv.cloud-upload-enabled=false，
 * 系统仍然完成本地上传状态闭环，但代码层面已经预留真实云端对接能力。
 */
@Service
public class CloudUploadService {

    @Resource
    private AgvConfigReader agvConfigReader;

    @Value("${agv.cloud-upload-enabled:false}")
    private boolean cloudUploadEnabled;

    @Value("${agv.cloud-upload-path:/agv/task/upload}")
    private String cloudUploadPath;

    @Value("${agv.cloud-api-key:}")
    private String defaultCloudApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public UploadResult uploadTaskData(AgvTask task,
                                       List<AgvFlaw> flaws,
                                       List<AgvUploadRecord> uploadRecords,
                                       List<AgvSensorRecord> sensorRecords,
                                       List<AgvIotActionRecord> actionRecords) {
        if (!cloudUploadEnabled) {
            return UploadResult.localOnly("未启用真实云端上传，已执行本地上传状态闭环");
        }

        String cloudUrl = agvConfigReader.getCloudUrl();
        if (isBlank(cloudUrl)) {
            throw new IllegalStateException("系统配置不完整：未设置云端地址 cloudUrl");
        }

        String targetUrl = buildTargetUrl(cloudUrl);

        Map<String, Object> payload = new HashMap<>();
        payload.put("task", task);
        payload.put("flaws", flaws);
        payload.put("uploadRecords", uploadRecords);
        payload.put("sensorRecords", sensorRecords);
        payload.put("iotActionRecords", actionRecords);
        payload.put("uploadTime", LocalDateTime.now().toString());
        payload.put("source", "agv-handheld-backend");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String apiKey = agvConfigReader.getCloudApiKey();
        if (isBlank(apiKey)) {
            apiKey = defaultCloudApiKey;
        }
        if (!isBlank(apiKey)) {
            headers.set("Authorization", apiKey);
        }

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(targetUrl, new HttpEntity<>(payload, headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("云端返回非成功状态：" + response.getStatusCodeValue());
            }
            return UploadResult.remoteSuccess(targetUrl, response.getStatusCodeValue(), response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("云端上传失败：" + targetUrl + " - " + e.getMessage(), e);
        }
    }

    private String buildTargetUrl(String cloudUrl) {
        String base = trimRightSlash(cloudUrl);
        if (base.endsWith("/upload") || base.contains("/upload?")) {
            return base;
        }
        String path = cloudUploadPath == null ? "" : cloudUploadPath.trim();
        if (path.isEmpty()) {
            return base;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    private String trimRightSlash(String value) {
        String text = value == null ? "" : value.trim();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class UploadResult {
        private boolean remote;
        private boolean success;
        private String targetUrl;
        private Integer httpStatus;
        private String message;
        private String rawResponse;

        public static UploadResult localOnly(String message) {
            UploadResult result = new UploadResult();
            result.remote = false;
            result.success = true;
            result.message = message;
            return result;
        }

        public static UploadResult remoteSuccess(String targetUrl, Integer httpStatus, String rawResponse) {
            UploadResult result = new UploadResult();
            result.remote = true;
            result.success = true;
            result.targetUrl = targetUrl;
            result.httpStatus = httpStatus;
            result.message = "云端上传成功";
            result.rawResponse = rawResponse;
            return result;
        }

        public boolean isRemote() { return remote; }
        public void setRemote(boolean remote) { this.remote = remote; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getTargetUrl() { return targetUrl; }
        public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
        public Integer getHttpStatus() { return httpStatus; }
        public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getRawResponse() { return rawResponse; }
        public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
    }
}

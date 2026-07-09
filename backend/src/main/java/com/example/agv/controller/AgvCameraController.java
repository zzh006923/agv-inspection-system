package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 摄像头设备信息代理接口。
 *
 * 老师接口文档中摄像头列表由 easy-api 提供，前端可以直接请求 easy-api；
 * 本接口提供后端代理版本，统一处理 Authorization，前端只需调用 /agv/camera/devices。
 */
@RestController
@RequestMapping("/agv/camera")
@CrossOrigin
public class AgvCameraController {

    @Value("${agv.easy-api-base-url:http://192.168.2.57/easy-api}")
    private String easyApiBaseUrl;

    @Value("${agv.easy-api-authorization:Basic YWRtaW4xMjM6QWRtaW5AMTIz}")
    private String easyApiAuthorization;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/devices")
    public AjaxResult deviceList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "999") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name
    ) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(trimRightSlash(easyApiBaseUrl) + "/devices")
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .queryParam("status", status == null ? "" : status)
                    .queryParam("id", id == null ? "" : id)
                    .queryParam("name", name == null ? "" : name)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", easyApiAuthorization);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            return AjaxResult.success(response.getBody());
        } catch (Exception e) {
            return AjaxResult.error("摄像头设备列表获取失败：" + e.getMessage());
        }
    }

    private String trimRightSlash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "http://192.168.2.57/easy-api";
        }
        String text = value.trim();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}

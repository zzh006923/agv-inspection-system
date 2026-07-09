package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class AgvCameraControllerTest {

    @Test
    void deviceListShouldReturnErrorWhenBaseUrlIsInvalid() {
        AgvCameraController controller = new AgvCameraController();
        ReflectionTestUtils.setField(controller, "easyApiBaseUrl", "not-a-http-url");
        ReflectionTestUtils.setField(controller, "easyApiAuthorization", "Basic test");

        AjaxResult result = controller.deviceList(1, 10, null, null, null);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("摄像头设备列表获取失败"));
    }
}

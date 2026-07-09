package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.service.AgvMovementStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgvMovementControllerTest {

    @Mock
    private AgvMovementStateService agvMovementStateService;

    @InjectMocks
    private AgvMovementController agvMovementController;

    @Test
    void heartbeatShouldReturnSuccessWhenServiceSuccess() {
        Map<String, Object> data = Map.of("connectionMode", "mock", "isRunning", false);
        when(agvMovementStateService.heartbeat()).thenReturn(data);

        AjaxResult result = agvMovementController.heartbeat();

        assertEquals(200, result.getCode());
        assertSame(data, result.getData());
    }

    @Test
    void forwardShouldReturnErrorWhenServiceThrowsException() {
        when(agvMovementStateService.forward()).thenThrow(new IllegalStateException("车辆连接失败"));

        AjaxResult result = agvMovementController.forward();

        assertEquals(500, result.getCode());
        assertEquals("车辆连接失败", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void stopShouldReturnSuccessWhenServiceSuccess() {
        Map<String, Object> data = Map.of("direction", "stop");
        when(agvMovementStateService.stop()).thenReturn(data);

        AjaxResult result = agvMovementController.stop();

        assertEquals(200, result.getCode());
        assertSame(data, result.getData());
    }

    @Test
    void backwardShouldReturnSuccessWhenServiceSuccess() {
        Map<String, Object> data = Map.of("direction", "backward");
        when(agvMovementStateService.backward()).thenReturn(data);

        AjaxResult result = agvMovementController.backward();

        assertEquals(200, result.getCode());
        assertSame(data, result.getData());
    }
}

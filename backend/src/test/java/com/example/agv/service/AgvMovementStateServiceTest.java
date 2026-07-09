package com.example.agv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgvMovementStateServiceTest {

    @Mock
    private AgvConfigReader agvConfigReader;

    @InjectMocks
    private AgvMovementStateService agvMovementStateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(agvMovementStateService, "controlMode", "mock");
    }

    @Test
    void heartbeatShouldReturnMockStopStateByDefault() {
        Map<String, Object> result = agvMovementStateService.heartbeat();

        assertEquals("mock", result.get("connectionMode"));
        assertEquals(false, result.get("isRunning"));
        assertEquals("stop", result.get("direction"));
        assertEquals(0.0, result.get("currentPosition"));
        assertNotNull(result.get("sysTime"));
    }

    @Test
    void forwardShouldChangeStateToRunningForward() {
        Map<String, Object> result = agvMovementStateService.forward();

        assertEquals("mock", result.get("connectionMode"));
        assertEquals(true, result.get("isRunning"));
        assertEquals("forward", result.get("direction"));
    }

    @Test
    void stopShouldChangeStateToStop() {
        agvMovementStateService.forward();

        Map<String, Object> result = agvMovementStateService.stop();

        assertEquals("mock", result.get("connectionMode"));
        assertEquals(false, result.get("isRunning"));
        assertEquals("stop", result.get("direction"));
    }

    @Test
    void backwardShouldChangeStateToRunningBackward() {
        Map<String, Object> result = agvMovementStateService.backward();

        assertEquals("mock", result.get("connectionMode"));
        assertEquals(true, result.get("isRunning"));
        assertEquals("backward", result.get("direction"));
        assertEquals(0.0, result.get("currentPosition"));
    }

    @Test
    void checkRealConnectionShouldReturnConnectedInMockMode() {
        Map<String, Object> result = agvMovementStateService.checkRealConnection();

        assertEquals("mock", result.get("connectionMode"));
        assertEquals(true, result.get("connected"));
        assertEquals("当前为 mock 模式，未连接真实车辆", result.get("message"));
    }

    @Test
    void checkRealConnectionShouldThrowWhenTcpConfigMissing() {
        ReflectionTestUtils.setField(agvMovementStateService, "controlMode", "tcp");
        when(agvConfigReader.getHost()).thenReturn(null);
        when(agvConfigReader.getDrivePort()).thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> agvMovementStateService.checkRealConnection()
        );

        assertTrue(exception.getMessage().contains("未设置车辆 IP 或行驶端口"));
    }
}

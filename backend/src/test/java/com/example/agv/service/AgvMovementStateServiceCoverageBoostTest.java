package com.example.agv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgvMovementStateServiceCoverageBoostTest {

    @Mock
    private AgvConfigReader agvConfigReader;

    @InjectMocks
    private AgvMovementStateService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "controlMode", "mock");
        ReflectionTestUtils.setField(service, "timeoutMs", 100);
    }

    @Test
    void mockHeartbeatShouldAdvanceForwardAndClampBackwardPosition() {
        ReflectionTestUtils.setField(service, "running", true);
        ReflectionTestUtils.setField(service, "direction", "forward");
        ReflectionTestUtils.setField(service, "currentPosition", 0.0);
        ReflectionTestUtils.setField(service, "lastUpdateTime", LocalDateTime.now().minusSeconds(4));
        Map<String, Object> forward = service.heartbeat();
        assertEquals("mock", forward.get("connectionMode"));
        assertEquals(true, forward.get("isRunning"));
        assertTrue(((Double) forward.get("currentPosition")) >= 2.0);

        ReflectionTestUtils.setField(service, "running", true);
        ReflectionTestUtils.setField(service, "direction", "backward");
        ReflectionTestUtils.setField(service, "currentPosition", 0.2);
        ReflectionTestUtils.setField(service, "lastUpdateTime", LocalDateTime.now().minusSeconds(4));
        Map<String, Object> backward = service.heartbeat();
        assertEquals(0.0, backward.get("currentPosition"));
    }

    @Test
    void privateUtilityMethodsShouldHandleJsonBlankSlashCommandAndRound() {
        Map<String, Object> parsed = ReflectionTestUtils.invokeMethod(service, "parsePossibleJson", "{\"data\":{\"direction\":\"forward\"}}");
        assertNotNull(parsed);
        assertTrue(parsed.containsKey("data"));

        Map<String, Object> invalid = ReflectionTestUtils.invokeMethod(service, "parsePossibleJson", "not-json");
        assertNotNull(invalid);
        assertTrue(invalid.isEmpty());

        String defaultUrl = ReflectionTestUtils.invokeMethod(service, "trimRightSlash", " ");
        assertEquals("http://192.168.2.57/prod-api", defaultUrl);
        String trimmed = ReflectionTestUtils.invokeMethod(service, "trimRightSlash", " http://host/api/// ");
        assertEquals("http://host/api", trimmed);

        String normalized = ReflectionTestUtils.invokeMethod(service, "normalizeCommand", "FORWARD\\r\\n");
        assertEquals("FORWARD\r\n", normalized);
        String appended = ReflectionTestUtils.invokeMethod(service, "normalizeCommand", "STOP");
        assertEquals("STOP\n", appended);

        Double rounded = ReflectionTestUtils.invokeMethod(service, "round", 1.236);
        assertEquals(1.24, rounded);
    }

    @Test
    void tcpModeShouldRejectMissingConfigForCommands() {
        ReflectionTestUtils.setField(service, "controlMode", "tcp");
        ReflectionTestUtils.setField(service, "forwardCommand", "FORWARD");
        when(agvConfigReader.getHost()).thenReturn(null);
        when(agvConfigReader.getDrivePort()).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.forward());

        assertTrue(exception.getMessage().contains("未设置车辆 IP 或行驶端口"));
    }

    @Test
    void tcpCheckShouldWrapConnectionFailure() {
        ReflectionTestUtils.setField(service, "controlMode", "tcp");
        when(agvConfigReader.getHost()).thenReturn("127.0.0.1");
        when(agvConfigReader.getDrivePort()).thenReturn(1);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.checkRealConnection());

        assertTrue(exception.getMessage().contains("AGV TCP 连接失败"));
        assertTrue(exception.getMessage().contains("127.0.0.1:1"));
    }

    @Test
    void httpModeShouldWrapInvalidRemoteCall() {
        ReflectionTestUtils.setField(service, "controlMode", "http");
        ReflectionTestUtils.setField(service, "vehicleApiBaseUrl", "http://127.0.0.1:1/prod-api///");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.heartbeat());

        assertTrue(exception.getMessage().contains("真实车辆 HTTP 接口调用失败"));
        assertTrue(exception.getMessage().contains("/agv/movement/heartbeat"));
    }

    @Test
    void stopForwardBackwardShouldKeepMockStateTransitions() {
        Map<String, Object> forward = service.forward();
        assertEquals("forward", forward.get("direction"));
        Map<String, Object> backward = service.backward();
        assertEquals("backward", backward.get("direction"));
        Map<String, Object> stop = service.stop();
        assertEquals("stop", stop.get("direction"));
        assertEquals(false, stop.get("isRunning"));
    }
}

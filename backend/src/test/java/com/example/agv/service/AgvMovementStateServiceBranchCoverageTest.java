package com.example.agv.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class AgvMovementStateServiceBranchCoverageTest {

    @Mock
    private AgvConfigReader agvConfigReader;

    @InjectMocks
    private AgvMovementStateService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "timeoutMs", 1000);
    }

    @Test
    void httpHeartbeatShouldMergeJsonUpstreamDataIntoResult() {
        ReflectionTestUtils.setField(service, "controlMode", "http");
        ReflectionTestUtils.setField(service, "vehicleApiBaseUrl", "http://vehicle.example.com/prod-api///");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://vehicle.example.com/prod-api/agv/movement/heartbeat"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"data\":{\"direction\":\"forward\",\"isRunning\":true}}", MediaType.APPLICATION_JSON));

        Map<String, Object> result = service.heartbeat();

        assertEquals("http", result.get("connectionMode"));
        assertEquals("forward", result.get("direction"));
        assertEquals(true, result.get("isRunning"));
        assertTrue(result.containsKey("upstream"));
        server.verify();
    }

    @Test
    void httpForwardShouldKeepRawResponseWhenBodyIsNotJson() {
        ReflectionTestUtils.setField(service, "controlMode", "http");
        ReflectionTestUtils.setField(service, "vehicleApiBaseUrl", "http://vehicle.example.com/prod-api");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://vehicle.example.com/prod-api/agv/movement/forward"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("OK", MediaType.TEXT_PLAIN));

        Map<String, Object> result = service.forward();

        assertEquals("http", result.get("connectionMode"));
        assertEquals("OK", result.get("rawResponse"));
        server.verify();
    }

    @Test
    void tcpHeartbeatShouldParseJsonResponse() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = startOneLineTcpServer(serverSocket, "{\"connected\":true,\"direction\":\"stop\"}");
            ReflectionTestUtils.setField(service, "controlMode", "tcp");
            ReflectionTestUtils.setField(service, "heartbeatCommand", "HEARTBEAT");
            when(agvConfigReader.getHost()).thenReturn("127.0.0.1");
            when(agvConfigReader.getDrivePort()).thenReturn(serverSocket.getLocalPort());

            Map<String, Object> result = service.heartbeat();

            assertEquals("tcp", result.get("connectionMode"));
            assertEquals("stop", result.get("direction"));
            assertEquals(true, result.get("connected"));
            assertTrue(String.valueOf(result.get("rawResponse")).contains("connected"));
            serverThread.join(1000);
        }
    }

    @Test
    void tcpHeartbeatShouldFallbackToMockShapeWhenResponseIsNotJson() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            Thread serverThread = startOneLineTcpServer(serverSocket, "PONG");
            ReflectionTestUtils.setField(service, "controlMode", "tcp");
            ReflectionTestUtils.setField(service, "heartbeatCommand", "HEARTBEAT");
            when(agvConfigReader.getHost()).thenReturn("127.0.0.1");
            when(agvConfigReader.getDrivePort()).thenReturn(serverSocket.getLocalPort());

            Map<String, Object> result = service.heartbeat();

            assertEquals("tcp", result.get("connectionMode"));
            assertEquals("PONG", result.get("rawResponse"));
            assertTrue(String.valueOf(result.get("message")).contains("非 JSON"));
            serverThread.join(1000);
        }
    }

    @Test
    void privateUtilityBranchesShouldHandleNullBlankAndNormalValues() {
        Map<String, Object> blankParsed = ReflectionTestUtils.invokeMethod(service, "parsePossibleJson", (String) null);
        assertNotNull(blankParsed);
        assertTrue(blankParsed.isEmpty());

        String nullCommand = ReflectionTestUtils.invokeMethod(service, "normalizeCommand", (String) null);
        assertEquals("\n", nullCommand);

        String noSlash = ReflectionTestUtils.invokeMethod(service, "trimRightSlash", "http://host/api");
        assertEquals("http://host/api", noSlash);

        Boolean blank = ReflectionTestUtils.invokeMethod(service, "isBlank", "  ");
        Boolean notBlank = ReflectionTestUtils.invokeMethod(service, "isBlank", "x");
        assertTrue(blank);
        assertFalse(notBlank);
    }

    private Thread startOneLineTcpServer(ServerSocket serverSocket, String responseLine) {
        Thread thread = new Thread(() -> {
            try (Socket socket = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 OutputStream outputStream = socket.getOutputStream()) {
                reader.readLine();
                outputStream.write((responseLine + "\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (Exception ignored) {
                // 单元测试中只需要模拟一次车辆响应。
            }
        });
        thread.start();
        return thread;
    }
}

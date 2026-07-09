package com.example.agv.service;

import com.example.agv.entity.AgvTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class CloudUploadServiceCoverageBoostTest {

    @Mock
    private AgvConfigReader agvConfigReader;

    @InjectMocks
    private CloudUploadService service;

    @Test
    void remoteUploadShouldPostPayloadAndUseConfigApiKey() {
        ReflectionTestUtils.setField(service, "cloudUploadEnabled", true);
        ReflectionTestUtils.setField(service, "cloudUploadPath", "custom/upload");
        ReflectionTestUtils.setField(service, "defaultCloudApiKey", "default-key");
        when(agvConfigReader.getCloudUrl()).thenReturn("http://cloud.example.com/base/");
        when(agvConfigReader.getCloudApiKey()).thenReturn("config-key");

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://cloud.example.com/base/custom/upload"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "config-key"))
                .andRespond(withSuccess("OK", MediaType.TEXT_PLAIN));

        CloudUploadService.UploadResult result = service.uploadTaskData(new AgvTask(), List.of(), List.of(), List.of(), List.of());

        assertTrue(result.isRemote());
        assertTrue(result.isSuccess());
        assertEquals("http://cloud.example.com/base/custom/upload", result.getTargetUrl());
        assertEquals(200, result.getHttpStatus());
        assertEquals("OK", result.getRawResponse());
        server.verify();
    }

    @Test
    void remoteUploadShouldUseDefaultApiKeyWhenConfigKeyMissing() {
        ReflectionTestUtils.setField(service, "cloudUploadEnabled", true);
        ReflectionTestUtils.setField(service, "cloudUploadPath", "/agv/task/upload");
        ReflectionTestUtils.setField(service, "defaultCloudApiKey", "default-key");
        when(agvConfigReader.getCloudUrl()).thenReturn("http://cloud.example.com");
        when(agvConfigReader.getCloudApiKey()).thenReturn(" ");

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://cloud.example.com/agv/task/upload"))
                .andExpect(header("Authorization", "default-key"))
                .andRespond(withSuccess("OK", MediaType.TEXT_PLAIN));

        CloudUploadService.UploadResult result = service.uploadTaskData(new AgvTask(), List.of(), List.of(), List.of(), List.of());

        assertEquals("云端上传成功", result.getMessage());
        server.verify();
    }

    @Test
    void remoteUploadShouldThrowWhenCloudReturnsErrorStatus() {
        ReflectionTestUtils.setField(service, "cloudUploadEnabled", true);
        ReflectionTestUtils.setField(service, "cloudUploadPath", "/agv/task/upload");
        when(agvConfigReader.getCloudUrl()).thenReturn("http://cloud.example.com/upload");
        when(agvConfigReader.getCloudApiKey()).thenReturn(null);

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://cloud.example.com/upload"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("bad request"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.uploadTaskData(new AgvTask(), List.of(), List.of(), List.of(), List.of())
        );

        assertTrue(exception.getMessage().contains("云端上传失败"));
        server.verify();
    }

    @Test
    void buildTargetUrlShouldHandleUploadUrlsEmptyPathAndTrailingSlash() {
        ReflectionTestUtils.setField(service, "cloudUploadPath", "");
        String sameBase = ReflectionTestUtils.invokeMethod(service, "buildTargetUrl", " http://cloud.example.com/api/// ");
        assertEquals("http://cloud.example.com/api", sameBase);

        ReflectionTestUtils.setField(service, "cloudUploadPath", "another/upload");
        String explicitUpload = ReflectionTestUtils.invokeMethod(service, "buildTargetUrl", "http://cloud.example.com/api/upload?token=1");
        assertEquals("http://cloud.example.com/api/upload?token=1", explicitUpload);

        String withPath = ReflectionTestUtils.invokeMethod(service, "buildTargetUrl", "http://cloud.example.com/root/");
        assertEquals("http://cloud.example.com/root/another/upload", withPath);
    }

    @Test
    void uploadResultSettersShouldStoreManualValues() {
        CloudUploadService.UploadResult result = new CloudUploadService.UploadResult();
        result.setRemote(true);
        result.setSuccess(false);
        result.setTargetUrl("http://target");
        result.setHttpStatus(500);
        result.setMessage("失败");
        result.setRawResponse("raw");
        assertTrue(result.isRemote());
        assertFalse(result.isSuccess());
        assertEquals("http://target", result.getTargetUrl());
        assertEquals(500, result.getHttpStatus());
        assertEquals("失败", result.getMessage());
        assertEquals("raw", result.getRawResponse());
    }
}

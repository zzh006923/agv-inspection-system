package com.example.agv.service;

import com.example.agv.entity.AgvTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class CloudUploadServiceBranchCoverageTest {

    @Mock
    private AgvConfigReader agvConfigReader;

    @InjectMocks
    private CloudUploadService service;

    @Test
    void buildTargetUrlShouldCoverExistingUploadBaseNullPathAndSlashPath() {
        ReflectionTestUtils.setField(service, "cloudUploadPath", "/ignored");
        String uploadBase = ReflectionTestUtils.invokeMethod(service, "buildTargetUrl", "http://cloud.example.com/upload");
        assertEquals("http://cloud.example.com/upload", uploadBase);

        ReflectionTestUtils.setField(service, "cloudUploadPath", null);
        String nullPath = ReflectionTestUtils.invokeMethod(service, "buildTargetUrl", " http://cloud.example.com/root/// ");
        assertEquals("http://cloud.example.com/root", nullPath);

        ReflectionTestUtils.setField(service, "cloudUploadPath", "/already/slash");
        String slashPath = ReflectionTestUtils.invokeMethod(service, "buildTargetUrl", "http://cloud.example.com/root");
        assertEquals("http://cloud.example.com/root/already/slash", slashPath);
    }

    @Test
    void remoteUploadShouldSucceedWithoutAuthorizationHeaderWhenAllKeysBlank() {
        ReflectionTestUtils.setField(service, "cloudUploadEnabled", true);
        ReflectionTestUtils.setField(service, "cloudUploadPath", "/agv/task/upload");
        ReflectionTestUtils.setField(service, "defaultCloudApiKey", " ");
        when(agvConfigReader.getCloudUrl()).thenReturn("http://cloud.example.com");
        when(agvConfigReader.getCloudApiKey()).thenReturn(null);

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://cloud.example.com/agv/task/upload"))
                .andRespond(withSuccess("NO_AUTH_OK", MediaType.TEXT_PLAIN));

        CloudUploadService.UploadResult result = service.uploadTaskData(new AgvTask(), List.of(), List.of(), List.of(), List.of());

        assertTrue(result.isRemote());
        assertEquals("NO_AUTH_OK", result.getRawResponse());
        server.verify();
    }

    @Test
    void privateBlankAndTrimHelpersShouldCoverBothOutcomes() {
        Boolean nullBlank = ReflectionTestUtils.invokeMethod(service, "isBlank", (String) null);
        Boolean emptyBlank = ReflectionTestUtils.invokeMethod(service, "isBlank", "  ");
        Boolean notBlank = ReflectionTestUtils.invokeMethod(service, "isBlank", "token");
        assertTrue(nullBlank);
        assertTrue(emptyBlank);
        assertFalse(notBlank);

        String trimmed = ReflectionTestUtils.invokeMethod(service, "trimRightSlash", "http://cloud.example.com///");
        assertEquals("http://cloud.example.com", trimmed);
    }
}

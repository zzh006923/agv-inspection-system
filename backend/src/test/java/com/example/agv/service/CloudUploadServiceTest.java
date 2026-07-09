package com.example.agv.service;

import com.example.agv.entity.AgvTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudUploadServiceTest {

    @Mock
    private AgvConfigReader agvConfigReader;

    @InjectMocks
    private CloudUploadService cloudUploadService;

    @Test
    void uploadTaskDataShouldReturnLocalOnlyWhenCloudUploadDisabled() {
        ReflectionTestUtils.setField(cloudUploadService, "cloudUploadEnabled", false);

        CloudUploadService.UploadResult result = cloudUploadService.uploadTaskData(
                new AgvTask(), List.of(), List.of(), List.of(), List.of()
        );

        assertFalse(result.isRemote());
        assertTrue(result.isSuccess());
        assertEquals("未启用真实云端上传，已执行本地上传状态闭环", result.getMessage());
        assertNull(result.getTargetUrl());
    }

    @Test
    void uploadTaskDataShouldThrowWhenCloudEnabledButCloudUrlMissing() {
        ReflectionTestUtils.setField(cloudUploadService, "cloudUploadEnabled", true);
        when(agvConfigReader.getCloudUrl()).thenReturn(" ");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> cloudUploadService.uploadTaskData(new AgvTask(), List.of(), List.of(), List.of(), List.of())
        );

        assertTrue(exception.getMessage().contains("未设置云端地址"));
    }

    @Test
    void uploadResultRemoteSuccessShouldSetFields() {
        CloudUploadService.UploadResult result = CloudUploadService.UploadResult.remoteSuccess(
                "http://cloud.example.com/agv/task/upload",
                200,
                "OK"
        );

        assertTrue(result.isRemote());
        assertTrue(result.isSuccess());
        assertEquals("http://cloud.example.com/agv/task/upload", result.getTargetUrl());
        assertEquals(200, result.getHttpStatus());
        assertEquals("云端上传成功", result.getMessage());
        assertEquals("OK", result.getRawResponse());
    }
}

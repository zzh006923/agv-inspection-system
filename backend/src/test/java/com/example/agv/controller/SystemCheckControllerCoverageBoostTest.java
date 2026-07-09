package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.service.AgvConfigReader;
import com.example.agv.service.AgvMovementStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemCheckControllerCoverageBoostTest {

    @Mock private AgvConfigReader agvConfigReader;
    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private AgvMovementStateService agvMovementStateService;

    private SystemCheckController controller;

    @BeforeEach
    void setUp() {
        controller = new SystemCheckController();
        ReflectionTestUtils.setField(controller, "mockMode", false);
        ReflectionTestUtils.setField(controller, "agvConfigReader", agvConfigReader);
        ReflectionTestUtils.setField(controller, "dataSource", dataSource);
        ReflectionTestUtils.setField(controller, "agvMovementStateService", agvMovementStateService);
    }

    @Test
    void checkDbShouldPassWhenConnectionIsValidAndFailWhenInvalid() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(3)).thenReturn(true);
        AjaxResult success = controller.checkDb();
        assertEquals(200, success.getCode());

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(3)).thenReturn(false);
        AjaxResult invalid = controller.checkDb();
        assertEquals(500, invalid.getCode());
        assertEquals("数据库连接测试未通过", invalid.getMsg());
    }

    @Test
    void checkDbShouldReturnErrorWhenSQLExceptionOccurs() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("连接失败"));

        AjaxResult result = controller.checkDb();

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("数据库连接失败"));
    }

    @Test
    void checkAgvShouldReturnSuccessWhenRealConnectionWorksWithoutAnalysisPort() {
        when(agvMovementStateService.checkRealConnection()).thenReturn(Map.of("connectionMode", "mock"));
        when(agvConfigReader.getHost()).thenReturn(" ");
        when(agvConfigReader.getAnalysisPort()).thenReturn(null);

        AjaxResult result = controller.checkAgv();

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(true, data.get("passed"));
        assertEquals("AGV 真实控制接口连接正常", data.get("message"));
        assertNotNull(data.get("connection"));
    }

    @Test
    void checkAgvShouldReturnErrorWhenMovementCheckFails() {
        when(agvMovementStateService.checkRealConnection()).thenThrow(new IllegalStateException("AGV离线"));

        AjaxResult result = controller.checkAgv();

        assertEquals(500, result.getCode());
        assertEquals("AGV离线", result.getMsg());
    }

    @Test
    void checkCamShouldRejectMissingAndBlankCameraUrls() {
        when(agvConfigReader.getCameraUrls()).thenReturn(new String[0]);
        AjaxResult missing = controller.checkCam();
        assertEquals(500, missing.getCode());
        assertEquals("系统配置不完整：未设置摄像头地址", missing.getMsg());

        when(agvConfigReader.getCameraUrls()).thenReturn(new String[]{" ", null});
        AjaxResult blank = controller.checkCam();
        assertEquals(500, blank.getCode());
        assertEquals("系统配置不完整：摄像头地址为空", blank.getMsg());
    }

    @Test
    void checkCamShouldReportInvalidCameraAddress() {
        when(agvConfigReader.getCameraUrls()).thenReturn(new String[]{"not-a-valid-uri"});

        AjaxResult result = controller.checkCam();

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("摄像头检测异常"));
    }

    @Test
    void checkAllShouldMarkFailedWhenOneItemFails() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("数据库不可用"));
        when(agvMovementStateService.checkRealConnection()).thenThrow(new IllegalStateException("AGV离线"));
        when(agvConfigReader.getCameraUrls()).thenReturn(new String[0]);

        AjaxResult result = controller.checkAll();

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(false, data.get("passed"));
        assertEquals(false, data.get("mockMode"));
        assertEquals("系统自检存在异常", data.get("message"));
        assertNotNull(data.get("db"));
        assertNotNull(data.get("agv"));
        assertNotNull(data.get("cam"));
    }

    @Test
    void checkResultAccessorsShouldWork() {
        SystemCheckController.CheckResult result = new SystemCheckController.CheckResult();
        result.setItem("数据库连接");
        result.setPassed(true);
        result.setMessage("通过");
        assertEquals("数据库连接", result.getItem());
        assertTrue(result.isPassed());
        assertEquals("通过", result.getMessage());

        SystemCheckController.CheckResult constructed = new SystemCheckController.CheckResult("摄像头", false, "失败");
        assertEquals("摄像头", constructed.getItem());
        assertFalse(constructed.isPassed());
        assertEquals("失败", constructed.getMessage());
    }
}

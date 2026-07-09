package com.example.agv.controller;

import com.example.agv.entity.AgvIotActionRecord;
import com.example.agv.mapper.AgvFlawMapper;
import com.example.agv.mapper.AgvIotActionRecordMapper;
import com.example.agv.mapper.AgvSensorRecordMapper;
import com.example.agv.mapper.AgvTaskMapper;
import com.example.agv.service.AgvMovementStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgvSensorControllerBranchCoverageTest {

    @Mock
    private AgvSensorRecordMapper agvSensorRecordMapper;

    @Mock
    private AgvTaskMapper agvTaskMapper;

    @Mock
    private AgvFlawMapper agvFlawMapper;

    @Mock
    private AgvMovementStateService agvMovementStateService;

    @Mock
    private AgvIotActionRecordMapper agvIotActionRecordMapper;

    @InjectMocks
    private AgvSensorController controller;

    @Test
    void normalizeReportTypeShouldCoverAliasesAndBlankValues() {
        assertNull(ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", (String) null));
        assertNull(ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "  "));

        assertEquals("th", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "temperature_humidity"));
        assertEquals("th", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "temp_humi"));
        assertEquals("th", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "温湿度"));
        assertEquals("th", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "th"));

        assertEquals("temperature", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "temp"));
        assertEquals("temperature", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "temperature"));
        assertEquals("temperature", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "温度"));

        assertEquals("humidity", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "humi"));
        assertEquals("humidity", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "humidity"));
        assertEquals("humidity", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "湿度"));

        assertEquals("person", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "person"));
        assertEquals("person", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "people"));
        assertEquals("person", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "human"));
        assertEquals("person", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "人员"));
        assertEquals("person", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "人员检测"));

        assertEquals("light", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "light"));
        assertEquals("light", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "illumination"));
        assertEquals("light", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "光照"));
        assertEquals("light", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "照度"));

        assertEquals("smoke", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "smoke"));
        assertEquals("smoke", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "fire"));
        assertEquals("smoke", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "烟雾"));
        assertEquals("smoke", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", "烟感"));

        assertEquals("custom", ReflectionTestUtils.invokeMethod(controller, "normalizeReportType", " CUSTOM "));
    }

    @Test
    void buildTriggerInfoShouldCoverNormalAndAbnormalThresholdBranches() {
        Object normalTemp = ReflectionTestUtils.invokeMethod(controller, "buildTriggerInfo", "temperature", "30℃");
        assertEquals("正常", ReflectionTestUtils.getField(normalTemp, "status"));
        assertEquals("环境温度正常", ReflectionTestUtils.getField(normalTemp, "remark"));

        Object abnormalLight = ReflectionTestUtils.invokeMethod(controller, "buildTriggerInfo", "light", "20lux");
        assertEquals("异常", ReflectionTestUtils.getField(abnormalLight, "status"));
        assertEquals("隧道光照不足", ReflectionTestUtils.getField(abnormalLight, "flawName"));

        Object normalHumidity = ReflectionTestUtils.invokeMethod(controller, "buildTriggerInfo", "humidity", "40%");
        assertEquals("正常", ReflectionTestUtils.getField(normalHumidity, "status"));

        Object abnormalHumidity = ReflectionTestUtils.invokeMethod(controller, "buildTriggerInfo", "humidity", "80%");
        assertEquals("异常", ReflectionTestUtils.getField(abnormalHumidity, "status"));

        Object normalSmoke = ReflectionTestUtils.invokeMethod(controller, "buildTriggerInfo", "smoke", "false");
        assertEquals("正常", ReflectionTestUtils.getField(normalSmoke, "status"));

        Object abnormalSmoke = ReflectionTestUtils.invokeMethod(controller, "buildTriggerInfo", "smoke", "报警");
        assertEquals("异常", ReflectionTestUtils.getField(abnormalSmoke, "status"));
    }

    @Test
    void utilityMethodsShouldCoverNamesParsingAndTruthLikeBranches() {
        assertEquals("温度传感器", ReflectionTestUtils.invokeMethod(controller, "sensorNameOf", "temperature"));
        assertEquals("湿度传感器", ReflectionTestUtils.invokeMethod(controller, "sensorNameOf", "humidity"));
        assertEquals("人员检测传感器", ReflectionTestUtils.invokeMethod(controller, "sensorNameOf", "person"));
        assertEquals("光照传感器", ReflectionTestUtils.invokeMethod(controller, "sensorNameOf", "light"));
        assertEquals("烟雾传感器", ReflectionTestUtils.invokeMethod(controller, "sensorNameOf", "smoke"));
        assertEquals("未知传感器", ReflectionTestUtils.invokeMethod(controller, "sensorNameOf", "other"));

        assertEquals("default", ReflectionTestUtils.invokeMethod(controller, "emptyDefault", null, "default"));
        assertEquals("default", ReflectionTestUtils.invokeMethod(controller, "emptyDefault", "  ", "default"));
        assertEquals("value", ReflectionTestUtils.invokeMethod(controller, "emptyDefault", "value", "default"));

        Double nullNumber = ReflectionTestUtils.invokeMethod(controller, "parseSensorNumber", null, 9.0);
        Double blankNumber = ReflectionTestUtils.invokeMethod(controller, "parseSensorNumber", " ", 9.0);
        Double noDigits = ReflectionTestUtils.invokeMethod(controller, "parseSensorNumber", "abc", 9.0);
        Double validNumber = ReflectionTestUtils.invokeMethod(controller, "parseSensorNumber", "36.5℃", 9.0);
        Double invalidNumber = ReflectionTestUtils.invokeMethod(controller, "parseSensorNumber", "1-2-3", 9.0);
        assertEquals(9.0, nullNumber);
        assertEquals(9.0, blankNumber);
        assertEquals(9.0, noDigits);
        assertEquals(36.5, validNumber);
        assertEquals(9.0, invalidNumber);

        Boolean nullTruth = ReflectionTestUtils.invokeMethod(controller, "isTruthLike", (String) null);
        Boolean noTruth = ReflectionTestUtils.invokeMethod(controller, "isTruthLike", "no");
        assertFalse(Boolean.TRUE.equals(nullTruth));
        assertFalse(Boolean.TRUE.equals(noTruth));
        for (String truth : List.of("true", "1", "yes", "detected", "有人", "异常", "报警")) {
            Boolean truthResult = ReflectionTestUtils.invokeMethod(controller, "isTruthLike", truth);
            assertTrue(Boolean.TRUE.equals(truthResult));
        }
    }

    @Test
    void buildActionsFromSensorShouldCoverEverySensorTypeAndUnknownType() {
        List<?> personActions = ReflectionTestUtils.invokeMethod(controller, "buildActionsFromSensor", 1L, 10L, 20L, "person");
        List<?> lightActions = ReflectionTestUtils.invokeMethod(controller, "buildActionsFromSensor", 1L, 10L, 20L, "light");
        List<?> smokeActions = ReflectionTestUtils.invokeMethod(controller, "buildActionsFromSensor", 1L, 10L, 20L, "smoke");
        List<?> humidityActions = ReflectionTestUtils.invokeMethod(controller, "buildActionsFromSensor", 1L, 10L, 20L, "humidity");
        List<?> temperatureActions = ReflectionTestUtils.invokeMethod(controller, "buildActionsFromSensor", 1L, 10L, 20L, "temperature");
        List<?> unknownActions = ReflectionTestUtils.invokeMethod(controller, "buildActionsFromSensor", 1L, 10L, 20L, "unknown");

        assertEquals(2, personActions.size());
        assertEquals(1, lightActions.size());
        assertEquals(2, smokeActions.size());
        assertEquals(1, humidityActions.size());
        assertEquals(1, temperatureActions.size());
        assertTrue(unknownActions.isEmpty());
        verify(agvMovementStateService, times(2)).stop();
        verify(agvIotActionRecordMapper, times(7)).insert(any(AgvIotActionRecord.class));
    }
}

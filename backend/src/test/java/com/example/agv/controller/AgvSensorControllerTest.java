package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.dto.SensorReportDTO;
import com.example.agv.entity.AgvSensorRecord;
import com.example.agv.entity.AgvTask;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgvSensorControllerTest {

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
    private AgvSensorController agvSensorController;


    @Test
    void listSensorRecordShouldReturnRecords() {
        AgvSensorRecord record = new AgvSensorRecord();
        record.setTaskId(1L);
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of(record));

        AjaxResult result = agvSensorController.listSensorRecord(1L);

        assertEquals(200, result.getCode());
        assertEquals(1, ((List<?>) result.getData()).size());
    }

    @Test
    void getSensorStatusShouldKeepLatestPerTypeAndCountAbnormal() {
        AgvSensorRecord smoke = new AgvSensorRecord();
        smoke.setSensorType("smoke");
        smoke.setStatus("异常");
        AgvSensorRecord light = new AgvSensorRecord();
        light.setSensorType("light");
        light.setStatus("正常");
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of(smoke, light));

        AjaxResult result = agvSensorController.getSensorStatus(1L);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertSame(smoke, data.get("smoke"));
        assertSame(light, data.get("light"));
        assertEquals(1, data.get("abnormalCount"));
    }

    @Test
    void addSensorRecordShouldRejectMissingTaskId() {
        AgvSensorRecord record = new AgvSensorRecord();
        record.setSensorType("light");

        AjaxResult result = agvSensorController.addSensorRecord(record);

        assertEquals(500, result.getCode());
        assertEquals("任务ID不能为空", result.getMsg());
        verify(agvSensorRecordMapper, never()).insert(any());
    }

    @Test
    void addSensorRecordShouldSetNormalStatusAndInsert() {
        AgvSensorRecord record = new AgvSensorRecord();
        record.setTaskId(1L);
        record.setSensorType("light");

        AjaxResult result = agvSensorController.addSensorRecord(record);

        assertEquals(200, result.getCode());
        assertEquals("正常", record.getStatus());
        assertEquals(0, record.getDeleteFlag());
        assertNotNull(record.getCreateTime());
        verify(agvSensorRecordMapper).insert(record);
    }

    @Test
    void reportSensorShouldRejectWhenNoRunningTaskCanBeResolved() {
        SensorReportDTO dto = new SensorReportDTO();
        dto.setSensorType("temperature");
        dto.setTemperature(new BigDecimal("30"));
        when(agvTaskMapper.selectOne(any())).thenReturn(null);

        AjaxResult result = agvSensorController.reportSensor(dto);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("未找到当前巡视中任务"));
    }

    @Test
    void reportTemperatureHumidityShouldWriteTwoNormalSensorRecords() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("connectionMode", "mock", "direction", "stop"));
        SensorReportDTO dto = new SensorReportDTO();
        dto.setTaskId(1L);
        dto.setSensorType("temperature_humidity");
        dto.setTemperature(new BigDecimal("28.5"));
        dto.setHumidity(new BigDecimal("60"));
        dto.setDistance(3.5);

        AjaxResult result = agvSensorController.reportSensor(dto);

        assertEquals(200, result.getCode());
        assertEquals(2, ((List<?>) result.getData()).size());
        verify(agvSensorRecordMapper, times(2)).insert(any());
        verify(agvFlawMapper, never()).insert(any());
        verify(agvIotActionRecordMapper, never()).insert(any());
    }

    @Test
    void triggerSensorEventShouldCreateFlawAndActionForLowLight() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("connectionMode", "mock", "direction", "stop"));
        AgvSensorController.SensorTriggerRequest request = new AgvSensorController.SensorTriggerRequest();
        request.setTaskId(1L);
        request.setSensorType("light");
        request.setSensorValue("20lux");
        request.setDistance(4.0);

        AjaxResult result = agvSensorController.triggerSensorEvent(request);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        AgvSensorRecord sensorRecord = (AgvSensorRecord) data.get("sensorRecord");
        assertEquals("光照传感器", sensorRecord.getSensorName());
        assertEquals("异常", sensorRecord.getStatus());
        assertEquals("提示开启补光并生成照明异常", data.get("message"));
        assertEquals(1, ((List<?>) data.get("actionRecords")).size());
        verify(agvSensorRecordMapper).insert(any());
        verify(agvFlawMapper).insert(any());
        verify(agvIotActionRecordMapper).insert(any());
    }

    @Test
    void triggerSensorEventShouldAcceptChinesePersonTypeAndStopAgv() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("connectionMode", "mock", "direction", "stop"));
        when(agvMovementStateService.stop()).thenReturn(Map.of("direction", "stop"));
        AgvSensorController.SensorTriggerRequest request = new AgvSensorController.SensorTriggerRequest();
        request.setTaskId(1L);
        request.setSensorType("人员检测");
        request.setSensorValue("有人");

        AjaxResult result = agvSensorController.triggerSensorEvent(request);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(2, ((List<?>) data.get("actionRecords")).size());
        verify(agvMovementStateService).stop();
        verify(agvIotActionRecordMapper, times(2)).insert(any());
    }
}

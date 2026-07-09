package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.dto.SensorReportDTO;
import com.example.agv.entity.AgvFlaw;
import com.example.agv.entity.AgvIotActionRecord;
import com.example.agv.entity.AgvSensorRecord;
import com.example.agv.mapper.AgvFlawMapper;
import com.example.agv.mapper.AgvIotActionRecordMapper;
import com.example.agv.mapper.AgvSensorRecordMapper;
import com.example.agv.mapper.AgvTaskMapper;
import com.example.agv.service.AgvMovementStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class AgvSensorControllerCoverageBoostTest {

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
    void reportSensorShouldRejectNullBodyAndMissingSensorType() {
        AjaxResult nullResult = controller.reportSensor(null);
        assertEquals(500, nullResult.getCode());
        assertEquals("上报数据不能为空", nullResult.getMsg());

        SensorReportDTO dto = new SensorReportDTO();
        dto.setTaskId(1L);
        AjaxResult missingType = controller.reportSensor(dto);
        assertEquals(500, missingType.getCode());
        assertEquals("传感器类型不能为空", missingType.getMsg());
        verifyNoInteractions(agvSensorRecordMapper, agvFlawMapper, agvIotActionRecordMapper);
    }

    @Test
    void reportSensorShouldRejectMissingRequiredValueForSingleTypeSensors() {
        SensorReportDTO temperature = new SensorReportDTO();
        temperature.setTaskId(1L);
        temperature.setSensorType("temperature");
        assertEquals("温度传感器缺少 temperature 数据", controller.reportSensor(temperature).getMsg());

        SensorReportDTO humidity = new SensorReportDTO();
        humidity.setTaskId(1L);
        humidity.setSensorType("humidity");
        assertEquals("湿度传感器缺少 humidity 数据", controller.reportSensor(humidity).getMsg());

        SensorReportDTO light = new SensorReportDTO();
        light.setTaskId(1L);
        light.setSensorType("light");
        assertEquals("光照传感器缺少 lightValue 数据", controller.reportSensor(light).getMsg());

        SensorReportDTO th = new SensorReportDTO();
        th.setTaskId(1L);
        th.setSensorType("th");
        assertEquals("温湿度传感器缺少 temperature 或 humidity 数据", controller.reportSensor(th).getMsg());
    }

    @Test
    void reportTemperatureShouldCreateAbnormalFlawAndFanAction() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        SensorReportDTO dto = new SensorReportDTO();
        dto.setTaskId(1L);
        dto.setSensorType("温度");
        dto.setTemperature(new BigDecimal("36.5"));
        dto.setDistance(10.0);

        AjaxResult result = controller.reportSensor(dto);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        AgvSensorRecord sensorRecord = (AgvSensorRecord) data.get("sensorRecord");
        AgvFlaw flaw = (AgvFlaw) data.get("flaw");
        assertEquals("temperature", sensorRecord.getSensorType());
        assertEquals("异常", sensorRecord.getStatus());
        assertEquals("隧道温度异常", flaw.getFlawName());
        assertEquals(1, ((List<?>) data.get("actionRecords")).size());
        verify(agvIotActionRecordMapper).insert(any(AgvIotActionRecord.class));
    }

    @Test
    void reportHumidityShouldCreateAbnormalFlawAndPowerAction() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        SensorReportDTO dto = new SensorReportDTO();
        dto.setTaskId(1L);
        dto.setSensorType("湿度");
        dto.setHumidity(new BigDecimal("88"));

        AjaxResult result = controller.reportSensor(dto);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        AgvFlaw flaw = (AgvFlaw) data.get("flaw");
        assertEquals("隧道湿度异常", flaw.getFlawName());
        assertEquals(1, ((List<?>) data.get("actionRecords")).size());
        verify(agvIotActionRecordMapper).insert(any());
    }

    @Test
    void reportLightNormalShouldOnlyInsertNormalSensorRecord() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        SensorReportDTO dto = new SensorReportDTO();
        dto.setTaskId(1L);
        dto.setSensorType("illumination");
        dto.setLightValue(120.0);

        AjaxResult result = controller.reportSensor(dto);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        AgvSensorRecord sensorRecord = (AgvSensorRecord) data.get("sensorRecord");
        assertEquals("光照传感器", sensorRecord.getSensorName());
        assertEquals("正常", sensorRecord.getStatus());
        assertNull(data.get("flaw"));
        verify(agvSensorRecordMapper).insert(sensorRecord);
        verify(agvFlawMapper, never()).insert(any());
        verify(agvIotActionRecordMapper, never()).insert(any());
    }

    @Test
    void reportSmokeShouldHandleNormalAndHighValueBranches() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        when(agvMovementStateService.stop()).thenReturn(Map.of("direction", "stop"));

        SensorReportDTO normal = new SensorReportDTO();
        normal.setTaskId(1L);
        normal.setSensorType("smoke");
        normal.setSmokeDetected(false);
        normal.setSmokeValue(10.0);
        AjaxResult normalResult = controller.reportSensor(normal);
        assertEquals(200, normalResult.getCode());
        assertNull(((Map<?, ?>) normalResult.getData()).get("flaw"));

        SensorReportDTO high = new SensorReportDTO();
        high.setTaskId(1L);
        high.setSensorType("fire");
        high.setSmokeValue(60.0);
        AjaxResult highResult = controller.reportSensor(high);
        assertEquals(200, highResult.getCode());
        Map<?, ?> data = (Map<?, ?>) highResult.getData();
        assertEquals("触发声光报警并建议终止巡检", data.get("message"));
        assertEquals(2, ((List<?>) data.get("actionRecords")).size());
        verify(agvMovementStateService).stop();
        verify(agvIotActionRecordMapper, times(2)).insert(any());
    }

    @Test
    void reportPersonShouldHandleDetectedAndNotDetectedBranches() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        when(agvMovementStateService.stop()).thenReturn(Map.of("direction", "stop"));

        SensorReportDTO none = new SensorReportDTO();
        none.setTaskId(1L);
        none.setSensorType("human");
        none.setPersonDetected(false);
        AjaxResult normalResult = controller.reportSensor(none);
        assertEquals(200, normalResult.getCode());
        assertNull(((Map<?, ?>) normalResult.getData()).get("flaw"));

        SensorReportDTO detected = new SensorReportDTO();
        detected.setTaskId(1L);
        detected.setSensorType("person");
        detected.setPersonDetected(true);
        AjaxResult abnormalResult = controller.reportSensor(detected);
        assertEquals(200, abnormalResult.getCode());
        Map<?, ?> data = (Map<?, ?>) abnormalResult.getData();
        assertEquals("自动停车并生成安全事件", data.get("message"));
        assertEquals(2, ((List<?>) data.get("actionRecords")).size());
        verify(agvMovementStateService).stop();
    }

    @Test
    void triggerSensorEventShouldValidateRequestAndType() {
        assertEquals("请求体不能为空", controller.triggerSensorEvent(null).getMsg());

        AgvSensorController.SensorTriggerRequest missingTask = new AgvSensorController.SensorTriggerRequest();
        missingTask.setSensorType("light");
        assertEquals("任务ID不能为空", controller.triggerSensorEvent(missingTask).getMsg());

        AgvSensorController.SensorTriggerRequest missingType = new AgvSensorController.SensorTriggerRequest();
        missingType.setTaskId(1L);
        assertEquals("传感器类型不能为空", controller.triggerSensorEvent(missingType).getMsg());
    }

    @Test
    void triggerSensorEventShouldWriteNormalUnknownSensorWithoutFlaw() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        AgvSensorController.SensorTriggerRequest request = new AgvSensorController.SensorTriggerRequest();
        request.setTaskId(1L);
        request.setSensorType("unknown-custom");
        request.setSensorValue("abc");

        AjaxResult result = controller.triggerSensorEvent(request);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        AgvSensorRecord sensorRecord = (AgvSensorRecord) data.get("sensorRecord");
        assertEquals("未知传感器", sensorRecord.getSensorName());
        assertEquals("正常", sensorRecord.getStatus());
        assertNull(data.get("flaw"));
    }

    @Test
    void safeHeartbeatFailureShouldBeReturnedInResponseData() {
        when(agvMovementStateService.heartbeat()).thenThrow(new IllegalStateException("车辆离线"));
        SensorReportDTO dto = new SensorReportDTO();
        dto.setTaskId(1L);
        dto.setSensorType("light");
        dto.setLightValue(100.0);

        AjaxResult result = controller.reportSensor(dto);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertTrue(String.valueOf(data.get("movementStatus")).contains("小车状态读取失败"));
    }

    @Test
    void addSensorRecordShouldRejectMissingTypeAndKeepExistingStatus() {
        AgvSensorRecord missingType = new AgvSensorRecord();
        missingType.setTaskId(1L);
        assertEquals("传感器类型不能为空", controller.addSensorRecord(missingType).getMsg());

        AgvSensorRecord existing = new AgvSensorRecord();
        existing.setTaskId(1L);
        existing.setSensorType("temperature");
        existing.setStatus("异常");
        existing.setDeleteFlag(9);
        AjaxResult result = controller.addSensorRecord(existing);
        assertEquals(200, result.getCode());
        assertEquals("异常", existing.getStatus());
        assertEquals(9, existing.getDeleteFlag());
        verify(agvSensorRecordMapper).insert(existing);
    }

    @Test
    void reportShouldResolveRunningTaskWhenTaskIdIsMissing() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        com.example.agv.entity.AgvTask runningTask = new com.example.agv.entity.AgvTask();
        runningTask.setId(99L);
        when(agvTaskMapper.selectOne(any())).thenReturn(runningTask);
        SensorReportDTO dto = new SensorReportDTO();
        dto.setSensorType("light");
        dto.setLightValue(100.0);

        AjaxResult result = controller.reportSensor(dto);

        assertEquals(200, result.getCode());
        ArgumentCaptor<AgvSensorRecord> captor = ArgumentCaptor.forClass(AgvSensorRecord.class);
        verify(agvSensorRecordMapper).insert(captor.capture());
        assertEquals(99L, captor.getValue().getTaskId());
    }
}

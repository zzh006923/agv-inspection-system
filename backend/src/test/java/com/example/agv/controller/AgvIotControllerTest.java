package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.entity.AgvIotActionRecord;
import com.example.agv.mapper.AgvFlawMapper;
import com.example.agv.mapper.AgvIotActionRecordMapper;
import com.example.agv.mapper.AgvSensorRecordMapper;
import com.example.agv.service.AgvMovementStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgvIotControllerTest {

    @Mock
    private AgvSensorRecordMapper agvSensorRecordMapper;

    @Mock
    private AgvFlawMapper agvFlawMapper;

    @Mock
    private AgvIotActionRecordMapper agvIotActionRecordMapper;

    @Mock
    private AgvMovementStateService agvMovementStateService;

    @InjectMocks
    private AgvIotController agvIotController;


    @Test
    void capabilityShouldReturnSupportedModules() {
        AjaxResult result = agvIotController.capability();

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertTrue(((List<?>) data.get("sensorModules")).size() >= 4);
        assertTrue(((List<?>) data.get("actuatorModules")).contains("声光报警器：alarm"));
    }

    @Test
    void overviewShouldReturnRecordsMovementAndSummary() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("connectionMode", "mock", "direction", "stop"));
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of());
        when(agvIotActionRecordMapper.selectList(any())).thenReturn(List.of());

        AjaxResult result = agvIotController.overview(1L);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertNotNull(data.get("sensorRecords"));
        assertNotNull(data.get("actionRecords"));
        assertNotNull(data.get("movementStatus"));
        assertNotNull(data.get("summary"));
    }

    @Test
    void controlDeviceShouldRejectMissingDeviceType() {
        AgvIotController.DeviceControlRequest request = new AgvIotController.DeviceControlRequest();
        request.setTaskId(1L);
        request.setAction("开启");

        AjaxResult result = agvIotController.controlDevice(request);

        assertEquals(500, result.getCode());
        assertEquals("设备类型不能为空", result.getMsg());
        verify(agvIotActionRecordMapper, never()).insert(any());
    }

    @Test
    void controlDeviceShouldStopAgvAndInsertActionRecord() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("connectionMode", "mock", "direction", "stop"));
        when(agvMovementStateService.stop()).thenReturn(Map.of("direction", "stop"));
        AgvIotController.DeviceControlRequest request = new AgvIotController.DeviceControlRequest();
        request.setTaskId(1L);
        request.setDeviceType("agv");
        request.setAction("安全停车");
        request.setCommandText("执行停车");

        AjaxResult result = agvIotController.controlDevice(request);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        AgvIotActionRecord record = (AgvIotActionRecord) data.get("actionRecord");
        assertEquals("AGV巡线车", record.getDeviceName());
        assertEquals("安全停车", record.getAction());
        assertEquals("已停车", record.getAfterStatus());
        verify(agvMovementStateService).stop();
        verify(agvIotActionRecordMapper).insert(record);
    }

    @Test
    void triggerSceneShouldRejectUnknownSceneType() {
        AgvIotController.SceneRequest request = new AgvIotController.SceneRequest();
        request.setTaskId(1L);
        request.setSceneType("unknown");

        AjaxResult result = agvIotController.triggerScene(request);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("未知场景类型"));
    }

    @Test
    void triggerFireSceneShouldCreateSensorFlawAndActions() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("connectionMode", "mock", "direction", "stop"));
        when(agvMovementStateService.stop()).thenReturn(Map.of("direction", "stop"));
        AgvIotController.SceneRequest request = new AgvIotController.SceneRequest();
        request.setTaskId(1L);
        request.setSceneType("fire");
        request.setDistance(5.0);

        AjaxResult result = agvIotController.triggerScene(request);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals("烟雾安全报警模式", data.get("sceneName"));
        assertEquals(2, ((List<?>) data.get("actionRecords")).size());
        verify(agvSensorRecordMapper).insert(any());
        verify(agvFlawMapper).insert(any());
        verify(agvIotActionRecordMapper, times(2)).insert(any());
        verify(agvMovementStateService).stop();
    }

    @Test
    void voiceCommandShouldMapOpenLightToActionRecord() {
        AgvIotController.VoiceCommandRequest request = new AgvIotController.VoiceCommandRequest();
        request.setTaskId(1L);
        request.setCommand("打开补光灯");

        AjaxResult result = agvIotController.voiceCommand(request);

        assertEquals(200, result.getCode());
        AgvIotActionRecord record = (AgvIotActionRecord) result.getData();
        assertEquals("voice", record.getTriggerType());
        assertEquals("light", record.getDeviceType());
        assertEquals("开启", record.getAction());
        verify(agvIotActionRecordMapper).insert(record);
    }

    @Test
    void voiceCommandShouldRejectUnknownCommand() {
        AgvIotController.VoiceCommandRequest request = new AgvIotController.VoiceCommandRequest();
        request.setTaskId(1L);
        request.setCommand("唱首歌");

        AjaxResult result = agvIotController.voiceCommand(request);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("未识别语音指令"));
    }
}

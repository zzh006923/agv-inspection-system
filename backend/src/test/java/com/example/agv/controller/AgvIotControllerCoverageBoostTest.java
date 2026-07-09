package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.entity.AgvIotActionRecord;
import com.example.agv.entity.AgvSensorRecord;
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
class AgvIotControllerCoverageBoostTest {

    @Mock
    private AgvSensorRecordMapper agvSensorRecordMapper;

    @Mock
    private AgvFlawMapper agvFlawMapper;

    @Mock
    private AgvIotActionRecordMapper agvIotActionRecordMapper;

    @Mock
    private AgvMovementStateService agvMovementStateService;

    @InjectMocks
    private AgvIotController controller;

    @Test
    void controlDeviceShouldValidateTaskIdAndAction() {
        AgvIotController.DeviceControlRequest noTask = new AgvIotController.DeviceControlRequest();
        noTask.setDeviceType("light");
        noTask.setAction("开启");
        assertEquals("任务ID不能为空", controller.controlDevice(noTask).getMsg());

        AgvIotController.DeviceControlRequest noAction = new AgvIotController.DeviceControlRequest();
        noAction.setTaskId(1L);
        noAction.setDeviceType("light");
        assertEquals("控制动作不能为空", controller.controlDevice(noAction).getMsg());
        verify(agvIotActionRecordMapper, never()).insert(any());
    }

    @Test
    void controlDeviceShouldControlLightWithoutStoppingAgv() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        AgvIotController.DeviceControlRequest request = new AgvIotController.DeviceControlRequest();
        request.setTaskId(1L);
        request.setDeviceType("light");
        request.setAction("开启");
        request.setSceneName("手动补光");
        request.setCommandText("打开补光灯");

        AjaxResult result = controller.controlDevice(request);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        AgvIotActionRecord record = (AgvIotActionRecord) data.get("actionRecord");
        assertEquals("AGV补光灯", record.getDeviceName());
        assertEquals("关闭", record.getBeforeStatus());
        assertEquals("已开启", record.getAfterStatus());
        verify(agvMovementStateService, never()).stop();
    }

    @Test
    void triggerSceneShouldRejectMissingTaskId() {
        AgvIotController.SceneRequest request = new AgvIotController.SceneRequest();
        request.setSceneType("safety");

        AjaxResult result = controller.triggerScene(request);

        assertEquals(500, result.getCode());
        assertEquals("任务ID不能为空", result.getMsg());
        verifyNoInteractions(agvSensorRecordMapper, agvFlawMapper, agvIotActionRecordMapper);
    }

    @Test
    void triggerSafetySceneShouldStopAgvAndCreateTwoActions() {
        when(agvMovementStateService.stop()).thenReturn(Map.of("direction", "stop"));
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        AgvIotController.SceneRequest request = new AgvIotController.SceneRequest();
        request.setTaskId(1L);
        request.setSceneType("safety");
        request.setDistance(2.5);

        AjaxResult result = controller.triggerScene(request);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals("隧道安全巡检模式", data.get("sceneName"));
        assertEquals(2, ((List<?>) data.get("actionRecords")).size());
        verify(agvMovementStateService).stop();
        verify(agvSensorRecordMapper).insert(any());
        verify(agvFlawMapper).insert(any());
        verify(agvIotActionRecordMapper, times(2)).insert(any());
    }

    @Test
    void triggerEnvironmentAndLightingScenesShouldCreateExpectedActions() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));

        AgvIotController.SceneRequest environment = new AgvIotController.SceneRequest();
        environment.setTaskId(1L);
        environment.setSceneType("environment");
        AjaxResult environmentResult = controller.triggerScene(environment);
        assertEquals(200, environmentResult.getCode());
        assertEquals("隧道环境监测模式", ((Map<?, ?>) environmentResult.getData()).get("sceneName"));

        AgvIotController.SceneRequest lighting = new AgvIotController.SceneRequest();
        lighting.setTaskId(2L);
        lighting.setSceneType("lighting");
        AjaxResult lightingResult = controller.triggerScene(lighting);
        assertEquals(200, lightingResult.getCode());
        assertEquals("隧道补光巡检模式", ((Map<?, ?>) lightingResult.getData()).get("sceneName"));

        verify(agvIotActionRecordMapper, times(2)).insert(any());
        verify(agvMovementStateService, never()).stop();
    }

    @Test
    void voiceCommandShouldValidateTaskIdAndCommand() {
        AgvIotController.VoiceCommandRequest noTask = new AgvIotController.VoiceCommandRequest();
        noTask.setCommand("查询状态");
        assertEquals("任务ID不能为空", controller.voiceCommand(noTask).getMsg());

        AgvIotController.VoiceCommandRequest noCommand = new AgvIotController.VoiceCommandRequest();
        noCommand.setTaskId(1L);
        assertEquals("语音指令不能为空", controller.voiceCommand(noCommand).getMsg());
    }

    @Test
    void voiceCommandShouldMapQueryToOverview() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of(new AgvSensorRecord()));
        when(agvIotActionRecordMapper.selectList(any())).thenReturn(List.of(new AgvIotActionRecord()));
        AgvIotController.VoiceCommandRequest request = new AgvIotController.VoiceCommandRequest();
        request.setTaskId(1L);
        request.setCommand("查询当前环境状态");

        AjaxResult result = controller.voiceCommand(request);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals("状态查询", data.get("intent"));
        assertNotNull(data.get("overview"));
    }

    @Test
    void voiceCommandShouldMapSafetyAndFireScenes() {
        when(agvMovementStateService.stop()).thenReturn(Map.of("direction", "stop"));
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));

        AgvIotController.VoiceCommandRequest safety = new AgvIotController.VoiceCommandRequest();
        safety.setTaskId(1L);
        safety.setCommand("进入人员安全保护模式");
        assertEquals(200, controller.voiceCommand(safety).getCode());

        AgvIotController.VoiceCommandRequest fire = new AgvIotController.VoiceCommandRequest();
        fire.setTaskId(1L);
        fire.setCommand("进入烟雾报警模式");
        assertEquals(200, controller.voiceCommand(fire).getCode());

        verify(agvMovementStateService, times(2)).stop();
        verify(agvIotActionRecordMapper, times(4)).insert(any());
    }

    @Test
    void voiceCommandShouldMapCloseLightPowerRestoreAndCloseAll() {
        AgvIotController.VoiceCommandRequest closeLight = new AgvIotController.VoiceCommandRequest();
        closeLight.setTaskId(1L);
        closeLight.setCommand("关闭灯");
        AgvIotActionRecord closeLightRecord = (AgvIotActionRecord) controller.voiceCommand(closeLight).getData();
        assertEquals("关闭", closeLightRecord.getAction());
        assertEquals("已开启", closeLightRecord.getBeforeStatus());
        assertEquals("已关闭", closeLightRecord.getAfterStatus());

        AgvIotController.VoiceCommandRequest powerOff = new AgvIotController.VoiceCommandRequest();
        powerOff.setTaskId(1L);
        powerOff.setCommand("远程断电");
        AgvIotActionRecord powerOffRecord = (AgvIotActionRecord) controller.voiceCommand(powerOff).getData();
        assertEquals("power", powerOffRecord.getDeviceType());
        assertEquals("开启", powerOffRecord.getAction());

        AgvIotController.VoiceCommandRequest restore = new AgvIotController.VoiceCommandRequest();
        restore.setTaskId(1L);
        restore.setCommand("恢复供电");
        AgvIotActionRecord restoreRecord = (AgvIotActionRecord) controller.voiceCommand(restore).getData();
        assertEquals("关闭", restoreRecord.getAction());

        AgvIotController.VoiceCommandRequest closeAll = new AgvIotController.VoiceCommandRequest();
        closeAll.setTaskId(1L);
        closeAll.setCommand("关闭所有设备");
        AjaxResult closeAllResult = controller.voiceCommand(closeAll);
        assertEquals(200, closeAllResult.getCode());
        assertEquals(3, ((List<?>) closeAllResult.getData()).size());
    }

    @Test
    void voiceCommandShouldMapEmergencyStop() {
        when(agvMovementStateService.stop()).thenReturn(Map.of("direction", "stop"));
        AgvIotController.VoiceCommandRequest request = new AgvIotController.VoiceCommandRequest();
        request.setTaskId(1L);
        request.setCommand("执行停车");

        AjaxResult result = controller.voiceCommand(request);

        assertEquals(200, result.getCode());
        AgvIotActionRecord record = (AgvIotActionRecord) result.getData();
        assertEquals("agv", record.getDeviceType());
        assertEquals("安全停车", record.getAction());
        verify(agvMovementStateService).stop();
    }

    @Test
    void requestDtoAccessorsShouldStoreValues() {
        AgvIotController.DeviceControlRequest device = new AgvIotController.DeviceControlRequest();
        device.setTaskId(1L);
        device.setDeviceType("alarm");
        device.setAction("报警");
        device.setCommandText("触发声光报警");
        device.setSceneName("演示场景");
        assertEquals(1L, device.getTaskId());
        assertEquals("alarm", device.getDeviceType());
        assertEquals("报警", device.getAction());
        assertEquals("触发声光报警", device.getCommandText());
        assertEquals("演示场景", device.getSceneName());

        AgvIotController.SceneRequest scene = new AgvIotController.SceneRequest();
        scene.setTaskId(2L);
        scene.setSceneType("fire");
        scene.setDistance(3.5);
        assertEquals(2L, scene.getTaskId());
        assertEquals("fire", scene.getSceneType());
        assertEquals(3.5, scene.getDistance());

        AgvIotController.VoiceCommandRequest voice = new AgvIotController.VoiceCommandRequest();
        voice.setTaskId(3L);
        voice.setCommand("打开补光灯");
        voice.setDistance(4.5);
        assertEquals(3L, voice.getTaskId());
        assertEquals("打开补光灯", voice.getCommand());
        assertEquals(4.5, voice.getDistance());
    }
}

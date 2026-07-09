package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.entity.AgvFlaw;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgvIotControllerBranchCoverageTest {

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
    void controlDeviceShouldRejectMissingDeviceTypeAndStopAgvWhenAgvParking() {
        AgvIotController.DeviceControlRequest noDevice = new AgvIotController.DeviceControlRequest();
        noDevice.setTaskId(1L);
        noDevice.setAction("开启");
        assertEquals("设备类型不能为空", controller.controlDevice(noDevice).getMsg());

        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        AgvIotController.DeviceControlRequest agvStop = new AgvIotController.DeviceControlRequest();
        agvStop.setTaskId(1L);
        agvStop.setDeviceType("agv");
        agvStop.setAction("安全停车");
        AjaxResult result = controller.controlDevice(agvStop);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        AgvIotActionRecord record = (AgvIotActionRecord) data.get("actionRecord");
        assertEquals("AGV已执行安全停车", record.getFeedback());
        verify(agvMovementStateService).stop();
    }

    @Test
    void triggerSceneShouldUseDefaultSafetySceneAndRejectUnknownScene() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        AgvIotController.SceneRequest defaultSafety = new AgvIotController.SceneRequest();
        defaultSafety.setTaskId(1L);
        AjaxResult safetyResult = controller.triggerScene(defaultSafety);
        assertEquals(200, safetyResult.getCode());
        assertEquals("隧道安全巡检模式", ((Map<?, ?>) safetyResult.getData()).get("sceneName"));

        AgvIotController.SceneRequest unknown = new AgvIotController.SceneRequest();
        unknown.setTaskId(2L);
        unknown.setSceneType("unknown");
        AjaxResult unknownResult = controller.triggerScene(unknown);
        assertEquals(500, unknownResult.getCode());
        assertTrue(unknownResult.getMsg().contains("未知场景类型"));
    }

    @Test
    void triggerFireSceneShouldCreateSmokeFlawAndTwoActions() {
        when(agvMovementStateService.heartbeat()).thenReturn(Map.of("direction", "stop"));
        AgvIotController.SceneRequest fire = new AgvIotController.SceneRequest();
        fire.setTaskId(1L);
        fire.setSceneType("fire");
        fire.setDistance(null);

        AjaxResult result = controller.triggerScene(fire);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals("烟雾安全报警模式", data.get("sceneName"));
        assertEquals(2, ((List<?>) data.get("actionRecords")).size());
        verify(agvMovementStateService).stop();
        verify(agvFlawMapper).insert(any(AgvFlaw.class));
        verify(agvIotActionRecordMapper, times(2)).insert(any(AgvIotActionRecord.class));
    }

    @Test
    void voiceCommandShouldCoverOpenLightAndUnrecognizedBranches() {
        AgvIotController.VoiceCommandRequest openLight = new AgvIotController.VoiceCommandRequest();
        openLight.setTaskId(1L);
        openLight.setCommand("打开补光灯");
        AgvIotActionRecord openRecord = (AgvIotActionRecord) controller.voiceCommand(openLight).getData();
        assertEquals("light", openRecord.getDeviceType());
        assertEquals("开启", openRecord.getAction());

        AgvIotController.VoiceCommandRequest unknown = new AgvIotController.VoiceCommandRequest();
        unknown.setTaskId(1L);
        unknown.setCommand("随便说一句");
        AjaxResult unknownResult = controller.voiceCommand(unknown);
        assertEquals(500, unknownResult.getCode());
        assertTrue(unknownResult.getMsg().contains("未识别语音指令"));
    }

    @Test
    void privateDefaultStatusAndNameBranchesShouldReturnExpectedValues() {
        assertEquals("未知", ReflectionTestUtils.invokeMethod(controller, "defaultBeforeStatus", (String) null));
        assertEquals("关闭", ReflectionTestUtils.invokeMethod(controller, "defaultBeforeStatus", "开启"));
        assertEquals("关闭", ReflectionTestUtils.invokeMethod(controller, "defaultBeforeStatus", "报警"));
        assertEquals("供电中", ReflectionTestUtils.invokeMethod(controller, "defaultBeforeStatus", "断电保护"));
        assertEquals("运行中", ReflectionTestUtils.invokeMethod(controller, "defaultBeforeStatus", "安全停车"));
        assertEquals("已开启", ReflectionTestUtils.invokeMethod(controller, "defaultBeforeStatus", "关闭"));
        assertEquals("已断电", ReflectionTestUtils.invokeMethod(controller, "defaultBeforeStatus", "恢复供电"));
        assertEquals("未知", ReflectionTestUtils.invokeMethod(controller, "defaultBeforeStatus", "自定义动作"));

        assertEquals("未知", ReflectionTestUtils.invokeMethod(controller, "defaultAfterStatus", (String) null));
        assertEquals("已停车", ReflectionTestUtils.invokeMethod(controller, "defaultAfterStatus", "安全停车"));
        assertEquals("报警中", ReflectionTestUtils.invokeMethod(controller, "defaultAfterStatus", "报警"));
        assertEquals("已断电", ReflectionTestUtils.invokeMethod(controller, "defaultAfterStatus", "断电保护"));
        assertEquals("已开启", ReflectionTestUtils.invokeMethod(controller, "defaultAfterStatus", "开启"));
        assertEquals("已关闭", ReflectionTestUtils.invokeMethod(controller, "defaultAfterStatus", "关闭"));
        assertEquals("供电中", ReflectionTestUtils.invokeMethod(controller, "defaultAfterStatus", "恢复供电"));
        assertEquals("已执行", ReflectionTestUtils.invokeMethod(controller, "defaultAfterStatus", "自定义动作"));

        assertEquals("温度传感器", ReflectionTestUtils.invokeMethod(controller, "defaultSensorName", "temperature"));
        assertEquals("湿度传感器", ReflectionTestUtils.invokeMethod(controller, "defaultSensorName", "humidity"));
        assertEquals("光照传感器", ReflectionTestUtils.invokeMethod(controller, "defaultSensorName", "light"));
        assertEquals("烟雾传感器", ReflectionTestUtils.invokeMethod(controller, "defaultSensorName", "smoke"));
        assertEquals("人在传感器", ReflectionTestUtils.invokeMethod(controller, "defaultSensorName", "person"));
        assertEquals("未知传感器", ReflectionTestUtils.invokeMethod(controller, "defaultSensorName", "other"));

        assertEquals("AGV补光灯", ReflectionTestUtils.invokeMethod(controller, "defaultDeviceName", "light"));
        assertEquals("远程断电模块", ReflectionTestUtils.invokeMethod(controller, "defaultDeviceName", "power"));
        assertEquals("声光报警器", ReflectionTestUtils.invokeMethod(controller, "defaultDeviceName", "alarm"));
        assertEquals("AGV巡线车", ReflectionTestUtils.invokeMethod(controller, "defaultDeviceName", "agv"));
        assertEquals("未知执行设备", ReflectionTestUtils.invokeMethod(controller, "defaultDeviceName", "fan"));
    }

    @Test
    void insertSensorAndMaybeFlawShouldReturnOnlySensorWhenCreateFlawIsFalse() {
        Object result = ReflectionTestUtils.invokeMethod(controller, "insertSensorAndMaybeFlaw",
                1L, "temperature", "28℃", 3.0, "正常", "温度正常", false);

        assertTrue(result instanceof AgvSensorRecord);
        AgvSensorRecord record = (AgvSensorRecord) result;
        assertEquals("温度传感器", record.getSensorName());
        assertEquals("正常", record.getStatus());
        verify(agvSensorRecordMapper).insert(record);
        verify(agvFlawMapper, never()).insert(any());
    }
}

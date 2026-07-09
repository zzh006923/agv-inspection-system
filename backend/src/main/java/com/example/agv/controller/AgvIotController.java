package com.example.agv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.agv.common.AjaxResult;
import com.example.agv.entity.AgvFlaw;
import com.example.agv.entity.AgvIotActionRecord;
import com.example.agv.entity.AgvSensorRecord;
import com.example.agv.mapper.AgvFlawMapper;
import com.example.agv.mapper.AgvIotActionRecordMapper;
import com.example.agv.mapper.AgvSensorRecordMapper;
import com.example.agv.service.AgvMovementStateService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AIoT 多传感器巡检联动模块
 *
 * 作用：把温湿度、光照、烟雾、人员检测等传感器能力
 * 融合到 AGV 隧道巡检后端中，实现“环境感知—风险判断—设备联动—结果记录”的闭环。
 *
 * 可演示内容：
 * 1. 巡检传感器状态查询；
 * 2. 传感器异常触发执行设备动作；
 * 3. 语音/AI 指令映射到设备控制；
 * 4. 场景模式触发多个设备联动；
 * 5. 所有动作写入数据库，便于前端展示和答辩截图。
 */
@RestController
@RequestMapping("/agv/iot")
@CrossOrigin
public class AgvIotController {

    @Resource
    private AgvSensorRecordMapper agvSensorRecordMapper;

    @Resource
    private AgvFlawMapper agvFlawMapper;

    @Resource
    private AgvIotActionRecordMapper agvIotActionRecordMapper;

    @Resource
    private AgvMovementStateService agvMovementStateService;

    /**
     * 查看本模块支持的传感器、执行设备和语音指令。
     */
    @GetMapping("/capability")
    public AjaxResult capability() {
        Map<String, Object> data = new HashMap<>();
        data.put("sensorModules", Arrays.asList(
                "温湿度传感器：temperature / humidity",
                "光照传感器：light",
                "烟雾传感器：smoke",
                "人在传感器：person"
        ));
        data.put("actuatorModules", Arrays.asList(
        		"AGV补光灯：light",
        		"远程断电模块：power",
        		"声光报警器：alarm",
        		"AGV安全停车：agv"
        ));
        data.put("voiceExamples", Arrays.asList(
                "查询当前环境状态",
                "打开补光灯",
                "关闭补光灯",
                "触发声光报警",
                "执行安全停车",
                "远程断电",
                "恢复供电",
                "进入烟雾报警模式",
                "进入人员安全保护模式"
        ));
        return AjaxResult.success(data);
    }

    /**
     * AIoT 总览：返回某个任务下的传感器记录、执行动作记录和 AGV 当前状态。
     */
    @GetMapping("/overview/{taskId}")
    public AjaxResult overview(@PathVariable Long taskId) {
        Map<String, Object> data = new HashMap<>();
        data.put("sensorRecords", selectSensorRecords(taskId));
        data.put("actionRecords", selectActionRecords(taskId));
        data.put("movementStatus", agvMovementStateService.heartbeat());
        data.put("summary", buildSummary(taskId));
        return AjaxResult.success(data);
    }

    /**
     * 手动控制执行设备。用于证明后端具备“设备控制”能力。
     */
    @PostMapping("/device/control")
    public AjaxResult controlDevice(@RequestBody DeviceControlRequest request) {
        if (request.getTaskId() == null) {
            return AjaxResult.error("任务ID不能为空");
        }
        if (request.getDeviceType() == null || request.getDeviceType().trim().isEmpty()) {
            return AjaxResult.error("设备类型不能为空");
        }
        if (request.getAction() == null || request.getAction().trim().isEmpty()) {
            return AjaxResult.error("控制动作不能为空");
        }

        String deviceName = defaultDeviceName(request.getDeviceType());
        String feedback = deviceName + "已" + request.getAction();

        if ("agv".equals(request.getDeviceType()) && request.getAction().contains("停车")) {
            agvMovementStateService.stop();
            feedback = "AGV已执行安全停车";
        }

        AgvIotActionRecord actionRecord = insertAction(
                request.getTaskId(),
                "manual",
                request.getCommandText(),
                request.getSceneName(),
                request.getDeviceType(),
                deviceName,
                request.getAction(),
                "成功",
                feedback
        );

        Map<String, Object> data = new HashMap<>();
        data.put("actionRecord", actionRecord);
        data.put("movementStatus", agvMovementStateService.heartbeat());
        return AjaxResult.success(data);
    }

    /**
     * 场景联动：一个场景触发多个设备动作。
     */
    @PostMapping("/scene/trigger")
    public AjaxResult triggerScene(@RequestBody SceneRequest request) {
        if (request.getTaskId() == null) {
            return AjaxResult.error("任务ID不能为空");
        }
        String sceneType = request.getSceneType() == null ? "safety" : request.getSceneType().trim();
        Double distance = request.getDistance() == null ? 0.0 : request.getDistance();

        List<Object> createdRecords = new ArrayList<>();
        List<AgvIotActionRecord> actions = new ArrayList<>();
        String sceneName;

        if ("safety".equals(sceneType)) {
            sceneName = "隧道安全巡检模式";
            createdRecords.add(insertSensorAndMaybeFlaw(request.getTaskId(), "person", "detected", distance,
                    "异常", "人在传感器检测到人员靠近", true));
            agvMovementStateService.stop();
            actions.add(insertAction(request.getTaskId(), "scene", "进入隧道安全巡检模式", sceneName,
                    "agv", "AGV巡线车", "安全停车", "成功", "检测到人员靠近，AGV已自动停车"));
            actions.add(insertAction(request.getTaskId(), "scene", "进入隧道安全巡检模式", sceneName,
                    "alarm", "声光报警模块", "开启", "成功", "已开启声光报警提醒现场人员"));
        } else if ("environment".equals(sceneType)) {
            sceneName = "隧道环境监测模式";
            createdRecords.add(insertSensorAndMaybeFlaw(request.getTaskId(), "humidity", "86%", distance,
                    "异常", "湿度超过80%阈值", true));
            actions.add(insertAction(request.getTaskId(), "scene", "进入环境监测模式", sceneName,
                    "power", "远程断电模块", "断电保护", "成功", "检测到隧道湿度异常，已执行远程断电保护并上报管理端"));
        } else if ("lighting".equals(sceneType)) {
            sceneName = "隧道补光巡检模式";
            createdRecords.add(insertSensorAndMaybeFlaw(request.getTaskId(), "light", "20lux", distance,
                    "异常", "光照低于阈值", true));
            actions.add(insertAction(request.getTaskId(), "scene", "进入隧道补光巡检模式", sceneName,
                    "light", "AGV补光灯", "开启", "成功", "检测到隧道光照不足，已开启AGV补光灯"));
        } else if ("fire".equals(sceneType)) {
            sceneName = "烟雾安全报警模式";
            createdRecords.add(insertSensorAndMaybeFlaw(request.getTaskId(), "smoke", "true", distance,
                    "异常", "检测到烟雾异常", true));
            agvMovementStateService.stop();
            actions.add(insertAction(request.getTaskId(), "scene", "进入烟雾安全报警模式", sceneName,
                    "alarm", "声光报警模块", "开启", "成功", "检测到烟雾，已开启声光报警"));
            actions.add(insertAction(request.getTaskId(), "scene", "进入烟雾安全报警模式", sceneName,
                    "agv", "AGV巡线车", "安全停车", "成功", "烟雾异常，AGV已停止巡检"));
        } else {
            return AjaxResult.error("未知场景类型，可选：safety/environment/lighting/fire");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("sceneName", sceneName);
        data.put("sensorOrFlawRecords", createdRecords);
        data.put("actionRecords", actions);
        data.put("feedback", sceneName + "已执行完成");
        data.put("movementStatus", agvMovementStateService.heartbeat());
        return AjaxResult.success(data);
    }

    /**
     * AI / 语音指令入口：把自然语言命令映射到设备动作、状态查询或场景联动。
     * 这里不训练大模型，采用关键词规则模拟“意图识别”，方便课程演示。
     */
    @PostMapping("/voice/command")
    public AjaxResult voiceCommand(@RequestBody VoiceCommandRequest request) {
        if (request.getTaskId() == null) {
            return AjaxResult.error("任务ID不能为空");
        }
        if (request.getCommand() == null || request.getCommand().trim().isEmpty()) {
            return AjaxResult.error("语音指令不能为空");
        }

        String command = request.getCommand().trim();
        Double distance = request.getDistance() == null ? 0.0 : request.getDistance();

        if (command.contains("查询") || command.contains("状态") || command.contains("温湿度") || command.contains("环境")) {
            Map<String, Object> data = new HashMap<>();
            data.put("intent", "状态查询");
            data.put("feedback", "已查询当前环境与设备状态");
            data.put("overview", overview(request.getTaskId()).getData());
            return AjaxResult.success(data);
        }

        if (command.contains("安全") || command.contains("有人") || command.contains("人员")) {
            SceneRequest scene = new SceneRequest();
            scene.setTaskId(request.getTaskId());
            scene.setSceneType("safety");
            scene.setDistance(distance);
            return triggerScene(scene);
        }

        if (command.contains("烟") || command.contains("报警") || command.contains("火")) {
            SceneRequest scene = new SceneRequest();
            scene.setTaskId(request.getTaskId());
            scene.setSceneType("fire");
            scene.setDistance(distance);
            return triggerScene(scene);
        }

        if (command.contains("补光") || command.contains("开灯") || command.contains("打开灯")) {
            return doVoiceDeviceAction(request.getTaskId(), command, "light", "开启");
        }
        if (command.contains("关灯") || command.contains("关闭灯")) {
            return doVoiceDeviceAction(request.getTaskId(), command, "light", "关闭");
        }
        if (command.contains("远程断电") || (command.contains("断电") && !command.contains("恢复"))) {
            return doVoiceDeviceAction(request.getTaskId(), command, "power", "开启");
        }
        if (command.contains("恢复供电") || command.contains("恢复通电")) {
            return doVoiceDeviceAction(request.getTaskId(), command, "power", "关闭");
        }
        if (command.contains("安全停车") || command.contains("执行停车") || command.contains("紧急停车")) {
            agvMovementStateService.stop();
            AgvIotActionRecord record = insertAction(request.getTaskId(), "voice", command,
                    "AI语音控制", "agv", "AGV巡线车", "安全停车", "成功", "AGV已执行安全停车");
            return AjaxResult.success(record);
        }
        if (command.contains("关闭所有") || command.contains("全部关闭")) {
            List<AgvIotActionRecord> actions = new ArrayList<>();
            actions.add(insertAction(request.getTaskId(), "voice", command, "关闭所有设备", "light", "AGV补光灯", "关闭", "成功", "AGV补光灯已关闭"));
            actions.add(insertAction(request.getTaskId(), "voice", command, "关闭所有设备", "power", "远程断电模块", "恢复供电", "成功", "远程断电模块已恢复供电"));
            actions.add(insertAction(request.getTaskId(), "voice", command, "关闭所有设备", "alarm", "声光报警器", "关闭", "成功", "声光报警器已关闭"));
            return AjaxResult.success(actions);
        }

        return AjaxResult.error("未识别语音指令，请使用：查询当前环境状态、打开补光灯、关闭补光灯、触发声光报警、执行安全停车、远程断电、恢复供电、进入烟雾报警模式、进入人员安全保护模式等");
    }

    private AjaxResult doVoiceDeviceAction(Long taskId, String command, String deviceType, String action) {
        String deviceName = defaultDeviceName(deviceType);
        AgvIotActionRecord record = insertAction(taskId, "voice", command, "AI语音控制", deviceType,
                deviceName, action, "成功", deviceName + "已" + action);
        return AjaxResult.success(record);
    }

    private Object insertSensorAndMaybeFlaw(Long taskId, String sensorType, String sensorValue,
                                            Double distance, String status, String remark,
                                            boolean createFlaw) {
        AgvSensorRecord sensor = new AgvSensorRecord();
        sensor.setTaskId(taskId);
        sensor.setSensorType(sensorType);
        sensor.setSensorName(defaultSensorName(sensorType));
        sensor.setSensorValue(sensorValue);
        sensor.setStatus(status);
        sensor.setDistance(distance);
        sensor.setAction(remark);
        sensor.setRemark(remark);
        sensor.setCreateTime(LocalDateTime.now());
        sensor.setDeleteFlag(0);
        agvSensorRecordMapper.insert(sensor);

        if (!createFlaw) {
            return sensor;
        }

        AgvFlaw flaw = new AgvFlaw();
        flaw.setTaskId(taskId);
        flaw.setRound(1);
        flaw.setFlawDistance(distance);
        flaw.setShown(0);
        flaw.setConfirmed(0);
        flaw.setUploaded(0);
        flaw.setCountNum(1);
        flaw.setFlawLength(0.0);
        flaw.setFlawArea(0.0);
        flaw.setFlawImage("");
        flaw.setFlawImageUrl("");
        flaw.setFlawRtsp("");
        flaw.setCreateTime(LocalDateTime.now());
        flaw.setDeleteFlag(0);

        if ("person".equals(sensorType)) {
            flaw.setFlawType("安全事件");
            flaw.setFlawName("人员靠近安全事件");
            flaw.setFlawDesc("人在传感器检测到人员靠近，系统触发AGV自动停车和声光报警");
            flaw.setLevel("高");
            flaw.setSource("人员检测");
        } else if ("humidity".equals(sensorType)) {
            flaw.setFlawType("环境异常");
            flaw.setFlawName("隧道湿度异常");
            flaw.setFlawDesc("湿度传感器检测到湿度过高，系统触发远程断电模块联动");
            flaw.setLevel("中");
            flaw.setSource("湿度传感器");
        } else if ("light".equals(sensorType)) {
            flaw.setFlawType("照明异常");
            flaw.setFlawName("隧道光照不足");
            flaw.setFlawDesc("光照传感器检测到亮度不足，系统触发补光灯联动");
            flaw.setLevel("低");
            flaw.setSource("光照传感器");
        } else if ("smoke".equals(sensorType)) {
            flaw.setFlawType("安全风险");
            flaw.setFlawName("隧道烟雾异常");
            flaw.setFlawDesc("烟雾传感器检测到异常烟雾，系统触发声光报警和AGV停车");
            flaw.setLevel("高");
            flaw.setSource("烟雾传感器");
        }
        flaw.setRemark("AIoT场景联动生成");
        agvFlawMapper.insert(flaw);

        Map<String, Object> data = new HashMap<>();
        data.put("sensorRecord", sensor);
        data.put("flaw", flaw);
        return data;
    }

    private AgvIotActionRecord insertAction(Long taskId,
								            Long sensorRecordId,
								            Long flawId,
								            String sceneType,
								            String triggerType,
								            String commandText,
								            String sceneName,
								            String deviceType,
								            String deviceName,
								            String action,
								            String beforeStatus,
								            String afterStatus,
								            String result,
								            String feedback) {
        AgvIotActionRecord record = new AgvIotActionRecord();

		record.setTaskId(taskId);
		record.setSensorRecordId(sensorRecordId);
		record.setFlawId(flawId);
		record.setSceneType(sceneType);
		
		record.setTriggerType(triggerType);
		record.setCommandText(commandText);
		record.setSceneName(sceneName);
		
		record.setDeviceType(deviceType);
		record.setDeviceName(deviceName);
		record.setAction(action);
		
		record.setBeforeStatus(beforeStatus);
		record.setAfterStatus(afterStatus);
		
		record.setResult(result);
		record.setFeedback(feedback);
		
		record.setCreateTime(LocalDateTime.now());
		record.setDeleteFlag(0);
		
		agvIotActionRecordMapper.insert(record);
		return record;
	}
		
	private AgvIotActionRecord insertAction(Long taskId,
								            String triggerType,
								            String commandText,
								            String sceneName,
								            String deviceType,
								            String deviceName,
								            String action,
								            String result,
								            String feedback) {
		String beforeStatus = defaultBeforeStatus(action);
		String afterStatus = defaultAfterStatus(action);
		
		return insertAction(
				taskId,
				null,
				null,
				null,
				triggerType,
				commandText,
				sceneName,
				deviceType,
				deviceName,
				action,
				beforeStatus,
				afterStatus,
				result,
				feedback
		   );
		}
		
	private String defaultBeforeStatus(String action) {
		if (action == null) {
		   return "未知";
		}
		
		if (action.contains("开启")) {
		    return "关闭";
		}
		
		if (action.contains("报警")) {
		    return "关闭";
		}
		
		if (action.contains("断电")) {
		    return "供电中";
		}
		
		if (action.contains("停车")) {
		    return "运行中";
		}
		
		if (action.contains("关闭")) {
		    return "已开启";
		}
		
		if (action.contains("恢复")) {
		   return "已断电";
		}
		
		return "未知";
		}
		
	private String defaultAfterStatus(String action) {
		if (action == null) {
		   return "未知";
		}
		
		if (action.contains("停车")) {
		   return "已停车";
		}
		
		if (action.contains("报警")) {
		   return "报警中";
		}
		
		if (action.contains("断电")) {
		   return "已断电";
		}
		
		if (action.contains("开启")) {
		   return "已开启";
		}
		
		if (action.contains("关闭")) {
		   return "已关闭";
		}
		
		if (action.contains("恢复")) {
		   return "供电中";
		}
		
		return "已执行";
		}
    private List<AgvSensorRecord> selectSensorRecords(Long taskId) {
        QueryWrapper<AgvSensorRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0).eq("task_id", taskId).orderByDesc("create_time");
        return agvSensorRecordMapper.selectList(wrapper);
    }

    private List<AgvIotActionRecord> selectActionRecords(Long taskId) {
        QueryWrapper<AgvIotActionRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0).eq("task_id", taskId).orderByDesc("create_time");
        return agvIotActionRecordMapper.selectList(wrapper);
    }

    private Map<String, Object> buildSummary(Long taskId) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("taskId", taskId);
        summary.put("sensorCount", selectSensorRecords(taskId).size());
        summary.put("actionCount", selectActionRecords(taskId).size());
        summary.put("flow", "数据采集—状态判断—设备控制—结果反馈");
        summary.put("innovation", "将温湿度、光照、烟雾、人员检测等多传感器与AGV隧道安全巡检联动");
        return summary;
    }

    private String defaultSensorName(String type) {
        if ("temperature".equals(type)) return "温度传感器";
        if ("humidity".equals(type)) return "湿度传感器";
        if ("light".equals(type)) return "光照传感器";
        if ("smoke".equals(type)) return "烟雾传感器";
        if ("person".equals(type)) return "人在传感器";
        return "未知传感器";
    }

    private String defaultDeviceName(String type) {
        if ("light".equals(type)) return "AGV补光灯";
        if ("power".equals(type)) return "远程断电模块";
        if ("alarm".equals(type)) return "声光报警器";
        if ("agv".equals(type)) return "AGV巡线车";
        return "未知执行设备";
    }

    public static class DeviceControlRequest {
        private Long taskId;
        private String deviceType;
        private String action;
        private String commandText;
        private String sceneName;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getCommandText() { return commandText; }
        public void setCommandText(String commandText) { this.commandText = commandText; }
        public String getSceneName() { return sceneName; }
        public void setSceneName(String sceneName) { this.sceneName = sceneName; }
    }

    public static class SceneRequest {
        private Long taskId;
        private String sceneType;
        private Double distance;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getSceneType() { return sceneType; }
        public void setSceneType(String sceneType) { this.sceneType = sceneType; }
        public Double getDistance() { return distance; }
        public void setDistance(Double distance) { this.distance = distance; }
    }

    public static class VoiceCommandRequest {
        private Long taskId;
        private String command;
        private Double distance;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public Double getDistance() { return distance; }
        public void setDistance(Double distance) { this.distance = distance; }
    }
}

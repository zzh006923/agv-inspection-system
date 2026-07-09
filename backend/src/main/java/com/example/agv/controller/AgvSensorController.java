package com.example.agv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.agv.common.AjaxResult;
import com.example.agv.dto.SensorReportDTO;
import com.example.agv.entity.AgvFlaw;
import com.example.agv.entity.AgvIotActionRecord;
import com.example.agv.entity.AgvSensorRecord;
import com.example.agv.entity.AgvTask;
import com.example.agv.mapper.AgvFlawMapper;
import com.example.agv.mapper.AgvIotActionRecordMapper;
import com.example.agv.mapper.AgvSensorRecordMapper;
import com.example.agv.mapper.AgvTaskMapper;
import com.example.agv.service.AgvMovementStateService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agv/sensor")
@CrossOrigin
public class AgvSensorController {

    @Resource
    private AgvSensorRecordMapper agvSensorRecordMapper;

    @Resource
    private AgvTaskMapper agvTaskMapper;

    @Resource
    private AgvFlawMapper agvFlawMapper;

    @Resource
    private AgvMovementStateService agvMovementStateService;

    @Resource
    private AgvIotActionRecordMapper agvIotActionRecordMapper;

    @GetMapping("/list")
    public AjaxResult listSensorRecord(@RequestParam Long taskId) {
        QueryWrapper<AgvSensorRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId);
        wrapper.eq("delete_flag", 0);
        wrapper.orderByAsc("distance");

        List<AgvSensorRecord> list = agvSensorRecordMapper.selectList(wrapper);
        return AjaxResult.success(list);
    }

    @GetMapping("/status/{taskId}")
    public AjaxResult getSensorStatus(@PathVariable Long taskId) {
        QueryWrapper<AgvSensorRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId);
        wrapper.eq("delete_flag", 0);
        wrapper.orderByDesc("create_time");

        List<AgvSensorRecord> records = agvSensorRecordMapper.selectList(wrapper);

        Map<String, Object> result = new HashMap<>();
        int abnormalCount = 0;

        for (AgvSensorRecord record : records) {
            String type = record.getSensorType();

            if (!result.containsKey(type)) {
                result.put(type, record);
            }

            if ("异常".equals(record.getStatus())) {
                abnormalCount++;
            }
        }

        result.put("abnormalCount", abnormalCount);
        return AjaxResult.success(result);
    }

    @PostMapping
    public AjaxResult addSensorRecord(@RequestBody AgvSensorRecord record) {
        if (record.getTaskId() == null) {
            return AjaxResult.error("任务ID不能为空");
        }

        if (record.getSensorType() == null || record.getSensorType().trim().isEmpty()) {
            return AjaxResult.error("传感器类型不能为空");
        }

        if (record.getStatus() == null || record.getStatus().trim().isEmpty()) {
            record.setStatus("正常");
        }

        if (record.getCreateTime() == null) {
            record.setCreateTime(LocalDateTime.now());
        }

        if (record.getDeleteFlag() == null) {
            record.setDeleteFlag(0);
        }

        agvSensorRecordMapper.insert(record);
        return AjaxResult.success(record);
    }

    /**
     * 兼容 sensor-mini-prototype 的传感器上报格式。
     *
     * 真实传感器 / 传感器脚本可以 POST 到：
     * /agv/sensor/report
     *
     * 如果没有传 taskId，则自动挂到当前最新的“巡视中”任务下面。
     */
    @PostMapping("/report")
    public AjaxResult reportSensor(@RequestBody SensorReportDTO dto) {
    	System.out.println(">>> 收到 /agv/sensor/report 上报请求");
    	if (dto == null) {
            return AjaxResult.error("上报数据不能为空");
        }

        Long taskId = resolveReportTaskId(dto.getTaskId());

        if (taskId == null) {
            return AjaxResult.error("未找到当前巡视中任务，请先在前端启动巡检任务，或在传感器上报时传入 taskId");
        }

        String sensorType = normalizeReportType(dto.getSensorType());

        if (sensorType == null) {
            return AjaxResult.error("传感器类型不能为空");
        }

        List<Object> results = new ArrayList<>();

        if ("th".equals(sensorType)) {
            if (dto.getTemperature() != null) {
                Object tempResult = handleOneSensorReport(
                        taskId,
                        "temperature",
                        dto.getTemperature() + "℃",
                        dto.getDistance(),
                        dto.getTemperature().compareTo(new BigDecimal("35")) >= 0
                );
                results.add(tempResult);
            }

            if (dto.getHumidity() != null) {
                Object humiResult = handleOneSensorReport(
                        taskId,
                        "humidity",
                        dto.getHumidity() + "%",
                        dto.getDistance(),
                        dto.getHumidity().compareTo(new BigDecimal("80")) >= 0
                );
                results.add(humiResult);
            }

            if (results.isEmpty()) {
                return AjaxResult.error("温湿度传感器缺少 temperature 或 humidity 数据");
            }

            return AjaxResult.success(results);
        }

        if ("temperature".equals(sensorType)) {
            BigDecimal value = dto.getTemperature();

            if (value == null) {
                return AjaxResult.error("温度传感器缺少 temperature 数据");
            }

            Object result = handleOneSensorReport(
                    taskId,
                    "temperature",
                    value + "℃",
                    dto.getDistance(),
                    value.compareTo(new BigDecimal("35")) >= 0
            );

            return AjaxResult.success(result);
        }

        if ("humidity".equals(sensorType)) {
            BigDecimal value = dto.getHumidity();

            if (value == null) {
                return AjaxResult.error("湿度传感器缺少 humidity 数据");
            }

            Object result = handleOneSensorReport(
                    taskId,
                    "humidity",
                    value + "%",
                    dto.getDistance(),
                    value.compareTo(new BigDecimal("80")) >= 0
            );

            return AjaxResult.success(result);
        }

        if ("person".equals(sensorType)) {
            boolean detected = Boolean.TRUE.equals(dto.getPersonDetected());

            Object result = handleOneSensorReport(
                    taskId,
                    "person",
                    detected ? "detected" : "none",
                    dto.getDistance(),
                    detected
            );

            return AjaxResult.success(result);
        }

        if ("light".equals(sensorType)) {
            Double value = dto.getLightValue();

            if (value == null) {
                return AjaxResult.error("光照传感器缺少 lightValue 数据");
            }

            Object result = handleOneSensorReport(
                    taskId,
                    "light",
                    value + "lux",
                    dto.getDistance(),
                    value < 50
            );

            return AjaxResult.success(result);
        }

        if ("smoke".equals(sensorType)) {
            boolean smokeDetected = Boolean.TRUE.equals(dto.getSmokeDetected());

            if (dto.getSmokeValue() != null && dto.getSmokeValue() >= 50) {
                smokeDetected = true;
            }

            String value = dto.getSmokeValue() == null
                    ? String.valueOf(smokeDetected)
                    : String.valueOf(dto.getSmokeValue());

            Object result = handleOneSensorReport(
                    taskId,
                    "smoke",
                    value,
                    dto.getDistance(),
                    smokeDetected
            );

            return AjaxResult.success(result);
        }

        return AjaxResult.error("不支持的传感器类型：" + dto.getSensorType());
    }

    /**
     * 原有前端按钮或测试接口仍然可以使用：
     * POST /agv/sensor/trigger
     */
    @PostMapping("/trigger")
    public AjaxResult triggerSensorEvent(@RequestBody SensorTriggerRequest request) {
        if (request == null) {
            return AjaxResult.error("请求体不能为空");
        }

        if (request.getTaskId() == null) {
            return AjaxResult.error("任务ID不能为空");
        }

        if (request.getSensorType() == null || request.getSensorType().trim().isEmpty()) {
            return AjaxResult.error("传感器类型不能为空");
        }

        String sensorType = normalizeReportType(request.getSensorType());

        if (sensorType == null) {
            return AjaxResult.error("传感器类型不能为空");
        }

        request.setSensorType(sensorType);

        Map<String, Object> data = handleTriggerRequest(request);
        return AjaxResult.success(data);
    }

    /**
     * report 接口内部使用。
     * abnormal=true：复用 trigger 逻辑，写传感器记录、故障记录、联动记录。
     * abnormal=false：只写一条正常传感器记录。
     */
    private Object handleOneSensorReport(Long taskId,
                                         String sensorType,
                                         String sensorValue,
                                         Double distance,
                                         boolean abnormal) {
        if (abnormal) {
            SensorTriggerRequest request = new SensorTriggerRequest();
            request.setTaskId(taskId);
            request.setSensorType(sensorType);
            request.setSensorValue(sensorValue);
            request.setDistance(distance);
            return handleTriggerRequest(request);
        }

        AgvSensorRecord record = new AgvSensorRecord();
        record.setTaskId(taskId);
        record.setSensorType(sensorType);
        record.setSensorName(sensorNameOf(sensorType));
        record.setSensorValue(sensorValue);
        record.setStatus("正常");
        record.setDistance(distance);
        record.setAction("无");
        record.setRemark("传感器正常上报");
        record.setCreateTime(LocalDateTime.now());
        record.setDeleteFlag(0);

        agvSensorRecordMapper.insert(record);

        Map<String, Object> data = new HashMap<>();
        data.put("sensorRecord", record);
        data.put("flaw", null);
        data.put("actionRecords", new ArrayList<>());
        data.put("movementStatus", safeHeartbeat());
        data.put("message", "传感器正常上报");

        return data;
    }

    /**
     * 统一处理异常触发逻辑。
     */
    private Map<String, Object> handleTriggerRequest(SensorTriggerRequest request) {
        String sensorType = normalizeReportType(request.getSensorType());
        request.setSensorType(sensorType);

        TriggerInfo info = buildTriggerInfo(sensorType, request.getSensorValue());

        AgvSensorRecord sensorRecord = new AgvSensorRecord();
        sensorRecord.setTaskId(request.getTaskId());
        sensorRecord.setSensorType(sensorType);
        sensorRecord.setSensorName(info.sensorName);
        sensorRecord.setSensorValue(info.sensorValue);
        sensorRecord.setStatus(info.status);
        sensorRecord.setDistance(request.getDistance());
        sensorRecord.setAction(info.action);
        sensorRecord.setRemark(info.remark);
        sensorRecord.setCreateTime(LocalDateTime.now());
        sensorRecord.setDeleteFlag(0);

        agvSensorRecordMapper.insert(sensorRecord);

        AgvFlaw flaw = null;
        List<AgvIotActionRecord> actionRecords = new ArrayList<>();

        if ("异常".equals(info.status)) {
            flaw = buildFlawFromSensor(request, info);
            agvFlawMapper.insert(flaw);

            actionRecords = buildActionsFromSensor(
                    request.getTaskId(),
                    sensorRecord.getId(),
                    flaw.getId(),
                    sensorType
            );
        }

        Map<String, Object> data = new HashMap<>();
        data.put("sensorRecord", sensorRecord);
        data.put("flaw", flaw);
        data.put("actionRecords", actionRecords);
        data.put("movementStatus", safeHeartbeat());
        data.put("message", info.action);

        return data;
    }

    /**
     * 没传 taskId 时，自动找最新一个“巡视中”任务。
     */
    private Long resolveReportTaskId(Long taskId) {
        if (taskId != null && taskId > 0) {
            return taskId;
        }

        QueryWrapper<AgvTask> wrapper = new QueryWrapper<>();
        wrapper.eq("task_status", "巡视中");
        wrapper.eq("delete_flag", 0);
        wrapper.orderByDesc("id");
        wrapper.last("LIMIT 1");

        AgvTask runningTask = agvTaskMapper.selectOne(wrapper);
        return runningTask == null ? null : runningTask.getId();
    }

    private String normalizeReportType(String sensorType) {
        if (sensorType == null || sensorType.trim().isEmpty()) {
            return null;
        }

        String type = sensorType.trim().toLowerCase();

        if ("temperature_humidity".equals(type)
                || "temp_humi".equals(type)
                || "温湿度".equals(type)
                || "th".equals(type)) {
            return "th";
        }

        if ("temp".equals(type)
                || "temperature".equals(type)
                || "温度".equals(type)) {
            return "temperature";
        }

        if ("humi".equals(type)
                || "humidity".equals(type)
                || "湿度".equals(type)) {
            return "humidity";
        }

        if ("person".equals(type)
                || "people".equals(type)
                || "human".equals(type)
                || "人员".equals(type)
                || "人员检测".equals(type)) {
            return "person";
        }

        if ("light".equals(type)
                || "illumination".equals(type)
                || "光照".equals(type)
                || "照度".equals(type)) {
            return "light";
        }

        if ("smoke".equals(type)
                || "fire".equals(type)
                || "烟雾".equals(type)
                || "烟感".equals(type)) {
            return "smoke";
        }

        return type;
    }

    private TriggerInfo buildTriggerInfo(String sensorType, String sensorValue) {
        TriggerInfo info = new TriggerInfo();

        if ("person".equals(sensorType)) {
            info.sensorName = "人员检测传感器";
            info.sensorValue = emptyDefault(sensorValue, "detected");

            boolean detected = isTruthLike(info.sensorValue);

            if (detected) {
                info.status = "异常";
                info.action = "自动停车并生成安全事件";
                info.remark = "检测到人员靠近巡线车";
                info.flawType = "安全事件";
                info.flawName = "人员靠近安全事件";
                info.flawDesc = "人员检测模块发现巡线车前方存在人员靠近，系统已触发自动停车";
                info.level = "高";
                info.source = "人员检测";
            } else {
                fillNormalInfo(info, "人员检测", "未检测到人员靠近");
            }

            return info;
        }

        if ("humidity".equals(sensorType)) {
            info.sensorName = "湿度传感器";
            info.sensorValue = emptyDefault(sensorValue, "86%");

            double humiValue = parseSensorNumber(info.sensorValue, 86.0);

            if (humiValue >= 80) {
                info.status = "异常";
                info.action = "生成隧道湿度异常记录";
                info.remark = "湿度超过80%阈值";
                info.flawType = "环境异常";
                info.flawName = "隧道湿度异常";
                info.flawDesc = "湿度传感器检测到当前区域湿度过高";
                info.level = "中";
                info.source = "湿度传感器";
            } else {
                fillNormalInfo(info, "湿度传感器", "环境湿度正常");
            }

            return info;
        }

        if ("temperature".equals(sensorType)) {
            info.sensorName = "温度传感器";
            info.sensorValue = emptyDefault(sensorValue, "28.5℃");

            double tempValue = parseSensorNumber(info.sensorValue, 28.5);

            if (tempValue >= 35) {
                info.status = "异常";
                info.action = "生成隧道温度异常记录";
                info.remark = "温度超过35℃阈值";
                info.flawType = "环境异常";
                info.flawName = "隧道温度异常";
                info.flawDesc = "温度传感器检测到当前隧道区域温度过高";
                info.level = "中";
                info.source = "温度传感器";
            } else {
                fillNormalInfo(info, "温度传感器", "环境温度正常");
            }

            return info;
        }

        if ("light".equals(sensorType)) {
            info.sensorName = "光照传感器";
            info.sensorValue = emptyDefault(sensorValue, "20lux");

            double lightValue = parseSensorNumber(info.sensorValue, 20.0);

            if (lightValue < 50) {
                info.status = "异常";
                info.action = "提示开启补光并生成照明异常";
                info.remark = "隧道光照低于阈值";
                info.flawType = "照明异常";
                info.flawName = "隧道光照不足";
                info.flawDesc = "光照传感器检测到当前隧道区域亮度不足";
                info.level = "低";
                info.source = "光照传感器";
            } else {
                fillNormalInfo(info, "光照传感器", "隧道光照正常");
            }

            return info;
        }

        if ("smoke".equals(sensorType)) {
            info.sensorName = "烟雾传感器";
            info.sensorValue = emptyDefault(sensorValue, "true");

            double smokeValue = parseSensorNumber(info.sensorValue, -1);
            boolean smokeAbnormal = isTruthLike(info.sensorValue) || smokeValue >= 50;

            if (smokeAbnormal) {
                info.status = "异常";
                info.action = "触发声光报警并建议终止巡检";
                info.remark = "检测到烟雾异常";
                info.flawType = "安全风险";
                info.flawName = "隧道烟雾异常";
                info.flawDesc = "烟雾传感器检测到异常烟雾，建议操作员及时处理";
                info.level = "高";
                info.source = "烟雾传感器";
            } else {
                fillNormalInfo(info, "烟雾传感器", "未检测到烟雾异常");
            }

            return info;
        }

        info.sensorName = "未知传感器";
        info.sensorValue = emptyDefault(sensorValue, "unknown");
        fillNormalInfo(info, "未知来源", "未知类型传感器记录");
        return info;
    }

    private void fillNormalInfo(TriggerInfo info, String source, String remark) {
        info.status = "正常";
        info.action = "无";
        info.remark = remark;
        info.flawType = null;
        info.flawName = null;
        info.flawDesc = null;
        info.level = null;
        info.source = source;
    }

    private AgvFlaw buildFlawFromSensor(SensorTriggerRequest request, TriggerInfo info) {
        AgvFlaw flaw = new AgvFlaw();
        flaw.setTaskId(request.getTaskId());
        flaw.setRound(1);
        flaw.setFlawType(info.flawType);
        flaw.setFlawName(info.flawName);
        flaw.setFlawDesc(info.flawDesc);
        flaw.setFlawDistance(request.getDistance());
        flaw.setFlawImage("");
        flaw.setFlawImageUrl("");
        flaw.setFlawRtsp("");
        flaw.setShown(0);
        flaw.setConfirmed(0);
        flaw.setUploaded(0);
        flaw.setLevel(info.level);
        flaw.setCountNum(1);
        flaw.setFlawLength(0.0);
        flaw.setFlawArea(0.0);
        flaw.setSource(info.source);
        flaw.setRemark("AIoT环境感知生成");
        flaw.setCreateTime(LocalDateTime.now());
        flaw.setDeleteFlag(0);
        return flaw;
    }

    private List<AgvIotActionRecord> buildActionsFromSensor(Long taskId,
                                                            Long sensorRecordId,
                                                            Long flawId,
                                                            String sensorType) {
        List<AgvIotActionRecord> actions = new ArrayList<>();

        if ("person".equals(sensorType)) {
            agvMovementStateService.stop();

            actions.add(insertIotAction(taskId, sensorRecordId, flawId,
                    "safety", "人员靠近安全保护模式",
                    "agv", "AGV巡线车", "安全停车",
                    "运行中", "已停车",
                    "检测到人员靠近，AGV已自动停车"));

            actions.add(insertIotAction(taskId, sensorRecordId, flawId,
                    "safety", "人员靠近安全保护模式",
                    "alarm", "声光报警器", "安全提醒",
                    "关闭", "提醒中",
                    "检测到人员靠近，已触发声光报警提醒"));
        }

        if ("light".equals(sensorType)) {
            actions.add(insertIotAction(taskId, sensorRecordId, flawId,
                    "lighting", "隧道补光巡检模式",
                    "light", "AGV补光灯", "开启",
                    "关闭", "已开启",
                    "检测到隧道光照不足，已开启AGV补光灯"));
        }

        if ("smoke".equals(sensorType)) {
            agvMovementStateService.stop();

            actions.add(insertIotAction(taskId, sensorRecordId, flawId,
                    "fire", "烟雾安全报警模式",
                    "alarm", "声光报警器", "报警",
                    "关闭", "报警中",
                    "检测到烟雾异常，已触发声光报警"));

            actions.add(insertIotAction(taskId, sensorRecordId, flawId,
                    "fire", "烟雾安全报警模式",
                    "agv", "AGV巡线车", "安全停车",
                    "运行中", "已停车",
                    "检测到烟雾异常，AGV已停止巡检"));
        }

        if ("humidity".equals(sensorType)) {
            actions.add(insertIotAction(taskId, sensorRecordId, flawId,
                    "environment", "隧道环境监测模式",
                    "power", "远程断电模块", "断电保护",
                    "供电中", "已断电",
                    "检测到湿度异常，已切断非必要外接设备电源并上报管理端"));
        }

        if ("temperature".equals(sensorType)) {
            actions.add(insertIotAction(taskId, sensorRecordId, flawId,
                    "environment", "隧道高温监测模式",
                    "fan", "隧道通风设备", "开启通风",
                    "关闭", "已开启",
                    "检测到温度过高，已建议开启通风并上报管理端"));
        }

        return actions;
    }

    private AgvIotActionRecord insertIotAction(Long taskId,
                                               Long sensorRecordId,
                                               Long flawId,
                                               String sceneType,
                                               String sceneName,
                                               String deviceType,
                                               String deviceName,
                                               String action,
                                               String beforeStatus,
                                               String afterStatus,
                                               String feedback) {
        AgvIotActionRecord record = new AgvIotActionRecord();

        record.setTaskId(taskId);
        record.setSensorRecordId(sensorRecordId);
        record.setFlawId(flawId);
        record.setSceneType(sceneType);

        record.setTriggerType("sensor");
        record.setCommandText("传感器异常自动触发");
        record.setSceneName(sceneName);

        record.setDeviceType(deviceType);
        record.setDeviceName(deviceName);
        record.setAction(action);

        record.setBeforeStatus(beforeStatus);
        record.setAfterStatus(afterStatus);

        record.setResult("成功");
        record.setFeedback(feedback);

        record.setCreateTime(LocalDateTime.now());
        record.setDeleteFlag(0);

        agvIotActionRecordMapper.insert(record);
        return record;
    }

    private Object safeHeartbeat() {
        try {
            return agvMovementStateService.heartbeat();
        } catch (Exception e) {
            return "小车状态读取失败：" + e.getMessage();
        }
    }

    private String sensorNameOf(String sensorType) {
        if ("temperature".equals(sensorType)) {
            return "温度传感器";
        }

        if ("humidity".equals(sensorType)) {
            return "湿度传感器";
        }

        if ("person".equals(sensorType)) {
            return "人员检测传感器";
        }

        if ("light".equals(sensorType)) {
            return "光照传感器";
        }

        if ("smoke".equals(sensorType)) {
            return "烟雾传感器";
        }

        return "未知传感器";
    }

    private String emptyDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    private double parseSensorNumber(String value, double defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            String number = value.replaceAll("[^0-9.\\-]", "");

            if (number.isEmpty()) {
                return defaultValue;
            }

            return Double.parseDouble(number);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean isTruthLike(String value) {
        if (value == null) {
            return false;
        }

        String text = value.trim().toLowerCase();

        return "true".equals(text)
                || "1".equals(text)
                || "yes".equals(text)
                || "detected".equals(text)
                || "有人".equals(text)
                || "异常".equals(text)
                || "报警".equals(text);
    }

    public static class SensorTriggerRequest {
        private Long taskId;
        private String sensorType;
        private String sensorValue;
        private Double distance;

        public Long getTaskId() {
            return taskId;
        }

        public void setTaskId(Long taskId) {
            this.taskId = taskId;
        }

        public String getSensorType() {
            return sensorType;
        }

        public void setSensorType(String sensorType) {
            this.sensorType = sensorType;
        }

        public String getSensorValue() {
            return sensorValue;
        }

        public void setSensorValue(String sensorValue) {
            this.sensorValue = sensorValue;
        }

        public Double getDistance() {
            return distance;
        }

        public void setDistance(Double distance) {
            this.distance = distance;
        }
    }

    private static class TriggerInfo {
        private String sensorName;
        private String sensorValue;
        private String status;
        private String action;
        private String remark;
        private String flawType;
        private String flawName;
        private String flawDesc;
        private String level;
        private String source;
    }
}
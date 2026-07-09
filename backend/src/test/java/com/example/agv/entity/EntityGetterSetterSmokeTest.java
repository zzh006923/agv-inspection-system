package com.example.agv.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityGetterSetterSmokeTest {

    @Test
    void agvTaskShouldStoreImportantFields() {
        AgvTask task = new AgvTask();
        LocalDateTime now = LocalDateTime.now();
        task.setId(1L);
        task.setTaskCode("TASK-001");
        task.setTaskName("一号巡检");
        task.setStartPos("A点");
        task.setTaskTrip("A-B-C");
        task.setCreator("张三");
        task.setExecutor("李四");
        task.setExecTime(now);
        task.setEndTime(now.plusMinutes(10));
        task.setCreateTime(now.minusMinutes(1));
        task.setTaskStatus("待上传");
        task.setRound(1);
        task.setUploaded(0);
        task.setRemark("备注");
        task.setCloudTaskId(100L);
        task.setDeleteFlag(0);

        assertEquals(1L, task.getId());
        assertEquals("TASK-001", task.getTaskCode());
        assertEquals("一号巡检", task.getTaskName());
        assertEquals("A点", task.getStartPos());
        assertEquals("A-B-C", task.getTaskTrip());
        assertEquals("张三", task.getCreator());
        assertEquals("李四", task.getExecutor());
        assertEquals(now, task.getExecTime());
        assertEquals(now.plusMinutes(10), task.getEndTime());
        assertEquals(now.minusMinutes(1), task.getCreateTime());
        assertEquals("待上传", task.getTaskStatus());
        assertEquals(1, task.getRound());
        assertEquals(0, task.getUploaded());
        assertEquals("备注", task.getRemark());
        assertEquals(100L, task.getCloudTaskId());
        assertEquals(0, task.getDeleteFlag());
    }

    @Test
    void agvFlawShouldStoreImportantFields() {
        AgvFlaw flaw = new AgvFlaw();
        LocalDateTime now = LocalDateTime.now();
        flaw.setId(10L);
        flaw.setTaskId(1L);
        flaw.setRound(1);
        flaw.setFlawType("结构缺陷");
        flaw.setFlawName("裂缝");
        flaw.setFlawDesc("疑似裂缝");
        flaw.setFlawDistance(12.5);
        flaw.setFlawImage("local.jpg");
        flaw.setFlawImageUrl("http://img");
        flaw.setFlawRtsp("rtsp://cam");
        flaw.setShown(0);
        flaw.setConfirmed(1);
        flaw.setUploaded(0);
        flaw.setLevel("中");
        flaw.setCountNum(1);
        flaw.setFlawLength(2.0);
        flaw.setFlawArea(0.5);
        flaw.setSource("模型识别");
        flaw.setRemark("需复核");
        flaw.setCreateTime(now);
        flaw.setDeleteFlag(0);

        assertEquals(10L, flaw.getId());
        assertEquals(1L, flaw.getTaskId());
        assertEquals(1, flaw.getRound());
        assertEquals("结构缺陷", flaw.getFlawType());
        assertEquals("裂缝", flaw.getFlawName());
        assertEquals("疑似裂缝", flaw.getFlawDesc());
        assertEquals(12.5, flaw.getFlawDistance());
        assertEquals("local.jpg", flaw.getFlawImage());
        assertEquals("http://img", flaw.getFlawImageUrl());
        assertEquals("rtsp://cam", flaw.getFlawRtsp());
        assertEquals(0, flaw.getShown());
        assertEquals(1, flaw.getConfirmed());
        assertEquals(0, flaw.getUploaded());
        assertEquals("中", flaw.getLevel());
        assertEquals(1, flaw.getCountNum());
        assertEquals(2.0, flaw.getFlawLength());
        assertEquals(0.5, flaw.getFlawArea());
        assertEquals("模型识别", flaw.getSource());
        assertEquals("需复核", flaw.getRemark());
        assertEquals(now, flaw.getCreateTime());
        assertEquals(0, flaw.getDeleteFlag());
    }

    @Test
    void sensorIotUploadAndConfigEntitiesShouldStoreFields() {
        LocalDateTime now = LocalDateTime.now();

        AgvSensorRecord sensor = new AgvSensorRecord();
        sensor.setId(1L);
        sensor.setTaskId(2L);
        sensor.setSensorType("smoke");
        sensor.setSensorName("烟雾传感器");
        sensor.setSensorValue("55");
        sensor.setStatus("异常");
        sensor.setDistance(5.5);
        sensor.setAction("报警");
        sensor.setRemark("烟雾异常");
        sensor.setCreateTime(now);
        sensor.setDeleteFlag(0);
        assertEquals("smoke", sensor.getSensorType());
        assertEquals("烟雾传感器", sensor.getSensorName());
        assertEquals("55", sensor.getSensorValue());
        assertEquals("异常", sensor.getStatus());
        assertEquals(5.5, sensor.getDistance());
        assertEquals("报警", sensor.getAction());
        assertEquals("烟雾异常", sensor.getRemark());
        assertEquals(now, sensor.getCreateTime());
        assertEquals(0, sensor.getDeleteFlag());

        AgvIotActionRecord action = new AgvIotActionRecord();
        action.setId(3L);
        action.setTaskId(2L);
        action.setTriggerType("sensor");
        action.setCommandText("传感器异常自动触发");
        action.setSceneName("烟雾安全报警模式");
        action.setDeviceType("alarm");
        action.setDeviceName("声光报警器");
        action.setAction("报警");
        action.setBeforeStatus("关闭");
        action.setAfterStatus("报警中");
        action.setResult("成功");
        action.setFeedback("已报警");
        action.setSensorRecordId(1L);
        action.setFlawId(10L);
        action.setSceneType("fire");
        action.setCreateTime(now);
        action.setDeleteFlag(0);
        assertEquals("sensor", action.getTriggerType());
        assertEquals("alarm", action.getDeviceType());
        assertEquals("报警中", action.getAfterStatus());
        assertEquals(1L, action.getSensorRecordId());
        assertEquals(10L, action.getFlawId());
        assertEquals("fire", action.getSceneType());

        AgvUploadRecord upload = new AgvUploadRecord();
        upload.setId(4L);
        upload.setTaskId(2L);
        upload.setInfo("任务信息");
        upload.setType("任务");
        upload.setStatus("已上传");
        upload.setUploadTime(now);
        upload.setRemark("上传成功");
        upload.setCreateTime(now);
        upload.setProgress(100);
        upload.setUploadResult("OK");
        upload.setDeleteFlag(0);
        assertEquals("任务信息", upload.getInfo());
        assertEquals("任务", upload.getType());
        assertEquals("已上传", upload.getStatus());
        assertEquals(100, upload.getProgress());
        assertEquals("OK", upload.getUploadResult());

        AgvConfig config = new AgvConfig();
        config.setId(5L);
        config.setHost("192.168.2.2");
        config.setDrivePort(9001);
        config.setAnalysisPort(9002);
        config.setCloudUrl("http://cloud");
        config.setCloudApiKey("key");
        config.setDbHost("localhost");
        config.setDbPort(3306);
        config.setDbName("agv");
        config.setDbUsername("root");
        config.setDbPassword("1234");
        config.setControlProtocol("http");
        config.setCam1("rtsp://cam1");
        config.setUsername1("admin");
        config.setPassword1("pwd");
        config.setCam2("rtsp://cam2");
        config.setUsername2("admin2");
        config.setPassword2("pwd2");
        config.setCam3("rtsp://cam3");
        config.setUsername3("admin3");
        config.setPassword3("pwd3");
        config.setCam4("rtsp://cam4");
        config.setUsername4("admin4");
        config.setPassword4("pwd4");
        config.setUpdateTime(now);
        config.setDeleteFlag(0);
        assertEquals("192.168.2.2", config.getHost());
        assertEquals(9001, config.getDrivePort());
        assertEquals(9002, config.getAnalysisPort());
        assertEquals("http://cloud", config.getCloudUrl());
        assertEquals("key", config.getCloudApiKey());
        assertEquals("localhost", config.getDbHost());
        assertEquals(3306, config.getDbPort());
        assertEquals("agv", config.getDbName());
        assertEquals("root", config.getDbUsername());
        assertEquals("1234", config.getDbPassword());
        assertEquals("http", config.getControlProtocol());
        assertEquals("rtsp://cam1", config.getCam1());
        assertEquals("admin", config.getUsername1());
        assertEquals("pwd", config.getPassword1());
        assertEquals("rtsp://cam2", config.getCam2());
        assertEquals("admin2", config.getUsername2());
        assertEquals("pwd2", config.getPassword2());
        assertEquals("rtsp://cam3", config.getCam3());
        assertEquals("admin3", config.getUsername3());
        assertEquals("pwd3", config.getPassword3());
        assertEquals("rtsp://cam4", config.getCam4());
        assertEquals("admin4", config.getUsername4());
        assertEquals("pwd4", config.getPassword4());
        assertEquals(now, config.getUpdateTime());
        assertEquals(0, config.getDeleteFlag());
    }
}

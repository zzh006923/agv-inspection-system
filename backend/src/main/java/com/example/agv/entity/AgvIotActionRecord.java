package com.example.agv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * AIoT 联动执行记录
 * 用于记录由传感器、语音指令、场景模式触发的执行设备动作，
 * 例如补光灯、远程断电模块、声光报警、AGV 停车等。
 */
@TableName("agv_iot_action_record")
public class AgvIotActionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String triggerType;
    private String commandText;
    private String sceneName;
    private String deviceType;
    private String deviceName;
    private String action;
    private String result;
    private String feedback;
    private LocalDateTime createTime;
    private Long sensorRecordId;
    private Long flawId;
    private String sceneType;
    private String beforeStatus;
    private String afterStatus;

    @TableLogic
    private Integer deleteFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public String getCommandText() { return commandText; }
    public void setCommandText(String commandText) { this.commandText = commandText; }

    public String getSceneName() { return sceneName; }
    public void setSceneName(String sceneName) { this.sceneName = sceneName; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Integer getDeleteFlag() { return deleteFlag; }
    public void setDeleteFlag(Integer deleteFlag) { this.deleteFlag = deleteFlag; }
    
    public Long getSensorRecordId() { return sensorRecordId; }
    public void setSensorRecordId(Long sensorRecordId) { this.sensorRecordId = sensorRecordId; }

    public Long getFlawId() { return flawId; }
    public void setFlawId(Long flawId) { this.flawId = flawId; }

    public String getSceneType() { return sceneType; }
    public void setSceneType(String sceneType) { this.sceneType = sceneType; }

    public String getBeforeStatus() { return beforeStatus; }
    public void setBeforeStatus(String beforeStatus) { this.beforeStatus = beforeStatus; }

    public String getAfterStatus() { return afterStatus; }
    public void setAfterStatus(String afterStatus) { this.afterStatus = afterStatus; }
}

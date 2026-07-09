package com.example.agv.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SensorReportDTO {

    @JsonAlias({"device_id", "device"})
    private String deviceId;

    @JsonAlias({"task_id"})
    private Long taskId;

    @JsonAlias({"sensor_type", "type"})
    private String sensorType;

    @JsonAlias({"temp"})
    private BigDecimal temperature;

    @JsonAlias({"humi"})
    private BigDecimal humidity;

    @JsonAlias({"person", "detected"})
    private Boolean personDetected;

    @JsonAlias({"lightLux", "lux", "light_value"})
    private Double lightValue;

    @JsonAlias({"smoke", "smoke_detected"})
    private Boolean smokeDetected;

    @JsonAlias({"smoke_value"})
    private Double smokeValue;

    private Double distance;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportTime;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

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

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public BigDecimal getHumidity() {
        return humidity;
    }

    public void setHumidity(BigDecimal humidity) {
        this.humidity = humidity;
    }

    public Boolean getPersonDetected() {
        return personDetected;
    }

    public void setPersonDetected(Boolean personDetected) {
        this.personDetected = personDetected;
    }

    public Double getLightValue() {
        return lightValue;
    }

    public void setLightValue(Double lightValue) {
        this.lightValue = lightValue;
    }

    public Boolean getSmokeDetected() {
        return smokeDetected;
    }

    public void setSmokeDetected(Boolean smokeDetected) {
        this.smokeDetected = smokeDetected;
    }

    public Double getSmokeValue() {
        return smokeValue;
    }

    public void setSmokeValue(Double smokeValue) {
        this.smokeValue = smokeValue;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public LocalDateTime getReportTime() {
        return reportTime;
    }

    public void setReportTime(LocalDateTime reportTime) {
        this.reportTime = reportTime;
    }
}
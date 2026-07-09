package com.example.agv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;

import java.time.LocalDateTime;


@TableName("agv_config")
public class AgvConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String host;

    private Integer drivePort;

    private Integer analysisPort;

    private String cloudUrl;

    private String cloudApiKey;

    private String dbHost;

    private Integer dbPort;

    private String dbName;

    private String dbUsername;

    private String dbPassword;

    private String controlProtocol;

    private String cam1;

    private String username1;

    private String password1;

    private String cam2;

    private String username2;

    private String password2;

    private String cam3;

    private String username3;

    private String password3;

    private String cam4;

    private String username4;

    private String password4;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleteFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getDrivePort() {
        return drivePort;
    }

    public void setDrivePort(Integer drivePort) {
        this.drivePort = drivePort;
    }

    public Integer getAnalysisPort() {
        return analysisPort;
    }

    public void setAnalysisPort(Integer analysisPort) {
        this.analysisPort = analysisPort;
    }

    public String getCloudUrl() {
        return cloudUrl;
    }

    public void setCloudUrl(String cloudUrl) {
        this.cloudUrl = cloudUrl;
    }

    public String getCloudApiKey() {
        return cloudApiKey;
    }

    public void setCloudApiKey(String cloudApiKey) {
        this.cloudApiKey = cloudApiKey;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public Integer getDbPort() {
        return dbPort;
    }

    public void setDbPort(Integer dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getControlProtocol() {
        return controlProtocol;
    }

    public void setControlProtocol(String controlProtocol) {
        this.controlProtocol = controlProtocol;
    }

    public String getCam1() {
        return cam1;
    }

    public void setCam1(String cam1) {
        this.cam1 = cam1;
    }

    public String getUsername1() {
        return username1;
    }

    public void setUsername1(String username1) {
        this.username1 = username1;
    }

    public String getPassword1() {
        return password1;
    }

    public void setPassword1(String password1) {
        this.password1 = password1;
    }

    public String getCam2() {
        return cam2;
    }

    public void setCam2(String cam2) {
        this.cam2 = cam2;
    }

    public String getUsername2() {
        return username2;
    }

    public void setUsername2(String username2) {
        this.username2 = username2;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    public String getCam3() {
        return cam3;
    }

    public void setCam3(String cam3) {
        this.cam3 = cam3;
    }

    public String getUsername3() {
        return username3;
    }

    public void setUsername3(String username3) {
        this.username3 = username3;
    }

    public String getPassword3() {
        return password3;
    }

    public void setPassword3(String password3) {
        this.password3 = password3;
    }

    public String getCam4() {
        return cam4;
    }

    public void setCam4(String cam4) {
        this.cam4 = cam4;
    }

    public String getUsername4() {
        return username4;
    }

    public void setUsername4(String username4) {
        this.username4 = username4;
    }

    public String getPassword4() {
        return password4;
    }

    public void setPassword4(String password4) {
        this.password4 = password4;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

}

package com.example.agv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;

import java.time.LocalDateTime;


@TableName("agv_flaw")
public class AgvFlaw {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Integer round;

    private String flawType;

    private String flawName;

    private String flawDesc;

    private Double flawDistance;

    private String flawImage;

    private String flawImageUrl;

    private String flawRtsp;

    private Integer shown;

    private Integer confirmed;

    private Integer uploaded;

    private String level;

    private Integer countNum;

    private Double flawLength;

    private Double flawArea;

    private String source;

    private String remark;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleteFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public String getFlawType() {
        return flawType;
    }

    public void setFlawType(String flawType) {
        this.flawType = flawType;
    }

    public String getFlawName() {
        return flawName;
    }

    public void setFlawName(String flawName) {
        this.flawName = flawName;
    }

    public String getFlawDesc() {
        return flawDesc;
    }

    public void setFlawDesc(String flawDesc) {
        this.flawDesc = flawDesc;
    }

    public Double getFlawDistance() {
        return flawDistance;
    }

    public void setFlawDistance(Double flawDistance) {
        this.flawDistance = flawDistance;
    }

    public String getFlawImage() {
        return flawImage;
    }

    public void setFlawImage(String flawImage) {
        this.flawImage = flawImage;
    }

    public String getFlawImageUrl() {
        return flawImageUrl;
    }

    public void setFlawImageUrl(String flawImageUrl) {
        this.flawImageUrl = flawImageUrl;
    }

    public String getFlawRtsp() {
        return flawRtsp;
    }

    public void setFlawRtsp(String flawRtsp) {
        this.flawRtsp = flawRtsp;
    }

    public Integer getShown() {
        return shown;
    }

    public void setShown(Integer shown) {
        this.shown = shown;
    }

    public Integer getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Integer confirmed) {
        this.confirmed = confirmed;
    }

    public Integer getUploaded() {
        return uploaded;
    }

    public void setUploaded(Integer uploaded) {
        this.uploaded = uploaded;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getCountNum() {
        return countNum;
    }

    public void setCountNum(Integer countNum) {
        this.countNum = countNum;
    }

    public Double getFlawLength() {
        return flawLength;
    }

    public void setFlawLength(Double flawLength) {
        this.flawLength = flawLength;
    }

    public Double getFlawArea() {
        return flawArea;
    }

    public void setFlawArea(Double flawArea) {
        this.flawArea = flawArea;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

}

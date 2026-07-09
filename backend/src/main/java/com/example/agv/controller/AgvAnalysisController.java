package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.entity.AgvFlaw;
import com.example.agv.mapper.AgvFlawMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/agv/analysis")
@CrossOrigin
public class AgvAnalysisController {

    @Resource
    private AgvFlawMapper agvFlawMapper;

    /**
     * 接收裂缝识别算法结果
     */
    @PostMapping("/result")
    public AjaxResult receiveAnalysisResult(@RequestBody AnalysisResultRequest request) {
        if (request.getTaskId() == null) {
            return AjaxResult.error("任务ID不能为空");
        }

        AgvFlaw flaw = new AgvFlaw();
        flaw.setTaskId(request.getTaskId());
        flaw.setRound(request.getRound() == null ? 1 : request.getRound());

        flaw.setFlawType("结构缺陷");
        flaw.setFlawName("隧道裂缝");
        flaw.setFlawDesc(request.getDescription() == null
                ? "裂缝分割模型检测到隧道壁面存在疑似裂缝"
                : request.getDescription());

        flaw.setFlawDistance(request.getDistance());
        flaw.setFlawImage(request.getImageUrl());
        flaw.setFlawImageUrl(request.getImageUrl());
        flaw.setFlawRtsp(request.getRtspUrl());

        flaw.setFlawLength(request.getCrackLength() == null ? 0.0 : request.getCrackLength());
        flaw.setFlawArea(request.getCrackArea() == null ? 0.0 : request.getCrackArea());
        flaw.setLevel(request.getLevel() == null ? "中" : request.getLevel());
        flaw.setSource("裂缝分割模型");

        flaw.setShown(0);
        flaw.setConfirmed(0);
        flaw.setUploaded(0);
        flaw.setCountNum(1);
        flaw.setRemark("基于 Crack-seg 裂缝分割数据集训练模型生成的识别结果");

        flaw.setCreateTime(LocalDateTime.now());
        flaw.setDeleteFlag(0);

        agvFlawMapper.insert(flaw);

        return AjaxResult.success(flaw);
    }

    public static class AnalysisResultRequest {
        private Long taskId;
        private Integer round;
        private Double distance;
        private String imageUrl;
        private String rtspUrl;
        private Double crackLength;
        private Double crackArea;
        private Double confidence;
        private String level;
        private String description;

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

        public Double getDistance() {
            return distance;
        }

        public void setDistance(Double distance) {
            this.distance = distance;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getRtspUrl() {
            return rtspUrl;
        }

        public void setRtspUrl(String rtspUrl) {
            this.rtspUrl = rtspUrl;
        }

        public Double getCrackLength() {
            return crackLength;
        }

        public void setCrackLength(Double crackLength) {
            this.crackLength = crackLength;
        }

        public Double getCrackArea() {
            return crackArea;
        }

        public void setCrackArea(Double crackArea) {
            this.crackArea = crackArea;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
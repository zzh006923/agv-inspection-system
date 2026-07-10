package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.dto.ai.AiChatRequest;
import com.example.agv.dto.ai.AiChatResponse;
import com.example.agv.service.ai.AgvAiService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/agv/ai")
@CrossOrigin
public class AgvAiController {

    @Resource
    private AgvAiService agvAiService;

    /**
     * 自由问答：支持用户围绕当前任务连续追问。
     */
    @PostMapping("/chat")
    public AjaxResult chat(@RequestBody AiChatRequest request) {
        AiChatResponse response = agvAiService.chat(request);
        return AjaxResult.success(response);
    }

    /**
     * 任务复盘：针对当前任务生成 AI 辅助判断和处理建议。
     */
    @PostMapping("/task-review/{taskId}")
    public AjaxResult taskReview(@PathVariable Long taskId,
                                 @RequestBody(required = false) AiChatRequest request) {
        AiChatResponse response = agvAiService.taskReview(taskId, request);
        return AjaxResult.success(response);
    }

    /**
     * 故障复盘：针对单条故障生成研判、误报可能和建议备注。
     */
    @PostMapping("/flaw-review/{flawId}")
    public AjaxResult flawReview(@PathVariable Long flawId,
                                 @RequestBody(required = false) AiChatRequest request) {
        AiChatResponse response = agvAiService.flawReview(flawId, request);
        return AjaxResult.success(response);
    }
}

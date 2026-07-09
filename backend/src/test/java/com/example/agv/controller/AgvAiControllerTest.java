package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.dto.ai.AiChatRequest;
import com.example.agv.dto.ai.AiChatResponse;
import com.example.agv.service.ai.AgvAiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgvAiControllerTest {

    @Mock
    private AgvAiService agvAiService;

    @InjectMocks
    private AgvAiController agvAiController;

    @Test
    void chatShouldReturnAiResponse() {
        AiChatRequest request = new AiChatRequest();
        request.setQuestion("当前任务能否上传");
        AiChatResponse response = new AiChatResponse("建议先复核", "conv-001", "msg-001");
        when(agvAiService.chat(request)).thenReturn(response);

        AjaxResult result = agvAiController.chat(request);

        assertEquals(200, result.getCode());
        assertSame(response, result.getData());
    }

    @Test
    void taskReviewShouldReturnAiResponse() {
        AiChatRequest request = new AiChatRequest();
        AiChatResponse response = new AiChatResponse("任务复盘结果", "conv-001", "msg-001");
        when(agvAiService.taskReview(1L, request)).thenReturn(response);

        AjaxResult result = agvAiController.taskReview(1L, request);

        assertEquals(200, result.getCode());
        assertSame(response, result.getData());
    }

    @Test
    void flawReviewShouldReturnAiResponse() {
        AiChatRequest request = new AiChatRequest();
        AiChatResponse response = new AiChatResponse("故障研判结果", "conv-001", "msg-001");
        when(agvAiService.flawReview(10L, request)).thenReturn(response);

        AjaxResult result = agvAiController.flawReview(10L, request);

        assertEquals(200, result.getCode());
        assertSame(response, result.getData());
    }
}

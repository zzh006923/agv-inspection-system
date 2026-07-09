package com.example.agv.service.ai;

import com.example.agv.dto.ai.AiChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class DifyServiceTest {

    @Test
    void chatShouldReturnConfigMessageWhenApiKeyIsEmpty() {
        DifyService difyService = new DifyService();
        ReflectionTestUtils.setField(difyService, "difyApiKey", " ");

        AiChatResponse response = difyService.chat("当前任务是否可以上传", "conv-001");

        assertTrue(response.getAnswer().contains("Dify API Key 未配置"));
        assertEquals("conv-001", response.getConversationId());
        assertEquals("", response.getMessageId());
    }

    @Test
    void chatShouldUseEmptyConversationIdWhenConversationIdIsNull() {
        DifyService difyService = new DifyService();
        ReflectionTestUtils.setField(difyService, "difyApiKey", "");

        AiChatResponse response = difyService.chat("你好", null);

        assertTrue(response.getAnswer().contains("Dify API Key 未配置"));
        assertEquals("", response.getConversationId());
    }
}

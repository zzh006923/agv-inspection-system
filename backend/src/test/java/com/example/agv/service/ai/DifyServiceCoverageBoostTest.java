package com.example.agv.service.ai;

import com.example.agv.dto.ai.AiChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DifyServiceCoverageBoostTest {

    @Test
    void chatShouldCallRemoteAndCleanThinkTags() {
        DifyService service = new DifyService();
        ReflectionTestUtils.setField(service, "difyBaseUrl", " http://dify.example.com/v1/// ");
        ReflectionTestUtils.setField(service, "difyApiKey", " key-001 ");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("http://dify.example.com/v1/chat-messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"answer\":\"<think>推理过程</think>最终建议\",\"conversation_id\":\"conv-2\",\"message_id\":\"msg-2\"}", MediaType.APPLICATION_JSON));

        AiChatResponse response = service.chat("当前任务能否上传", "conv-1");

        assertEquals("最终建议", response.getAnswer());
        assertEquals("conv-2", response.getConversationId());
        assertEquals("msg-2", response.getMessageId());
        server.verify();
    }

    @Test
    void chatShouldReturnFallbackWhenRemoteCallFails() {
        DifyService failedService = new DifyService();
        ReflectionTestUtils.setField(failedService, "difyBaseUrl", "http://dify.example.com/v1");
        ReflectionTestUtils.setField(failedService, "difyApiKey", "key");
        RestTemplate failedRestTemplate = (RestTemplate) ReflectionTestUtils.getField(failedService, "restTemplate");
        MockRestServiceServer failedServer = MockRestServiceServer.createServer(failedRestTemplate);
        failedServer.expect(requestTo("http://dify.example.com/v1/chat-messages"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("error"));

        AiChatResponse failed = failedService.chat(null, "conv");

        assertTrue(failed.getAnswer().contains("调用 Dify 失败"));
        assertEquals("conv", failed.getConversationId());
        failedServer.verify();
    }

}

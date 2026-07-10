package com.example.agv.service.ai;

import com.example.agv.dto.ai.AiChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@Service
public class DifyService {

    @Value("${dify.base-url:http://localhost/v1}")
    private String difyBaseUrl;

    @Value("${dify.api-key:}")
    private String difyApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public AiChatResponse chat(String query, String conversationId) {
        if (difyApiKey == null || difyApiKey.trim().isEmpty()) {
            return new AiChatResponse("Dify API Key 未配置，请先在 application.yml 中配置 dify.api-key。", safeConversationId(conversationId), "");
        }

        String url = trimEndSlash(difyBaseUrl) + "/chat-messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.setBearerAuth(difyApiKey.trim());

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", new HashMap<>());
        body.put("query", query == null ? "" : query);
        body.put("response_mode", "blocking");
        body.put("conversation_id", safeConversationId(conversationId));
        body.put("user", "agv-operator");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map responseBody = response.getBody();
            if (responseBody == null) {
                return new AiChatResponse("AI 服务暂无响应，请稍后重试。", safeConversationId(conversationId), "");
            }

            String answer = toStringValue(responseBody.get("answer"));
            String newConversationId = toStringValue(responseBody.get("conversation_id"));
            String messageId = toStringValue(responseBody.get("message_id"));
            return new AiChatResponse(cleanAnswer(answer), newConversationId, messageId);
        } catch (RestClientException e) {
            return new AiChatResponse("调用 Dify 失败，请确认 Dify 服务、Ollama 模型和 API Key 是否正常。错误信息：" + e.getMessage(), safeConversationId(conversationId), "");
        }
    }

    private String cleanAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer
                .replaceAll("(?s)<think>.*?</think>", "")
                .replace("<think>", "")
                .replace("</think>", "")
                .trim();
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safeConversationId(String conversationId) {
        return conversationId == null ? "" : conversationId;
    }

    private String trimEndSlash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "http://localhost/v1";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}

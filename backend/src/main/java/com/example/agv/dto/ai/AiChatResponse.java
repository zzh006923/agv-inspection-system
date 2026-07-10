package com.example.agv.dto.ai;

/**
 * 后端返回给前端的 AI 回复结构。
 */
public class AiChatResponse {

    private String answer;
    private String conversationId;
    private String messageId;

    public AiChatResponse() {
    }

    public AiChatResponse(String answer, String conversationId, String messageId) {
        this.answer = answer;
        this.conversationId = conversationId;
        this.messageId = messageId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}

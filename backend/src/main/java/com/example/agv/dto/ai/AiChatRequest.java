package com.example.agv.dto.ai;

/**
 * 前端调用 AI 助手时传入的请求参数。
 * taskId / flawId 用于后端自动查询当前任务、故障、传感器和联动记录；
 * question 是用户自由提问；conversationId 用于 Dify 连续对话。
 */
public class AiChatRequest {

    private Long taskId;
    private Long flawId;
    private String question;
    private String context;
    private String conversationId;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getFlawId() {
        return flawId;
    }

    public void setFlawId(Long flawId) {
        this.flawId = flawId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}

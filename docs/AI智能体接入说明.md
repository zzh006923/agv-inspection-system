# AI智能体接入说明

## 1. 文档目的

说明AGV智能巡检系统中AI智能体的接入方式，包括后端服务结构、Dify API集成、前端组件调用以及数据交互流程。

AI智能体用于辅助运维人员处理巡检任务，覆盖自由问答、任务复盘、故障研判和备注生成四个场景。

## 2. 文件结构

后端路径（src/main/java/com/example/agv/）：

```
src/main/java/com/example/agv/
├── controller/
│   └── AgvAiController.java
├── service/ai/
│   ├── AgvAiService.java
│   └── DifyService.java
└── dto/ai/
    ├── AiChatRequest.java
    └── AiChatResponse.java
```

前端路径（src/）：

```
src/
├── components/
│   ├── AiAssistantPanel.vue
│   └── AiAssistantPanel.test.js
└── api/
    └── agv.js
```

## 3. 后端接口

控制器AgvAiController提供三个POST接口，路径统一以 /agv/ai开头。

### 3.1自由问答

```
POST /agv/ai/chat
```

接收AiChatRequest，由AgvAiService组装prompt后调用DifyService.chat()，返回AiChatResponse。

请求体主要字段：

```
taskId            任务ID
flawId            故障ID（可选）
question          用户问题
context           前端补充的页面上下文
conversationId    Dify对话ID（多轮对话用）
```

### 3.2任务复盘

```
POST /agv/ai/task-review/{taskId}
```

AgvAiService查询任务及其关联的故障、传感器、AIoT记录，按固定格式拼接上下文，要求Dify按"总体判断、关键风险、异常关联分析、下一步操作建议、系统处理建议"五个维度回答。

### 3.3故障研判

```
POST /agv/ai/flaw-review/{flawId}
```

AgvAiService查询单条故障及其所属任务的上下文，要求Dify按"故障判断、判断依据、误报可能、处理建议、建议备注"五个维度回答。

## 4. 上下文构建策略

AgvAiService.buildContext() 组装六类数据：

- 任务信息（编号、名称、状态、地点、执行人）
- 任务风险概览（故障总数、待确认数、传感器异常数）
- 当前选中故障详情（如有）
- 故障列表（最多10条，按时间倒序）
- 传感器记录（最多10条，按时间倒序）
- AIoT联动记录（最多10条，按时间倒序）

上下文不是逐字段转储，而是筛选后的摘要，让Dify基于有效信息做判断。

## 5. DifyService调用逻辑

DifyService核心流程：

```
1. 检查dify.api-key是否配置，未配置则返回提示信息
2. 拼接Dify Chat API地址：{base-url}/chat-messages
3. 设置HTTP头：Content-Type application/json，Authorization Bearer {api-key}
4. 构造请求体，response_mode为blocking
5. 调用RestTemplate.exchange() 发送POST请求
6. 从响应中提取answer、conversation_id、message_id
7. 对answer执行cleanAnswer() 过滤think标签
8. 返回AiChatResponse
```

对应后端配置项：

```yaml
dify:
  base-url: http://localhost/v1
  api-key: "app-TZezjmuxO8Wm5C4KaS2ddDHO"
```

## 6. 前端调用方式

AiAssistantPanel.vue通过aiApi对象调用后端接口：

```javascript
// 自由问答
aiApi.chat({
  taskId,
  flawId,
  question,
  context,
  conversationId
})

// 任务复盘
aiApi.taskReview(taskId, payload)

// 故障研判
aiApi.flawReview(flawId, payload)
```

组件内部流程：

```
用户输入问题或点击快捷按钮
  → sendToAi(text, mode)
    → buildContext() 从props中组装上下文
    → 根据mode选择调用chat / taskReview / flawReview
    → unwrap(result) 解析响应
    → normalizeAnswer() 过滤异常文本
    → 追加到messages数组并滚动到底部
```

异常处理覆盖四种情况：

- 空回复：显示默认提示"AI暂未返回有效内容"
- 异常文本：检测Traceback / Internal Server Error / SyntaxError等模式并自动隐藏
- 超长内容：超过2000字符自动截断
- 后端500错误：返回具体操作建议

## 7. 时序

```
用户操作 → AiAssistantPanel → aiApi → AgvAiController → AgvAiService
  → DifyService → HTTP POST → Dify Chat API → Ollama → Dify回复
  → DifyService → AgvAiService → AgvAiController → aiApi → AiAssistantPanel → 展示
```

## 8. 注意事项

Dify API Key建议通过环境变量注入而非硬编码在配置文件中提交到仓库。AI辅助判断不能替代人工现场复核。当任务数据量较大时，上下文中只包含最近5到10条记录，不覆盖全部历史数据。
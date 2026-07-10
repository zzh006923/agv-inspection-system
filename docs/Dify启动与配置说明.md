# Dify启动与配置说明

## 1. 文档目的

说明AGV智能巡检系统中AI能力依赖的Dify平台如何安装、启动和配置。

系统使用Dify编排Chat Flow工作流，对接Ollama本地模型，为巡检任务提供AI辅助判断。Dify和Ollama均需正常运行，AI助手功能才能使用。

## 2. 组件关系

AGV后端服务以HTTP POST方式调用Dify API，Dify工作流中的LLM节点对接Ollama提供的本地模型，Ollama默认监听11434端口。

## 3. Dify安装

### 3.1 Docker部署（推荐）

```bash
git clone https://github.com/langgenius/dify.git
cd dify/docker
cp .env.example .env
docker compose up -d
```

启动后访问http://localhost，首次访问时注册管理员账号。

### 3.2环境要求

CPU 4核以上，内存8 GB以上，磁盘50 GB以上可用空间，Docker Engine 20.10+，Docker Compose v2.0+。

## 4. Ollama模型配置

### 4.1安装

Linux / macOS：
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

Windows：从https://ollama.com/download下载安装包。

### 4.2下载模型

```bash
ollama pull qwen2.5:7b
ollama pull llama3.1:8b
```

### 4.3允许外部访问

Linux：
```bash
export OLLAMA_HOST=0.0.0.0
```

Windows PowerShell：
```powershell
$env:OLLAMA_HOST = "0.0.0.0"
```

Dify使用Docker部署时，Dify容器需访问宿主机Ollama，Linux下使用host.docker.internal或宿主机IP地址。

## 5. Dify工作流配置

### 5.1添加模型供应商

登录Dify管理台，进入设置 -> 模型供应商，添加Ollama：

- 模型名称：qwen2.5:7b（与已下载模型一致）
- 基础URL：http://host.docker.internal:11434
- 模型类型：LLM

### 5.2创建Chat Flow应用

在工作室页面创建新应用，类型选择Chat Flow，命名为"AGV智能巡检助手"。

编辑工作流，添加LLM节点，配置SYSTEM Prompt：

```
你是一名地铁隧道巡检运维专家。你的职责是根据现场巡检数据，
协助运维人员判断任务是否可以上传、故障是否可能误报、下一步如何操作。
回答要简明、专业、直接，不要复述页面字段，优先给出结论和建议。
```

温度设为0.3，最大Token设为2000。发布应用后生成API Key。

## 6. 后端配置

编辑application.yml：

```yaml
dify:
  base-url: http://localhost/v1
  api-key: "app-xxxxxxxxxxxx"
```

API Key建议通过环境变量DIFY_API_KEY注入，避免硬编码在配置文件中。

验证连接：

```bash
curl -X POST "http://localhost:8088/agv/ai/chat" \
  -H "Content-Type: application/json" \
  -d '{"taskId": 1, "question": "当前任务状态如何？", "context": "任务信息：\n- 任务状态：待上传\n- 故障总数：2\n- 未确认故障数：1"}'
```

正常返回时data.answer字段包含AI回复内容。

## 7. 前端环境变量

```
VITE_API_BASE_URL=http://localhost:8088
```

前端通过后端代理调用Dify，不直接暴露Dify API Key。

## 8. 常见问题

### 8.1 Dify容器无法启动

```bash
docker compose logs
```

常见原因包括端口冲突（修改 .env中的EXPOSE_NGINX_PORT）、磁盘不足（清理Docker空间）、内存不足（增加Docker资源限制）。

### 8.2 Ollama模型加载失败

```bash
ollama ps
ollama pull qwen2.5:7b
```

检查Ollama端口监听状态：

```bash
netstat -ano | grep 11434
```

### 8.3 Dify报模型供应商找不到

进入Dify设置确认Ollama已添加且状态为已连接。Docker部署时基础URL使用host.docker.internal而非localhost。

### 8.4 Dify报500

检查Dify工作流中LLM节点配置的模型是否与Ollama中已下载的模型一致。在Dify中测试LLM节点是否正常工作。确认Ollama服务可用：

```bash
curl http://localhost:11434/api/tags
```

### 8.5 API Key安全

开发环境使用环境变量注入，不硬编码在配置文件。提交代码时将真实API Key添加到 .gitignore。定期在Dify管理台重新生成API Key。

## 9. 启动检查清单

- Dify服务已启动并正常运行
- Ollama已安装并下载了兼容模型
- Dify中已添加Ollama模型供应商
- Dify中已创建Chat Flow应用并发布
- API Key已生成并配置到后端
- 后端服务已重启使配置生效
- curl验证AI接口返回正常
- 前端VITE_API_BASE_URL指向正确的后端地址
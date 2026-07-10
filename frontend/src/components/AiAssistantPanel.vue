<template>
  <el-card class="ai-assistant-card" shadow="never">
    <div class="ai-header">
      <div class="ai-title-text">
        <h3>智能巡检 AI 助手</h3>
        <p>支持自由问答、上传判断、巡检复盘和故障研判</p>
      </div>
      <el-tag type="success" effect="dark" class="dify-tag">Dify</el-tag>
    </div>

    <div class="quick-actions">
      <el-button size="small" @click="askPreset('这个任务现在能不能直接上传？为什么？')">是否可上传</el-button>
      <el-button size="small" @click="askPreset('当前任务是否需要人工复核？下一步应该怎么做？')">复核建议</el-button>
      <el-button size="small" @click="askTaskReview">巡检复盘</el-button>
      <el-button size="small" :disabled="!selectedFlaw" @click="askFlawReview">故障研判</el-button>
      <el-button
        size="small"
        :disabled="!selectedFlaw"
        @click="askPreset('帮我生成一条故障备注，简短一点，可以直接复制到系统备注栏。')"
      >
        生成备注
      </el-button>
    </div>

    <!-- 只有这里滚动，标题、按钮和输入框都固定不动 -->
    <div ref="chatWindowRef" class="chat-window">
      <div v-if="!messages.length" class="chat-empty">
        <div class="empty-icon">💬</div>
        <p>可以询问：能否上传、是否误报、下一步怎么处理、备注怎么写</p>
      </div>

      <div
        v-for="(m, index) in messages"
        :key="index"
        class="chat-msg"
        :class="m.role"
      >
        <div class="msg-role">{{ m.role === 'user' ? '我' : 'AI助手' }}</div>
        <div class="msg-bubble">{{ m.content }}</div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="question"
        type="textarea"
        :rows="2"
        resize="none"
        clearable
        placeholder="例如：为什么不建议直接上传？这个故障可能是误报吗？"
        @keyup.ctrl.enter="sendQuestion"
      />
      <el-button type="primary" :loading="loading" @click="sendQuestion">发送</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiApi, unwrap } from '../api/agv'

const props = defineProps({
  taskId: {
    type: Number,
    required: true
  },
  task: {
    type: Object,
    default: () => ({})
  },
  flaws: {
    type: Array,
    default: () => []
  },
  sensorRecords: {
    type: Array,
    default: () => []
  },
  actionRecords: {
    type: Array,
    default: () => []
  },
  summary: {
    type: Object,
    default: () => ({})
  },
  selectedFlaw: {
    type: Object,
    default: null
  }
})

const question = ref('')
const loading = ref(false)
const conversationId = ref('')
const messages = ref([])
const chatWindowRef = ref(null)

function buildContext() {
  const task = props.task || {}
  const selectedFlaw = props.selectedFlaw || {}

  const flawText = props.flaws.length
    ? props.flaws.slice(0, 5).map(f => (
      `- ${f.flawName || '故障'}，类型：${f.flawType || '-'}，距离：${f.flawDistance || 0}m，状态：${Number(f.confirmed) === 1 ? '已确认' : '待确认'}，备注：${f.remark || '-'}`
    )).join('\n')
    : '暂无故障记录'

  const sensorText = props.sensorRecords.length
    ? props.sensorRecords.slice(0, 5).map(s => (
      `- ${s.sensorName || s.sensorType || '传感器'}：${s.sensorValue || '-'}，状态：${s.status || '-'}，动作：${s.action || '-'}，备注：${s.remark || '-'}`
    )).join('\n')
    : '暂无传感器记录'

  const actionText = props.actionRecords.length
    ? props.actionRecords.slice(0, 5).map(a => (
      `- ${a.deviceName || a.deviceType || '设备'}，动作：${a.action || '-'}，结果：${a.result || '-'}，反馈：${a.feedback || '-'}`
    )).join('\n')
    : '暂无联动记录'

  return `
任务信息：
- 任务编号：${task.taskCode || '-'}
- 任务名称：${task.taskName || '-'}
- 任务状态：${task.taskStatus || '-'}
- 起始地点：${task.startPos || '-'}
- 执行人：${task.executor || '-'}

上传前检查：
- 故障总数：${props.summary.flawCount ?? props.flaws.length}
- 未确认故障数：${props.summary.unconfirmedCount ?? props.flaws.filter(f => Number(f.confirmed) !== 1).length}
- 是否满足上传条件：${props.summary.canUpload ? '是' : '否'}

当前选中故障：
- 名称：${selectedFlaw.flawName || '-'}
- 类型：${selectedFlaw.flawType || '-'}
- 等级：${selectedFlaw.level || '-'}
- 距离：${selectedFlaw.flawDistance || '-'}m
- 来源：${selectedFlaw.source || '-'}
- 描述：${selectedFlaw.flawDesc || selectedFlaw.remark || '-'}

故障列表：
${flawText}

传感器记录：
${sensorText}

AIoT联动记录：
${actionText}
`.trim()
}

function pickAnswer(data) {
  if (typeof data === 'string') return data
  return (
    data?.answer ||
    data?.data?.answer ||
    data?.result ||
    data?.data?.result ||
    data?.message ||
    data?.msg ||
    ''
  )
}

function normalizeAnswer(data) {
  const raw = String(pickAnswer(data) || '').trim()

  if (!raw) {
    return 'AI 暂未返回有效内容，请检查 Dify 服务、Ollama 模型或后端接口配置。'
  }

  const suspiciousPatterns = [
    /\.venv\\Scripts\\python\.exe/i,
    /realtime_crack_watch\.py/i,
    /Traceback \(most recent call last\)/i,
    /HTTPConnectionPool/i,
    /INTERNAL SERVER ERROR/i,
    /Internal Server Error/i,
    /subprocess-exited-with-error/i,
    /SyntaxError:/i,
    /PowerShell/i
  ]

  if (suspiciousPatterns.some(pattern => pattern.test(raw))) {
    return 'AI 返回了调试信息或异常文本，已自动隐藏。请检查 Dify 工作流、Ollama 模型兼容性、API Key 或后端日志。'
  }

  // 避免过长 JSON / 日志撑坏卡片
  return raw.length > 2000 ? `${raw.slice(0, 2000)}\n\n……内容较长，已截断显示。` : raw
}

function normalizeError(error) {
  const raw = String(error?.response?.data?.msg || error?.response?.data?.message || error?.message || '')

  if (raw.includes('500') || raw.includes('Internal Server Error') || raw.includes('INTERNAL SERVER ERROR')) {
    return 'AI 助手暂时无法连接：Dify 内部服务异常。请检查 Dify 工作流模型是否兼容、Ollama 模型是否可用，以及后端 API Key 是否正确。'
  }

  if (raw.includes('timeout') || raw.includes('timed out')) {
    return 'AI 助手响应超时，请确认 Dify 服务和 Ollama 模型正在运行。'
  }

  return raw || 'AI 请求失败，请检查 Dify 服务、Ollama 模型和后端接口。'
}

async function scrollToBottom() {
  await nextTick()
  const el = chatWindowRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

async function sendToAi(text, mode = 'chat') {
  if (!text || !text.trim()) {
    ElMessage.warning('请输入问题')
    return
  }

  const context = buildContext()
  messages.value.push({ role: 'user', content: text })
  await scrollToBottom()
  loading.value = true

  try {
    let result
    const payload = {
      taskId: props.taskId,
      flawId: props.selectedFlaw?.id,
      question: text,
      context,
      conversationId: conversationId.value
    }

    if (mode === 'taskReview') {
      result = await aiApi.taskReview(props.taskId, payload)
    } else if (mode === 'flawReview' && props.selectedFlaw?.id) {
      result = await aiApi.flawReview(props.selectedFlaw.id, payload)
    } else {
      result = await aiApi.chat(payload)
    }

    const data = unwrap(result, 'AI分析失败')
    conversationId.value = data?.conversationId || conversationId.value
    messages.value.push({ role: 'assistant', content: normalizeAnswer(data) })
    await scrollToBottom()
  } catch (e) {
    const msg = normalizeError(e)
    messages.value.push({ role: 'assistant', content: msg })
    ElMessage.error(msg)
    await scrollToBottom()
  } finally {
    loading.value = false
    question.value = ''
  }
}

function sendQuestion() {
  sendToAi(question.value)
}

function askPreset(text) {
  sendToAi(text)
}

function askTaskReview() {
  sendToAi('请进行巡检复盘，并重点告诉我下一步应该怎么处理。', 'taskReview')
}

function askFlawReview() {
  if (!props.selectedFlaw?.id) {
    ElMessage.warning('请先选择一条故障记录')
    return
  }
  sendToAi('请对当前选中的故障进行研判，判断是否可能属实、是否可能误报，以及下一步如何处理。', 'flawReview')
}
</script>

<style scoped>
.ai-assistant-card {
  border: 1px solid rgba(59, 130, 246, 0.28);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.98), #ffffff);
  overflow: hidden;
}

.ai-assistant-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  overflow: hidden;
}

.ai-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.ai-title-text h3 {
  margin: 0;
  color: #1f2937;
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
}

.ai-title-text p {
  margin: 6px 0 0;
  color: var(--agv-muted, #64748b);
  font-size: 13px;
  line-height: 1.5;
}

.dify-tag {
  flex-shrink: 0;
  border-radius: 10px;
  padding: 0 12px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex-shrink: 0;
}

.quick-actions :deep(.el-button) {
  margin-left: 0;
  border-radius: 9px;
}

.chat-window {
  height: 260px;
  overflow-y: auto;
  overflow-x: hidden;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 16px;
  padding: 12px;
  background: #f8fafc;
  box-sizing: border-box;
  flex-shrink: 0;
  scrollbar-width: thin;
}

.chat-window::-webkit-scrollbar {
  width: 8px;
}

.chat-window::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.55);
}

.chat-window::-webkit-scrollbar-track {
  background: transparent;
}

.chat-empty {
  height: 100%;
  min-height: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  text-align: center;
  color: #94a3b8;
  line-height: 1.7;
}

.chat-empty p {
  max-width: 260px;
  margin: 0 auto;
}

.empty-icon {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: #eef6ff;
  font-size: 24px;
}

.chat-msg {
  margin-bottom: 12px;
  line-height: 1.6;
}

.chat-msg:last-child {
  margin-bottom: 0;
}

.msg-role {
  font-weight: 700;
  font-size: 12px;
  margin-bottom: 5px;
}

.chat-msg.user {
  text-align: right;
}

.chat-msg.user .msg-role {
  color: #2563eb;
}

.chat-msg.assistant .msg-role {
  color: #047857;
}

.msg-bubble {
  display: inline-block;
  max-width: 92%;
  padding: 9px 11px;
  border-radius: 13px;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
  font-size: 14px;
  text-align: left;
}

.chat-msg.user .msg-bubble {
  background: #dbeafe;
  color: #1e3a8a;
}

.chat-msg.assistant .msg-bubble {
  background: #ffffff;
  color: #334155;
  border: 1px solid #e2e8f0;
}

.chat-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: end;
  flex-shrink: 0;
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: 12px;
  line-height: 1.5;
}

.chat-input :deep(.el-button) {
  height: 54px;
  border-radius: 12px;
}
</style>

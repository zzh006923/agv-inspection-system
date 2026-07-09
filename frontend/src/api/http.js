import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8088',
  timeout: 12000
})

function friendlyHttpError(error) {
  const status = error?.response?.status
  const data = error?.response?.data
  const rawMessage = String(data?.msg || data?.message || error?.message || '网络请求失败')

  // 避免把 Dify / Spring Boot 的长 JSON、堆栈和命令行日志直接弹到前端页面上
  if (status >= 500 || rawMessage.includes('Internal Server Error') || rawMessage.includes('INTERNAL SERVER ERROR')) {
    return '服务器内部错误：请检查后端、Dify 工作流、Ollama 模型或 API Key 配置。'
  }

  if (rawMessage.includes('timeout') || rawMessage.includes('timed out')) {
    return '请求超时：请检查后端服务或网络连接。'
  }

  return rawMessage.length > 120 ? `${rawMessage.slice(0, 120)}...` : rawMessage
}

http.interceptors.response.use(
  response => response.data,
  error => {
    const message = friendlyHttpError(error)
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export function assertSuccess(result, fallback = '操作失败') {
  if (!result) throw new Error(fallback)
  if (typeof result.code !== 'undefined' && result.code !== 200) {
    throw new Error(result.msg || fallback)
  }
  return result.data ?? result
}

export default http

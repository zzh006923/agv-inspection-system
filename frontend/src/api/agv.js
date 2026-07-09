import http, { assertSuccess } from './http'

export const systemApi = {
  checkAll: () => http.get('/system/check/all'),
  checkFs: () => http.get('/system/check/fs'),
  checkDb: () => http.get('/system/check/db'),
  checkAgv: () => http.get('/system/check/agv'),
  checkCam: () => http.get('/system/check/cam'),
  getMockMode: () => http.get('/system/check/mock-mode'),
  setMockMode: enabled => http.put('/system/check/mock-mode', null, { params: { enabled } })
}

export const configApi = {
  get: () => http.get('/agv/config'),
  update: data => http.put('/agv/config', data)
}

export const taskApi = {
  list: params => http.get('/agv/task/list', { params }),
  get: id => http.get(`/agv/task/${id}`),
  nextCode: () => http.get('/agv/task/next-code'),
  add: data => http.post('/agv/task', data),
  update: data => http.put('/agv/task', data),
  remove: id => http.delete(`/agv/task/${id}`),
  start: id => http.post(`/agv/task/start/${id}`),
  end: (id, isAbort = false) => http.post(`/agv/task/end/${id}`, null, { params: { isAbort } }),
  preupload: id => http.get(`/agv/task/preupload/${id}`),
  upload: id => http.post(`/agv/task/upload/${id}`)
}

export const flawApi = {
  list: params => http.get('/agv/flaw/list', { params }),
  get: id => http.get(`/agv/flaw/${id}`),
  add: data => http.post('/agv/flaw', data),
  update: data => http.put('/agv/flaw', data),
  remove: id => http.delete(`/agv/flaw/${id}`),
  live: taskId => http.get(`/agv/flaw/live/${taskId}`),
  check: taskId => http.get(`/agv/flaw/check/${taskId}`)
}

export const movementApi = {
  heartbeat: () => http.get('/agv/movement/heartbeat'),
  forward: () => http.post('/agv/movement/forward'),
  stop: () => http.post('/agv/movement/stop'),
  backward: () => http.post('/agv/movement/backward')
}

export const sensorApi = {
  list: taskId => http.get('/agv/sensor/list', { params: { taskId } }),
  status: taskId => http.get(`/agv/sensor/status/${taskId}`),
  add: data => http.post('/agv/sensor', data),
  trigger: data => http.post('/agv/sensor/trigger', data)
}

export const iotApi = {
  capability: () => http.get('/agv/iot/capability'),
  overview: taskId => http.get(`/agv/iot/overview/${taskId}`),
  deviceControl: data => http.post('/agv/iot/device/control', data),
  sceneTrigger: data => http.post('/agv/iot/scene/trigger', data),
  voiceCommand: data => http.post('/agv/iot/voice/command', data)
}

export const cameraApi = {
  devices: params => http.get('/agv/camera/devices', { params })
}

export const analysisApi = {
  result: data => http.post('/agv/analysis/result', data)
}

export const aiApi = {
  chat: data => http.post('/agv/ai/chat', data),
  taskReview: (taskId, data) => http.post(`/agv/ai/task-review/${taskId}`, data || {}),
  flawReview: (flawId, data) => http.post(`/agv/ai/flaw-review/${flawId}`, data || {})
}


export function unwrap(result, fallback) {
  return assertSuccess(result, fallback)
}

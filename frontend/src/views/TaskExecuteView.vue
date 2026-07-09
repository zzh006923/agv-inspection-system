<template>
  <div class="app-shell execute-page">
    <header class="app-header">
      <div class="app-title">
        <h1>实时任务执行</h1>
        <span>{{ task?.taskCode || '加载中' }} · 视频监控、车辆控制、故障轮询、传感器联动</span>
      </div>
      <div class="header-actions">
        <el-button @click="router.push('/tasks')">返回任务列表</el-button>
        <el-button type="success" :icon="CircleCheck" @click="finish(false)">完成巡检</el-button>
        <el-button type="danger" :icon="WarningFilled" @click="finish(true)">终止巡检</el-button>
      </div>
    </header>

    <div class="execute-layout">
      <main class="left-panel">
        <el-card class="page-card" shadow="never">
          <div class="section-title">
            <h2>多路视频监控</h2>
            <div class="video-tools">
              <el-select v-model="selectedCameraIndex" style="width: 220px" @change="switchCamera">
                <el-option v-for="(cam, idx) in cameras" :key="cam.key" :label="cam.name" :value="idx" />
              </el-select>
              <el-switch v-model="audioOn" active-text="音频开" inactive-text="静音" />
              <el-button :icon="Refresh" @click="refreshVideo">刷新监控</el-button>
            </div>
          </div>
          <VideoPlayer :key="videoKey" :url="currentCamera?.playUrl" :camera-name="currentCamera?.name" :muted="!audioOn" title="AGV 实时视频" />
        </el-card>

        <el-card class="page-card progress-card" shadow="never">
          <div class="section-title">
            <h2>巡检进度跟踪</h2>
            <span class="muted">当前位置 {{ currentPosition }}m / {{ totalDistance }}m</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            <div class="car-marker" :style="{ left: progressPercent + '%' }">🚛</div>
            <div v-for="f in flaws" :key="f.id" class="flaw-marker" :class="{ confirmed: f.confirmed === 1, false: f.remark?.includes('误报') }" :style="{ left: flawPercent(f) + '%' }" @click="openFlaw(f)">📍</div>
          </div>
          <div class="progress-legend">
            <span><i class="status-dot running"></i>AGV当前位置</span>
            <span><i class="status-dot warning"></i>待确认故障/异常</span>
            <span><i class="status-dot danger"></i>已确认故障</span>
          </div>
        </el-card>
      </main>

      <aside class="right-panel">
        <el-card class="page-card" shadow="never">
          <div class="section-title"><h3>车辆状态</h3><StatusTag :value="movement?.connectionMode || 'mock'" /></div>
          <div class="vehicle-state">
            <div><span>系统时间</span><strong>{{ movement?.sysTime || '-' }}</strong></div>
            <div><span>运行状态</span><strong><i class="status-dot" :class="movement?.isRunning ? 'running' : 'warning'"></i>{{ movement?.isRunning ? '运行中' : '已停止' }}</strong></div>
            <div><span>行驶方向</span><strong>{{ movement?.direction || 'stop' }}</strong></div>
            <div><span>故障数量</span><strong>{{ flaws.length }}</strong></div>
          </div>
          <div class="control-pad">
            <el-button type="primary" :icon="Top" @click="move('forward')">前进</el-button>
            <el-button type="danger" :icon="SwitchButton" @click="move('stop')">停止</el-button>
            <el-button :icon="Bottom" @click="move('backward')">后退</el-button>
          </div>
        </el-card>

        <el-card class="page-card" shadow="never">
          <div class="section-title"><h3>AIoT 多传感器创新</h3><el-tag type="success" effect="dark">可演示</el-tag></div>
          <div class="sensor-grid">
            <el-button @click="triggerSensor('temperature', '28.5℃')">温度正常</el-button>
            <el-button type="warning" @click="triggerSensor('humidity', '86%')">湿度异常</el-button>
            <el-button type="warning" @click="triggerSensor('light', '20lux')">光照不足</el-button>
            <el-button type="danger" @click="triggerSensor('smoke', 'true')">烟雾报警</el-button>
            <el-button type="danger" @click="triggerSensor('person', 'detected')">人员靠近</el-button>
            <el-button type="primary" @click="simulateCrack">裂缝识别</el-button>
          </div>
          <el-divider />
          <div class="scene-grid">
            <el-button @click="triggerScene('safety')">安全保护模式</el-button>
            <el-button @click="triggerScene('environment')">环境监测模式</el-button>
            <el-button @click="triggerScene('lighting')">补光巡检模式</el-button>
            <el-button @click="triggerScene('fire')">烟雾报警模式</el-button>
          </div>
          <el-input v-model="voiceCommand" class="voice-input" placeholder="如：查询当前环境状态、打开补光灯、执行安全停车" @keyup.enter="sendVoice">
            <template #append><el-button :icon="Microphone" @click="sendVoice">发送</el-button></template>
          </el-input>
        </el-card>

        <el-card class="page-card records" shadow="never">
          <div class="section-title"><h3>实时故障与联动记录</h3><el-button link @click="refreshAll">刷新</el-button></div>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="故障" name="flaw">
              <el-scrollbar height="260px">
                <div v-for="f in flaws" :key="f.id" class="record-item" @click="openFlaw(f)">
                  <div><strong>{{ f.flawName }}</strong><StatusTag :value="f.level" /></div>
                  <p>{{ f.flawDesc || f.remark }}</p>
                  <span>{{ f.flawDistance || 0 }}m · {{ f.source || '系统检测' }}</span>
                </div>
                <el-empty v-if="!flaws.length" description="暂无故障记录" />
              </el-scrollbar>
            </el-tab-pane>
            <el-tab-pane label="传感器" name="sensor">
              <el-scrollbar height="260px">
                <div v-for="s in sensorRecords" :key="s.id" class="record-item">
                  <div><strong>{{ s.sensorName }}</strong><StatusTag :value="s.status" /></div>
                  <p>{{ s.sensorValue }} · {{ s.action || s.remark }}</p>
                  <span>{{ s.distance || 0 }}m · {{ s.createTime }}</span>
                </div>
                <el-empty v-if="!sensorRecords.length" description="暂无传感器记录" />
              </el-scrollbar>
            </el-tab-pane>
            <el-tab-pane label="联动" name="iot">
              <el-scrollbar height="260px">
                <div v-for="a in actionRecords" :key="a.id" class="record-item">
                  <div><strong>{{ a.deviceName }}</strong><StatusTag :value="a.result" /></div>
                  <p>{{ a.sceneName || a.triggerType }}：{{ a.action }}</p>
                  <span>{{ a.feedback }}</span>
                </div>
                <el-empty v-if="!actionRecords.length" description="暂无联动记录" />
              </el-scrollbar>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </aside>
    </div>

    <FlawDialog v-model="flawDialog" :flaw="selectedFlaw" @saved="refreshFlaws" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Bottom, CircleCheck, Microphone, Refresh, SwitchButton, Top, WarningFilled } from '@element-plus/icons-vue'
import { analysisApi, cameraApi, configApi, flawApi, iotApi, movementApi, sensorApi, taskApi, unwrap } from '../api/agv'
import { normalizeDistance, percent } from '../utils/format'
import StatusTag from '../components/StatusTag.vue'
import VideoPlayer from '../components/VideoPlayer.vue'
import FlawDialog from '../components/FlawDialog.vue'

const route = useRoute()
const router = useRouter()
const taskId = Number(route.params.id)
const task = ref(null)
const config = ref(null)
const movement = ref(null)
const flaws = ref([])
const sensorRecords = ref([])
const actionRecords = ref([])
const cameras = ref([])
const selectedCameraIndex = ref(0)
const audioOn = ref(false)
const videoKey = ref(0)
const activeTab = ref('flaw')
const voiceCommand = ref('')
const flawDialog = ref(false)
const selectedFlaw = ref(null)
let timer = null

const totalDistance = computed(() => normalizeDistance(task.value?.taskTrip))
const currentPosition = computed(() => Number(movement.value?.currentPosition || 0))
const progressPercent = computed(() => percent(currentPosition.value, totalDistance.value))
const currentCamera = computed(() => cameras.value[selectedCameraIndex.value] || cameras.value[0])

onMounted(async () => {
  await loadBase()
  await refreshAll()
  timer = setInterval(refreshAll, 3000)
})
onBeforeUnmount(() => timer && clearInterval(timer))

async function loadBase() {
  try {
    task.value = unwrap(await taskApi.get(taskId), '读取任务失败')
    config.value = unwrap(await configApi.get(), '读取配置失败')
    await loadCameras()
  } catch (e) { ElMessage.error(e.message) }
}
async function loadCameras() {
  const local = [1, 2, 3, 4].map(i => ({ key: `cam${i}`, name: ['前方视角','左侧视角','右侧视角','后方视角'][i - 1], url: config.value?.[`cam${i}`] || '', playUrl: buildPlayUrl(config.value?.[`cam${i}`], i) }))
  cameras.value = local
  try {
    const result = unwrap(await cameraApi.devices({ page: 1, size: 999 }), '获取摄像头列表失败')
    const parsed = typeof result === 'string' ? JSON.parse(result) : result
    const list =
  parsed?.items ||
  parsed?.data?.items ||
  parsed?.data?.list ||
  parsed?.data ||
  parsed?.rows ||
  []
    if (Array.isArray(list) && list.length) {
      cameras.value = list.slice(0, 4).map((d, idx) => ({ key: d.id || `device-${idx}`, name: d.name || `摄像头${idx + 1}`, url: d.url || d.stream || '', playUrl: buildDevicePlayUrl(d, idx) }))
    }
  } catch (_) {}
}
function getWebrtcBase() {
  return (import.meta.env.VITE_WEBRTC_BASE_URL || 'http://192.168.2.57/webrtc-api').replace(/\/$/, '')
}

function getEnvStreamId(idx) {
  return import.meta.env[`VITE_CAMERA_STREAM_${idx}`] || `cam${idx}`
}

function toFlvUrl(cameraId) {
  return `${getWebrtcBase()}/live/${cameraId}_01.flv`
}

function buildPlayUrl(raw, idx) {
  // raw 可能是 RTSP 地址，但浏览器不能直接播 RTSP
  // 所以这里统一转成 webrtc-api 的 flv 播放地址
  return toFlvUrl(getEnvStreamId(idx))
}

function buildDevicePlayUrl(d, idx) {
  // easy-api 返回的 d.url 是 rtsp://...，不能直接给前端播放
  // 必须使用 d.id 拼接 webrtc-api/live/{id}_01.flv
  const cameraId =
    d.id ||
    d.deviceId ||
    d.cameraId ||
    d.channelId ||
    d.streamId ||
    getEnvStreamId(idx + 1)

  return toFlvUrl(cameraId)
}
function switchCamera() { videoKey.value++ }
function refreshVideo() { videoKey.value++; ElMessage.success('视频通道已刷新') }
async function refreshAll() {
  await Promise.allSettled([refreshMovement(), refreshFlaws(), refreshIot()])
}
async function refreshMovement() {
  try { movement.value = unwrap(await movementApi.heartbeat(), '车辆心跳失败') } catch (_) {}
}
async function refreshFlaws() {
  try {
    const result = await flawApi.list({ taskId, pageNum: 1, pageSize: 999 })
    flaws.value = result.rows || []
    const live = unwrap(await flawApi.live(taskId), '获取实时故障失败')
    if (Array.isArray(live) && live.length) {
      live.forEach(f => ElNotification.warning({ title: '发现新的故障/异常', message: `${f.flawName} · ${f.flawDistance || 0}m`, duration: 4500 }))
    }
  } catch (_) {}
}
async function refreshIot() {
  try {
    const data = unwrap(await iotApi.overview(taskId), '读取AIoT总览失败')
    sensorRecords.value = data.sensorRecords || []
    actionRecords.value = data.actionRecords || []
    if (data.movementStatus) movement.value = data.movementStatus
  } catch (_) {}
}
async function move(action) {
  try {
    if (action === 'forward') movement.value = unwrap(await movementApi.forward(), '前进失败')
    if (action === 'stop') movement.value = unwrap(await movementApi.stop(), '停止失败')
    if (action === 'backward') movement.value = unwrap(await movementApi.backward(), '后退失败')
    ElMessage.success('车辆控制指令已发送')
  } catch (e) { ElMessage.error(e.message) }
}
async function finish(isAbort) {
  await ElMessageBox.confirm(isAbort ? '确定终止当前巡检？系统会先发送停车指令，再进入复盘上传流程。' : '确定完成当前巡检并进入复盘页？', isAbort ? '终止巡检' : '完成巡检', { type: isAbort ? 'warning' : 'success' })
  try {
    await movementApi.stop().catch(() => null)
    const saved = unwrap(await taskApi.end(taskId, isAbort), '结束任务失败')
    ElMessage.success(isAbort ? '任务已终止，进入复盘页' : '任务已完成，进入复盘页')
    router.push(`/task/${saved.id}/detail`)
  } catch (e) { ElMessage.error(e.message) }
}
async function triggerSensor(sensorType, sensorValue) {
  try {
    const data = unwrap(await sensorApi.trigger({ taskId, sensorType, sensorValue, distance: currentPosition.value }), '触发传感器失败')
    ElMessage.success(data.message || '传感器事件已生成')
    await refreshAll()
  } catch (e) { ElMessage.error(e.message) }
}
async function triggerScene(sceneType) {
  try {
    const data = unwrap(await iotApi.sceneTrigger({ taskId, sceneType, distance: currentPosition.value }), '场景联动失败')
    ElMessage.success(data.feedback || '场景联动已执行')
    await refreshAll()
  } catch (e) { ElMessage.error(e.message) }
}
async function sendVoice() {
  if (!voiceCommand.value.trim()) return ElMessage.warning('请先输入语音/AI指令')
  try {
    const data = unwrap(await iotApi.voiceCommand({ taskId, command: voiceCommand.value, distance: currentPosition.value }), '语音指令执行失败')
    ElMessage.success(data.feedback || '语音指令已执行')
    voiceCommand.value = ''
    await refreshAll()
  } catch (e) { ElMessage.error(e.message) }
}
async function simulateCrack() {
  try {
    unwrap(await analysisApi.result({ taskId, round: task.value?.round || 1, distance: currentPosition.value, imageUrl: '', crackLength: 12.6, crackArea: 2.8, confidence: 0.91, level: '中', description: '裂缝分割模型检测到隧道壁疑似裂缝，建议人工复核' }), '裂缝结果写入失败')
    ElMessage.success('裂缝识别结果已写入故障记录')
    await refreshAll()
  } catch (e) { ElMessage.error(e.message) }
}
function flawPercent(flaw) { return percent(flaw.flawDistance || 0, totalDistance.value) }
function openFlaw(flaw) { selectedFlaw.value = { ...flaw }; flawDialog.value = true }
</script>

<style scoped>
.execute-layout { display: grid; grid-template-columns: minmax(0, 1fr) 430px; gap: 16px; }
.left-panel, .right-panel { display: grid; gap: 16px; align-content: start; }
.video-tools { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.progress-card { padding-bottom: 8px; }
.progress-legend { display: flex; gap: 20px; margin-top: 16px; color: var(--agv-muted); font-size: 13px; }
.vehicle-state { display: grid; gap: 12px; }
.vehicle-state div { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; background: #f8fafc; border: 1px solid var(--agv-border); border-radius: 12px; }
.vehicle-state span { color: var(--agv-muted); }
.vehicle-state strong { display: flex; align-items: center; }
.control-pad { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-top: 14px; }
.sensor-grid, .scene-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.voice-input { margin-top: 12px; }
.record-item { padding: 12px; border-radius: 14px; border: 1px solid var(--agv-border); background: #fff; margin-bottom: 10px; cursor: pointer; }
.record-item div { display: flex; justify-content: space-between; gap: 10px; align-items: center; }
.record-item p { margin: 7px 0; color: var(--agv-text); }
.record-item span { color: var(--agv-muted); font-size: 12px; }
@media (max-width: 1320px) { .execute-layout { grid-template-columns: 1fr; } }
</style>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="app-title">
        <h1>系统设置</h1>
        <span>配置车辆 IP、端口、云端服务、数据库与四路摄像头；保存后建议重新自检</span>
      </div>
      <div class="header-actions">
        <el-button @click="router.push('/tasks')">任务列表</el-button>
        <el-button @click="router.push('/init')">返回自检</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
      </div>
    </header>

    <div class="settings-layout">
      <main>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="116px">
          <el-card class="page-card" shadow="never">
            <div class="section-title"><h2>巡检车与真实小车接入</h2><el-tag :type="mockMode ? 'warning' : 'success'">{{ mockMode ? '演示模式' : '真实设备模式' }}</el-tag></div>
            <div class="grid grid-2">
              <el-form-item label="车辆 IP" prop="host"><el-input v-model="form.host" placeholder="192.168.2.57 / 192.168.2.2" /></el-form-item>
              <el-form-item label="通信协议" prop="controlProtocol">
                <el-select v-model="form.controlProtocol" style="width: 100%"><el-option label="HTTP" value="http" /><el-option label="TCP" value="tcp" /><el-option label="Mock" value="mock" /></el-select>
              </el-form-item>
              <el-form-item label="行驶端口" prop="drivePort"><el-input-number v-model="form.drivePort" :min="1" :max="65535" style="width: 100%" /></el-form-item>
              <el-form-item label="分析端口" prop="analysisPort"><el-input-number v-model="form.analysisPort" :min="1" :max="65535" style="width: 100%" /></el-form-item>
            </div>
            <el-alert show-icon :closable="false" type="info" title="无车调试时保持演示模式；连接车载 WiFi 后可切换真实设备模式，再执行系统自检。" />
          </el-card>

          <el-card class="page-card" shadow="never">
            <div class="section-title"><h2>数据库配置</h2><el-button link @click="checkDb">测试连接</el-button></div>
            <div class="grid grid-2">
              <el-form-item label="数据库地址" prop="dbHost"><el-input v-model="form.dbHost" /></el-form-item>
              <el-form-item label="数据库端口" prop="dbPort"><el-input-number v-model="form.dbPort" :min="1" :max="65535" style="width: 100%" /></el-form-item>
              <el-form-item label="数据库名称" prop="dbName"><el-input v-model="form.dbName" /></el-form-item>
              <el-form-item label="用户名" prop="dbUsername"><el-input v-model="form.dbUsername" /></el-form-item>
              <el-form-item label="密码" prop="dbPassword"><el-input v-model="form.dbPassword" type="password" show-password /></el-form-item>
            </div>
          </el-card>

          <el-card class="page-card" shadow="never">
            <div class="section-title"><h2>云端上传配置</h2></div>
            <div class="grid grid-2">
              <el-form-item label="云端地址" prop="cloudUrl"><el-input v-model="form.cloudUrl" placeholder="http://192.168.2.57/prod-api" /></el-form-item>
              <el-form-item label="API密钥"><el-input v-model="form.cloudApiKey" type="password" show-password placeholder="如有真实云端接口则填写" /></el-form-item>
            </div>
          </el-card>

          <el-card class="page-card" shadow="never">
            <div class="section-title"><h2>四路摄像头与音视频</h2><el-button link @click="checkCam">测试摄像头</el-button></div>
            <el-tabs v-model="activeCamera">
              <el-tab-pane v-for="i in 4" :key="i" :label="cameraLabels[i - 1]" :name="String(i)">
                <div class="grid grid-2">
                  <el-form-item :label="`摄像头${i}地址`"><el-input v-model="form[`cam${i}`]" placeholder="rtsp://... 或 webrtc/http 播放地址" /></el-form-item>
                  <el-form-item label="账号"><el-input v-model="form[`username${i}`]" /></el-form-item>
                  <el-form-item label="密码"><el-input v-model="form[`password${i}`]" type="password" show-password /></el-form-item>
                  <el-form-item label="视角说明"><el-input :model-value="cameraLabels[i - 1]" disabled /></el-form-item>
                </div>
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </el-form>
      </main>

      <aside class="side-panel">
        <el-card class="page-card" shadow="never">
          <div class="section-title"><h3>调试模式</h3></div>
          <el-switch v-model="mockMode" active-text="演示模式" inactive-text="真实模式" :loading="mockLoading" @change="changeMock" />
          <p class="hint">演示模式下自检与车辆状态由后端模拟，适合尚未连接小车时完成页面验收；真实模式会检测车辆、数据库、摄像头等真实连接。</p>
        </el-card>

        <el-card class="page-card" shadow="never">
          <div class="section-title"><h3>连接检测</h3><el-button link @click="runCheck">全部检测</el-button></div>
          <div class="check-mini" v-for="item in checkItems" :key="item.key">
            <span><i class="status-dot" :class="item.passed ? 'success' : item.error ? 'danger' : 'warning'"></i>{{ item.label }}</span>
            <small>{{ item.message }}</small>
          </div>
        </el-card>

        <el-card class="page-card" shadow="never">
          <div class="section-title"><h3>摄像头设备列表</h3><el-button link @click="loadDevices">刷新</el-button></div>
          <el-scrollbar height="280px">
            <div v-for="d in devices" :key="d.id || d.name" class="device-line">
              <strong>{{ d.name || d.id || '摄像头设备' }}</strong>
              <p>ID：{{ d.id || '-' }}</p>
              <p>状态：{{ d.status === true || d.online === true ? '在线' : '离线' }}</p>
            </div>
            <el-empty v-if="!devices.length" description="未获取到 easy-api 摄像头列表，可先检查后端代理或车载 WiFi" />
          </el-scrollbar>
        </el-card>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cameraApi, configApi, systemApi, unwrap } from '../api/agv'

const router = useRouter()
const formRef = ref(null)
const saving = ref(false)
const mockMode = ref(true)
const mockLoading = ref(false)
const activeCamera = ref('1')
const devices = ref([])
const cameraLabels = ['前方视角', '左侧视角', '右侧视角', '后方视角']
const checkItems = ref([
  { key: 'fs', label: '文件系统', passed: false, error: false, message: '未检测' },
  { key: 'db', label: '数据库', passed: false, error: false, message: '未检测' },
  { key: 'agv', label: 'AGV通信', passed: false, error: false, message: '未检测' },
  { key: 'cam', label: '摄像头', passed: false, error: false, message: '未检测' }
])
const form = reactive({
  id: null,
  host: '192.168.2.2', drivePort: 9001, analysisPort: 9002, controlProtocol: 'http',
  cloudUrl: 'http://192.168.2.57/prod-api', cloudApiKey: '',
  dbHost: 'localhost', dbPort: 3306, dbName: 'agv_inspection', dbUsername: 'root', dbPassword: '1234',
  cam1: '', username1: 'admin', password1: '123456', cam2: '', username2: 'admin', password2: '123456',
  cam3: '', username3: 'admin', password3: '123456', cam4: '', username4: 'admin', password4: '123456'
})
const rules = {
  host: [{ required: true, message: '请填写车辆IP', trigger: 'blur' }],
  drivePort: [{ required: true, message: '请填写行驶端口', trigger: 'blur' }],
  analysisPort: [{ required: true, message: '请填写分析端口', trigger: 'blur' }],
  dbHost: [{ required: true, message: '请填写数据库地址', trigger: 'blur' }],
  dbPort: [{ required: true, message: '请填写数据库端口', trigger: 'blur' }],
  dbName: [{ required: true, message: '请填写数据库名称', trigger: 'blur' }],
  dbUsername: [{ required: true, message: '请填写数据库用户名', trigger: 'blur' }],
  cloudUrl: [{ required: true, message: '请填写云端地址', trigger: 'blur' }]
}

onMounted(async () => {
  await loadConfig()
  await loadMockMode()
  loadDevices()
})
async function loadConfig() {
  try { Object.assign(form, unwrap(await configApi.get(), '读取配置失败')) } catch (e) { ElMessage.error(e.message) }
}
async function loadMockMode() {
  try { mockMode.value = !!unwrap(await systemApi.getMockMode(), '读取模式失败') } catch (_) {}
}
async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const saved = unwrap(await configApi.update({ ...form }), '保存配置失败')
    Object.assign(form, saved)
    ElMessage.success('系统设置已保存，请重新执行系统自检')
  } catch (e) { ElMessage.error(e.message) }
  finally { saving.value = false }
}
async function changeMock(value) {
  mockLoading.value = true
  try {
    unwrap(await systemApi.setMockMode(value), '切换模式失败')
    ElMessage.success(value ? '已切换为演示模式' : '已切换为真实设备模式')
  } catch (e) { ElMessage.error(e.message) }
  finally { mockLoading.value = false }
}
function patchCheck(key, patch) { Object.assign(checkItems.value.find(i => i.key === key), patch) }
async function runOne(key, label, call) {
  patchCheck(key, { message: '检测中...', passed: false, error: false })
  try {
    const data = unwrap(await call(), `${label}检测失败`)
    patchCheck(key, { passed: true, error: false, message: data?.message || '通过' })
  } catch (e) { patchCheck(key, { passed: false, error: true, message: e.message }) }
}
async function runCheck() {
  await runOne('fs', '文件系统', systemApi.checkFs)
  await runOne('db', '数据库', systemApi.checkDb)
  await runOne('agv', '车辆通信', systemApi.checkAgv)
  await runOne('cam', '摄像头', systemApi.checkCam)
}
function checkDb() { runOne('db', '数据库', systemApi.checkDb) }
function checkCam() { runOne('cam', '摄像头', systemApi.checkCam) }
async function loadDevices() {
  try {
    const result = unwrap(
      await cameraApi.devices({ page: 1, size: 999, status: '', id: '', name: '' }),
      '获取摄像头设备失败'
    )

    const parsed = typeof result === 'string' ? JSON.parse(result) : result

    const list =
      parsed?.items ||
      parsed?.data?.items ||
      parsed?.data?.list ||
      parsed?.data?.records ||
      parsed?.data?.rows ||
      parsed?.rows ||
      parsed?.records ||
      parsed?.list ||
      []

    devices.value = Array.isArray(list) ? list : []

    console.log('摄像头设备列表原始返回：', parsed)
    console.log('解析后的摄像头设备列表：', devices.value)
  } catch (e) {
    console.error('获取摄像头设备失败：', e)
    devices.value = []
  }
}
</script>

<style scoped>
.settings-layout { display: grid; grid-template-columns: minmax(0, 1fr) 390px; gap: 16px; }
main { display: grid; gap: 16px; }
main .page-card { margin-bottom: 16px; }
.side-panel { display: grid; gap: 16px; align-content: start; }
.hint { color: var(--agv-muted); line-height: 1.7; margin: 12px 0 0; }
.check-mini { padding: 12px; border-radius: 14px; background: #fff; border: 1px solid var(--agv-border); margin-bottom: 10px; }
.check-mini span { display: flex; align-items: center; font-weight: 700; }
.check-mini small { display: block; color: var(--agv-muted); margin-top: 6px; line-height: 1.5; }
.device-line { padding: 12px; border: 1px solid var(--agv-border); border-radius: 14px; background: #fff; margin-bottom: 10px; }
.device-line p { margin: 5px 0 0; color: var(--agv-muted); font-size: 13px; }
@media (max-width: 1200px) { .settings-layout { grid-template-columns: 1fr; } }
</style>

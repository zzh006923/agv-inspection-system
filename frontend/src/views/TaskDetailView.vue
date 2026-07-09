<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="app-title">
        <h1>任务详情与故障复盘</h1>
        <span>{{ task?.taskCode || '加载中' }} · 故障确认、误报标记、传感器记录与数据上传</span>
      </div>
      <div class="header-actions">
        <el-button @click="router.push('/tasks')">返回任务列表</el-button>
        <el-button v-if="task?.taskStatus === '巡视中'" type="primary" @click="router.push(`/task/${taskId}/execute`)" >继续巡视</el-button>
        <el-button v-if="task?.taskStatus === '待上传'" type="success" :loading="uploading" @click="uploadTask">上传巡检数据</el-button>
      </div>
    </header>

    <div class="grid grid-4 stats">
      <StatCard label="故障总数" :value="summary.flawCount" icon="Warning" type="orange" />
      <StatCard label="未确认" :value="localUnconfirmedCount" icon="QuestionFilled" type="red" />
      <StatCard label="传感器记录" :value="sensorRecords.length" icon="Odometer" />
      <StatCard label="上传状态" :value="task?.taskStatus || '-'" :desc="localCanUpload ? '已满足上传条件' : '需先完成确认'" icon="UploadFilled" type="green" />
    </div>

    <div class="detail-layout">
      <main class="main-panel">
        <el-card ref="previewCard" class="page-card" shadow="never">
          <div class="section-title">
            <h2>巡检路线与故障定位</h2>
            <span class="muted">任务距离：{{ totalDistance }}m</span>
          </div>
          <div class="progress-track review-track">
            <div class="progress-fill" style="width: 100%"></div>
            <div v-for="f in flaws" :key="f.id" class="flaw-marker" :class="{ confirmed: f.confirmed === 1, false: isFalseAlarm(f) }" :style="{ left: flawPercent(f) + '%' }" @click="viewFlaw(f)">📍</div>
          </div>
          <div class="preview-box">
            <div class="preview-img">
             <img
                 v-if="selectedImage"
                 :key="selectedImage"
                 class="flaw-photo"
                 :src="selectedImage"
                 @error="tryNextImage"
             />
            <div v-else class="empty-img">
                <el-icon size="52"><Picture /></el-icon>
                 <p>请选择故障记录查看图片；传感器异常可能没有图片。</p>
            </div>
            </div>
            <div class="preview-info">
              <h3>{{ selectedFlaw?.flawName || '暂无选中故障' }}</h3>
              <p>{{ selectedFlaw?.flawDesc || '巡检完成后，可在此复核故障图片、确认是否属实，并补充现场说明。' }}</p>
              <div class="info-list">
                <div><span>缺陷类型</span><strong>{{ selectedFlaw?.flawType || '-' }}</strong></div>
                <div><span>缺陷等级</span><StatusTag :value="selectedFlaw?.level" /></div>
                <div><span>所在位置</span><strong>{{ selectedFlaw?.flawDistance || 0 }}m</strong></div>
                <div><span>来源</span><strong>{{ selectedFlaw?.source || '-' }}</strong></div>
                <div><span>确认状态</span><StatusTag :value="flawConfirmLabel(selectedFlaw)" /></div>
              </div>
              <el-button type="primary" :disabled="!selectedFlaw" @click="openFlaw(selectedFlaw)">打开复核弹窗</el-button>
            </div>
          </div>
        </el-card>

        <el-card class="page-card" shadow="never">
          <div class="section-title"><h2>故障历史列表</h2><el-button link @click="refresh">刷新</el-button></div>
          <el-table :data="flaws" stripe>
            <el-table-column prop="flawName" label="名称" min-width="170" show-overflow-tooltip />
            <el-table-column prop="flawType" label="类型" width="120" />
            <el-table-column label="等级" width="90"><template #default="{ row }"><StatusTag :value="row.level" /></template></el-table-column>
            <el-table-column label="距离" width="90"><template #default="{ row }">{{ row.flawDistance || 0 }}m</template></el-table-column>
            <el-table-column prop="source" label="来源" width="120" />
            <el-table-column label="确认" width="110"><template #default="{ row }"><StatusTag :value="flawConfirmLabel(row)" /></template></el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewFlaw(row)">查看</el-button>
                <el-button link type="success" @click="openFlaw(row)">{{ row.confirmed === 1 ? '修改' : '复核' }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </main>

      <aside class="side-panel">
        <el-card class="page-card" shadow="never">
          <div class="section-title"><h3>任务基本信息</h3><StatusTag :value="task?.taskStatus" /></div>
          <div class="info-list compact">
            <div><span>任务名称</span><strong>{{ task?.taskName || '-' }}</strong></div>
            <div><span>起始地点</span><strong>{{ task?.startPos || '-' }}</strong></div>
            <div><span>创建人</span><strong>{{ task?.creator || '-' }}</strong></div>
            <div><span>执行人</span><strong>{{ task?.executor || '-' }}</strong></div>
            <div><span>开始时间</span><strong>{{ formatTime(task?.execTime) }}</strong></div>
            <div><span>结束时间</span><strong>{{ formatTime(task?.endTime) }}</strong></div>
          </div>
        </el-card>

        <el-card class="page-card" shadow="never">
          <div class="section-title"><h3>上传前检查</h3><el-button link @click="loadPreupload">检查</el-button></div>
          <el-alert :type="localCanUpload ? 'success' : 'warning'" show-icon :closable="false" :title="localCanUpload ? '故障已全部复核，可上传巡检数据' : '仍存在待确认故障，上传按钮将不可用或后端拒绝上传'" />
          <div class="review-progress-meta">
            <span>复核进度：{{ confirmedReviewCount }} / {{ totalReviewCount }}</span>
            <span>{{ displayProgress }}%</span>
          </div>
          <el-progress class="upload-progress" :percentage="displayProgress" :status="displayProgress === 100 ? 'success' : undefined" />
          <el-scrollbar height="190px">
            <div v-for="r in uploadRecords" :key="r.id" class="record-line">
              <strong>{{ r.info }}</strong><StatusTag :value="r.status" />
              <p>{{ r.uploadResult || r.remark }}</p>
            </div>
            <el-empty v-if="!uploadRecords.length" description="暂无上传记录" />
          </el-scrollbar>
        </el-card>

        <AiAssistantPanel
          :task-id="taskId"
          :task="task"
          :flaws="flaws"
          :sensor-records="sensorRecords"
          :action-records="actionRecords"
          :summary="summary"
          :selected-flaw="selectedFlaw"
        />

        <el-card class="page-card sensor-loop-card" shadow="never">
          <div class="section-title">
            <h3>传感器与联动闭环</h3>
            <span class="section-subtitle">每类传感器仅展示最新一次上报</span>
          </div>
          <el-tabs>
            <el-tab-pane label="传感器">
              <div class="sensor-list-panel">
                <div v-for="s in latestSensorRecords" :key="sensorRecordKey(s)" class="sensor-card">
                  <div class="sensor-head">
                    <div>
                      <strong>{{ s.sensorName || sensorNameText(s.sensorType) }}</strong>
                      <p>{{ sensorTypeText(s.sensorType) }}</p>
                    </div>
                    <StatusTag :value="s.status || '正常'" />
                  </div>
                  <div class="sensor-main-value">{{ s.sensorValue || '-' }}</div>
                  <div class="sensor-meta">
                    <span>{{ s.remark || '传感器上报' }}</span>
                    <span>上报时间：{{ formatTime(s.createTime || s.reportTime) }}</span>
                  </div>
                  <div v-if="s.distance !== null && s.distance !== undefined" class="sensor-distance">
                    所在位置：{{ s.distance }}m
                  </div>
                </div>
                <el-empty v-if="!latestSensorRecords.length" description="暂无传感器记录" />
              </div>
            </el-tab-pane>
            <el-tab-pane label="设备动作">
              <div class="sensor-list-panel">
                <div v-for="a in latestActionRecords" :key="a.id || `${a.deviceType}-${a.createTime}`" class="action-card">
                  <div class="sensor-head">
                    <div>
                      <strong>{{ a.deviceName || a.deviceType || '联动设备' }}</strong>
                      <p>{{ a.sceneName || a.sceneType || 'AIoT联动' }}</p>
                    </div>
                    <StatusTag :value="a.result || '成功'" />
                  </div>
                  <div class="sensor-main-value">{{ a.action || '-' }}</div>
                  <div class="sensor-meta">
                    <span>{{ a.feedback || '设备动作已记录' }}</span>
                    <span>执行时间：{{ formatTime(a.createTime) }}</span>
                  </div>
                </div>
                <el-empty v-if="!latestActionRecords.length" description="暂无联动记录" />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </aside>
    </div>

    <FlawDialog v-model="flawDialog" :flaw="selectedFlaw" @saved="afterFlawSaved" />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { flawApi, iotApi, taskApi, unwrap } from '../api/agv'
import { formatTime, normalizeDistance, percent } from '../utils/format'
import { resolveImageCandidates } from '../utils/image'
import StatCard from '../components/StatCard.vue'
import StatusTag from '../components/StatusTag.vue'
import FlawDialog from '../components/FlawDialog.vue'
import AiAssistantPanel from '../components/AiAssistantPanel.vue'

const route = useRoute()
const router = useRouter()
const taskId = Number(route.params.id)
const task = ref(null)
const flaws = ref([])
const uploadRecords = ref([])
const sensorRecords = ref([])
const actionRecords = ref([])
const summary = ref({ flawCount: 0, unconfirmedCount: 0, canUpload: false })
const selectedFlaw = ref(null)
const flawDialog = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const previewCard = ref(null)
const totalDistance = computed(() => normalizeDistance(task.value?.taskTrip))
const totalReviewCount = computed(() => flaws.value.length || summary.value.flawCount || 0)
const confirmedReviewCount = computed(() => flaws.value.filter(f => Number(f.confirmed) === 1).length)
const localUnconfirmedCount = computed(() => Math.max(0, totalReviewCount.value - confirmedReviewCount.value))
const localCanUpload = computed(() => totalReviewCount.value > 0 && localUnconfirmedCount.value === 0)
const reviewProgress = computed(() => {
  if (!totalReviewCount.value) return 0
  return Math.round((confirmedReviewCount.value / totalReviewCount.value) * 100)
})
const displayProgress = computed(() => uploading.value ? uploadProgress.value : reviewProgress.value)
const imageIndex = ref(0)
const imageCandidates = computed(() => {
  const f = selectedFlaw.value || {}
  return resolveImageCandidates(
    f.flawImage ||
    f.flawImageUrl ||
    f.imageUrl ||
    f.image ||
    f.flaw_image ||
    f.flaw_image_url ||
    f.flawImg ||
    f.imgUrl ||
    ''
  )
})
const selectedImage = computed(() => imageCandidates.value[imageIndex.value] || '')
const latestSensorRecords = computed(() => {
  const source = Array.isArray(sensorRecords.value) ? sensorRecords.value : []
  const map = new Map()

  source.forEach(item => {
    const key = item.sensorType || item.sensorName || `sensor-${item.id || Math.random()}`
    const old = map.get(key)
    const currentTime = timeValue(item.createTime || item.reportTime)
    const oldTime = old ? timeValue(old.createTime || old.reportTime) : -1

    if (!old || currentTime >= oldTime) {
      map.set(key, item)
    }
  })

  const order = ['humidity', 'temperature', 'light', 'smoke', 'person']
  return Array.from(map.values()).sort((a, b) => {
    const ia = order.indexOf(a.sensorType)
    const ib = order.indexOf(b.sensorType)
    if (ia !== -1 || ib !== -1) return (ia === -1 ? 99 : ia) - (ib === -1 ? 99 : ib)
    return timeValue(b.createTime || b.reportTime) - timeValue(a.createTime || a.reportTime)
  })
})
const latestActionRecords = computed(() => {
  const source = Array.isArray(actionRecords.value) ? actionRecords.value : []
  return [...source]
    .sort((a, b) => timeValue(b.createTime) - timeValue(a.createTime))
    .slice(0, 8)
})
watch(() => selectedFlaw.value?.id, () => { imageIndex.value = 0 })

function timeValue(value) {
  if (!value) return 0
  const t = new Date(value).getTime()
  return Number.isNaN(t) ? 0 : t
}
function sensorNameText(type) {
  const map = {
    humidity: '湿度传感器',
    temperature: '温度传感器',
    light: '光照传感器',
    smoke: '烟雾传感器',
    person: '人员检测传感器'
  }
  return map[type] || '未知传感器'
}
function sensorTypeText(type) {
  const map = {
    humidity: '环境湿度',
    temperature: '环境温度',
    light: '隧道光照',
    smoke: '烟雾安全',
    person: '人员安全'
  }
  return map[type] || '传感器状态'
}
function sensorRecordKey(record) {
  return record.sensorType || record.id || record.sensorName || 'sensor'
}

onMounted(refresh)

async function refresh() {
  await Promise.allSettled([loadTask(), refreshFlaws(), loadPreupload(), loadIot()])
}
async function loadTask() {
  try { task.value = unwrap(await taskApi.get(taskId), '读取任务失败') } catch (e) { ElMessage.error(e.message) }
}
async function refreshFlaws() {
  try {
    const result = await flawApi.list({ taskId, pageNum: 1, pageSize: 999 })
    flaws.value = result.rows || []
    if (!selectedFlaw.value && flaws.value.length) selectedFlaw.value = flaws.value[0]
    if (selectedFlaw.value) selectedFlaw.value = flaws.value.find(f => f.id === selectedFlaw.value.id) || flaws.value[0] || null
  } catch (e) { ElMessage.error(e.message) }
}
async function loadPreupload() {
  try {
    const data = unwrap(await taskApi.preupload(taskId), '上传前检查失败')
    task.value = data.task || task.value
    flaws.value = data.flaws || flaws.value
    uploadRecords.value = data.uploadRecords || []
    summary.value = data.summary || summary.value
    uploadProgress.value = summary.value.canUpload ? 100 : reviewProgress.value
  } catch (_) {}
}
async function loadIot() {
  try {
    const data = unwrap(await iotApi.overview(taskId), '读取传感器记录失败')
    sensorRecords.value = data.sensorRecords || []
    actionRecords.value = data.actionRecords || []
  } catch (_) {}
}
async function loadFullFlaw(row) {
  if (!row?.id) return row

  try {
    const detail = unwrap(await flawApi.get(row.id), '读取故障详情失败')
    return {
      ...row,
      ...detail
    }
  } catch (e) {
    console.warn('读取故障详情失败，使用列表数据：', e)
    return row
  }
}

async function selectFlaw(row) {
  const full = await loadFullFlaw(row)
  selectedFlaw.value = { ...full }
  imageIndex.value = 0

  console.log('当前故障记录：', selectedFlaw.value)
  console.log('图片候选地址：', imageCandidates.value)
}

async function viewFlaw(row) {
  await selectFlaw(row)
  await nextTick()
  previewCard.value?.$el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function tryNextImage() {
  if (imageIndex.value < imageCandidates.value.length - 1) {
    imageIndex.value += 1
  }
}

async function openFlaw(row) {
  await selectFlaw(row)
  flawDialog.value = true
}
function isFalseAlarm(row) { return !!row && String(row.remark || '').includes('误报') }
function flawConfirmLabel(row) {
  if (!row) return '待确认'
  if (isFalseAlarm(row)) return '误报'
  return Number(row.confirmed) === 1 ? '已确认' : '待确认'
}
async function afterFlawSaved(saved) {
  if (saved?.id) {
    const index = flaws.value.findIndex(f => f.id === saved.id)
    if (index !== -1) flaws.value.splice(index, 1, saved)
    selectedFlaw.value = saved
  }
  await refreshFlaws()
  await loadPreupload()
}
function flawPercent(row) { return percent(row.flawDistance || 0, totalDistance.value) }
async function uploadTask() {
  await Promise.allSettled([refreshFlaws(), loadPreupload()])
  if (!localCanUpload.value) {
    ElMessage.warning('请先确认所有故障或标记误报，再上传数据')
    return
  }
  await ElMessageBox.confirm('确定上传当前任务、故障、传感器和AIoT联动记录吗？', '上传确认', { type: 'success' })
  uploading.value = true
  uploadProgress.value = 90
  try {
    const data = unwrap(await taskApi.upload(taskId), '上传失败')
    task.value = data.task
    uploadRecords.value = data.uploadRecords || []
    flaws.value = data.flaws || []
    uploadProgress.value = 100
    ElMessage.success('巡检数据上传完成，任务已归档')
  } catch (e) { ElMessage.error(e.message) }
  finally { uploading.value = false }
}
</script>

<style scoped>
.stats { margin-bottom: 16px; }
.detail-layout { display: grid; grid-template-columns: minmax(0, 1fr) 420px; gap: 16px; }
.main-panel, .side-panel { display: grid; gap: 16px; align-content: start; }
.review-track { margin-bottom: 24px; }
.preview-box { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(320px, .65fr); gap: 18px; }
.preview-img { min-height: 430px; border-radius: 18px; background: #0f172a; display: grid; place-items: center; overflow: hidden; }
.preview-img .el-image { width: 100%; height: 430px; }
.flaw-photo {
  width: 100%;
  height: 430px;
  object-fit: contain;
  display: block;
  background: #0f172a;
}
.empty-img { text-align: center; color: #cbd5e1; }
.preview-info h3 { margin: 4px 0 10px; font-size: 22px; }
.preview-info p { color: var(--agv-muted); line-height: 1.7; }
.info-list { display: grid; gap: 10px; margin: 18px 0; }
.info-list div { display: flex; justify-content: space-between; gap: 12px; padding: 10px 12px; background: #f8fafc; border: 1px solid var(--agv-border); border-radius: 12px; }
.info-list span { color: var(--agv-muted); }
.info-list strong { text-align: right; }
.compact div { align-items: center; }
.review-progress-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 14px; color: var(--agv-muted); font-size: 13px; }
.upload-progress { margin: 8px 0 16px; }
.record-line { padding: 11px 12px; border-radius: 14px; border: 1px solid var(--agv-border); background: #fff; margin-bottom: 10px; }
.record-line strong { margin-right: 8px; }
.record-line p { margin: 6px 0 0; color: var(--agv-muted); font-size: 13px; }
.section-subtitle { color: var(--agv-muted); font-size: 13px; }
.sensor-loop-card :deep(.el-tabs__content) { padding-top: 4px; }
.sensor-list-panel {
  max-height: 280px;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
}
.sensor-card,
.action-card {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--agv-border);
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  margin-bottom: 12px;
}
.sensor-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}
.sensor-head strong {
  display: block;
  color: #1f2937;
  font-size: 17px;
  font-weight: 800;
}
.sensor-head p {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 12px;
}
.sensor-main-value {
  margin-top: 10px;
  color: #334155;
  font-size: 18px;
  font-weight: 700;
}
.sensor-meta {
  display: grid;
  gap: 4px;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}
.sensor-distance {
  display: inline-flex;
  margin-top: 8px;
  padding: 4px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
}
@media (max-width: 1320px) { .detail-layout, .preview-box { grid-template-columns: 1fr; } }
</style>

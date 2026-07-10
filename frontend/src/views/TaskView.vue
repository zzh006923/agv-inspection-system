<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="app-title">
        <h1>任务列表与巡检管理</h1>
        <span>支持任务搜索、新增、修改、启动、复盘与数据上传闭环</span>
      </div>
      <div class="header-actions">
        <el-button @click="router.push('/init')" :icon="Monitor">系统自检</el-button>
        <el-button @click="router.push('/settings')" :icon="Setting">系统设置</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增任务</el-button>
      </div>
    </header>

    <div class="grid grid-4 stats">
      <StatCard label="全部任务" :value="stats.total" icon="Tickets" />
      <StatCard label="待巡视" :value="stats.waiting" icon="Clock" type="orange" />
      <StatCard label="巡视中" :value="stats.running" icon="Van" />
      <StatCard label="待上传 / 已完成" :value="`${stats.upload}/${stats.done}`" icon="UploadFilled" type="green" />
    </div>

    <el-card class="page-card search-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="任务编号"><el-input v-model="query.taskCode" clearable placeholder="TASK2026..." /></el-form-item>
        <el-form-item label="创建人"><el-input v-model="query.creator" clearable placeholder="创建人" /></el-form-item>
        <el-form-item label="执行人"><el-input v-model="query.executor" clearable placeholder="执行人" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.taskStatus" clearable placeholder="全部状态" style="width: 150px">
            <el-option label="待巡视" value="待巡视" />
            <el-option label="巡视中" value="巡视中" />
            <el-option label="待上传" value="待上传" />
            <el-option label="已完成" value="已完成" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadTasks">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="page-card" shadow="never">
      <el-table v-loading="loading" :data="tasks" stripe height="calc(100vh - 390px)">
        <el-table-column label="任务编号" min-width="170">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="goDetail(row)">{{ row.taskCode }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="taskName" label="任务名称" min-width="210" show-overflow-tooltip />
        <el-table-column prop="startPos" label="起始地点" min-width="140" show-overflow-tooltip />
        <el-table-column prop="taskTrip" label="距离" width="90" />
        <el-table-column prop="creator" label="创建人" width="110" />
        <el-table-column prop="executor" label="执行人" width="110" />
        <el-table-column label="状态" width="110"><template #default="{ row }"><StatusTag :value="row.taskStatus" /></template></el-table-column>
        <el-table-column label="创建时间" min-width="155"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.taskStatus === '待巡视'" type="primary" link @click="start(row)">启动</el-button>
            <el-button v-if="row.taskStatus === '巡视中'" type="primary" link @click="router.push(`/task/${row.id}/execute`)">继续巡视</el-button>
            <el-button v-if="row.taskStatus === '待巡视'" link @click="openEdit(row)">修改</el-button>
            <el-button v-if="row.taskStatus === '待巡视'" type="danger" link @click="remove(row)">删除</el-button>
            <el-button v-if="row.taskStatus === '待上传'" type="warning" link @click="router.push(`/task/${row.id}/detail`)">复盘上传</el-button>
            <el-button v-if="row.taskStatus === '已完成'" type="success" link @click="router.push(`/task/${row.id}/detail`)">查看记录</el-button>
            <el-button link @click="goDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next, jumper" :total="total" @change="loadTasks" />
      </div>
    </el-card>

    <TaskFormDialog v-model="dialogVisible" :mode="dialogMode" :task="selectedTask" @saved="afterSaved" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Monitor, Plus, Refresh, Search, Setting } from '@element-plus/icons-vue'
import { taskApi, unwrap } from '../api/agv'
import { formatTime } from '../utils/format'
import StatCard from '../components/StatCard.vue'
import StatusTag from '../components/StatusTag.vue'
import TaskFormDialog from '../components/TaskFormDialog.vue'

const router = useRouter()
const loading = ref(false)
const tasks = ref([])
const total = ref(0)
const query = reactive({ taskCode: '', creator: '', executor: '', taskStatus: '', pageNum: 1, pageSize: 10 })
const dialogVisible = ref(false)
const dialogMode = ref('add')
const selectedTask = ref(null)
const stats = computed(() => ({
  total: total.value,
  waiting: tasks.value.filter(t => t.taskStatus === '待巡视').length,
  running: tasks.value.filter(t => t.taskStatus === '巡视中').length,
  upload: tasks.value.filter(t => t.taskStatus === '待上传').length,
  done: tasks.value.filter(t => t.taskStatus === '已完成').length
}))

onMounted(loadTasks)

async function loadTasks() {
  loading.value = true
  try {
    const result = await taskApi.list({ ...query })
    tasks.value = result.rows || []
    total.value = result.total || 0
  } catch (e) { ElMessage.error(e.message) }
  finally { loading.value = false }
}
function resetQuery() {
  Object.assign(query, { taskCode: '', creator: '', executor: '', taskStatus: '', pageNum: 1 })
  loadTasks()
}
function openAdd() { dialogMode.value = 'add'; selectedTask.value = null; dialogVisible.value = true }
function openEdit(row) { dialogMode.value = 'edit'; selectedTask.value = { ...row }; dialogVisible.value = true }
async function afterSaved(saved, autoStart) {
  await loadTasks()
  if (autoStart) await start(saved)
}
async function start(row) {
  try {
    const task = unwrap(await taskApi.start(row.id), '启动任务失败')
    ElMessage.success('任务已启动，进入实时巡视页')
    router.push(`/task/${task.id}/execute`)
  } catch (e) { ElMessage.error(e.message) }
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除任务 ${row.taskCode} 吗？只有待巡视任务允许删除。`, '删除确认', { type: 'warning' })
  try {
    unwrap(await taskApi.remove(row.id), '删除失败')
    ElMessage.success('任务已删除')
    loadTasks()
  } catch (e) { ElMessage.error(e.message) }
}
function goDetail(row) {
  if (row.taskStatus === '巡视中') router.push(`/task/${row.id}/execute`)
  else router.push(`/task/${row.id}/detail`)
}
</script>

<style scoped>
.stats { margin-bottom: 16px; }
.search-card { margin-bottom: 16px; }
.pager { display: flex; justify-content: flex-end; padding-top: 16px; }
</style>

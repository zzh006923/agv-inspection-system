<template>
  <el-dialog v-model="visible" :title="mode === 'edit' ? '修改巡检任务' : '新增巡检任务'" width="820px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
      <div class="grid grid-2">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="如：1号线 K12+300-K12+800 区间巡检" />
        </el-form-item>
        <el-form-item label="任务编号" prop="taskCode">
          <el-input v-model="form.taskCode" placeholder="自动生成，可手动调整">
            <template #append>
              <el-button @click="loadNextCode">生成</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="起始地点" prop="startPos">
          <el-input v-model="form.startPos" placeholder="如：A端风井入口" />
        </el-form-item>
        <el-form-item label="巡检距离" prop="taskTrip">
          <el-input v-model="form.taskTrip" placeholder="如：500m" />
        </el-form-item>
        <el-form-item label="创建人" prop="creator">
          <el-input v-model="form.creator" placeholder="运维管理员" />
        </el-form-item>
        <el-form-item label="执行人" prop="executor">
          <el-input v-model="form.executor" placeholder="巡线车操作员" />
        </el-form-item>
      </div>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可填写路线说明、注意事项或传感器巡检重点" />
      </el-form-item>
      <el-form-item v-if="mode === 'add'" label="创建后">
        <el-checkbox v-model="autoStart">立即启动并进入实时巡视页</el-checkbox>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存任务</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { taskApi, unwrap } from '../api/agv'

const props = defineProps({ modelValue: Boolean, task: Object, mode: { type: String, default: 'add' } })
const emit = defineEmits(['update:modelValue', 'saved'])
const visible = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const formRef = ref(null)
const saving = ref(false)
const autoStart = ref(false)
const form = reactive({ id: null, taskCode: '', taskName: '', startPos: '', taskTrip: '', creator: '', executor: '', remark: '', round: 1 })
const rules = {
  taskName: [{ required: true, message: '请填写任务名称', trigger: 'blur' }],
  taskCode: [{ required: true, message: '请填写任务编号', trigger: 'blur' }],
  startPos: [{ required: true, message: '请填写起始地点', trigger: 'blur' }],
  taskTrip: [{ required: true, message: '请填写巡检距离', trigger: 'blur' }],
  creator: [{ required: true, message: '请填写创建人', trigger: 'blur' }],
  executor: [{ required: true, message: '请填写执行人', trigger: 'blur' }]
}

watch(() => props.modelValue, async opened => {
  if (!opened) return
  Object.assign(form, { id: null, taskCode: '', taskName: '', startPos: '', taskTrip: '', creator: '', executor: '', remark: '', round: 1 }, props.task || {})
  autoStart.value = false
  if (props.mode === 'add' && !form.taskCode) await loadNextCode()
})

async function loadNextCode() {
  try {
    const code = unwrap(await taskApi.nextCode(), '获取任务编号失败')
    form.taskCode = code
  } catch (e) { ElMessage.error(e.message) }
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    const saved = unwrap(props.mode === 'edit' ? await taskApi.update(payload) : await taskApi.add(payload), '保存任务失败')
    ElMessage.success('任务保存成功')
    emit('saved', saved, autoStart.value)
    visible.value = false
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saving.value = false
  }
}
</script>

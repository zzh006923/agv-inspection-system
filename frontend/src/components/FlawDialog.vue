<template>
  <el-dialog v-model="visible" title="故障/异常复核确认" width="980px" destroy-on-close>
    <div class="flaw-layout">
      <div class="flaw-image">
        <el-image v-if="imageUrl" :src="imageUrl" fit="contain" :preview-src-list="[imageUrl]" @error="tryNextImage" />
        <div v-else class="empty-img">
          <el-icon size="46"><Picture /></el-icon>
          <p>暂无故障图片；可通过裂缝识别接口或传感器事件补充图片。</p>
        </div>
      </div>
      <el-form :model="form" label-width="90px" class="flaw-form">
        <el-form-item label="名称"><el-input v-model="form.flawName" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="form.flawType" /></el-form-item>
        <el-form-item label="等级">
          <el-select v-model="form.level" style="width: 100%"><el-option label="高" value="高" /><el-option label="中" value="中" /><el-option label="低" value="低" /></el-select>
        </el-form-item>
        <el-form-item label="位置"><el-input v-model="form.flawDistance"><template #append>m</template></el-input></el-form-item>
        <el-form-item label="复核结果">
          <el-radio-group v-model="confirmMode">
            <el-radio-button label="confirmed">属实</el-radio-button>
            <el-radio-button label="false">误报</el-radio-button>
            <el-radio-button label="pending">待确认</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-alert v-if="confirmMode === 'pending'" class="mode-tip" type="warning" show-icon :closable="false" title="当前选择为待确认，保存后仍不会计入已复核，也不会通过上传前检查。" />
        <el-alert v-else-if="confirmMode === 'false'" class="mode-tip" type="info" show-icon :closable="false" title="当前选择为误报，保存后会计入已复核，但备注会标记为误报。" />
        <el-alert v-else class="mode-tip" type="success" show-icon :closable="false" title="当前选择为属实，保存后会计入已复核。" />
        <el-form-item label="描述"><el-input v-model="form.flawDesc" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" placeholder="填写复核意见、现场处理建议等" /></el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button :type="confirmMode === 'pending' ? 'warning' : 'primary'" :loading="saving" @click="save">{{ saveButtonText }}</el-button>
    </template>
  </el-dialog>
</template>
<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { flawApi, unwrap } from '../api/agv'
import { resolveImageCandidates } from '../utils/image'

const props = defineProps({ modelValue: Boolean, flaw: Object })
const emit = defineEmits(['update:modelValue', 'saved'])
const visible = computed({ get: () => props.modelValue, set: v => emit('update:modelValue', v) })
const form = reactive({})
const confirmMode = ref('pending')
const saving = ref(false)
const imageIndex = ref(0)
const imageCandidates = computed(() => resolveImageCandidates(
  form.flawImage ||
  form.flawImageUrl ||
  form.imageUrl ||
  form.image ||
  form.flaw_image ||
  form.flaw_image_url ||
  form.flawImg ||
  form.imgUrl ||
  ''
))
const imageUrl = computed(() => imageCandidates.value[imageIndex.value] || '')
const saveButtonText = computed(() => {
  if (confirmMode.value === 'pending') return '保存为待确认'
  if (confirmMode.value === 'false') return '标记为误报'
  return '确认属实'
})

watch(() => props.modelValue, opened => {
  if (!opened) return
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, props.flaw || {})
  confirmMode.value = isFalseAlarm(form) ? 'false' : Number(form.confirmed) === 1 ? 'confirmed' : 'pending'
  imageIndex.value = 0
})

function isFalseAlarm(row) { return String(row?.remark || '').includes('误报') }
function stripFalseAlarmRemark(text) {
  return String(text || '')
    .replace(/【误报】/g, '')
    .replace(/\[误报\]/g, '')
    .replace(/误报：?/g, '')
    .trim()
}

async function save() {
  saving.value = true
  try {
    const payload = { ...form }
    const cleanedRemark = stripFalseAlarmRemark(payload.remark)

    if (confirmMode.value === 'pending') {
      payload.confirmed = 0
      payload.remark = cleanedRemark || '仍需现场进一步复核'
    } else if (confirmMode.value === 'false') {
      payload.confirmed = 1
      payload.remark = `【误报】${cleanedRemark || '经人工复核判断为误报'}`
    } else {
      payload.confirmed = 1
      payload.remark = cleanedRemark || '人工复核确认属实'
    }

    const saved = unwrap(await flawApi.update(payload), '保存故障复核结果失败')
    if (confirmMode.value === 'pending') ElMessage.warning('已保存为待确认，不计入复核完成')
    else if (confirmMode.value === 'false') ElMessage.success('已标记为误报')
    else ElMessage.success('已确认属实')
    emit('saved', saved)
    visible.value = false
  } catch (e) { ElMessage.error(e.message) }
  finally { saving.value = false }
}
function tryNextImage() { if (imageIndex.value < imageCandidates.value.length - 1) imageIndex.value += 1 }
</script>
<style scoped>
.flaw-layout { display: grid; grid-template-columns: minmax(0, 1fr) 390px; gap: 18px; }
.flaw-image { min-height: 430px; border-radius: 18px; background: #0f172a; display: grid; place-items: center; overflow: hidden; }
.flaw-image .el-image { width: 100%; height: 430px; }
.empty-img { text-align: center; color: #cbd5e1; padding: 24px; }
.flaw-form { padding-top: 4px; }
.mode-tip { margin: -4px 0 14px 90px; width: calc(100% - 90px); }
@media (max-width: 900px) { .flaw-layout { grid-template-columns: 1fr; } .mode-tip { margin-left: 0; width: 100%; } }
</style>

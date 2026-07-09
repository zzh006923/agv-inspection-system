<template>
  <el-tag :type="tagType" effect="light" round>{{ value || '-' }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ value: { type: [String, Number], default: '' } })
const tagType = computed(() => {
  const v = String(props.value || '')
  if (['已完成', '正常', '已上传', '成功'].includes(v)) return 'success'
  if (['巡视中', '运行中', '已开启', '报警中'].includes(v)) return 'primary'
  if (['待巡视', '待上传', '疑似', '低', '中'].includes(v)) return 'warning'
  if (['异常', '高', '失败', '误报'].includes(v)) return 'danger'
  return 'info'
})
</script>

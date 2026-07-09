<template>
  <div class="stat-card">
    <div class="stat-icon" :class="type"><el-icon><component :is="icon" /></el-icon></div>
    <div>
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value">{{ value }}</div>
      <div class="stat-desc" v-if="desc">{{ desc }}</div>
    </div>
  </div>
</template>
<script setup>
defineProps({
  label: String,
  value: [String, Number],
  desc: String,
  icon: { type: String, default: 'Monitor' },
  type: { type: String, default: 'blue' }
})
</script>
<style scoped>
.stat-card {
  min-height: 92px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, #fff, #f8fbff);
  border: 1px solid var(--agv-border);
}
.stat-icon {
  width: 48px; height: 48px; border-radius: 15px;
  display: grid; place-items: center;
  font-size: 22px;
  background: rgba(37,99,235,.12); color: #2563eb;
}
.stat-icon.green { background: rgba(22,163,74,.12); color: #16a34a; }
.stat-icon.orange { background: rgba(245,158,11,.14); color: #d97706; }
.stat-icon.red { background: rgba(229,72,77,.12); color: #e5484d; }
.stat-label { color: var(--agv-muted); font-size: 13px; }
.stat-value { font-size: 24px; font-weight: 800; margin-top: 3px; }
.stat-desc { color: var(--agv-muted); font-size: 12px; margin-top: 2px; }
</style>
